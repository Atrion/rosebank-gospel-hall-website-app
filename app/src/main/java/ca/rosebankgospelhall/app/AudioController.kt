package ca.rosebankgospelhall.app

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

/** Compose-friendly wrapper around a MediaController connected to [PlaybackService]. */
class AudioController(ctx: Context) {
    private val appCtx = ctx.applicationContext
    private var controller: MediaController? = null
    private val future = MediaController.Builder(
        appCtx, SessionToken(appCtx, ComponentName(appCtx, PlaybackService::class.java)),
    ).buildAsync()

    var currentUrl by mutableStateOf<String?>(null); private set
    var title by mutableStateOf(""); private set
    var postId by mutableStateOf<Int?>(null); private set
    var isPlaying by mutableStateOf(false); private set
    var positionMs by mutableLongStateOf(0L); private set
    var durationMs by mutableLongStateOf(0L); private set
    var speed by mutableFloatStateOf(1f); private set

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
            tick()
        }

        override fun onPlaybackStateChanged(state: Int) {
            tick()
            if (state == Player.STATE_ENDED) isPlaying = false
        }
    }

    init {
        future.addListener({
            try {
                val c = future.get()
                controller = c
                c.addListener(listener)
                c.currentMediaItem?.let {
                    currentUrl = it.mediaId
                    title = it.mediaMetadata.title?.toString().orEmpty()
                }
                isPlaying = c.isPlaying
                tick()
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(appCtx))
    }

    fun play(url: String, title: String, art: String?, postId: Int?) {
        val c = controller ?: return
        if (currentUrl == url) { toggle(); return }
        val meta = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist("Rosebank Gospel Hall")
            .apply { art?.let { setArtworkUri(Uri.parse(it)) } }
            .build()
        c.setMediaItem(MediaItem.Builder().setMediaId(url).setUri(url).setMediaMetadata(meta).build())
        c.setPlaybackSpeed(speed)
        c.prepare()
        c.play()
        currentUrl = url
        this.title = title
        this.postId = postId
        positionMs = 0L
        durationMs = 0L
    }

    fun toggle() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        positionMs = ms
    }

    fun skip(deltaMs: Long) {
        val c = controller ?: return
        val max = if (durationMs > 0) durationMs else Long.MAX_VALUE
        seekTo((c.currentPosition + deltaMs).coerceIn(0L, max))
    }

    fun cycleSpeed() {
        val next = when (speed) {
            1f -> 1.25f
            1.25f -> 1.5f
            1.5f -> 2f
            else -> 1f
        }
        speed = next
        controller?.setPlaybackSpeed(next)
    }

    fun stop() {
        controller?.let { it.stop(); it.clearMediaItems() }
        currentUrl = null
        isPlaying = false
        positionMs = 0L
        durationMs = 0L
    }

    fun tick() {
        val c = controller ?: return
        positionMs = c.currentPosition
        val d = c.duration
        if (d != C.TIME_UNSET && d > 0) durationMs = d
    }

    fun release() {
        MediaController.releaseFuture(future)
    }
}
