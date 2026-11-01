package com.filetor.app.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileCopy
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DriveFolderUpload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filetor.app.data.ArchiveManager
import com.filetor.app.data.FileEntry
import com.filetor.app.data.FileRepository
import com.filetor.app.data.OperationManager
import com.filetor.app.data.Prefs
import com.filetor.app.ui.AppViewModel
import com.filetor.app.ui.Screen
import com.filetor.app.util.FileActions
import com.filetor.app.util.FileType
import java.io.File

sealed interface ActionDialog {
    data class Menu(val entry: FileEntry) : ActionDialog
    data class Rename(val entry: FileEntry) : ActionDialog
    data class BatchRename(val entries: List<FileEntry>) : ActionDialog
    data class Delete(val entries: List<FileEntry>) : ActionDialog
    data class Compress(val entries: List<FileEntry>, val destDir: String) : ActionDialog
    data class Properties(val entries: List<FileEntry>) : ActionDialog
    data class PickFolder(val entries: List<FileEntry>, val move: Boolean) : ActionDialog
    data class ExtractTo(val archive: File) : ActionDialog
    data class Apk(val file: File) : ActionDialog
    data class NewItem(val dir: String) : ActionDialog
}

@Stable
class FileActionState(val vm: AppViewModel, private val context: Context) {
    var dialog by mutableStateOf<ActionDialog?>(null)
    var onDirectory: (String) -> Unit = { vm.openInFiles(it) }
    var onChanged: () -> Unit = {}

    fun open(entry: FileEntry, siblings: List<FileEntry> = emptyList(), forceExternal: Boolean = false) {
        val f = entry.file
        if (entry.isDirectory) { onDirectory(entry.path); return }
        if (vm.pickMode) { vm.onPick?.invoke(listOf(f)); return }
        Prefs.addRecent(entry.path)
        if (forceExternal) { FileActions.openExternal(context, f, forceChooser = true); return }
        when {
            entry.type == FileType.APK -> dialog = ActionDialog.Apk(f)
            entry.type == FileType.ARCHIVE && ArchiveManager.isBrowsable(f) -> vm.push(Screen.ArchiveView(entry.path))
            entry.type == FileType.IMAGE -> {
                val imgs = siblings.filter { it.type == FileType.IMAGE }.map { it.path }
                vm.push(Screen.ImageViewer(entry.path, imgs.ifEmpty { listOf(entry.path) }))
            }
            FileType.isEditable(entry.name) && entry.size < 8L * 1024 * 1024 -> vm.push(Screen.TextEditor(entry.path))
            else -> FileActions.openExternal(context, f)
        }
    }

    fun extract(archive: File, mode: Int) {
        val parent = archive.parentFile ?: return
        when (mode) {
            0 -> OperationManager.extract(archive, parent, null)
            1 -> {
                val name = archive.name.removeSuffix(".gz").removeSuffix(".bz2").removeSuffix(".xz").substringBeforeLast('.').removeSuffix(".tar")
                OperationManager.extract(archive, FileRepository.uniqueTarget(parent, name.ifEmpty { "extraido" }), null)
            }
            else -> dialog = ActionDialog.ExtractTo(archive)
        }
    }
}

@Composable
fun rememberFileActions(vm: AppViewModel): FileActionState {
    val context = LocalContext.current
    return remember(vm) { FileActionState(vm, context.applicationContext) }
}

