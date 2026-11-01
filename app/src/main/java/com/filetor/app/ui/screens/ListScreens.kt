package com.filetor.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.filetor.app.data.Category
import com.filetor.app.data.FileEntry
import com.filetor.app.data.FileRepository
import com.filetor.app.data.MediaRepository
import com.filetor.app.data.Prefs
import com.filetor.app.ui.AppViewModel
import com.filetor.app.ui.components.ActionDialog
import com.filetor.app.ui.components.EmptyState
import com.filetor.app.ui.components.FileActionHost
import com.filetor.app.ui.components.FileRow
import com.filetor.app.ui.components.FileThumb
import com.filetor.app.ui.components.SubTopBar
import com.filetor.app.ui.components.rememberFileActions
import com.filetor.app.util.FileType
import com.filetor.app.util.Format
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryScreen(vm: AppViewModel, category: Category) {
    val context = LocalContext.current
    val actions = rememberFileActions(vm)
    var items by remember { mutableStateOf<List<FileEntry>?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    actions.onChanged = { reload++ }
    LaunchedEffect(category, reload) { items = MediaRepository.query(context, category) }
    LaunchedEffect(Unit) { com.filetor.app.data.OperationManager.events.collect { reload++ } }
    val list = items
    Column(Modifier.fillMaxSize()) {
        SubTopBar(category.label, list?.let { "${it.size} archivos · ${Format.size(it.sumOf { e -> e.size })}" } ?: "Cargando…", onBack = { vm.pop() })
        if (list == null) LinearProgressIndicator(Modifier.fillMaxWidth())
        when {
            list == null -> {}
            list.isEmpty() -> EmptyState(category.type.icon, "No hay ${category.label.lowercase()}", "No se encontraron archivos de este tipo")
            category == Category.IMAGES || category == Category.VIDEOS -> {
                LazyVerticalGrid(GridCells.Adaptive(110.dp), contentPadding = PaddingValues(6.dp, 6.dp, 6.dp, 120.dp)) {
                    items(list, key = { it.path }) { e ->
                        Box(
                            Modifier.padding(3.dp).aspectRatio(1f).clip(RoundedCornerShape(14.dp))
                                .combinedClickable(onClick = { actions.open(e, list) }, onLongClick = { actions.dialog = ActionDialog.Menu(e) })
                        ) { FileThumb(e, 200.dp, Modifier.fillMaxSize()) }
                    }
                }
            }
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                items(list, key = { it.path }) { e ->
                    FileRow(e, false, onClick = { actions.open(e, list) }, onLongClick = { actions.dialog = ActionDialog.Menu(e) },
                        subtitle = Format.size(e.size) + " · " + Format.date(e.lastModified) + " · " + (File(e.path).parentFile?.name ?: ""),
                        trailing = { IconButton({ actions.dialog = ActionDialog.Menu(e) }) { Icon(Icons.Rounded.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } })
                }
            }
        }
    }
    FileActionHost(actions)
}

@Composable
fun SearchScreen(vm: AppViewModel, root: String) {
    val context = LocalContext.current
    val actions = rememberFileActions(vm)
    var query by remember { mutableStateOf("") }
    var type by remember { mutableStateOf<FileType?>(null) }
    var hidden by remember { mutableStateOf(Prefs.showHidden) }
    val results = remember { mutableStateListOf<FileEntry>() }
    var searching by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    actions.onChanged = { reload++ }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(200); runCatching { focus.requestFocus() } }
    LaunchedEffect(query, type, hidden, reload) {
        results.clear()
        if (query.isBlank() && type == null) { searching = false; return@LaunchedEffect }
        delay(300)
        searching = true
        val buffer = mutableListOf<FileEntry>()
        var last = System.currentTimeMillis()
        FileRepository.search(root, query, type, hidden).collect { e ->
            buffer += e
            if (System.currentTimeMillis() - last > 250 || buffer.size > 100) { results.addAll(buffer); buffer.clear(); last = System.currentTimeMillis() }
            if (results.size > 5000) return@collect
        }
        results.addAll(buffer)
        searching = false
    }
    Column(Modifier.fillMaxSize()) {
        SubTopBar("Buscar", "En " + FileRepository.displayPath(context, root), onBack = { vm.pop() })
        OutlinedTextField(
            query, { query = it }, singleLine = true, placeholder = { Text("Nombre del archivo (admite * y ?)") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(Icons.Rounded.Close, null) } },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).focusRequester(focus),
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(hidden, { hidden = !hidden }, label = { Text("Incluir ocultos") })
            listOf(FileType.FOLDER, FileType.IMAGE, FileType.VIDEO, FileType.AUDIO, FileType.DOCUMENT, FileType.PDF, FileType.ARCHIVE, FileType.APK, FileType.TEXT).forEach { t ->
                FilterChip(type == t, { type = if (type == t) null else t }, label = { Text(t.label) },
                    leadingIcon = { Icon(t.icon, null, Modifier.padding(0.dp)) })
            }
        }
        if (searching) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
        if (!searching && results.isEmpty()) {
            if (query.isNotBlank() || type != null) EmptyState(Icons.Rounded.SearchOff, "Sin resultados", "Prueba con otro nombre o filtro")
            else EmptyState(Icons.Rounded.Search, "Busca en tus archivos", "Escribe un nombre o elige un tipo de archivo")
        } else {
            Text("${results.size} resultados" + if (searching) "…" else "", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                items(results.toList(), key = { it.path }) { e ->
                    FileRow(e, false, onClick = { actions.open(e, results) }, onLongClick = { actions.dialog = ActionDialog.Menu(e) },
                        subtitle = FileRepository.displayPath(context, File(e.path).parent ?: ""),
                        trailing = {
                            IconButton({ vm.openInFiles(File(e.path).parent ?: root) }) { Icon(Icons.Rounded.FolderOpen, "Abrir ubicación", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        })
                }
            }
        }
    }
    FileActionHost(actions)
}

@Composable
fun RecentsScreen(vm: AppViewModel) {
    val actions = rememberFileActions(vm)
    val list = Prefs.recents.map { File(it) }.filter { it.exists() }.map { FileEntry.from(it, false) }
    Column(Modifier.fillMaxSize()) {
        SubTopBar("Abiertos recientemente", "${list.size} archivos", onBack = { vm.pop() }) {
            if (list.isNotEmpty()) IconButton({ Prefs.clearRecents() }) { Icon(Icons.Rounded.DeleteSweep, "Borrar historial") }
        }
        if (list.isEmpty()) EmptyState(Icons.Rounded.History, "Sin historial", "Los archivos que abras aparecerán aquí")
        else LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
            items(list, key = { it.path }) { e ->
                FileRow(e, false, onClick = { actions.open(e) }, onLongClick = { actions.dialog = ActionDialog.Menu(e) },
                    trailing = { IconButton({ Prefs.removeRecent(e.path) }) { Icon(Icons.Rounded.Close, "Quitar", tint = MaterialTheme.colorScheme.onSurfaceVariant) } })
            }
        }
    }
    FileActionHost(actions)
}
