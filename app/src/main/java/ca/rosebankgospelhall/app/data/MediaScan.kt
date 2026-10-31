package ca.rosebankgospelhall.app.data

/** Finds video links anywhere in raw HTML (iframes, links, data attributes, JSON, plain URLs). */
object MediaScan {
    private val yt = Regex("""(?:youtube(?:-nocookie)?\.com/(?:embed/|v/|shorts/|live/|watch\?(?:[^"'\s<>]*?[&;?])?v=)|youtu\.be/)([A-Za-z0-9_-]{11})""")
    private val ytData = Regex("""(?:data-(?:video-id|youtube-id|yt-id|videoid)|videoId)["'\s:=]+["']?([A-Za-z0-9_-]{11})""", RegexOption.IGNORE_CASE)
    private val vimeo = Regex("""(?:player\.vimeo\.com/video/|vimeo\.com/)(\d{6,})""")
    private val rumble = Regex("""https?://rumble\.com/(?:embed/[A-Za-z0-9]+|v[A-Za-z0-9-]+\.html)""")
    private val file = Regex("""https?://[^\s"'<>]+?\.(?:mp4|webm|mov|m3u8)(?:\?[^\s"'<>]*)?""", RegexOption.IGNORE_CASE)

    fun key(url: String): String = HtmlParser.youtubeId(url) ?: url

    fun thumbFor(html: String): String? =
        ids(html).firstOrNull()?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }

    private fun clean(raw: String) = raw.replace("\\/", "/").replace("&amp;", "&").replace("&#038;", "&")

    private fun ids(raw: String): List<String> {
        val s = clean(raw)
        return (yt.findAll(s).map { it.groupValues[1] } + ytData.findAll(s).map { it.groupValues[1] })
            .distinct().toList()
    }

    fun scan(raw: String): List<Block.Video> {
        val s = clean(raw)
        val out = mutableListOf<Block.Video>()
        for (id in ids(raw)) {
            out += Block.Video(
                "https://www.youtube.com/watch?v=$id",
                "https://img.youtube.com/vi/$id/hqdefault.jpg",
                "Watch on YouTube",
            )
        }
        vimeo.findAll(s).map { it.groupValues[1] }.distinct().forEach {
            out += Block.Video("https://vimeo.com/$it", null, "Watch on Vimeo")
        }
        rumble.findAll(s).map { it.value }.distinct().forEach {
            out += Block.Video(it, null, "Watch on Rumble")
        }
        file.findAll(s).map { it.value }.distinct().forEach {
            out += Block.Video(it, null, "Play video")
        }
        return out.distinctBy { key(it.url) }
    }
}
