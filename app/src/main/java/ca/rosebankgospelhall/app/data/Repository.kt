package ca.rosebankgospelhall.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class HttpException(val code: Int) : IOException("HTTP $code")

class Repository(private val ctx: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
    private val cacheFile get() = File(ctx.filesDir, "posts_cache.json")

    private fun get(url: String): String {
        val req = Request.Builder().url(url)
            .header("User-Agent", "RosebankGospelHallApp/1.0 (Android)")
            .build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw HttpException(r.code)
            return r.body?.string() ?: throw IOException("Empty response")
        }
    }

    /** Fetches every post. Uses the WordPress REST API, falling back to the RSS feed. */
    suspend fun fetchPosts(): List<Post> = withContext(Dispatchers.IO) {
        val posts = try { viaRest() } catch (e: Exception) { viaRss() }
        if (posts.isEmpty()) throw IOException("No posts found")
        val sorted = posts.sortedByDescending { it.dateMillis }
        runCatching { cacheFile.writeText(JSONArray(sorted.map { it.toJson() }).toString()) }
        sorted
    }

    fun cached(): List<Post> = runCatching {
        val arr = JSONArray(cacheFile.readText())
        (0 until arr.length()).map { postFromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    suspend fun fetchPage(slug: String): PageData = withContext(Dispatchers.IO) {
        try {
            val arr = JSONArray(get("${Site.BASE}/wp-json/wp/v2/pages?slug=$slug&_fields=title,content,link"))
            if (arr.length() == 0) throw IOException("Page not found")
            val o = arr.getJSONObject(0)
            PageData(
                Jsoup.parse(o.getJSONObject("title").getString("rendered")).text(),
                o.getJSONObject("content").getString("rendered"),
            )
        } catch (e: Exception) {
            val url = "${Site.BASE}/$slug/"
            val doc = Jsoup.parse(get(url), url)
            doc.select("header,footer,nav,script,style,form").remove()
            val main = doc.selectFirst("main") ?: doc.body()
            PageData(doc.title().substringBefore(" - "), main.html())
        }
    }

    // ---- WordPress REST ----

    private fun viaRest(): List<Post> {
        val out = mutableListOf<Post>()
        for (page in 1..6) {
            val body = try {
                get("${Site.BASE}/wp-json/wp/v2/posts?per_page=50&page=$page&_embed=wp:term,wp:featuredmedia,author")
            } catch (e: HttpException) {
                if (page > 1 && e.code == 400) break else throw e
            }
            val arr = JSONArray(body)
            for (i in 0 until arr.length()) out += parseWp(arr.getJSONObject(i))
            if (arr.length() < 50) break
        }
        return out
    }

    private fun parseWp(o: JSONObject): Post {
        val emb = o.optJSONObject("_embedded")
        val terms = mutableListOf<Term>()
        val groups = emb?.optJSONArray("wp:term")
        if (groups != null) {
            for (i in 0 until groups.length()) {
                val g = groups.optJSONArray(i) ?: continue
                for (j in 0 until g.length()) {
                    val t = g.optJSONObject(j) ?: continue
                    terms += Term(
                        t.optInt("id"),
                        Jsoup.parse(t.optString("name")).text(),
                        t.optString("slug"),
                        t.optString("taxonomy"),
                    )
                }
            }
        }
        val content = o.optJSONObject("content")?.optString("rendered").orEmpty()
        val featured = emb?.optJSONArray("wp:featuredmedia")?.optJSONObject(0)?.optString("source_url").orEmpty()
        val image = featured.ifBlank { Jsoup.parse(content).selectFirst("img")?.attr("src").orEmpty() }.ifBlank { null }
        val author = emb?.optJSONArray("author")?.optJSONObject(0)?.optString("name")?.ifBlank { null }
        val excerpt = Jsoup.parse(o.optJSONObject("excerpt")?.optString("rendered").orEmpty()).text()
            .replace(Regex("\\s*\\[…\\]\\s*$"), "…")
        val gmt = o.optString("date_gmt")
        val millis = runCatching { LocalDateTime.parse(gmt).toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrDefault(0L)
        return Post(
            id = o.getInt("id"),
            title = Jsoup.parse(o.getJSONObject("title").getString("rendered")).text(),
            excerpt = excerpt,
            contentHtml = content,
            link = o.optString("link"),
            dateMillis = millis,
            imageUrl = image,
            author = author,
            terms = terms,
        )
    }

    // ---- RSS fallback ----

    private fun viaRss(): List<Post> {
        val out = mutableListOf<Post>()
        for (page in 1..3) {
            val xml = try {
                get("${Site.BASE}/feed/?paged=$page")
            } catch (e: HttpException) {
                if (page > 1) break else throw e
            }
            val items = Jsoup.parse(xml, "", Parser.xmlParser()).select("item")
            if (items.isEmpty()) break
            for (item in items) {
                val link = item.selectFirst("link")?.text().orEmpty()
                val summary = item.selectFirst("description")?.text().orEmpty()
                val content = item.selectFirst("content|encoded")?.text() ?: summary
                val millis = runCatching {
                    ZonedDateTime.parse(item.selectFirst("pubDate")?.text().orEmpty(), DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toInstant().toEpochMilli()
                }.getOrDefault(0L)
                out += Post(
                    id = link.hashCode() and 0x7fffffff,
                    title = Jsoup.parse(item.selectFirst("title")?.text().orEmpty()).text(),
                    excerpt = Jsoup.parse(summary).text().replace(Regex("\\s*\\[…\\]\\s*$"), "…"),
                    contentHtml = content,
                    link = link,
                    dateMillis = millis,
                    imageUrl = Jsoup.parse(content).selectFirst("img")?.attr("src")?.ifBlank { null },
                    author = item.selectFirst("dc|creator")?.text()?.ifBlank { null },
                    terms = item.select("category").map {
                        Term(0, it.text(), it.text().lowercase().replace(' ', '-'), "category")
                    },
                )
            }
            if (items.size < 10) break
        }
        return out
    }
}
