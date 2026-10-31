package ca.rosebankgospelhall.app.data

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("rgh_prefs", Context.MODE_PRIVATE)

    var themeMode: Int
        get() = sp.getInt("theme", 0)
        set(v) = sp.edit().putInt("theme", v).apply()

    var textScale: Float
        get() = sp.getFloat("text_scale", 1f)
        set(v) = sp.edit().putFloat("text_scale", v).apply()

    var notify: Boolean
        get() = sp.getBoolean("notify", false)
        set(v) = sp.edit().putBoolean("notify", v).apply()

    var promptDismissed: Boolean
        get() = sp.getBoolean("prompt_dismissed", false)
        set(v) = sp.edit().putBoolean("prompt_dismissed", v).apply()

    var lastSeenMillis: Long
        get() = sp.getLong("last_seen", 0L)
        set(v) = sp.edit().putLong("last_seen", v).apply()

    var bookmarks: Set<Int>
        get() = (sp.getStringSet("bookmarks", emptySet()) ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet()
        set(v) = sp.edit().putStringSet("bookmarks", v.map { it.toString() }.toSet()).apply()
}
