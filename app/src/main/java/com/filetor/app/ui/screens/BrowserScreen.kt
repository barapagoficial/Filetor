package com.filetor.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.FileCopy
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filetor.app.data.ArchiveManager
import com.filetor.app.data.FileEntry
import com.filetor.app.data.FileRepository
import com.filetor.app.data.Prefs
import com.filetor.app.data.ViewMode
import com.filetor.app.ui.AppViewModel
import com.filetor.app.ui.BrowserTab
import com.filetor.app.ui.Screen
import com.filetor.app.ui.components.ActionDialog
import com.filetor.app.ui.components.EmptyState
import com.filetor.app.ui.components.FileActionHost
import com.filetor.app.ui.components.FileGridItem
import com.filetor.app.ui.components.FileRow
import com.filetor.app.ui.components.SortSheet
import com.filetor.app.ui.components.rememberFileActions
import com.filetor.app.util.FileActions
import com.filetor.app.util.FileType
import com.filetor.app.util.Format
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun BrowserScreen(vm: AppViewModel) {
    val tab = vm.tab
    val context = LocalContext.current
    val actions = rememberFileActions(vm)
    actions.onDirectory = { vm.navigate(it) }
    actions.onChanged = { vm.load(tab, keepSelection = true) }
    var showSort by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var filterMode by remember { mutableStateOf(false) }

    LaunchedEffect(tab) { if (tab.entries.isEmpty() && !tab.loading) vm.load(tab) }
    LaunchedEffect(Prefs.showHidden, Prefs.sortBy, Prefs.sortAsc, Prefs.foldersFirst) { vm.load(tab, keepSelection = true) }

    BackHandler(enabled = vm.mainTab == 1 && vm.stack.isEmpty()) {
        if (filterMode) { filterMode = false; tab.filter = "" }
        else if (!vm.back(tab)) vm.mainTab = 0
    }

    val selecting = tab.selection.isNotEmpty()
    val visible = remember(tab.entries, tab.filter) {
        if (tab.filter.isBlank()) tab.entries else tab.entries.filter { it.name.contains(tab.filter.trim(), ignoreCase = true) }
    }
    val selectedEntries = { tab.entries.filter { it.path in tab.selection } }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // ---------- Top bar ----------
            AnimatedContent(selecting, label = "topbar", transitionSpec = { fadeIn() togetherWith fadeOut() }) { sel ->
                if (sel) {
                    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).statusBarsPadding().height(64.dp).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        IconButton({ tab.selection.clear() }) { Icon(Icons.Rounded.Close, "Cancelar", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                        Column(Modifier.weight(1f)) {
                            Text("${tab.selection.size} seleccionados", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            val sz = selectedEntries().filter { !it.isDirectory }.sumOf { it.size }
                            if (sz > 0) Text(Format.size(sz), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                        IconButton({
                            if (tab.selection.size == visible.size) tab.selection.clear()
                            else { tab.selection.clear(); tab.selection.addAll(visible.map { it.path }) }
                        }) { Icon(Icons.Rounded.SelectAll, "Seleccionar todo", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                        IconButton({ val s = tab.selection.toSet(); tab.selection.clear(); tab.selection.addAll(visible.map { it.path }.filter { it !in s }) }) {
                            Icon(Icons.Rounded.FilterList, "Invertir selección", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                } else {
                    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).statusBarsPadding()) {
                        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton({ if (!vm.goUp(tab)) vm.mainTab = 0 }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Subir") }
                            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                                val vol = vm.volumes.firstOrNull { it.path == tab.path }
                                Text(vol?.label ?: tab.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (tab.loading) "Cargando…" else "${tab.entries.count { it.isDirectory }} carpetas · ${tab.entries.count { !it.isDirectory }} archivos",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton({ filterMode = !filterMode; if (!filterMode) tab.filter = "" }) { Icon(Icons.Rounded.FilterList, "Filtrar") }
                            IconButton({ vm.push(Screen.Search(tab.path)) }) { Icon(Icons.Rounded.Search, "Buscar") }
                            Box {
                                IconButton({ menu = true }) { Icon(Icons.Rounded.MoreVert, "Más") }
                                DropdownMenu(menu, { menu = false }) {
                                    DropdownMenuItem({ Text(if (Prefs.viewMode == ViewMode.LIST) "Vista de cuadrícula" else "Vista de lista") },
                                        { menu = false; Prefs.setView(if (Prefs.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST) },
                                        leadingIcon = { Icon(if (Prefs.viewMode == ViewMode.LIST) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.ViewList, null) })
                                    DropdownMenuItem({ Text("Ordenar") }, { menu = false; showSort = true }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Sort, null) })
                                    DropdownMenuItem({ Text(if (Prefs.showHidden) "Ocultar ocultos" else "Mostrar ocultos") }, { menu = false; Prefs.setHidden(!Prefs.showHidden) },
                                        leadingIcon = { Icon(if (Prefs.showHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null) })
                                    DropdownMenuItem({ Text("Nueva pestaña") }, { menu = false; vm.addTab(tab.path) }, leadingIcon = { Icon(Icons.Rounded.Tab, null) })
                                    val fav = Prefs.bookmarks.contains(tab.path)
                                    DropdownMenuItem({ Text(if (fav) "Quitar de favoritos" else "Añadir a favoritos") }, { menu = false; Prefs.toggleBookmark(tab.path) },
                                        leadingIcon = { Icon(if (fav) Icons.Rounded.Star else Icons.Rounded.StarBorder, null) })
                                    DropdownMenuItem({ Text("Seleccionar todo") }, { menu = false; tab.selection.clear(); tab.selection.addAll(visible.map { it.path }) },
                                        leadingIcon = { Icon(Icons.Rounded.SelectAll, null) })
                                    DropdownMenuItem({ Text("Actualizar") }, { menu = false; vm.load(tab) }, leadingIcon = { Icon(Icons.Rounded.Refresh, null) })
                                    DropdownMenuItem({ Text("Propiedades de la carpeta") }, { menu = false; actions.dialog = ActionDialog.Properties(listOf(FileEntry.from(File(tab.path)))) },
                                        leadingIcon = { Icon(Icons.Rounded.Info, null) })
                                }
                            }
                        }
                    }
                }
            }
            // ---------- Tabs ----------
            if (vm.tabs.size > 1) TabsStrip(vm)
            // ---------- Breadcrumbs ----------
            Breadcrumbs(vm, tab)
            AnimatedVisibility(filterMode) {
                OutlinedTextField(
                    tab.filter, { tab.filter = it }, singleLine = true, placeholder = { Text("Filtrar en esta carpeta…") },
                    leadingIcon = { Icon(Icons.Rounded.FilterList, null) },
                    trailingIcon = { if (tab.filter.isNotEmpty()) IconButton({ tab.filter = "" }) { Icon(Icons.Rounded.Close, null) } },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (tab.loading) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp)) else Spacer(Modifier.height(2.dp))

            // ---------- Content ----------
            Box(Modifier.weight(1f)) {
                when {
                    tab.error != null -> EmptyState(Icons.Rounded.Block, "No se puede acceder", tab.error ?: "")
                    !tab.loading && visible.isEmpty() -> EmptyState(Icons.Rounded.FolderOff,
                        if (tab.filter.isNotEmpty()) "Sin coincidencias" else "Carpeta vacía",
                        if (tab.filter.isNotEmpty()) "Ningún elemento contiene \"${tab.filter}\"" else "Usa el botón + para crear algo nuevo")
                    else -> FileListContent(vm, tab, visible, actions)
                }
            }

            // ---------- Clipboard bar ----------
            val clip = vm.clipboard
            AnimatedVisibility(clip != null && !selecting, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                if (clip != null) Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (clip.cut) Icons.Rounded.ContentCut else Icons.Rounded.ContentCopy, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (clip.cut) "Mover ${clip.paths.size} elemento(s)" else "Copiar ${clip.paths.size} elemento(s)",
                                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text(clip.paths.joinToString { File(it).name }, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                        }
                        TextButton({ vm.clipboard = null }) { Text("Cancelar") }
                        FilledTonalButton({ vm.paste(tab.path) }) {
                            Icon(Icons.Rounded.ContentPaste, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Pegar")
                        }
                    }
                }
            }

            // ---------- Selection actions ----------
            AnimatedVisibility(selecting, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                SelectionBar(vm, tab, actions, selectedEntries)
            }
        }

        // ---------- FAB ----------
        AnimatedVisibility(!selecting && vm.clipboard == null && !vm.pickMode, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it }) {
            ExtendedFloatingActionButton(
                onClick = { actions.dialog = ActionDialog.NewItem(tab.path) },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("Nuevo") },
                shape = RoundedCornerShape(20.dp),
            )
        }
    }

    if (showSort) SortSheet { showSort = false }
    FileActionHost(actions)
}

@Composable
private fun FileListContent(vm: AppViewModel, tab: BrowserTab, visible: List<FileEntry>, actions: com.filetor.app.ui.components.FileActionState) {
    val saved = tab.scroll[tab.path]
    val onClick: (FileEntry) -> Unit = { e ->
        if (tab.selection.isNotEmpty()) {
            if (e.path in tab.selection) tab.selection.remove(e.path) else tab.selection.add(e.path)
        } else actions.open(e, visible)
    }
    val onLong: (FileEntry) -> Unit = { e ->
        if (e.path in tab.selection) tab.selection.remove(e.path) else tab.selection.add(e.path)
    }
    if (Prefs.viewMode == ViewMode.LIST) {
        val state = remember(tab.path) { androidx.compose.foundation.lazy.LazyListState(saved?.first ?: 0, saved?.second ?: 0) }
        LaunchedEffect(state, tab.path) {
            snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }.collect { tab.scroll[tab.path] = it }
        }
        LazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)) {
            items(visible, key = { it.path }) { e ->
                FileRow(
                    e, selected = e.path in tab.selection,
                    onClick = { onClick(e) }, onLongClick = { onLong(e) },
                    bookmarked = Prefs.bookmarks.contains(e.path),
                    modifier = Modifier.animateItem(),
                    trailing = {
                        if (tab.selection.isEmpty()) IconButton({ actions.dialog = ActionDialog.Menu(e) }) {
                            Icon(Icons.Rounded.MoreVert, "Opciones", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                )
            }
        }
    } else {
        val state = remember(tab.path) { androidx.compose.foundation.lazy.grid.LazyGridState(saved?.first ?: 0, saved?.second ?: 0) }
        LaunchedEffect(state, tab.path) {
            snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }.collect { tab.scroll[tab.path] = it }
        }
        LazyVerticalGrid(GridCells.Adaptive(104.dp), state = state, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 120.dp)) {
            items(visible, key = { it.path }) { e ->
                FileGridItem(e, e.path in tab.selection, onClick = { onClick(e) }, onLongClick = {
                    if (tab.selection.isEmpty()) actions.dialog = ActionDialog.Menu(e) else onLong(e)
                })
            }
        }
    }
}

