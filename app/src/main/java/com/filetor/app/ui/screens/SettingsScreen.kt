package com.filetor.app.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoDelete
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DensitySmall
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.filetor.app.BuildConfig
import com.filetor.app.data.Prefs
import com.filetor.app.data.ThemeMode
import com.filetor.app.ui.AppViewModel
import com.filetor.app.ui.components.SectionTitle
import com.filetor.app.ui.components.SettingRow
import com.filetor.app.ui.components.SubTopBar
import com.filetor.app.ui.components.SwitchRow
import com.filetor.app.ui.theme.Accents
import com.filetor.app.ui.theme.isAppInDarkTheme

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val dark = isAppInDarkTheme()
    Column(Modifier.fillMaxSize()) {
        SubTopBar("Ajustes", onBack = { vm.pop() })
        LazyColumn(contentPadding = PaddingValues(bottom = 64.dp)) {
            item { SectionTitle("Apariencia") }
            item {
                SettingRow(Icons.Rounded.BrightnessMedium, "Tema") {}
                Row(Modifier.padding(start = 62.dp, end = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { m -> FilterChip(Prefs.themeMode == m, { Prefs.setTheme(m) }, label = { Text(m.label) }) }
                }
            }
            item {
                SettingRow(Icons.Rounded.ColorLens, "Color de acento", if (Prefs.dynamicColor) "Usando colores del fondo de pantalla" else Accents[Prefs.accent.coerceIn(0, Accents.size - 1)].name)
                LazyRow(contentPadding = PaddingValues(start = 62.dp, end = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(Accents) { i, a ->
                        val sel = !Prefs.dynamicColor && Prefs.accent == i
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(if (dark) a.dark else a.light)
                                .then(if (sel) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable { Prefs.setAccentColor(i); Prefs.setDynamic(false) },
                            contentAlignment = Alignment.Center,
                        ) { if (sel) Icon(Icons.Rounded.Check, null, tint = if (dark) Color.Black else Color.White) }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) item {
                SwitchRow(Icons.Rounded.ColorLens, "Material You", "Colores dinámicos según tu fondo de pantalla", Prefs.dynamicColor) { Prefs.setDynamic(it) }
            }
            item { SwitchRow(Icons.Rounded.Contrast, "Negro puro (AMOLED)", "Fondo totalmente negro en modo oscuro", Prefs.amoled) { Prefs.setAmoledBlack(it) } }
            item { SwitchRow(Icons.Rounded.DensitySmall, "Lista compacta", "Muestra más elementos en pantalla", Prefs.compactList) { Prefs.setCompact(it) } }
            item { HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
            item { SectionTitle("Explorador") }
            item { SwitchRow(Icons.Rounded.Visibility, "Mostrar archivos ocultos", "Archivos y carpetas que empiezan por punto", Prefs.showHidden) { Prefs.setHidden(it) } }
            item { SwitchRow(Icons.Rounded.Folder, "Carpetas primero", null, Prefs.foldersFirst) { Prefs.setFoldersFirstPref(it) } }
            item { SwitchRow(Icons.Rounded.Image, "Miniaturas", "Vista previa de imágenes, vídeos e iconos de APK", Prefs.showThumbnails) { Prefs.setThumbs(it) } }
            item { SwitchRow(Icons.Rounded.Numbers, "Contar elementos de carpetas", "Muestra cuántos elementos tiene cada carpeta", Prefs.showFileCount) { Prefs.setFileCount(it) } }
            item { HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
            item { SectionTitle("Papelera") }
            item { SwitchRow(Icons.Rounded.Delete, "Usar papelera", "Los elementos eliminados se pueden restaurar", Prefs.useTrash) { Prefs.setTrash(it) } }
            if (Prefs.useTrash) item {
                SettingRow(Icons.Rounded.AutoDelete, "Vaciar automáticamente", "Elementos con más de ${Prefs.trashDays} días") {}
                Row(Modifier.padding(start = 62.dp, end = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 15, 30, 60, 0).forEach { d -> FilterChip(Prefs.trashDays == d, { Prefs.setTrashRetention(d) }, label = { Text(if (d == 0) "Nunca" else "$d d") }) }
                }
            }
            item { SwitchRow(Icons.Rounded.Warning, "Confirmar antes de eliminar", null, Prefs.confirmDelete) { Prefs.setConfirm(it) } }
            item { HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
            item { SectionTitle("Acerca de") }
            item { SettingRow(Icons.Rounded.Info, "Filetor ${BuildConfig.VERSION_NAME}", "Gestor de archivos y compresor · ZIP, 7z, RAR, TAR, GZ, BZ2, XZ") }
        }
    }
}
