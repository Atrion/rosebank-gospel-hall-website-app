package ca.rosebankgospelhall.app.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/** Turns WordPress HTML into a flat list of native blocks (text, images, audio, video, buttons). */
object HtmlParser {
    private val WS = Regex("\\s+")
    private val inlineTags = setOf("a", "b", "strong", "i", "em", "u", "span", "small", "sup", "sub", "mark", "cite", "code", "abbr", "font", "big")
    private val skipTags = setOf("script", "style", "noscript", "form", "svg", "input", "select", "textarea", "nav", "button")
    private val audioExt = Regex("""\.(mp3|m4a|wav|ogg|aac)(\?.*)?$""", RegexOption.IGNORE_CASE)
    private val ytRegex = Regex("""(?:youtube(?:-nocookie)?\.com/(?:embed/|watch\?(?:.*&)?v=|shorts/|live/)|youtu\.be/)([A-Za-z0-9_-]{11})""")

    fun youtubeId(url: String): String? = ytRegex.find(url)?.groupValues?.get(1)

    fun parse(html: String, baseUrl: String, linkColor: Color, dedupe: Boolean = false): List<Block> {
        val body = Jsoup.parseBodyFragment(html, baseUrl).body()
        val out = mutableListOf<Block>()
        flow(body, out, linkColor)
        if (!dedupe) return out
        // Some page builders repeat sections for mobile and desktop; show each once.
        val seen = HashSet<String>()
        return out.filter { b ->
            val key = when (b) {
                is Block.Para -> "p:" + b.text.text
                is Block.Heading -> "h:" + b.text.text
                is Block.Bullet -> "b:" + b.text.text
                is Block.Button -> "x:" + b.label + b.url
                is Block.Image -> "i:" + b.url
                else -> null
            }
            key == null || seen.add(key)
        }
    }

    private fun textOf(b: Block): String = when (b) {
        is Block.Para -> b.text.text
        is Block.Heading -> b.text.text
        is Block.Quote -> b.text.text
        is Block.Bullet -> b.text.text
        is Block.Caption -> b.text.text
        is Block.Button -> b.label
        else -> ""
    }

    private val footerStrong = Regex("©|all rights reserved|powered by|gravel hill|subscribe", RegexOption.IGNORE_CASE)
    private val footerWeak = Regex("western rd|39049|bwthib|\\b902\\b", RegexOption.IGNORE_CASE)

    /** Drops site footer text (contact lines, copyright, sign-up) that a copied page may carry at the end. */
    fun trimFooter(blocks: List<Block>): List<Block> {
        var cut = blocks.size
        for ((i, b) in blocks.withIndex()) {
            if (i < blocks.size / 2) continue // a footer only ever appears in the second half
            val t = textOf(b)
            if (footerStrong.containsMatchIn(t) || (i >= blocks.size - 4 && footerWeak.containsMatchIn(t))) {
                cut = i
                break
            }
        }
        while (cut > 0 && (blocks[cut - 1] is Block.Heading || blocks[cut - 1] is Block.Divider ||
                blocks[cut - 1] is Block.Image || blocks[cut - 1] is Block.Caption)
        ) cut--
        return if (cut <= 0) blocks else blocks.subList(0, cut)
    }

    private fun trim(b: AnnotatedString.Builder): AnnotatedString = trim(b.toAnnotatedString())

    private fun trim(t: AnnotatedString): AnnotatedString {
        val s = t.text.indexOfFirst { !it.isWhitespace() }
        if (s < 0) return AnnotatedString("")
        val e = t.text.indexOfLast { !it.isWhitespace() } + 1
        return t.subSequence(s, e)
    }

    private fun videoBlock(id: String) = Block.Video(
        url = "https://www.youtube.com/watch?v=$id",
        thumb = "https://img.youtube.com/vi/$id/hqdefault.jpg",
        label = "Watch on YouTube",
    )

    private fun absolute(src: String): String = when {
        src.startsWith("//") -> "https:$src"
        src.startsWith("/") -> Site.BASE + src
        else -> src
    }

