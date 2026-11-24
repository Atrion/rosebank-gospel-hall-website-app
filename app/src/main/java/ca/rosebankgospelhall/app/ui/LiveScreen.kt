package ca.rosebankgospelhall.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ca.rosebankgospelhall.app.AppViewModel
import ca.rosebankgospelhall.app.VideoActivity
import ca.rosebankgospelhall.app.data.LiveStatus
import ca.rosebankgospelhall.app.data.Site

/** Checks whether the channel is live. If so, plays it in the in-app video player; if not, explains politely. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScreen(vm: AppViewModel, onBack: () -> Unit, onWatchRecent: () -> Unit) {
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    var attempt by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf<LiveStatus?>(null) }

    fun play(id: String) {
        if (vm.audio.isPlaying) vm.audio.toggle()
        ctx.safeStart(
            Intent(ctx, VideoActivity::class.java)
                .putExtra(VideoActivity.EXTRA_URL, "https://www.youtube.com/watch?v=$id"),
        )
    }

    LaunchedEffect(attempt) {
        status = null
        val result = vm.liveStatus()
        status = result
        if (result is LiveStatus.Live) play(result.videoId)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("Live") }, navigationIcon = { BackButton(onBack) }) },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val s = status) {
                null -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Checking for a live broadcast…",
                        style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                    )
                }
                is LiveStatus.Live -> LiveMessage(
                    title = "We are live now",
                    body = "The broadcast opens in a full-screen player. If you closed it, tap below to return.",
                ) {
                    Button(onClick = { play(s.videoId) }, modifier = Modifier.fillMaxWidth()) { Text("Watch live") }
                }
                LiveStatus.Offline -> LiveMessage(
                    title = "There is no live broadcast at the moment",
                    body = "We are not broadcasting live right now. Please check back later, " +
                        "or watch our recent messages in the meantime.",
                ) {
                    Button(onClick = { attempt++ }, modifier = Modifier.fillMaxWidth()) { Text("Check again") }
                    FilledTonalButton(onClick = onWatchRecent, modifier = Modifier.fillMaxWidth()) {
                        Text("Watch recent messages")
                    }
                    TextButton(onClick = { uri.openUri(Site.YOUTUBE) }) { Text("Open our YouTube channel") }
                }
                LiveStatus.Unknown -> LiveMessage(
                    title = "We could not check right now",
                    body = "Please check your connection and try again.",
                ) {
                    Button(onClick = { attempt++ }, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
                    TextButton(onClick = { uri.openUri(Site.YOUTUBE) }) { Text("Open our YouTube channel") }
                }
            }
        }
    }
}

@Composable
private fun LiveMessage(title: String, body: String, actions: @Composable () -> Unit) {
    Icon(Icons.Filled.LiveTv, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(16.dp))
    Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(
        body, style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    actions()
}
