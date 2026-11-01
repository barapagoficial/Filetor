package com.filetor.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filetor.app.data.AnalysisResult
import com.filetor.app.data.Analyzer
import com.filetor.app.data.DuplicateGroup
import com.filetor.app.data.FileEntry
import com.filetor.app.data.FileRepository
import com.filetor.app.data.OperationManager
import com.filetor.app.data.Prefs
import com.filetor.app.data.TrashManager
import com.filetor.app.ui.AppViewModel
import com.filetor.app.ui.Screen
import com.filetor.app.ui.components.ActionDialog
import com.filetor.app.ui.components.ConfirmDialog
import com.filetor.app.ui.components.Donut
import com.filetor.app.ui.components.EmptyState
import com.filetor.app.ui.components.FileActionHost
import com.filetor.app.ui.components.FileRow
import com.filetor.app.ui.components.FileThumb
import com.filetor.app.ui.components.IconTile
import com.filetor.app.ui.components.SectionTitle
import com.filetor.app.ui.components.SoftCard
import com.filetor.app.ui.components.SubTopBar
import com.filetor.app.ui.components.TypeIconBox
import com.filetor.app.ui.components.UsageBar
import com.filetor.app.ui.components.rememberFileActions
import com.filetor.app.util.FileType
import com.filetor.app.util.Format
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ToolsScreen(vm: AppViewModel) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
        item {
            Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)) {
                Text("Herramientas", style = MaterialTheme.typography.headlineMedium)
                Text("Limpia, analiza y organiza tu almacenamiento", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            ToolCard(Icons.Rounded.Analytics, Color(0xFF6366F1), "Analizar almacenamiento", "Descubre qué ocupa espacio: carpetas, tipos y archivos grandes") { vm.push(Screen.Analyzer) }
        }
        item {
            ToolCard(Icons.Rounded.ContentCopy, Color(0xFFE5578A), "Archivos duplicados", "Encuentra copias idénticas comparando su contenido y libera espacio") { vm.push(Screen.Duplicates) }
        }
        item {
            ToolCard(Icons.Rounded.DeleteSweep, Color(0xFFF08A1C), "Papelera de reciclaje",
                if (TrashManager.items.isEmpty()) "Vacía" else "${TrashManager.items.size} elementos · ${Format.size(TrashManager.totalSize)}") { vm.push(Screen.Trash) }
        }
        item {
            ToolCard(Icons.AutoMirrored.Rounded.ManageSearch, Color(0xFF14A3A3), "Búsqueda avanzada", "Busca en todo el almacenamiento con comodines y filtros por tipo") {
                vm.push(Screen.Search(FileRepository.primaryRoot))
            }
        }
        item { ToolCard(Icons.Rounded.History, Color(0xFF2F80ED), "Abiertos recientemente", "${Prefs.recents.size} archivos en el historial") { vm.push(Screen.Recents) } }
        if (Prefs.bookmarks.isNotEmpty()) {
            item { SectionTitle("Favoritos") }
            items(Prefs.bookmarks.toList()) { p ->
                val f = File(p)
                val e = FileEntry.from(f, false)
                FileRow(e, false, onClick = { if (f.isDirectory) vm.openInFiles(p) else vm.openInFiles(f.parent ?: p) }, onLongClick = {},
                    subtitle = FileRepository.displayPath(vm.getApplication<android.app.Application>(), f.parent ?: ""),
                    trailing = { IconButton({ Prefs.toggleBookmark(p) }) { Icon(Icons.Rounded.Star, "Quitar", tint = Color(0xFFF5B400)) } })
            }
        }
        item { ToolCard(Icons.Rounded.Settings, Color(0xFF64748B), "Ajustes", "Tema, colores, papelera y comportamiento") { vm.push(Screen.Settings) } }
    }
}

