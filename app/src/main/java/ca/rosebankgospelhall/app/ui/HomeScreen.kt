package ca.rosebankgospelhall.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.rosebankgospelhall.app.AppViewModel
import ca.rosebankgospelhall.app.data.Site
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

private data class Tile(val title: String, val note: String, val icon: ImageVector, val route: String)

private val tiles = listOf(
    Tile("Articles and tracts", "Read the gospel", Icons.AutoMirrored.Filled.Article, "posts?filter=articles"),
    Tile("Testimonies", "Changed lives", Icons.Filled.Groups, "posts?filter=testimony"),
    Tile("International", "Gospel worldwide", Icons.Filled.Public, "posts?filter=international"),
    Tile("Saved posts", "Read it later", Icons.Filled.Bookmark, "saved"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: AppViewModel,
    onPost: (Int) -> Unit,
    onNav: (String) -> Unit,
) {
    val posts by vm.posts.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val saved by vm.bookmarks.collectAsState()
    val showWelcome by vm.showWelcome.collectAsState()
    val scroll = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(32.dp)) {
                            AsyncImage(
                                model = Site.LOGO, contentDescription = null, contentScale = ContentScale.Fit,
                                modifier = Modifier.padding(3.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("Rosebank Gospel Hall", fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        PullToRefreshBox(isRefreshing = loading, onRefresh = { vm.refresh() }, modifier = Modifier.padding(pad)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                if (showWelcome) item { WelcomeCard(onClose = { vm.dismissWelcome() }) }

                error?.let { msg ->
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.WifiOff, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(msg, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f))
                            TextButton(onClick = { vm.refresh() }) { Text("Retry") }
                        }
                    }
                }

                if (posts.isEmpty()) {
                    item {
                        if (loading) {
                            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        } else {
                            EmptyState(Icons.AutoMirrored.Filled.Article, "No posts yet", "Pull down to refresh.", actionLabel = "Refresh", onAction = { vm.refresh() })
                        }
                    }
                } else {
                    item { SectionHeader("Latest", "See all") { onNav("posts") } }
                    item {
                        PostCard(
                            posts[0], saved = posts[0].id in saved,
                            onClick = { onPost(posts[0].id) }, onSave = { vm.toggleBookmark(posts[0].id) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    items(posts.drop(1).take(4), key = { it.id }) { CompactPostRow(it) { onPost(it.id) } }
                }

                item { SectionHeader("Explore") }
                items(tiles.chunked(2)) { row ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { t -> TileCard(t, Modifier.weight(1f)) { onNav(t.route) } }
                    }
                }

                item {
                    InfoCard(
                        icon = Icons.Filled.VolunteerActivism,
                        title = "Free food hamper",
                        body = "Find out how to receive or request a free food hamper.",
                        action = "Learn more",
                    ) { onNav("page/free-food-hamper") }
                }
                item {
                    InfoCard(
                        icon = Icons.Filled.Info,
                        title = "What we believe",
                        body = "Our faith rests on the Holy Bible, God's inerrant and verbally inspired word.",
                        action = "Read more",
                    ) { onNav("page/what-we-believe") }
                }
                item {
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Visit us", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(6.dp))
                            Text(Site.ADDRESS, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(14.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            ) {
                                FilledTonalButton(onClick = {
                                    ctx.safeStart(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(Site.ADDRESS))))
                                }) {
                                    Icon(Icons.Filled.Place, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Directions")
                                }
                                OutlinedButton(onClick = {
                                    ctx.safeStart(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Site.PHONE1.filter { it.isDigit() || it == '+' })))
                                }) {
                                    Icon(Icons.Filled.Phone, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Call")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeCard(onClose: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.large).background(BurgundyDeep),
    ) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 20.dp)) {
            Text(
                "Welcome",
                color = Color.White,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 30.sp,
                modifier = Modifier.padding(end = 40.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Read, watch and listen to messages from Rosebank Gospel Hall. " +
                    "Tap the bookmark on any post to save it for later.",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = BurgundyDeep),
            ) { Text("Get started") }
        }
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Close welcome", tint = Color.White)
        }
    }
}

@Composable
private fun TileCard(t: Tile, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(
        onClick = onClick, modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp)) {
            Surface(shape = CircleShape, color = cs.primaryContainer) {
                Icon(t.icon, null, tint = cs.onPrimaryContainer, modifier = Modifier.padding(10.dp).size(22.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(t.title, style = MaterialTheme.typography.titleMedium)
            Text(t.note, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, body: String, action: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow),
    ) {
        Row(Modifier.padding(20.dp)) {
            Icon(icon, null, tint = cs.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(action, style = MaterialTheme.typography.labelLarge, color = cs.primary)
            }
        }
    }
}
