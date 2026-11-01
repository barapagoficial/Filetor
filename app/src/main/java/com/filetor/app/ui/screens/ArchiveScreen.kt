package com.filetor.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DriveFolderUpload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filetor.app.data.ArchiveEntry
import com.filetor.app.data.ArchiveManager
import com.filetor.app.data.FileEntry
import com.filetor.app.data.FileRepository
import com.filetor.app.data.OperationManager
import com.filetor.app.ui.AppViewModel
import com.filetor.app.ui.Screen
import com.filetor.app.ui.components.EmptyState
import com.filetor.app.ui.components.FolderPickerDialog
import com.filetor.app.ui.components.PasswordDialog
import com.filetor.app.ui.components.SelectableThumb
import com.filetor.app.ui.components.SubTopBar
import com.filetor.app.util.FileActions
import com.filetor.app.util.FileType
import com.filetor.app.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArchiveScreen(vm: AppViewModel, path: String) {
    val file = remember(path) { File(path) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<ArchiveEntry>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf<String?>(null) }
    var askPassword by remember { mutableStateOf(false) }
    var dir by remember { mutableStateOf("") }
    val selection = remember { mutableStateListOf<String>() }
    var pickDest by remember { mutableStateOf(false) }
    var opening by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    val encrypted = remember(path) { mutableStateOf(false) }

    LaunchedEffect(path, password, reload) {
        error = null
        val res = withContext(Dispatchers.IO) {
            runCatching {
                encrypted.value = ArchiveManager.isEncrypted(file)
                ArchiveManager.list(file, password)
            }
        }
        res.onSuccess { entries = it }.onFailure { t ->
            if (t.javaClass.simpleName == "PasswordRequiredException") askPassword = true
            else error = t.message ?: t.javaClass.simpleName
        }
    }

    BackHandler {
        when {
            selection.isNotEmpty() -> selection.clear()
            dir.isNotEmpty() -> dir = if (dir.contains('/')) dir.substringBeforeLast('/') else ""
            else -> vm.pop()
        }
    }

    val all = entries
    val children = remember(all, dir) {
        all?.filter { it.parent == dir }?.sortedWith(compareBy<ArchiveEntry> { !it.isDirectory }.thenComparator { a, b -> FileRepository.naturalCompare(a.name, b.name) })
            ?: emptyList()
    }
    val totalSize = remember(all) { all?.filter { !it.isDirectory }?.sumOf { it.size.coerceAtLeast(0) } ?: 0L }

    fun extract(dest: File, only: Set<String>?) {
        val strip = if (only != null) dir else ""
        OperationManager.extract(file, dest, password, only, strip)
        selection.clear()
    }

    fun openEntry(e: ArchiveEntry) {
        if (e.isDirectory) { dir = e.path; return }
        opening = e.name
        scope.launch {
            val cache = File(context.cacheDir, "archive_preview/${file.name.hashCode()}")
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    cache.deleteRecursively(); cache.mkdirs()
                    ArchiveManager.extract(file, cache, password, setOf(e.path), e.parent)
                    File(cache, e.name).exists()
                }.getOrElse { t ->
                    if (t.javaClass.simpleName == "PasswordRequiredException") { askPassword = true }
                    false
                }
            }
            opening = null
            if (ok) {
                val out = File(cache, e.name)
                val entry = FileEntry.from(out, false)
                when {
                    entry.type == FileType.IMAGE -> vm.push(Screen.ImageViewer(out.absolutePath, listOf(out.absolutePath)))
                    FileType.isEditable(entry.name) && entry.size < 8L * 1024 * 1024 -> vm.push(Screen.TextEditor(out.absolutePath))
                    entry.type == FileType.ARCHIVE && ArchiveManager.isBrowsable(out) -> vm.push(Screen.ArchiveView(out.absolutePath))
                    else -> FileActions.openExternal(context, out)
                }
            } else if (!askPassword) vm.message("No se pudo abrir el archivo")
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (selection.isEmpty()) {
            SubTopBar(file.name, if (all == null) "Leyendo…" else "${all.count { !it.isDirectory }} archivos · ${Format.size(totalSize)} · ${Format.size(file.length())} comprimido",
                onBack = { vm.pop() }) {
                if (encrypted.value) Icon(Icons.Rounded.Lock, "Cifrado", Modifier.padding(end = 12.dp), tint = MaterialTheme.colorScheme.primary)
            }
        } else {
            SubTopBar("${selection.size} seleccionados", null, onBack = { selection.clear() }, background = MaterialTheme.colorScheme.primaryContainer) {
                IconButton({ if (selection.size == children.size) selection.clear() else { selection.clear(); selection.addAll(children.map { it.path }) } }) {
                    Icon(Icons.Rounded.SelectAll, "Todo")
                }
            }
        }
        // Path inside archive
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("/" + dir, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            if (dir.isNotEmpty()) IconButton({ dir = if (dir.contains('/')) dir.substringBeforeLast('/') else "" }) { Icon(Icons.Rounded.DriveFolderUpload, "Subir") }
        }
        if (all == null && error == null) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (opening != null) {
            Text("Abriendo $opening…", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        Box(Modifier.weight(1f)) {
            when {
                error != null -> EmptyState(Icons.Rounded.ErrorOutline, "No se pudo leer el archivo", error ?: "")
                all != null && children.isEmpty() -> EmptyState(Icons.Rounded.Unarchive, "Vacío")
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(children, key = { it.path }) { e ->
                        val sel = e.path in selection
                        val fe = remember(e) { FileEntry(e.path, e.name, e.isDirectory, e.size, e.time, false, -1) }
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 1.dp).clip(RoundedCornerShape(16.dp))
                                .background(if (sel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else androidx.compose.ui.graphics.Color.Transparent)
                                .combinedClickable(
                                    onClick = { if (selection.isNotEmpty()) { if (sel) selection.remove(e.path) else selection.add(e.path) } else openEntry(e) },
                                    onLongClick = { if (sel) selection.remove(e.path) else selection.add(e.path) },
                                ).padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SelectableThumb(fe.copy(path = ""), 44.dp, sel)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(e.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val sub = if (e.isDirectory) Format.items(all?.count { it.parent == e.path } ?: 0)
                                    else buildString {
                                        append(if (e.size >= 0) Format.size(e.size) else "?")
                                        if (e.compressedSize > 0 && e.size > 0) append(" · ${(100 - e.compressedSize * 100 / e.size.coerceAtLeast(1)).coerceAtLeast(0)}% compresión")
                                        if (e.time > 0) append(" · " + Format.date(e.time))
                                    }
                                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
        if (all != null) Surface(color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 6.dp) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val only = if (selection.isEmpty()) null else selection.toSet()
                FilledTonalButton({ pickDest = true }, Modifier.weight(1f)) {
                    Icon(Icons.Rounded.DriveFolderUpload, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Extraer en…", maxLines = 1)
                }
                Button({
                    val parent = file.parentFile ?: return@Button
                    val target = if (only == null) FileRepository.uniqueTarget(parent, file.name.substringBefore('.').ifEmpty { "extraido" }) else parent
                    extract(target, only)
                }, Modifier.weight(1f)) {
                    Icon(if (only == null) Icons.Rounded.CreateNewFolder else Icons.Rounded.Unarchive, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text(if (only == null) "Extraer todo" else "Extraer (${selection.size})", maxLines = 1)
                }
            }
        }
    }

    if (askPassword) PasswordDialog("Archivo protegido", "\"${file.name}\" está cifrado. Introduce la contraseña.", onDismiss = { askPassword = false; if (entries == null) vm.pop() }) {
        askPassword = false; password = it; reload++
    }
    if (pickDest) FolderPickerDialog("Extraer en…", file.parent ?: FileRepository.primaryRoot, vm.volumes, "Extraer aquí", onDismiss = { pickDest = false }) { dest ->
        pickDest = false
        extract(File(dest), if (selection.isEmpty()) null else selection.toSet())
    }
}

@Suppress("unused") private val unusedClose = Icons.Rounded.Close
