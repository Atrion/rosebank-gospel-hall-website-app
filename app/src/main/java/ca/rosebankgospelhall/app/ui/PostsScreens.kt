package ca.rosebankgospelhall.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import ca.rosebankgospelhall.app.AppViewModel
import ca.rosebankgospelhall.app.data.Block
import ca.rosebankgospelhall.app.data.HtmlParser
import ca.rosebankgospelhall.app.data.MediaScan
import ca.rosebankgospelhall.app.data.Site
import ca.rosebankgospelhall.app.data.PageData
import org.jsoup.Jsoup

private val sortOptions = listOf("Newest first", "Oldest first", "Title A to Z", "Title Z to A")

private data class Topic(val slug: String, val name: String, val count: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsScreen(vm: AppViewModel, title: String, preset: String?, initialFilter: String, onPost: (Int) -> Unit) {
    val posts by vm.posts.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val saved by vm.bookmarks.collectAsState()
    var filter by remember(initialFilter) { mutableStateOf(initialFilter) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(0) }
    var sortOpen by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    // Listen and Watch only look at audio / video posts; Posts looks at everything.
    val base = remember(posts, preset) { if (preset == null) posts else posts.filter { it.matches(preset) } }

    // Every category and tag found on the loaded posts becomes a filter.
    val topics = remember(base, preset) {
        base.flatMap { it.terms }
            .filter { it.slug.isNotBlank() && !it.slug.equals("uncategorized", true) }
            .filter { preset == null || !(it.slug.equals(preset, true) || (preset == "videos" && it.slug.equals("video", true))) }
            .distinctBy { it.slug.lowercase() }
            .map { t -> Topic(t.slug, t.name, base.count { it.matches(t.slug) }) }
            .sortedWith(compareByDescending<Topic> { it.count }.thenBy { it.name.lowercase() })
    }

    val shown = remember(base, filter, query, sort) {
        val list = base.filter { p ->
            p.matches(filter) && (query.isBlank() ||
                p.title.contains(query, ignoreCase = true) || p.excerpt.contains(query, ignoreCase = true))
        }
        when (sort) {
            1 -> list.sortedBy { it.dateMillis }
            2 -> list.sortedBy { it.title.lowercase() }
            3 -> list.sortedByDescending { it.title.lowercase() }
            else -> list.sortedByDescending { it.dateMillis }
        }
    }
    LaunchedEffect(searching) { if (searching) focus.requestFocus() }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (searching) {
                TopAppBar(
                    title = {
                        TextField(
                            value = query, onValueChange = { query = it },
                            placeholder = { Text("Search posts") }, singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { searching = false; query = "" }) { Icon(Icons.Filled.Close, "Close search") }
                    },
                )
            } else {
                TopAppBar(
                    title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                    actions = {
                        IconButton(onClick = { searching = true }) { Icon(Icons.Filled.Search, "Search") }
                        Box {
                            IconButton(onClick = { sortOpen = true }) { Icon(Icons.AutoMirrored.Filled.Sort, "Sort") }
                            DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                                sortOptions.forEachIndexed { i, label ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = { sort = i; sortOpen = false },
                                        leadingIcon = { if (sort == i) Icon(Icons.Filled.Check, null) },
                                    )
                                }
                            }
                        }
                    },
                )
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            if (topics.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item { FilterChip(selected = filter.isBlank(), onClick = { filter = "" }, label = { Text("All") }) }
                    items(topics, key = { it.slug }) { t ->
                        FilterChip(
                            selected = filter.equals(t.slug, true),
                            onClick = { filter = t.slug },
                            label = { Text("${t.name} (${t.count})") },
                        )
                    }
                }
            }
            PullToRefreshBox(isRefreshing = loading, onRefresh = { vm.refresh() }, modifier = Modifier.fillMaxSize()) {
                LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)) {
                    if (error != null && posts.isNotEmpty()) {
                        item {
                            Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.WifiOff, null, Modifier.padding(end = 8.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(error.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (shown.isEmpty()) {
                        item {
                            if (loading && posts.isEmpty()) {
                                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                            } else {
                                EmptyState(
                                    Icons.AutoMirrored.Filled.Article,
                                    if (posts.isEmpty()) "Nothing to show yet" else "No matching posts",
                                    if (posts.isEmpty()) (error ?: "Pull down to refresh.") else "Try a different filter or search.",
                                    actionLabel = if (posts.isEmpty()) "Try again" else null,
                                    onAction = if (posts.isEmpty()) ({ vm.refresh() }) else null,
                                )
                            }
                        }
                    }
                    items(shown, key = { it.id }) { p ->
                        PostCard(
                            p, saved = p.id in saved, onClick = { onPost(p.id) }, onSave = { vm.toggleBookmark(p.id) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(vm: AppViewModel, onBack: () -> Unit, onPost: (Int) -> Unit) {
    val posts by vm.posts.collectAsState()
    val saved by vm.bookmarks.collectAsState()
    val mine = remember(posts, saved) { posts.filter { it.id in saved } }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("Saved") }, navigationIcon = { BackButton(onBack) }) },
    ) { pad ->
        if (mine.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Filled.BookmarkBorder, "Nothing saved yet", "Tap the bookmark on any post to keep it here.")
            }
        } else {
            LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(mine, key = { it.id }) { p ->
                    PostCard(
                        p, saved = true, onClick = { onPost(p.id) }, onSave = { vm.toggleBookmark(p.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(vm: AppViewModel, id: Int, onBack: () -> Unit, onPost: (Int) -> Unit) {
    val posts by vm.posts.collectAsState()
    val loading by vm.loading.collectAsState()
    val saved by vm.bookmarks.collectAsState()
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    val cs = MaterialTheme.colorScheme
    val post = posts.firstOrNull { it.id == id }

    if (post == null) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = { TopAppBar(title = {}, navigationIcon = { BackButton(onBack) }) },
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                if (loading || posts.isEmpty()) CircularProgressIndicator() else Text("This post isn't available.")
            }
        }
        return
    }

    val linkColor = cs.primary
    val parsed = remember(post.id, linkColor) { HtmlParser.parse(post.contentHtml, post.link, linkColor) }
    val scanned = remember(post.id) { MediaScan.scan(post.contentHtml) }
    var live by remember(post.id) { mutableStateOf<List<Block.Video>?>(null) }
    val inParsed = parsed.filterIsInstance<Block.Video>()
    LaunchedEffect(post.id) {
        // Video posts whose content has no video: read the live page for it.
        if (post.isVideo && inParsed.isEmpty() && scanned.isEmpty()) {
            live = runCatching { vm.liveMedia(post.link) }.getOrDefault(emptyList())
        }
    }
    val videos = (inParsed + scanned + (live ?: emptyList())).distinctBy { MediaScan.key(it.url) }
    val hero = videos.firstOrNull()
    val parsedKeys = inParsed.map { MediaScan.key(it.url) }.toSet()
    val extras = videos.drop(1).filter { MediaScan.key(it.url) !in parsedKeys }
    val body = parsed.filterNot { it is Block.Video && hero != null && MediaScan.key(it.url) == MediaScan.key(hero.url) }
    val waitingLive = post.isVideo && videos.isEmpty() && live == null
    val noVideoFound = post.isVideo && videos.isEmpty() && live != null
    val blocks = buildList<Block> {
        addAll(extras)
        addAll(body)
        if (post.isAudio && parsed.none { it is Block.Audio || it is Block.Video }) {
            add(Block.Button("Open the website to listen", "ext:" + post.link))
        }
        if (noVideoFound) {
            add(Block.Button("Watch on the website", "ext:" + post.link))
            add(Block.Button("Open our YouTube channel", Site.YOUTUBE))
        }
    }
    val minutes = remember(post.id) {
        (Jsoup.parse(post.contentHtml).text().split(' ').size / 200).coerceAtLeast(1)
    }
    val related = remember(posts, post.id) {
        val keys = post.terms.filter { it.slug != "uncategorized" }.map { it.slug }.toSet()
        posts.filter { p -> p.id != post.id && p.terms.any { it.slug in keys } }.take(3)
    }
    val isSaved = post.id in saved

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(onClick = { vm.toggleBookmark(post.id) }) {
                        Icon(
                            if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (isSaved) "Remove from saved" else "Save",
                            tint = if (isSaved) cs.primary else cs.onSurface,
                        )
                    }
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, post.title)
                            putExtra(Intent.EXTRA_TEXT, post.title + "\n" + post.link)
                        }
                        ctx.safeStart(Intent.createChooser(send, null))
                    }) { Icon(Icons.Filled.Share, "Share") }
                    IconButton(onClick = { openCustomTab(ctx, post.link) }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, "Open on website")
                    }
                },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(bottom = 32.dp)) {
            if (hero != null) {
                contentBlocks(listOf(hero), vm.audio, post.title, post.imageUrl, post.id)
            } else if (post.imageUrl != null) {
                item { PostImage(post, Modifier.fillMaxWidth().aspectRatio(16f / 9f)) }
            }
            item {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)) {
                    val tags = post.terms.filter { it.slug != "uncategorized" }.take(3)
                    if (tags.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            tags.forEach { t ->
                                Surface(shape = CircleShape, color = cs.secondaryContainer) {
                                    Text(
                                        t.name, style = MaterialTheme.typography.labelMedium,
                                        color = cs.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        post.title, style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    Text(
                        post.dateText, style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Text(
                        (post.author?.let { "By $it. " } ?: "") + "$minutes min read",
                        style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant,
                    )
                }
            }
            if (waitingLive) {
                item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            }
            contentBlocks(blocks, vm.audio, post.title, post.imageUrl, post.id)
            if (related.isNotEmpty()) {
                item { SectionHeader("Keep reading") }
                items(related, key = { it.id }) { CompactPostRow(it) { onPost(it.id) } }
            }
        }
    }
}

private sealed interface PageState {
    data object Loading : PageState
    data object Failed : PageState
    data class Ready(val page: PageData) : PageState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageScreen(vm: AppViewModel, slug: String, title: String, onBack: () -> Unit) {
    var attempt by remember { mutableStateOf(0) }
    val state by produceState<PageState>(PageState.Loading, slug, attempt) {
        value = PageState.Loading
        value = try { PageState.Ready(vm.loadPage(slug)) } catch (e: Exception) { PageState.Failed }
    }
    val ctx = LocalContext.current
    val linkColor = MaterialTheme.colorScheme.primary

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(onClick = { openCustomTab(ctx, "https://rosebankgospelhall.ca/$slug/") }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, "Open on website")
                    }
                },
            )
        },
    ) { pad ->
        when (val s = state) {
            PageState.Loading -> Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            PageState.Failed -> Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    Icons.Filled.WifiOff, "Couldn't load this page", "Check your connection and try again.",
                    actionLabel = "Try again", onAction = { attempt++ },
                )
            }
            is PageState.Ready -> {
                val blocks = remember(s.page, linkColor) {
                    val parsed = HtmlParser.parse(s.page.html, "https://rosebankgospelhall.ca/$slug/", linkColor, dedupe = true)
                    if (s.page.scraped) HtmlParser.trimFooter(parsed) else parsed
                }
                if (blocks.isEmpty()) {
                    Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            Icons.AutoMirrored.Filled.OpenInNew, "Nothing to show here yet", "This page is easier to read on the website.",
                            actionLabel = "Open the website", onAction = { openCustomTab(ctx, "https://rosebankgospelhall.ca/$slug/") },
                        )
                    }
                } else {
                    LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(vertical = 12.dp)) {
                        contentBlocks(blocks, vm.audio, title, null, null)
                    }
                }
            }
        }
    }
}