@Composable
private fun TabsStrip(vm: AppViewModel) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        vm.tabs.forEachIndexed { i, t ->
            val sel = i == vm.currentTab
            Row(
                Modifier.clip(RoundedCornerShape(12.dp))
                    .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { vm.currentTab = i }
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(t.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(end = 2.dp))
                IconButton({ vm.closeTab(i) }, Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, "Cerrar", Modifier.size(16.dp), tint = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Box(Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable { vm.addTab(vm.tab.path) }.padding(8.dp)) {
            Icon(Icons.Rounded.Add, "Nueva pestaña", Modifier.size(18.dp))
        }
    }
}

@Composable
private fun Breadcrumbs(vm: AppViewModel, tab: BrowserTab) {
    val vol = vm.volumes.filter { tab.path.startsWith(it.path) }.maxByOrNull { it.path.length }
    val rootPath = vol?.path ?: "/"
    val rootLabel = when { vol == null -> "/"; vol.isPrimary -> "Interno"; else -> vol.label }
    val parts = tab.path.removePrefix(rootPath).split('/').filter { it.isNotEmpty() }
    val scroll = rememberScrollState()
    LaunchedEffect(tab.path) { scroll.animateScrollTo(scroll.maxValue) }
    LaunchedEffect(scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }
    Row(Modifier.fillMaxWidth().horizontalScroll(scroll).padding(horizontal = 12.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Crumb(rootLabel, parts.isEmpty()) { vm.navigate(rootPath, tab) }
        var acc = rootPath
        parts.forEachIndexed { i, p ->
            acc = "$acc/$p"
            val target = acc
            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Crumb(p, i == parts.size - 1) { vm.navigate(target, tab) }
        }
    }
}

@Composable
private fun Crumb(label: String, current: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
        color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .background(if (current) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun SelectionBar(vm: AppViewModel, tab: BrowserTab, actions: com.filetor.app.ui.components.FileActionState, selected: () -> List<FileEntry>) {
    val context = LocalContext.current
    var more by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 6.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            BarAction(Icons.Rounded.ContentCopy, "Copiar") { vm.copySelection(false) }
            BarAction(Icons.Rounded.ContentCut, "Cortar") { vm.copySelection(true) }
            BarAction(Icons.Rounded.Delete, "Eliminar") { actions.dialog = ActionDialog.Delete(selected()) }
            BarAction(Icons.Rounded.Share, "Compartir") { FileActions.share(context, selected().map { it.file }) }
            Box {
                BarAction(Icons.Rounded.MoreVert, "Más") { more = true }
                DropdownMenu(more, { more = false }) {
                    val sel = selected()
                    if (vm.pickMode && sel.none { it.isDirectory }) {
                        DropdownMenuItem({ Text("Seleccionar archivos") }, { more = false; vm.onPick?.invoke(sel.map { it.file }) }, leadingIcon = { Icon(Icons.Rounded.Check, null) })
                    }
                    DropdownMenuItem({ Text(if (sel.size == 1) "Renombrar" else "Renombrar en lote") }, {
                        more = false
                        actions.dialog = if (sel.size == 1) ActionDialog.Rename(sel[0]) else ActionDialog.BatchRename(sel)
                    }, leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, null) })
                    DropdownMenuItem({ Text("Comprimir") }, { more = false; actions.dialog = ActionDialog.Compress(sel, tab.path) }, leadingIcon = { Icon(Icons.Rounded.Compress, null) })
                    DropdownMenuItem({ Text("Copiar a…") }, { more = false; actions.dialog = ActionDialog.PickFolder(sel, false) }, leadingIcon = { Icon(Icons.Rounded.FileCopy, null) })
                    DropdownMenuItem({ Text("Mover a…") }, { more = false; actions.dialog = ActionDialog.PickFolder(sel, true) }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.DriveFileMove, null) })
                    if (sel.size == 1 && sel[0].type == FileType.ARCHIVE && ArchiveManager.isSupported(sel[0].file)) {
                        DropdownMenuItem({ Text("Extraer aquí") }, { more = false; tab.selection.clear(); actions.extract(sel[0].file, 0) }, leadingIcon = { Icon(Icons.Rounded.Compress, null) })
                    }
                    DropdownMenuItem({ Text("Añadir a favoritos") }, { more = false; sel.forEach { if (!Prefs.bookmarks.contains(it.path)) Prefs.toggleBookmark(it.path) }; tab.selection.clear() },
                        leadingIcon = { Icon(Icons.Rounded.Star, null) })
                    DropdownMenuItem({ Text("Propiedades") }, { more = false; actions.dialog = ActionDialog.Properties(sel) }, leadingIcon = { Icon(Icons.Rounded.Info, null) })
                }
            }
        }
    }
}

@Composable
fun BarAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

