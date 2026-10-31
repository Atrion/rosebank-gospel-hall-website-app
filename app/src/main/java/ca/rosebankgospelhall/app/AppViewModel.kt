package ca.rosebankgospelhall.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ca.rosebankgospelhall.app.data.PageData
import ca.rosebankgospelhall.app.data.Post
import ca.rosebankgospelhall.app.data.Prefs
import ca.rosebankgospelhall.app.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    private val prefs = Prefs(app)
    private val pages = HashMap<String, PageData>()

    val audio = AudioController(app)

    val posts = MutableStateFlow<List<Post>>(emptyList())
    val loading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val bookmarks = MutableStateFlow(prefs.bookmarks)
    val themeMode = MutableStateFlow(prefs.themeMode)
    val textScale = MutableStateFlow(prefs.textScale)
    val notify = MutableStateFlow(prefs.notify)
    val promptDismissed = MutableStateFlow(prefs.promptDismissed)

    init {
        if (prefs.notify) NewPostWorker.schedule(app)
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) { repo.cached() }
            if (posts.value.isEmpty()) posts.value = cached
            refresh()
        }
    }

    fun refresh() {
        if (loading.value) return
        viewModelScope.launch {
            loading.value = true
            try {
                val fresh = repo.fetchPosts()
                posts.value = fresh
                error.value = null
                val newest = fresh.maxOfOrNull { it.dateMillis } ?: 0L
                if (newest > prefs.lastSeenMillis) prefs.lastSeenMillis = newest
            } catch (e: Exception) {
                error.value = if (posts.value.isEmpty()) {
                    "Couldn't load posts. Check your connection and try again."
                } else {
                    "You're offline. Showing posts saved on this phone."
                }
            } finally {
                loading.value = false
            }
        }
    }

    suspend fun loadPage(slug: String): PageData =
        pages[slug] ?: repo.fetchPage(slug).also { pages[slug] = it }

    fun toggleBookmark(id: Int) {
        val next = if (id in bookmarks.value) bookmarks.value - id else bookmarks.value + id
        prefs.bookmarks = next
        bookmarks.value = next
    }

    fun setTheme(mode: Int) { prefs.themeMode = mode; themeMode.value = mode }
    fun setTextScale(v: Float) { prefs.textScale = v; textScale.value = v }
    fun dismissPrompt() { prefs.promptDismissed = true; promptDismissed.value = true }

    fun setNotify(on: Boolean) {
        prefs.notify = on
        notify.value = on
        val app = getApplication<Application>()
        if (on) NewPostWorker.schedule(app) else NewPostWorker.cancel(app)
    }

    override fun onCleared() {
        audio.release()
        super.onCleared()
    }
}
