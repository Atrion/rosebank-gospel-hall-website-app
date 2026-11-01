package ca.rosebankgospelhall.app.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import ca.rosebankgospelhall.app.AudioController
import ca.rosebankgospelhall.app.VideoActivity
import ca.rosebankgospelhall.app.data.Block
import ca.rosebankgospelhall.app.data.HtmlParser
import coil.compose.AsyncImage

private val vimeoId = Regex("""vimeo\.com/(?:video/)?(\d+)""")
private val playableFile = Regex("""\.(mp4|webm)(\?.*)?$""", RegexOption.IGNORE_CASE)

private fun canPlayInApp(url: String) =
    HtmlParser.youtubeId(url) != null || vimeoId.containsMatchIn(url) ||
        url.contains("rumble.com/embed/") || playableFile.containsMatchIn(url)

/** A video thumbnail. Tapping it opens the in-app full-screen player. */
@Composable
fun VideoBlock(b: Block.Video, audio: AudioController) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    Card(
        onClick = {
            if (canPlayInApp(b.url)) {
                if (audio.isPlaying) audio.toggle()
                ctx.safeStart(Intent(ctx, VideoActivity::class.java).putExtra(VideoActivity.EXTRA_URL, b.url))
            } else {
                uri.openUri(b.url)
            }
        },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh),
    ) {
        Box(contentAlignment = Alignment.Center) {
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