@Composable
fun FileActionHost(state: FileActionState) {
    val vm = state.vm
    val context = LocalContext.current
    val close = { state.dialog = null }
    when (val d = state.dialog) {
        null -> {}
        is ActionDialog.Menu -> ItemMenuSheet(state, d.entry, close)
        is ActionDialog.Rename -> TextInputDialog("Renombrar", d.entry.name, confirmText = "Renombrar", onDismiss = close) { name ->
            close()
            val target = File(d.entry.file.parentFile, name)
            if (name == d.entry.name) return@TextInputDialog
            if (target.exists() && !target.absolutePath.equals(d.entry.path, ignoreCase = true)) { vm.message("Ya existe un elemento con ese nombre"); return@TextInputDialog }
            if (d.entry.file.renameTo(target)) {
                Prefs.renamePath(d.entry.path, target.absolutePath)
                vm.message("Renombrado a $name"); state.onChanged()
                runCatching { android.media.MediaScannerConnection.scanFile(context, arrayOf(d.entry.path, target.absolutePath), null, null) }
            } else vm.message("No se pudo renombrar")
        }
        is ActionDialog.BatchRename -> BatchRenameDialog(d.entries, onDismiss = close) { pairs ->
            close()
            var ok = 0
            pairs.forEach { (f, n) ->
                val t = File(f.parentFile, n)
                if (t.absolutePath == f.absolutePath || (!t.exists() && f.renameTo(t))) ok++
            }
            vm.message("$ok de ${pairs.size} renombrados"); state.onChanged()
        }
        is ActionDialog.Delete -> {
            val n = d.entries.size
            val what = if (n == 1) "\"${d.entries[0].name}\"" else "$n elementos"
            val inTrash = d.entries.any { it.path.contains("/.FiletorTrash") }
            if (!Prefs.confirmDelete) androidx.compose.runtime.LaunchedEffect(d) {
                close()
                OperationManager.delete(d.entries.map { it.file }, toTrash = Prefs.useTrash && !inTrash)
                vm.tabs.forEach { it.selection.clear() }
            } else ConfirmDialog(
                title = "Eliminar",
                text = if (Prefs.useTrash && !inTrash) "¿Mover $what a la papelera? Podrás restaurarlo durante ${Prefs.trashDays} días." else "¿Eliminar $what de forma permanente? Esta acción no se puede deshacer.",
                confirmText = "Eliminar",
                destructive = true,
                checkboxLabel = if (Prefs.useTrash && !inTrash) "Eliminar permanentemente" else null,
                onDismiss = close,
            ) { permanent ->
                close()
                OperationManager.delete(d.entries.map { it.file }, toTrash = Prefs.useTrash && !inTrash && !permanent)
                vm.tabs.forEach { it.selection.clear() }
            }
        }
        is ActionDialog.Compress -> {
            val default = if (d.entries.size == 1) d.entries[0].name.substringBeforeLast('.').ifEmpty { d.entries[0].name } else File(d.destDir).name.ifEmpty { "archivo" }
            CompressDialog(default, onDismiss = close) { name, format, level, pw ->
                close()
                val dest = FileRepository.uniqueTarget(File(d.destDir), name)
                OperationManager.compress(d.entries.map { it.file }, dest, format, level, pw)
                vm.tabs.forEach { it.selection.clear() }
            }
        }
        is ActionDialog.Properties -> PropertiesSheet(d.entries, close)
        is ActionDialog.PickFolder -> FolderPickerDialog(
            title = if (d.move) "Mover a…" else "Copiar a…",
            start = d.entries.firstOrNull()?.file?.parent ?: FileRepository.primaryRoot,
            volumes = vm.volumes,
            confirmText = if (d.move) "Mover aquí" else "Copiar aquí",
            onDismiss = close,
        ) { dest ->
            close()
            OperationManager.copy(d.entries.map { it.file }, File(dest), d.move)
            vm.tabs.forEach { it.selection.clear() }
        }
        is ActionDialog.ExtractTo -> FolderPickerDialog("Extraer en…", d.archive.parent ?: FileRepository.primaryRoot, vm.volumes, "Extraer aquí", onDismiss = close) { dest ->
            close(); OperationManager.extract(d.archive, File(dest), null)
        }
        is ActionDialog.Apk -> ApkSheet(d.file, close) { close(); vm.push(Screen.ArchiveView(d.file.absolutePath)) }
        is ActionDialog.NewItem -> NewItemDialog(onDismiss = close) { name, folder ->
            close()
            val f = File(d.dir, name)
            if (f.exists()) { vm.message("Ya existe \"$name\""); return@NewItemDialog }
            val ok = if (folder) f.mkdirs() else runCatching { f.createNewFile() }.getOrDefault(false)
            vm.message(if (ok) (if (folder) "Carpeta creada" else "Archivo creado") else "No se pudo crear")
            state.onChanged()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemMenuSheet(state: FileActionState, entry: FileEntry, close: () -> Unit) {
    val vm = state.vm
    val context = LocalContext.current
    val f = entry.file
    val isArchive = entry.type == FileType.ARCHIVE && ArchiveManager.isSupported(f)
    ModalBottomSheet(onDismissRequest = close, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                FileThumb(entry, 48.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(subtitleFor(entry), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            fun act(block: () -> Unit): () -> Unit = { close(); block() }
            if (isArchive) {
                MenuRow(Icons.Rounded.Unarchive, "Extraer aquí", act { state.extract(f, 0) })
                MenuRow(Icons.Rounded.CreateNewFolder, "Extraer en \"${f.name.substringBefore('.')}/\"", act { state.extract(f, 1) })
                MenuRow(Icons.Rounded.DriveFolderUpload, "Extraer en…", act { state.extract(f, 2) })
                if (ArchiveManager.isBrowsable(f)) MenuRow(Icons.Rounded.FolderOpen, "Ver contenido", act { vm.push(Screen.ArchiveView(entry.path)) })
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
            }
            if (entry.isDirectory) {
                MenuRow(Icons.Rounded.Tab, "Abrir en pestaña nueva", act { vm.openInFiles(entry.path, newTab = true) })
            } else {
                MenuRow(Icons.AutoMirrored.Rounded.OpenInNew, "Abrir con…", act { state.open(entry, forceExternal = true) })
                if (FileType.isEditable(entry.name) || entry.type == FileType.OTHER) MenuRow(Icons.Rounded.Edit, "Editar como texto", act { vm.push(Screen.TextEditor(entry.path)) })
                MenuRow(Icons.Rounded.Share, "Compartir", act { FileActions.share(context, listOf(f)) })
            }
            MenuRow(Icons.Rounded.ContentCopy, "Copiar", act { vm.copySelection(false, listOf(entry.path)) })
            MenuRow(Icons.Rounded.ContentCut, "Cortar", act { vm.copySelection(true, listOf(entry.path)) })
            MenuRow(Icons.Rounded.FileCopy, "Copiar a…", act { state.dialog = ActionDialog.PickFolder(listOf(entry), false) })
            MenuRow(Icons.AutoMirrored.Rounded.DriveFileMove, "Mover a…", act { state.dialog = ActionDialog.PickFolder(listOf(entry), true) })
            MenuRow(Icons.Rounded.DriveFileRenameOutline, "Renombrar", act { state.dialog = ActionDialog.Rename(entry) })
            MenuRow(Icons.Rounded.Compress, "Comprimir", act { state.dialog = ActionDialog.Compress(listOf(entry), f.parent ?: FileRepository.primaryRoot) })
            val fav = Prefs.bookmarks.contains(entry.path)
            MenuRow(if (fav) Icons.Rounded.Star else Icons.Rounded.StarBorder, if (fav) "Quitar de favoritos" else "Añadir a favoritos", act { Prefs.toggleBookmark(entry.path) })
            MenuRow(Icons.Rounded.Link, "Copiar ruta", act { FileActions.copyText(context, "Ruta", entry.path) })
            MenuRow(Icons.Rounded.Info, "Propiedades", act { state.dialog = ActionDialog.Properties(listOf(entry)) })
            MenuRow(Icons.Rounded.Delete, "Eliminar", act { state.dialog = ActionDialog.Delete(listOf(entry)) }, MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (tint == MaterialTheme.colorScheme.onSurface) MaterialTheme.colorScheme.onSurfaceVariant else tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
