package ca.rosebankgospelhall.app

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.rosebankgospelhall.app.ui.AppRoot
import ca.rosebankgospelhall.app.ui.LocalReadScale
import ca.rosebankgospelhall.app.ui.RosebankTheme

class MainActivity : ComponentActivity() {
    private val startPost = mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startPost.value = intent.getIntExtra("postId", -1).takeIf { it > 0 }
        setContent {
            val vm: AppViewModel = viewModel()
            val mode by vm.themeMode.collectAsState()
            val scale by vm.textScale.collectAsState()
            val dark = when (mode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            SideEffect {
                val style = if (dark) {
                    SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                } else {
                    SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            RosebankTheme(dark) {
                CompositionLocalProvider(LocalReadScale provides scale) {
                    AppRoot(vm, startPost.value) { startPost.value = null }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        startPost.value = intent.getIntExtra("postId", -1).takeIf { it > 0 }
    }
}
