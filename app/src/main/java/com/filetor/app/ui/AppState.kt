package com.filetor.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.filetor.app.data.Category
import com.filetor.app.data.Clipboard
import com.filetor.app.data.FileEntry
import com.filetor.app.data.FileRepository
import com.filetor.app.data.OpEvent
import com.filetor.app.data.OperationManager
import com.filetor.app.data.StorageVolumeInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface Screen {
    data class CategoryView(val category: Category) : Screen
    data class ArchiveView(val path: String) : Screen
    data class TextEditor(val path: String) : Screen
    data class ImageViewer(val path: String, val siblings: List<String>) : Screen
    data class Search(val root: String) : Screen
    data object Analyzer : Screen
    data object Duplicates : Screen
    data object Trash : Screen
    data object Settings : Screen
    data object Recents : Screen
}

class BrowserTab(initial: String) {
    var path by mutableStateOf(initial)
    var entries by mutableStateOf<List<FileEntry>>(emptyList())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var filter by mutableStateOf("")
    val selection = mutableStateListOf<String>()
    val history = ArrayDeque<String>()
    val scroll = mutableMapOf<String, Pair<Int, Int>>()
    internal var job: Job? = null
    val title: String get() = File(path).name.ifEmpty { "/" }
}

data class UiMessage(val text: String, val action: String? = null, val onAction: (() -> Unit)? = null)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val stack = mutableStateListOf<Screen>()
    var mainTab by mutableStateOf(0)
    val tabs = mutableStateListOf(BrowserTab(FileRepository.primaryRoot))
    var currentTab by mutableStateOf(0)
    var clipboard by mutableStateOf<Clipboard?>(null)
    var volumes by mutableStateOf<List<StorageVolumeInfo>>(emptyList())
    var pickMode by mutableStateOf(false)
    var pickMultiple by mutableStateOf(false)
    var onPick: ((List<File>) -> Unit)? = null
    val messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    var passwordRequest by mutableStateOf<OpEvent.NeedPassword?>(null)

    val tab: BrowserTab get() = tabs[currentTab.coerceIn(0, tabs.size - 1)]

    init {
        refreshVolumes()
        viewModelScope.launch {
            OperationManager.events.collect { ev ->
                when (ev) {
                    is OpEvent.Finished -> { refreshAffected(ev.affectedDirs); message(ev.message) }
                    is OpEvent.Failed -> { refreshAffected(ev.affectedDirs); message("Error: ${ev.message}") }
                    is OpEvent.NeedPassword -> passwordRequest = ev
                }
                refreshVolumes()
            }
        }
    }

    fun message(text: String, action: String? = null, onAction: (() -> Unit)? = null) {
        messages.tryEmit(UiMessage(text, action, onAction))
    }

    fun refreshVolumes() { volumes = FileRepository.volumes(getApplication()) }

    private fun refreshAffected(dirs: Set<String>) {
        tabs.forEach { t -> if (dirs.any { it == t.path || it.startsWith(t.path + "/") || t.path.startsWith("$it/") }) load(t) }
        if (dirs.isEmpty()) tabs.forEach { load(it) }
    }

    fun push(s: Screen) { stack.add(s) }
    fun pop(): Boolean = if (stack.isNotEmpty()) { stack.removeAt(stack.size - 1); true } else false

    fun load(t: BrowserTab = tab, keepSelection: Boolean = false) {
        t.job?.cancel()
        t.loading = true
        t.job = viewModelScope.launch {
            try {
                t.entries = FileRepository.list(t.path)
                t.error = null
            } catch (e: Throwable) {
                t.entries = emptyList()
                t.error = e.message ?: "No se puede abrir"
            }
            if (!keepSelection) t.selection.clear() else t.selection.retainAll(t.entries.map { it.path }.toSet())
            t.loading = false
        }
    }

    fun navigate(path: String, t: BrowserTab = tab, addHistory: Boolean = true) {
        if (path == t.path) { load(t); return }
        if (addHistory) t.history.addLast(t.path)
        t.path = path
        t.filter = ""
        t.selection.clear()
        load(t)
    }

    fun openInFiles(path: String, newTab: Boolean = false) {
        stack.clear()
        mainTab = 1
        if (newTab) addTab(path) else navigate(path)
    }

    fun goUp(t: BrowserTab = tab): Boolean {
        val parent = File(t.path).parentFile ?: return false
        val roots = volumes.map { it.path }
        if (t.path in roots || t.path == "/") return false
        navigate(parent.absolutePath, t)
        return true
    }

    fun back(t: BrowserTab = tab): Boolean {
        if (t.selection.isNotEmpty()) { t.selection.clear(); return true }
        if (t.filter.isNotEmpty()) { t.filter = ""; return true }
        val prev = t.history.removeLastOrNull() ?: return goUp(t).also { if (it) t.history.removeLastOrNull() }
        t.path = prev
        t.selection.clear()
        load(t)
        return true
    }

    fun addTab(path: String = FileRepository.primaryRoot) {
        if (tabs.size >= 8) { message("Máximo 8 pestañas"); return }
        val t = BrowserTab(path)
        tabs.add(t)
        currentTab = tabs.size - 1
        load(t)
    }

    fun closeTab(index: Int) {
        if (tabs.size <= 1) return
        tabs.removeAt(index)
        if (currentTab >= tabs.size) currentTab = tabs.size - 1
        else if (currentTab > index) currentTab--
    }

    fun copySelection(cut: Boolean, paths: List<String> = tab.selection.toList()) {
        if (paths.isEmpty()) return
        clipboard = Clipboard(paths, cut)
        tab.selection.clear()
        message(if (cut) "${paths.size} elemento(s) listos para mover" else "${paths.size} elemento(s) copiados al portapapeles")
    }

    fun paste(destDir: String = tab.path) {
        val c = clipboard ?: return
        OperationManager.copy(c.paths.map { File(it) }, File(destDir), c.cut)
        if (c.cut) clipboard = null
    }
}
