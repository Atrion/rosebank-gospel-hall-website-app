package ca.rosebankgospelhall.app.data

import androidx.compose.ui.text.AnnotatedString
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Site {
    const val BASE = "https://rosebankgospelhall.ca"
    const val ADDRESS = "39049 Western Rd, Rosebank, PEI"
    const val PHONE1 = "+1 902 206 1453"
    const val PHONE2 = "+1 902 859 2485"
    const val EMAIL = "bwthib@gmail.com"
    const val DEVELOPER = "From Gravel Hill Enterprises"
    const val DEVELOPER_URL = "https://fromgravelhill.ca/"
    const val YOUTUBE = "https://www.youtube.com/@brodyt983/"
    const val LOGO = "https://i0.wp.com/rosebankgospelhall.ca/wp-content/uploads/2026/04/Rosebank_logo_trans-1.png?fit=270%2C270&ssl=1"
}

data class Term(val id: Int, val name: String, val slug: String, val taxonomy: String)

data class Post(
    val id: Int,
    val title: String,
    val excerpt: String,
    val contentHtml: String,
    val link: String,
    val dateMillis: Long,
    val imageUrl: String?,
    val author: String?,
    val terms: List<Term>,
) {
    fun has(slug: String) = terms.any { it.slug.equals(slug, ignoreCase = true) }
    val isAudio get() = has("audio")
    val isVideo get() = has("videos") || has("video")
    fun matches(slug: String) = when {
        slug.isBlank() -> true
        slug == "videos" -> isVideo
        else -> has(slug)
    }
    val label: String?
        get() = terms.firstOrNull { it.taxonomy == "category" && !it.slug.equals("uncategorized", true) }?.name
            ?: terms.firstOrNull { !it.slug.equals("uncategorized", true) }?.name
    val dateText: String
        get() = DATE_FMT.format(Instant.ofEpochMilli(dateMillis).atZone(ZoneId.of("America/Halifax")))
}

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

data class PageData(val title: String, val html: String, val scraped: Boolean = false)

sealed interface Block {
    data class Para(val text: AnnotatedString) : Block
    data class Heading(val level: Int, val text: AnnotatedString) : Block
    data class Quote(val text: AnnotatedString) : Block
    data class Bullet(val marker: String, val text: AnnotatedString) : Block
    data class Caption(val text: AnnotatedString) : Block
    data class Image(val url: String, val alt: String?) : Block
    data class Audio(val url: String, val title: String?) : Block
    data class Video(val url: String, val thumb: String?, val label: String) : Block
    data class Button(val label: String, val url: String) : Block
    data object Divider : Block
}

fun Post.toJson(): JSONObject = JSONObject()
    .put("id", id).put("title", title).put("excerpt", excerpt).put("content", contentHtml)
    .put("link", link).put("date", dateMillis).put("image", imageUrl ?: "").put("author", author ?: "")
    .put("terms", JSONArray(terms.map {
        JSONObject().put("id", it.id).put("name", it.name).put("slug", it.slug).put("tax", it.taxonomy)
    }))

fun postFromJson(o: JSONObject): Post {
    val ta = o.optJSONArray("terms") ?: JSONArray()
    val terms = (0 until ta.length()).map {
        val t = ta.getJSONObject(it)
        Term(t.optInt("id"), t.optString("name"), t.optString("slug"), t.optString("tax"))
    }
    return Post(
        id = o.getInt("id"), title = o.optString("title"), excerpt = o.optString("excerpt"),
        contentHtml = o.optString("content"), link = o.optString("link"), dateMillis = o.optLong("date"),
        imageUrl = o.optString("image").ifBlank { null }, author = o.optString("author").ifBlank { null },
        terms = terms,
    )
}