    private fun flow(el: Element, out: MutableList<Block>, lc: Color) {
        var sb = AnnotatedString.Builder()

        fun flush() {
            val t = trim(sb)
            sb = AnnotatedString.Builder()
            if (t.text.isEmpty()) return
            if (t.text.length < 120 && t.text.startsWith("http")) {
                youtubeId(t.text)?.let { out += videoBlock(it); return }
            }
            out += Block.Para(t)
        }

        for (n in el.childNodes()) {
            when (n) {
                is TextNode -> sb.append(n.text().replace(WS, " "))
                is Element -> {
                    val tag = n.normalName()
                    when {
                        tag in skipTags -> {}
                        tag == "br" -> sb.append("\n")
                        tag == "img" -> { flush(); image(n)?.let { out += it } }
                        tag == "audio" -> { flush(); audio(n)?.let { out += it } }
                        tag == "video" -> {
                            flush()
                            val src = n.attr("src").ifBlank { n.selectFirst("source")?.attr("src").orEmpty() }
                            if (src.isNotBlank()) out += Block.Video(absolute(src), n.attr("poster").ifBlank { null }, "Play video")
                        }
                        tag == "iframe" -> { flush(); iframe(n)?.let { out += it } }
                        tag == "a" -> {
                            val href = n.absUrl("href").ifBlank { n.attr("href") }
                            when {
                                n.selectFirst("img,audio,iframe,video") != null -> { flush(); flow(n, out, lc) }
                                youtubeId(href) != null && n.text().length < 80 -> { flush(); out += videoBlock(youtubeId(href)!!) }
                                audioExt.containsMatchIn(href) -> { flush(); out += Block.Audio(href, n.text().ifBlank { null }) }
                                n.className().contains("button") -> { flush(); out += Block.Button(n.text().ifBlank { "Open" }, href) }
                                else -> appendInline(n, sb, lc)
                            }
                        }
                        tag in inlineTags -> appendInline(n, sb, lc)
                        tag.length == 2 && tag[0] == 'h' && tag[1] in '1'..'6' -> {
                            flush()
                            val b = AnnotatedString.Builder(); appendChildren(n, b, lc)
                            val t = trim(b)
                            if (t.text.isNotEmpty()) out += Block.Heading(tag[1] - '0', t)
                        }
                        tag == "ul" || tag == "ol" -> { flush(); list(n, out, lc, tag == "ol") }
                        tag == "blockquote" -> {
                            flush()
                            val b = AnnotatedString.Builder(); appendChildren(n, b, lc)
                            val t = trim(b)
                            if (t.text.isNotEmpty()) out += Block.Quote(t)
                        }
                        tag == "figcaption" -> {
                            flush()
                            val b = AnnotatedString.Builder(); appendChildren(n, b, lc)
                            val t = trim(b)
                            if (t.text.isNotEmpty()) out += Block.Caption(t)
                        }
                        tag == "hr" -> { flush(); out += Block.Divider }
                        else -> { flush(); flow(n, out, lc) }
                    }
                }
            }
        }
        flush()
    }

    private fun list(el: Element, out: MutableList<Block>, lc: Color, ordered: Boolean) {
        var i = 1
        for (li in el.children()) {
            if (li.normalName() != "li") continue
            val b = AnnotatedString.Builder(); appendChildren(li, b, lc)
            val t = trim(b)
            if (t.text.isNotEmpty()) {
                out += Block.Bullet(if (ordered) "${i++}." else "•", t)
            }
        }
    }

    private fun image(n: Element): Block.Image? {
        if (n.hasClass("emoji")) return null
        val w = n.attr("width").toIntOrNull()
        if (w != null && w <= 2) return null
        val src = n.attr("data-lazy-src").ifBlank { n.attr("data-src") }.ifBlank { n.attr("src") }
        if (src.isBlank() || src.startsWith("data:")) return null
        return Block.Image(absolute(src), n.attr("alt").ifBlank { null })
    }

    private fun audio(n: Element): Block.Audio? {
        val src = n.attr("src").ifBlank { n.selectFirst("source")?.attr("src").orEmpty() }
        if (src.isBlank()) return null
        return Block.Audio(absolute(src), n.attr("title").ifBlank { null })
    }

    private fun iframe(n: Element): Block? {
        val src = absolute(
            n.attr("src").ifBlank { n.attr("data-src") }.ifBlank { n.attr("data-lazy-src") }.ifBlank { n.attr("data-litespeed-src") },
        )
        if (src.isBlank()) return null
        MediaScan.scan(src).firstOrNull()?.let { return it }
        return Block.Button("Open embedded content", src)
    }

    private fun appendChildren(el: Element, b: AnnotatedString.Builder, lc: Color) {
        for (n in el.childNodes()) {
            when (n) {
                is TextNode -> b.append(n.text().replace(WS, " "))
                is Element -> appendInline(n, b, lc)
            }
        }
    }

    private fun appendInline(el: Element, b: AnnotatedString.Builder, lc: Color) {
        when (el.normalName()) {
            "br" -> b.append("\n")
            "script", "style", "img", "iframe", "audio", "video" -> {}
            "b", "strong" -> b.withStyleCompat(SpanStyle(fontWeight = FontWeight.Bold)) { appendChildren(el, it, lc) }
            "i", "em", "cite" -> b.withStyleCompat(SpanStyle(fontStyle = FontStyle.Italic)) { appendChildren(el, it, lc) }
            "u" -> b.withStyleCompat(SpanStyle(textDecoration = TextDecoration.Underline)) { appendChildren(el, it, lc) }
            "a" -> {
                val href = el.absUrl("href").ifBlank { el.attr("href") }
                if (href.isBlank()) {
                    appendChildren(el, b, lc)
                } else {
                    b.pushLink(LinkAnnotation.Url(href, TextLinkStyles(SpanStyle(color = lc, textDecoration = TextDecoration.Underline))))
                    appendChildren(el, b, lc)
                    b.pop()
                }
            }
            "p", "div" -> { appendChildren(el, b, lc); b.append("\n") }
            else -> appendChildren(el, b, lc)
        }
    }

    private inline fun AnnotatedString.Builder.withStyleCompat(style: SpanStyle, block: (AnnotatedString.Builder) -> Unit) {
        val idx = pushStyle(style)
        block(this)
        pop(idx)
    }
}
