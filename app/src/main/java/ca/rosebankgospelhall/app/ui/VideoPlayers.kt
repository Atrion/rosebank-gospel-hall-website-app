package ca.rosebankgospelhall.app.ui

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import ca.rosebankgospelhall.app.AudioController
import ca.rosebankgospelhall.app.data.Block
import ca.rosebankgospelhall.app.data.HtmlParser
import ca.rosebankgospelhall.app.data.Site
import coil.compose.AsyncImage

private sealed interface Playable {
    data class Web(val url: String) : Playable
    data class File(val url: String) : Playable
    data object None : Playable
}

private val vimeoId = Regex("""vimeo\.com/(?:video/)?(\d+)""")
private val playableFile = Regex("""\.(mp4|webm)(\?.*)?$""", RegexOption.IGNORE_CASE)

private fun playableFor(url: String): Playable {
    HtmlParser.youtubeId(url)?.let {
        return Playable.Web("https://www.youtube.com/embed/$it?autoplay=1&playsinline=1&rel=0&origin=${Site.BASE}")
    }
    vimeoId.find(url)?.let { return Playable.Web("https://player.vimeo.com/video/${it.groupValues[1]}?autoplay=1") }
    if (url.contains("rumble.com/embed/")) return Playable.Web(url)
    if (playableFile.containsMatchIn(url)) return Playable.File(url)
    return Playable.None
}

/** A video card: shows a thumbnail, then plays right inside the page when tapped. */
@Composable
fun VideoBlock(b: Block.Video, audio: AudioController) {
    val cs = MaterialTheme.colorScheme
    val uri = LocalUriHandler.current
    val playable = remember(b.url) { playableFor(b.url) }
    var playing by remember(b.url) { mutableStateOf(false) }
    val where = if (HtmlParser.youtubeId(b.url) != null) "YouTube" else "the browser"

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh),
    ) {
        if (playing) {
            when (val p = playable) {
                is Playable.Web -> WebEmbed(p.url, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                is Playable.File -> FilePlayer(p.url, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                Playable.None -> {}
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Not playing?", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
                TextButton(onClick = { uri.openUri(b.url) }) { Text("Open in $where") }
            }
        } else {
            Box(
                Modifier.clickable {
                    if (playable == Playable.None) {
                        uri.openUri(b.url)
                    } else {
                        if (audio.isPlaying) audio.toggle()
                        playing = true
                    }
                },
                contentAlignment = Alignment.Center,
            ) {
                if (b.thumb != null) {
                    AsyncImage(
                        model = b.thumb, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    )
                } else {
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(BurgundyDeep))
                }
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.6f)) {
                    Icon(Icons.Filled.PlayArrow, "Play video", tint = Color.White, modifier = Modifier.padding(14.dp).size(36.dp))
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebEmbed(embedUrl: String, modifier: Modifier) {
    val ctx = LocalContext.current
    var custom by remember { mutableStateOf<View?>(null) }
    var callback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    val web = remember {
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            setBackgroundColor(android.graphics.Color.BLACK)
            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View, cb: CustomViewCallback) {
                    custom = view
                    callback = cb
                }

                override fun onHideCustomView() {
                    custom = null
                    callback = null
                }
            }
            val html = "<html><body style=\"margin:0;background:#000\">" +
                "<iframe src=\"$embedUrl\" style=\"position:absolute;top:0;left:0;width:100%;height:100%;border:0\" " +
                "allow=\"autoplay; encrypted-media; picture-in-picture; fullscreen\" allowfullscreen " +
                "referrerpolicy=\"strict-origin-when-cross-origin\"></iframe></body></html>"
            loadDataWithBaseURL(Site.BASE + "/", html, "text/html", "utf-8", null)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            web.stopLoading()
            web.destroy()
        }
    }
    AndroidView(factory = { web }, modifier = modifier)
    custom?.let { v ->
        Dialog(
            onDismissRequest = { callback?.onCustomViewHidden(); custom = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            AndroidView(
                factory = {
                    (v.parent as? ViewGroup)?.removeView(v)
                    v
                },
                modifier = Modifier.fillMaxSize().background(Color.Black),
            )
        }
    }
}

@Composable
private fun FilePlayer(url: String, modifier: Modifier) {
    val ctx = LocalContext.current
    val exo = remember {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) { onDispose { exo.release() } }
    AndroidView(factory = { PlayerView(it).apply { player = exo } }, modifier = modifier)
}
