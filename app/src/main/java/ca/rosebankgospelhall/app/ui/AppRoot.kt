package ca.rosebankgospelhall.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavGraph.Companion.findStartDestination
import ca.rosebankgospelhall.app.AppViewModel
import kotlinx.coroutines.delay

private data class NavTab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    NavTab("home", "Home", Icons.Filled.Home),
    NavTab("posts", "Posts", Icons.AutoMirrored.Filled.Article),
    NavTab("listen", "Listen", Icons.Filled.Headphones),
    NavTab("watch", "Watch", Icons.Filled.VideoLibrary),
    NavTab("more", "More", Icons.Filled.MoreHoriz),
)
private val tabRoutes = tabs.map { it.route }
private val pageTitles = mapOf(
    "what-we-believe" to "What we believe",
    "resources" to "Resources",
    "free-food-hamper" to "Free food hamper",
)

internal fun openCustomTab(ctx: Context, url: String) {
    val scheme = Uri.parse(url).scheme?.lowercase()
    if (scheme != "http" && scheme != "https") return
    try {
        CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(ctx, Uri.parse(url))
    } catch (_: Exception) {
        ctx.safeStart(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
fun rememberNotifyEnabler(vm: AppViewModel): () -> Unit {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.setNotify(true)
    }
    return {
        val needsPermission = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.setNotify(true)
    }
}

@Composable
fun AppRoot(vm: AppViewModel, startPostId: Int?, onStartConsumed: () -> Unit) {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    val audio = vm.audio
    val posts by vm.posts.collectAsState()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route?.substringBefore("?")
    val showBar = route in tabRoutes
    val enableNotify = rememberNotifyEnabler(vm)
    val notifyOn by vm.notify.collectAsState()
    val promptDone by vm.promptDismissed.collectAsState()

    fun navTo(r: String) {
        if (r in tabRoutes) {
            nav.navigate(r) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            nav.navigate(r) { launchSingleTop = true }
        }
    }

    // Links inside posts open in the app when they point at the Gospel Hall website.
    val handler = remember(posts, nav) {
        object : UriHandler {
            override fun openUri(uri: String) {
                if (uri.startsWith("ext:")) {
                    openCustomTab(ctx, uri.removePrefix("ext:"))
                    return
                }
                val u = uri.trim()
                // Only web, mail and phone links are ever opened.
                val scheme = Uri.parse(u).scheme?.lowercase()
                if (scheme != "http" && scheme != "https" && scheme != "mailto" && scheme != "tel") return
                val bare = u.removePrefix("https://").removePrefix("http://").removePrefix("www.").trimEnd('/')
                val host = "rosebankgospelhall.ca"
                when {
                    bare == host || bare.startsWith("$host/") -> {
                        val path = bare.removePrefix(host).trim('/')
                        val post = posts.firstOrNull {
                            it.link.removePrefix("https://").removePrefix("http://").removePrefix("www.")
                                .trimEnd('/').removePrefix(host).trim('/') == path
                        }
                        when {
                            post != null -> nav.navigate("post/${post.id}")
                            path.isEmpty() -> navTo("home")
                            path == "posts" -> navTo("posts")
                            path == "audio" -> navTo("listen")
                            path == "videos" -> navTo("watch")
                            path == "articles" -> nav.navigate("posts?filter=articles")
                            path == "international" -> nav.navigate("posts?filter=international")
                            path == "contact-us" -> nav.navigate("contact")
                            path in pageTitles -> nav.navigate("page/$path")
                            else -> openCustomTab(ctx, u)
                        }
                    }
                    bare.startsWith("youtube.com") || bare.startsWith("m.youtube.com") || bare.startsWith("youtu.be") ->
                        ctx.safeStart(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
                    u.startsWith("mailto:") || u.startsWith("tel:") ->
                        ctx.safeStart(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
                    else -> openCustomTab(ctx, u)
                }
            }
        }
    }

    // Keeps the progress bar moving while audio plays.
    LaunchedEffect(audio.isPlaying) {
        while (audio.isPlaying) {
            audio.tick()
            delay(500)
        }
        audio.tick()
    }

    // A tapped notification opens the post directly.
    LaunchedEffect(startPostId) {
        if (startPostId != null) {
            nav.navigate("post/$startPostId")
            onStartConsumed()
        }
    }

    val openPost: (Int) -> Unit = { nav.navigate("post/$it") }

    CompositionLocalProvider(LocalUriHandler provides handler) {
      Box(Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                Column {
                    if (audio.currentUrl != null) {
                        MiniPlayer(audio) { audio.postId?.let { nav.navigate("post/$it") } }
                    }
                    if (showBar) {
                        NavigationBar {
                            tabs.forEach { t ->
                                NavigationBarItem(
                                    selected = route == t.route,
                                    onClick = { navTo(t.route) },
                                    icon = { Icon(t.icon, contentDescription = null) },
                                    label = { Text(t.label) },
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                    }
                }
            },
        ) { pad ->
            NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
                composable("home") {
                    HomeScreen(vm, onPost = openPost, onNav = { navTo(it) })
                }
                composable(
                    "posts?filter={filter}",
                    arguments = listOf(navArgument("filter") { type = NavType.StringType; defaultValue = "" }),
                ) { be ->
                    PostsScreen(vm, "Posts", null, be.arguments?.getString("filter").orEmpty(), openPost)
                }
                composable("listen") { PostsScreen(vm, "Listen", "audio", "", openPost) }
                composable("watch") { PostsScreen(vm, "Watch", "videos", "", openPost) }
                composable("more") { MoreScreen(onNav = { navTo(it) }) }
                composable("saved") { SavedScreen(vm, onBack = { nav.popBackStack() }, onPost = openPost) }
                composable("settings") {
                    SettingsScreen(vm, onBack = { nav.popBackStack() }, enableNotify = enableNotify, onNav = { navTo(it) })
                }
                composable("licenses") { LicensesScreen(onBack = { nav.popBackStack() }) }
                composable("contact") { ContactScreen(onBack = { nav.popBackStack() }) }
                composable(
                    "post/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.IntType }),
                ) { be ->
                    PostDetailScreen(vm, be.arguments?.getInt("id") ?: -1, onBack = { nav.popBackStack() }, onPost = openPost)
                }
                composable(
                    "page/{slug}",
                    arguments = listOf(navArgument("slug") { type = NavType.StringType }),
                ) { be ->
                    val slug = be.arguments?.getString("slug").orEmpty()
                    PageScreen(vm, slug, pageTitles[slug] ?: "", onBack = { nav.popBackStack() })
                }
            }
        }
        if (!notifyOn && !promptDone) {
            NotificationPrompt(
                onYes = { vm.dismissPrompt(); enableNotify() },
                onNo = { vm.setNotify(false); vm.dismissPrompt() },
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
      }
    }
}
