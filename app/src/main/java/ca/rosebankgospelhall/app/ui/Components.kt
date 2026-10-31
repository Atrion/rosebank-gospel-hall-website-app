package ca.rosebankgospelhall.app.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ca.rosebankgospelhall.app.AudioController
import ca.rosebankgospelhall.app.data.Block
import ca.rosebankgospelhall.app.data.Post
import coil.compose.AsyncImage

val LocalReadScale = compositionLocalOf { 1f }

fun Context.safeStart(intent: Intent) {
    try { startActivity(intent) } catch (_: Exception) {}
}

fun formatTime(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

@Composable
fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
}

@Composable
fun SectionHeader(title: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            message, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun PostImage(post: Post, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    if (post.imageUrl != null) {
        AsyncImage(
            model = post.imageUrl, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = modifier.background(cs.surfaceVariant),
        )
    } else {
        Box(
            modifier.background(Brush.linearGradient(listOf(Burgundy, BurgundyDeep))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = Gold, modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun MediaBadge(post: Post, modifier: Modifier) {
    val icon = when {
        post.isVideo -> Icons.Filled.PlayArrow
        post.isAudio -> Icons.Filled.Headphones
        else -> null
    } ?: return
    Surface(modifier, shape = CircleShape, color = Color.Black.copy(alpha = 0.55f)) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.padding(8.dp).size(20.dp))
    }
}

@Composable
fun PostCard(post: Post, saved: Boolean, onClick: () -> Unit, onSave: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow),
    ) {
        Box {
            PostImage(post, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            MediaBadge(post, Modifier.align(Alignment.TopStart).padding(12.dp))
        }
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 4.dp)) {
            post.label?.let {
                Text(it, style = MaterialTheme.typography.labelLarge, color = cs.primary)
                Spacer(Modifier.height(4.dp))
            }
            Text(
                post.title, style = MaterialTheme.typography.titleLarge,
                maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(end = 12.dp),
            )
            if (post.excerpt.isNotBlank()) {
                Text(
                    post.excerpt, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp, end = 12.dp),
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    post.dateText, style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant, modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onSave) {
                    Icon(
                        if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = if (saved) "Remove from saved" else "Save",
                        tint = if (saved) cs.primary else cs.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun CompactPostRow(post: Post, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(84.dp).clip(RoundedCornerShape(14.dp))) {
            PostImage(post, Modifier.size(84.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            post.label?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = cs.primary) }
            Text(
                post.title, style = MaterialTheme.typography.titleMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Text(post.dateText, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
fun AudioPlayerCard(audio: AudioController, url: String, title: String, art: String?, postId: Int?) {
    val cs = MaterialTheme.colorScheme
    val active = audio.currentUrl == url
    var drag by remember { mutableStateOf<Float?>(null) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = cs.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(onClick = { audio.play(url, title, art, postId) }, modifier = Modifier.size(56.dp)) {
                    Icon(
                        if (active && audio.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (active && audio.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Listen to this message", style = MaterialTheme.typography.titleSmall)
                    Text(
                        title, style = MaterialTheme.typography.bodySmall, color = cs.onSecondaryContainer,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (active) {
                val progress = if (audio.durationMs > 0) (audio.positionMs.toFloat() / audio.durationMs).coerceIn(0f, 1f) else 0f
                Slider(
                    value = drag ?: progress,
                    onValueChange = { drag = it },
                    onValueChangeFinished = {
                        drag?.let { audio.seekTo((it * audio.durationMs).toLong()) }
                        drag = null
                    },
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatTime(audio.positionMs), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { audio.skip(-10_000) }) { Icon(Icons.Filled.Replay10, "Back 10 seconds") }
                    TextButton(onClick = { audio.cycleSpeed() }) {
                        Text("${audio.speed.toString().removeSuffix(".0")}x", fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = { audio.skip(30_000) }) { Icon(Icons.Filled.Forward30, "Forward 30 seconds") }
                    Spacer(Modifier.weight(1f))
                    Text(formatTime(audio.durationMs), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun MiniPlayer(audio: AudioController, onOpen: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    ) {
        Column {
            val progress = if (audio.durationMs > 0) (audio.positionMs.toFloat() / audio.durationMs).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(2.dp))
            Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Headphones, null)
                Spacer(Modifier.width(12.dp))
                Text(
                    audio.title, style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { audio.toggle() }) {
                    Icon(
                        if (audio.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (audio.isPlaying) "Pause" else "Play",
                    )
                }
                IconButton(onClick = { audio.stop() }) { Icon(Icons.Filled.Close, "Close player") }
            }
        }
    }
}

/** Adds every block of an article or page to a LazyColumn. */
fun LazyListScope.contentBlocks(
    blocks: List<Block>,
    audio: AudioController,
    postTitle: String,
    art: String?,
    postId: Int?,
) {
    items(blocks) { BlockView(it, audio, postTitle, art, postId) }
}

@Composable
private fun BlockView(b: Block, audio: AudioController, postTitle: String, art: String?, postId: Int?) {
    val cs = MaterialTheme.colorScheme
    val scale = LocalReadScale.current
    val body = MaterialTheme.typography.bodyLarge
    val bodyStyle = body.copy(fontSize = body.fontSize * scale, lineHeight = body.fontSize * scale * 1.55f)
    val uri = LocalUriHandler.current
    when (b) {
        is Block.Para -> Text(b.text, style = bodyStyle, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        is Block.Heading -> Text(
            b.text,
            style = when {
                b.level <= 2 -> MaterialTheme.typography.headlineSmall
                b.level == 3 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            },
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
        )
        is Block.Quote -> Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(cs.primary, RoundedCornerShape(2.dp)))
            Text(
                b.text, style = bodyStyle.copy(fontStyle = FontStyle.Italic),
                modifier = Modifier.padding(start = 14.dp),
            )
        }
        is Block.Bullet -> Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 2.dp)) {
            Text(b.marker, style = bodyStyle, modifier = Modifier.width(28.dp))
            Text(b.text, style = bodyStyle, modifier = Modifier.weight(1f))
        }
        is Block.Caption -> Text(
            b.text, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        )
        is Block.Image -> AsyncImage(
            model = b.url, contentDescription = b.alt, contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(MaterialTheme.shapes.medium).background(cs.surfaceVariant),
        )
        is Block.Audio -> AudioPlayerCard(audio, b.url, b.title ?: postTitle, art, postId)
        is Block.Video -> Card(
            onClick = { uri.openUri(b.url) },
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
                    Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.padding(14.dp).size(36.dp))
                }
            }
            Text(
                b.label, style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        is Block.Button -> Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            FilledTonalButton(onClick = { uri.openUri(b.url) }) { Text(b.label) }
        }
        Block.Divider -> HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
    }
}
