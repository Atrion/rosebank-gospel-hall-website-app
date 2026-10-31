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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.rosebankgospelhall.app.AppViewModel
import ca.rosebankgospelhall.app.data.Site

private data class Tile(val title: String, val note: String, val icon: ImageVector, val route: String)

private val tiles = listOf(
    Tile("Videos", "Watch teaching", Icons.Filled.VideoLibrary, "watch"),
    Tile("Audio", "Listen on the go", Icons.Filled.Headphones, "listen"),
    Tile("Articles and tracts", "Read the gospel", Icons.AutoMirrored.Filled.Article, "posts?filter=articles"),
    Tile("Testimonies", "Changed lives", Icons.Filled.Groups, "posts?filter=testimony"),
    Tile("International", "Gospel worldwide", Icons.Filled.Public, "posts?filter=international"),
    Tile("Resources", "Books and links", Icons.AutoMirrored.Filled.LibraryBooks, "page/resources"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: AppViewModel,
    onPost: (Int) -> Unit,
    onNav: (String) -> Unit,
    onEnableNotify: () -> Unit,
) {
    val posts by vm.posts.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val saved by vm.bookmarks.collectAsState()
    val notify by vm.notify.collectAsState()
    val dismissed by vm.promptDismissed.collectAsState()
    val scroll = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Rosebank Gospel Hall", fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold) },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        PullToRefreshBox(isRefreshing = loading, onRefresh = { vm.refresh() }, modifier = Modifier.padding(pad)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                item { Hero(onContact = { onNav("contact") }) }

                if (!notify && !dismissed) {
                    item {
                        Card(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = cs.tertiaryContainer),
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.NotificationsActive, null, tint = cs.onTertiaryContainer)
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Hear when something new is posted", style = MaterialTheme.typography.titleSmall, color = cs.onTertiaryContainer)
                                    Row {
                                        TextButton(onClick = onEnableNotify) { Text("Turn on") }
                                        TextButton(onClick = { vm.dismissPrompt() }) { Text("Not now") }
                                    }
                                }
                            }
                        }
                    }
                }

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
                        Column(Modifier.padding(20.dp)) {
                            Text("Visit us", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(6.dp))
                            Text(Site.ADDRESS, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
private fun Hero(onContact: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.large).background(BurgundyDeep).padding(24.dp),
    ) {
        Text(
            Site.VERSE,
            color = Color.White,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontSize = 21.sp,
            lineHeight = 31.sp,
        )
        Spacer(Modifier.height(14.dp))
        Text(Site.VERSE_REF, color = Gold, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onContact,
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = BurgundyDeep),
        ) { Text("Ask us a question") }
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
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
                TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) { Text(action) }
            }
        }
    }
}
