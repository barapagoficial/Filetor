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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filetor.app.data.Category
import com.filetor.app.data.FileEntry
import com.filetor.app.data.FileRepository
import com.filetor.app.data.Prefs
import com.filetor.app.data.StorageVolumeInfo
import com.filetor.app.ui.AppViewModel
import com.filetor.app.ui.Screen
import com.filetor.app.ui.components.FileThumb
import com.filetor.app.ui.components.IconTile
import com.filetor.app.ui.components.SectionTitle
import com.filetor.app.ui.components.UsageBar
import com.filetor.app.util.FileType
import com.filetor.app.util.Format
import java.io.File

@Composable
fun HomeScreen(vm: AppViewModel, onOpenFile: (FileEntry) -> Unit) {
    LaunchedEffect(Unit) { vm.refreshVolumes() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Filetor", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Tus archivos, en orden", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton({ vm.push(Screen.Search(FileRepository.primaryRoot)) }) { Icon(Icons.Rounded.Search, "Buscar") }
                IconButton({ vm.push(Screen.Settings) }) { Icon(Icons.Rounded.Settings, "Ajustes") }
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                items(vm.volumes, key = { it.path }) { v ->
                    StorageCard(v, if (vm.volumes.size == 1) Modifier.fillParentMaxWidth() else Modifier.width(300.dp)) { vm.openInFiles(v.path) }
                }
            }
        }
        item { SectionTitle("Categorías") }
        item { CategoriesGrid(vm) }
        item { SectionTitle("Acceso rápido") }
        item {
            val root = FileRepository.primaryRoot
            val quick = listOf(
                Triple("Descargas", "$root/Download", Icons.Rounded.Download),
                Triple("Cámara", "$root/DCIM", Icons.Rounded.CameraAlt),
                Triple("Imágenes", "$root/Pictures", Icons.Rounded.Photo),
                Triple("Documentos", "$root/Documents", Icons.Rounded.Description),
                Triple("Música", "$root/Music", Icons.Rounded.MusicNote),
                Triple("Películas", "$root/Movies", Icons.Rounded.Movie),
            ).filter { File(it.second).exists() }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(quick) { (label, path, icon) -> QuickChip(label, icon) { vm.openInFiles(path) } }
            }
        }
        if (Prefs.bookmarks.isNotEmpty()) {
            item { SectionTitle("Favoritos") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(Prefs.bookmarks.toList()) { p ->
                        val f = File(p)
                        QuickChip(f.name.ifEmpty { p }, if (f.isDirectory) Icons.Rounded.Star else FileType.of(f.name).icon) {
                            if (f.isDirectory) vm.openInFiles(p) else onOpenFile(FileEntry.from(f, false))
                        }
                    }
                }
            }
        }
        val recents = Prefs.recents.map { File(it) }.filter { it.exists() }.take(8)
        if (recents.isNotEmpty()) {
            item { SectionTitle("Abiertos recientemente", action = "Ver todo") { vm.push(Screen.Recents) } }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recents, key = { it.absolutePath }) { f ->
                        val e = FileEntry.from(f, false)
                        Column(
                            Modifier.width(96.dp).clip(RoundedCornerShape(16.dp)).clickable { onOpenFile(e) }.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            FileThumb(e, 84.dp)
                            Spacer(Modifier.height(6.dp))
                            Text(e.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageCard(v: StorageVolumeInfo, modifier: Modifier, onClick: () -> Unit) {
    val dark = com.filetor.app.ui.theme.isAppInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary.let { if (dark) lerpColor(it, Color.Black, 0.5f) else it }
    val tertiary = MaterialTheme.colorScheme.tertiary.let { if (dark) lerpColor(it, Color.Black, 0.5f) else it }
    val fraction = Format.percent(v.used, v.total)
    Box(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(primary, lerpColor(primary, tertiary, 0.55f))))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(
                        when { v.isPrimary -> Icons.Rounded.PhoneAndroid; v.label.contains("USB", true) -> Icons.Rounded.Usb; else -> Icons.Rounded.SdStorage },
                        null, tint = Color.White,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(v.label, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${Format.size(v.free)} libres", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = Color.White)
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${(fraction * 100).toInt()}%", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("${Format.size(v.used)} de ${Format.size(v.total)}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.padding(bottom = 6.dp))
            }
            Spacer(Modifier.height(10.dp))
            UsageBar(fraction, color = Color.White, warnWhenFull = false)
        }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t)


private data class CatItem(val category: Category, val icon: ImageVector, val color: Color)

@Composable
private fun CategoriesGrid(vm: AppViewModel) {
    val items = listOf(
        CatItem(Category.IMAGES, Icons.Rounded.Image, FileType.IMAGE.color),
        CatItem(Category.VIDEOS, Icons.Rounded.VideoFile, FileType.VIDEO.color),
        CatItem(Category.AUDIO, Icons.Rounded.AudioFile, FileType.AUDIO.color),
        CatItem(Category.DOCUMENTS, Icons.Rounded.Description, FileType.DOCUMENT.color),
        CatItem(Category.DOWNLOADS, Icons.Rounded.Download, Color(0xFF0EA5E9)),
        CatItem(Category.ARCHIVES, Icons.Rounded.FolderZip, FileType.ARCHIVE.color),
        CatItem(Category.APKS, Icons.Rounded.Android, FileType.APK.color),
        CatItem(Category.RECENT, Icons.Rounded.History, Color(0xFF6366F1)),
    )
    Column(Modifier.padding(horizontal = 12.dp)) {
        items.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { c ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).clickable {
                            if (c.category == Category.DOWNLOADS) vm.openInFiles(FileRepository.primaryRoot + "/Download")
                            else vm.push(Screen.CategoryView(c.category))
                        }.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        IconTile(c.icon, c.color, 54.dp)
                        Spacer(Modifier.height(8.dp))
                        Text(c.category.label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable(onClick = onClick)
            .padding(start = 10.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 160.dp))
    }
}

@Suppress("unused")
private val folderIcon = Icons.Rounded.Folder