@Composable
private fun ToolCard(icon: ImageVector, color: Color, title: String, subtitle: String, onClick: () -> Unit) {
    SoftCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), onClick = onClick) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, color, 52.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AnalyzerScreen(vm: AppViewModel) {
    val actions = rememberFileActions(vm)
    var result by remember { mutableStateOf<AnalysisResult?>(null) }
    var progress by remember { mutableStateOf("Preparando…") }
    var count by remember { mutableIntStateOf(0) }
    var reload by remember { mutableIntStateOf(0) }
    var volumeIdx by remember { mutableIntStateOf(0) }
    val vol = vm.volumes.getOrNull(volumeIdx)
    val root = vol?.path ?: FileRepository.primaryRoot
    actions.onChanged = { reload++ }
    LaunchedEffect(root, reload) {
        result = null
        result = Analyzer.analyze(root) { p, c -> progress = p; count = c }
    }
    LaunchedEffect(Unit) { OperationManager.events.collect { reload++ } }
    Column(Modifier.fillMaxSize()) {
        SubTopBar("Análisis de almacenamiento", vol?.label, onBack = { vm.pop() })
        val r = result
        if (r == null) {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Analizando…", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("$count archivos revisados", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth().clip(CircleShape))
                Spacer(Modifier.height(8.dp))
                Text(progress.removePrefix(root), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
            if (vm.volumes.size > 1) item {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.volumes.forEachIndexed { i, v -> FilterChip(i == volumeIdx, { volumeIdx = i }, label = { Text(v.label) }) }
                }
            }
            item {
                val types = r.byType.entries.sortedByDescending { it.value }
                SoftCard(Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(130.dp), contentAlignment = Alignment.Center) {
                                Donut(types.map { Format.percent(it.value, r.totalSize) to it.key.color }, Modifier.fillMaxSize())
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(Format.size(r.totalSize), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${r.fileCount} archivos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(Modifier.width(20.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                types.take(6).forEach { (t, size) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(10.dp).clip(CircleShape).background(t.color))
                                        Spacer(Modifier.width(8.dp))
                                        Text(t.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1)
                                        Text(Format.size(size), style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                        if (vol != null) {
                            Spacer(Modifier.height(16.dp))
                            Text("Espacio usado del volumen: ${Format.size(vol.used)} de ${Format.size(vol.total)}", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(6.dp))
                            UsageBar(Format.percent(vol.used, vol.total))
                            val other = (vol.used - r.totalSize).coerceAtLeast(0)
                            Text("Sistema y apps: ${Format.size(other)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            if (r.oldDownloads.isNotEmpty() || r.emptyFolders.isNotEmpty()) {
                item { SectionTitle("Sugerencias de limpieza") }
                if (r.oldDownloads.isNotEmpty()) item {
                    Suggestion(Icons.Rounded.CleaningServices, "Descargas antiguas", "${r.oldDownloads.size} archivos con más de 90 días · ${Format.size(r.oldDownloads.sumOf { it.size })}", "Revisar") {
                        actions.dialog = ActionDialog.Delete(r.oldDownloads)
                    }
                }
                if (r.emptyFolders.isNotEmpty()) item {
                    Suggestion(Icons.Rounded.FolderOpen, "Carpetas vacías", "${r.emptyFolders.size} carpetas sin contenido", "Eliminar") {
                        actions.dialog = ActionDialog.Delete(r.emptyFolders.map { FileEntry.from(File(it), false) })
                    }
                }
            }
            item { SectionTitle("Carpetas más grandes") }
            items(r.topFolders.take(12), key = { "f" + it.first.path }) { (e, size) ->
                Row(Modifier.fillMaxWidth().clickable { vm.openInFiles(e.path) }.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    TypeIconBox(FileType.FOLDER, 40.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row { Text(e.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); Text(Format.size(size), style = MaterialTheme.typography.labelLarge) }
                        Spacer(Modifier.height(6.dp))
                        UsageBar(Format.percent(size, r.topFolders.firstOrNull()?.second ?: 1), height = 6.dp)
                    }
                }
            }
            item { SectionTitle("Archivos más grandes") }
            items(r.largestFiles.take(30), key = { "l" + it.path }) { e ->
                FileRow(e, false, onClick = { actions.open(e) }, onLongClick = { actions.dialog = ActionDialog.Menu(e) },
                    subtitle = Format.size(e.size) + " · " + (File(e.path).parentFile?.name ?: ""),
                    trailing = { IconButton({ actions.dialog = ActionDialog.Delete(listOf(e)) }) { Icon(Icons.Rounded.Delete, "Eliminar", tint = MaterialTheme.colorScheme.onSurfaceVariant) } })
            }
        }
    }
    FileActionHost(actions)
}

@Composable
private fun Suggestion(icon: ImageVector, title: String, subtitle: String, action: String, onClick: () -> Unit) {
    SoftCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick) { Text(action) }
        }
    }
}

@Composable
fun DuplicatesScreen(vm: AppViewModel) {
    val actions = rememberFileActions(vm)
    var groups by remember { mutableStateOf<List<DuplicateGroup>?>(null) }
    var status by remember { mutableStateOf("Escaneando…") }
    var prog by remember { mutableFloatStateOf(-1f) }
    var minSize by remember { mutableStateOf(100L * 1024) }
    var reload by remember { mutableIntStateOf(0) }
    val marked = remember { mutableStateListOf<String>() }
    LaunchedEffect(minSize, reload) {
        groups = null; marked.clear()
        groups = Analyzer.findDuplicates(FileRepository.primaryRoot, minSize) { s, p -> status = s; prog = p }
    }
    LaunchedEffect(Unit) { OperationManager.events.collect { reload++ } }
    Column(Modifier.fillMaxSize()) {
        val g = groups
        SubTopBar("Archivos duplicados", g?.let { "${it.size} grupos · ${Format.size(it.sumOf { d -> d.wasted })} recuperables" }, onBack = { vm.pop() })
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0L to "Todos", 100L * 1024 to "> 100 KB", 1024L * 1024 to "> 1 MB", 10L * 1024 * 1024 to "> 10 MB").forEach { (v, l) ->
                FilterChip(minSize == v, { minSize = v }, label = { Text(l) })
            }
        }
        when {
            g == null -> Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(status, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                if (prog < 0) LinearProgressIndicator(Modifier.fillMaxWidth()) else LinearProgressIndicator(progress = { prog }, modifier = Modifier.fillMaxWidth())
            }
            g.isEmpty() -> EmptyState(Icons.Rounded.CheckCircle, "¡Sin duplicados!", "No se encontraron archivos repetidos")
            else -> {
                Box(Modifier.weight(1f)) {
                    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                        item {
                            TextButton({
                                marked.clear()
                                g.forEach { grp -> grp.files.drop(1).forEach { marked.add(it.path) } }
                            }, Modifier.padding(horizontal = 12.dp)) { Text("Marcar todas las copias (conservar la más antigua)") }
                        }
                        g.forEachIndexed { gi, grp ->
                            item(key = "h$gi") {
                                Text("${grp.files.size} copias · ${Format.size(grp.size)} cada una", style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp))
                            }
                            items(grp.files, key = { "d$gi" + it.path }) { e ->
                                Row(Modifier.fillMaxWidth().clickable { if (e.path in marked) marked.remove(e.path) else marked.add(e.path) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(e.path in marked, { if (it) marked.add(e.path) else marked.remove(e.path) })
                                    FileThumb(e, 40.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(e.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(FileRepository.displayPath(vm.getApplication<android.app.Application>(), File(e.path).parent ?: "") + " · " + Format.date(e.lastModified),
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
                if (marked.isNotEmpty()) Surface(color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 6.dp) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        val all = g.flatMap { it.files }
                        val sel = all.filter { it.path in marked }.distinctBy { it.path }
                        Text("${sel.size} marcados · ${Format.size(sel.sumOf { it.size })}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                        Button({ actions.dialog = ActionDialog.Delete(sel) }) { Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Eliminar") }
                    }
                }
            }
        }
    }
    FileActionHost(actions)
}

@Composable
fun TrashScreen(vm: AppViewModel) {
    val scope = rememberCoroutineScope()
    var confirmEmpty by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { TrashManager.load() }
    Column(Modifier.fillMaxSize()) {
        SubTopBar("Papelera", if (TrashManager.items.isEmpty()) "Vacía" else "${TrashManager.items.size} elementos · ${Format.size(TrashManager.totalSize)}", onBack = { vm.pop() }) {
            if (TrashManager.items.isNotEmpty()) TextButton({ confirmEmpty = true }) { Text("Vaciar") }
        }
        if (TrashManager.items.isEmpty()) {
            EmptyState(Icons.Rounded.Delete, "La papelera está vacía",
                if (Prefs.useTrash) "Los elementos eliminados se guardan aquí ${Prefs.trashDays} días antes de borrarse definitivamente" else "La papelera está desactivada en Ajustes")
        } else LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
            items(TrashManager.items.toList(), key = { it.id }) { t ->
                val e = FileEntry(t.trashedPath, t.name, t.isDirectory, t.size, t.deletedAt, false)
                val daysLeft = if (Prefs.trashDays > 0) Prefs.trashDays - ((System.currentTimeMillis() - t.deletedAt) / 86_400_000L).toInt() else -1
                FileRow(e, false, onClick = {}, onLongClick = {},
                    subtitle = "${Format.size(t.size)} · ${File(t.originalPath).parentFile?.name ?: ""}" + if (daysLeft >= 0) " · quedan $daysLeft días" else "",
                    trailing = {
                        IconButton({ scope.launch { val ok = TrashManager.restore(t); vm.message(if (ok) "Restaurado: ${t.name}" else "No se pudo restaurar"); vm.load() } }) {
                            Icon(Icons.Rounded.Restore, "Restaurar", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton({ scope.launch { TrashManager.delete(t) } }) { Icon(Icons.Rounded.DeleteForever, "Eliminar", tint = MaterialTheme.colorScheme.error) }
                    })
            }
        }
    }
    if (confirmEmpty) ConfirmDialog("Vaciar papelera", "Se eliminarán ${TrashManager.items.size} elementos de forma permanente.", "Vaciar", destructive = true,
        onDismiss = { confirmEmpty = false }) { confirmEmpty = false; scope.launch { TrashManager.empty(); vm.message("Papelera vaciada"); vm.refreshVolumes() } }
}

@Suppress("unused") private val keepStorage = Icons.Rounded.Storage
