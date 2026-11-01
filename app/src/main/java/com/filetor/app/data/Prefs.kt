package com.filetor.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray

/** Observable app settings, persisted in SharedPreferences. */
object Prefs {
    private lateinit var sp: SharedPreferences

    var themeMode by mutableStateOf(ThemeMode.SYSTEM); private set
    var dynamicColor by mutableStateOf(false); private set
    var accent by mutableStateOf(0); private set
    var amoled by mutableStateOf(false); private set
    var showHidden by mutableStateOf(false); private set
    var foldersFirst by mutableStateOf(true); private set
    var sortBy by mutableStateOf(SortBy.NAME); private set
    var sortAsc by mutableStateOf(true); private set
    var viewMode by mutableStateOf(ViewMode.LIST); private set
    var showThumbnails by mutableStateOf(true); private set
    var useTrash by mutableStateOf(true); private set
    var confirmDelete by mutableStateOf(true); private set
    var trashDays by mutableStateOf(30); private set
    var showFileCount by mutableStateOf(true); private set
    var compactList by mutableStateOf(false); private set

    val bookmarks = mutableStateListOf<String>()
    val recents = mutableStateListOf<String>()

    fun init(context: Context) {
        sp = context.getSharedPreferences("filetor", Context.MODE_PRIVATE)
        themeMode = runCatching { ThemeMode.valueOf(sp.getString("theme", "SYSTEM")!!) }.getOrDefault(ThemeMode.SYSTEM)
        dynamicColor = sp.getBoolean("dynamic", false)
        accent = sp.getInt("accent", 0)
        amoled = sp.getBoolean("amoled", false)
        showHidden = sp.getBoolean("hidden", false)
        foldersFirst = sp.getBoolean("foldersFirst", true)
        sortBy = runCatching { SortBy.valueOf(sp.getString("sortBy", "NAME")!!) }.getOrDefault(SortBy.NAME)
        sortAsc = sp.getBoolean("sortAsc", true)
        viewMode = runCatching { ViewMode.valueOf(sp.getString("view", "LIST")!!) }.getOrDefault(ViewMode.LIST)
        showThumbnails = sp.getBoolean("thumbs", true)
        useTrash = sp.getBoolean("trash", true)
        confirmDelete = sp.getBoolean("confirmDelete", true)
        trashDays = sp.getInt("trashDays", 30)
        showFileCount = sp.getBoolean("fileCount", true)
        compactList = sp.getBoolean("compact", false)
        bookmarks.clear(); bookmarks.addAll(readList("bookmarks"))
        recents.clear(); recents.addAll(readList("recents"))
    }

    private fun readList(key: String): List<String> = runCatching {
        val arr = JSONArray(sp.getString(key, "[]"))
        List(arr.length()) { arr.getString(it) }
    }.getOrDefault(emptyList())

    private fun writeList(key: String, list: List<String>) {
        sp.edit().putString(key, JSONArray(list).toString()).apply()
    }

    fun setTheme(v: ThemeMode) { themeMode = v; sp.edit().putString("theme", v.name).apply() }
    fun setDynamic(v: Boolean) { dynamicColor = v; sp.edit().putBoolean("dynamic", v).apply() }
    fun setAccentColor(v: Int) { accent = v; sp.edit().putInt("accent", v).apply() }
    fun setAmoledBlack(v: Boolean) { amoled = v; sp.edit().putBoolean("amoled", v).apply() }
    fun setHidden(v: Boolean) { showHidden = v; sp.edit().putBoolean("hidden", v).apply() }
    fun setFoldersFirstPref(v: Boolean) { foldersFirst = v; sp.edit().putBoolean("foldersFirst", v).apply() }
    fun setSort(by: SortBy, asc: Boolean) {
        sortBy = by; sortAsc = asc
        sp.edit().putString("sortBy", by.name).putBoolean("sortAsc", asc).apply()
    }
    fun setView(v: ViewMode) { viewMode = v; sp.edit().putString("view", v.name).apply() }
    fun setThumbs(v: Boolean) { showThumbnails = v; sp.edit().putBoolean("thumbs", v).apply() }
    fun setTrash(v: Boolean) { useTrash = v; sp.edit().putBoolean("trash", v).apply() }
    fun setConfirm(v: Boolean) { confirmDelete = v; sp.edit().putBoolean("confirmDelete", v).apply() }
    fun setTrashRetention(v: Int) { trashDays = v; sp.edit().putInt("trashDays", v).apply() }
    fun setFileCount(v: Boolean) { showFileCount = v; sp.edit().putBoolean("fileCount", v).apply() }
    fun setCompact(v: Boolean) { compactList = v; sp.edit().putBoolean("compact", v).apply() }

    fun toggleBookmark(path: String) {
        if (bookmarks.contains(path)) bookmarks.remove(path) else bookmarks.add(path)
        writeList("bookmarks", bookmarks.toList())
    }

    fun addRecent(path: String) {
        recents.remove(path)
        recents.add(0, path)
        while (recents.size > 40) recents.removeAt(recents.size - 1)
        writeList("recents", recents.toList())
    }

    fun clearRecents() { recents.clear(); writeList("recents", emptyList()) }

    fun removeRecent(path: String) { recents.remove(path); writeList("recents", recents.toList()) }

    fun renamePath(old: String, new: String) {
        val bi = bookmarks.indexOf(old)
        if (bi >= 0) { bookmarks[bi] = new; writeList("bookmarks", bookmarks.toList()) }
        val ri = recents.indexOf(old)
        if (ri >= 0) { recents[ri] = new; writeList("recents", recents.toList()) }
    }

    // Raw storage helpers for other components
    fun getString(key: String, def: String): String = sp.getString(key, def) ?: def
    fun putString(key: String, value: String) { sp.edit().putString(key, value).apply() }
}
