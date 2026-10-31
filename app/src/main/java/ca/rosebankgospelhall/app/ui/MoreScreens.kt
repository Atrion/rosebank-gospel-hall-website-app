package ca.rosebankgospelhall.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import ca.rosebankgospelhall.app.AppViewModel
import ca.rosebankgospelhall.app.data.Site

@Composable
private fun InfoRow(icon: ImageVector, title: String, note: String? = null, trailing: @Composable (() -> Unit)? = null, onClick: (() -> Unit)? = null) {
    ListItem(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(title) },
        supportingContent = note?.let { { Text(it) } },
        trailingContent = trailing,
    )
}

@Composable
private fun Group(label: String) {
    Text(
        label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(onNav: (String) -> Unit) {
    val uri = LocalUriHandler.current
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { CenterAlignedTopAppBar(title = { Text("More", style = MaterialTheme.typography.titleLarge) }) },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
            Group("About us")
            InfoRow(Icons.Filled.Info, "What we believe", "Our statement of faith") { onNav("page/what-we-believe") }
            InfoRow(Icons.AutoMirrored.Filled.LibraryBooks, "Resources", "Books, links and materials to share") { onNav("page/resources") }
            InfoRow(Icons.Filled.VolunteerActivism, "Free food hamper", "Help for families in need") { onNav("page/free-food-hamper") }
            InfoRow(Icons.Filled.Email, "Contact us", "Call, email or find the hall") { onNav("contact") }
            Group("Your app")
            InfoRow(Icons.Filled.Bookmark, "Saved posts") { onNav("saved") }
            InfoRow(Icons.Filled.Settings, "Settings", "Theme, text size, notifications") { onNav("settings") }
            Group("Elsewhere")
            InfoRow(Icons.Filled.VideoLibrary, "YouTube channel", "More videos and teaching") { uri.openUri(Site.YOUTUBE) }
            InfoRow(Icons.Filled.Language, "Visit the website", "rosebankgospelhall.ca") { uri.openUri(Site.BASE) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    fun dial(n: String) = ctx.safeStart(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + n.filter { it.isDigit() || it == '+' })))
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("Contact us") }, navigationIcon = { BackButton(onBack) }) },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
            Text(
                "Have a question about the Bible or the gospel? We would be glad to open the Scriptures with you.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(20.dp),
            )
            InfoRow(Icons.Filled.Place, "Address", Site.ADDRESS) {
                ctx.safeStart(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(Site.ADDRESS))))
            }
            HorizontalDivider(Modifier.padding(horizontal = 20.dp))
            InfoRow(Icons.Filled.Phone, "Call", Site.PHONE1) { dial(Site.PHONE1) }
            InfoRow(Icons.Filled.Phone, "Call", Site.PHONE2) { dial(Site.PHONE2) }
            HorizontalDivider(Modifier.padding(horizontal = 20.dp))
            InfoRow(Icons.Filled.Email, "Email", Site.EMAIL) {
                ctx.safeStart(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + Site.EMAIL)))
            }
            InfoRow(Icons.AutoMirrored.Filled.OpenInNew, "Send a message online", "Opens the contact form") {
                uri.openUri(Site.BASE + "/contact-us/")
            }
            HorizontalDivider(Modifier.padding(horizontal = 20.dp))
            InfoRow(Icons.Filled.VideoLibrary, "YouTube channel") { uri.openUri(Site.YOUTUBE) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit, enableNotify: () -> Unit) {
    val mode by vm.themeMode.collectAsState()
    val scale by vm.textScale.collectAsState()
    val notify by vm.notify.collectAsState()
    val body = MaterialTheme.typography.bodyLarge
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { BackButton(onBack) }) },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
            Group("Appearance")
            Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                listOf("System", "Light", "Dark").forEachIndexed { i, label ->
                    FilterChip(
                        selected = mode == i, onClick = { vm.setTheme(i) }, label = { Text(label) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
            Group("Reading text size")
            Slider(
                value = scale, onValueChange = { vm.setTextScale(it) }, valueRange = 0.9f..1.6f,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Text(
                "For I delivered unto you first of all that which I also received.",
                style = body.copy(fontSize = body.fontSize * scale, lineHeight = body.fontSize * scale * 1.55f),
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Group("Notifications")
            InfoRow(
                Icons.Filled.Info, "New post alerts", "Checks a few times a day",
                trailing = {
                    Switch(checked = notify, onCheckedChange = { if (it) enableNotify() else vm.setNotify(false) })
                },
            )
            Group("About")
            Text(
                "Posts and pages come from rosebankgospelhall.ca. Version 1.1.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
