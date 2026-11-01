package com.filetor.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.filetor.app.data.Analyzer
import com.filetor.app.data.FileEntry
import com.filetor.app.data.FileRepository
import com.filetor.app.data.OperationManager
import com.filetor.app.util.FileActions
import com.filetor.app.util.FileType
import com.filetor.app.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesSheet(entries: List<FileEntry>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val single = entries.singleOrNull()
    var stats by remember { mutableStateOf<FileRepository.DirStats?>(null) }
    LaunchedEffect(entries) {
        var size = 0L; var files = 0; var folders = 0
        for (e in entries) {
            if (e.isDirectory) {
                val s = FileRepository.dirStats(e.file) { p -> stats = FileRepository.DirStats(size + p.size, files + p.files, folders + p.folders) }
                size += s.size; files += s.files; folders += s.folders + 1
            } else { size += e.size; files++ }
            stats = FileRepository.DirStats(size, files, folders)
        }
    }
    val hashes = remember { mutableStateMapOf<String, String>() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (single != null) FileThumb(single, 56.dp) else TypeIconBox(FileType.OTHER, 56.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(single?.name ?: "${entries.size} elementos", style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(single?.type?.label ?: "Selección múltiple", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            val s = stats
            InfoLine("Tamaño", if (s == null) "Calculando…" else "${Format.size(s.size)} (${"%,d".format(s.size)} bytes)")
            if (entries.any { it.isDirectory }) InfoLine("Contenido", if (s == null) "…" else "${s.files} archivos, ${s.folders} carpetas")
            if (single != null) {
                InfoLine("Ubicación", single.file.parent ?: "/", onClick = { FileActions.copyText(context, "Ruta", single.path) })
                InfoLine("Modificado", Format.dateTime(single.lastModified))
                val f = single.file
                InfoLine("Permisos", buildString {
                    append(if (f.canRead()) "Lectura" else "Sin lectura")
                    append(" · "); append(if (f.canWrite()) "Escritura" else "Solo lectura")
                    if (single.isHidden) append(" · Oculto")
                })
                if (!single.isDirectory) {
                    InfoLine("Tipo MIME", FileType.mime(single.name))
                    Spacer(Modifier.height(8.dp))
                    Text("Sumas de verificación", style = MaterialTheme.typography.titleSmall)
                    listOf("MD5", "SHA-1", "SHA-256").forEach { alg ->
                        HashLine(alg, hashes[alg], onCompute = {
                            hashes[alg] = "…"
                            scope.launch { hashes[alg] = runCatching { Analyzer.fileHash(f, alg) }.getOrDefault("Error") }
                        }, onCopy = { FileActions.copyText(context, alg, it) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkSheet(file: File, onDismiss: () -> Unit, onBrowse: () -> Unit) {
    val context = LocalContext.current
    val info by produceState<FileActions.ApkInfo?>(null, file) {
        value = withContext(Dispatchers.IO) { runCatching { FileActions.apkInfo(context, file) }.getOrNull() }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            val i = info
            Row(verticalAlignment = Alignment.CenterVertically) {
                val bmp = remember(i) { i?.icon?.let { runCatching { it.toBitmap(144, 144).asImageBitmap() }.getOrNull() } }
                if (bmp != null) Image(bmp, null, Modifier.size(64.dp)) else TypeIconBox(FileType.APK, 64.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(i?.label ?: file.name, style = MaterialTheme.typography.titleLarge)
                    Text(i?.packageName ?: "Analizando…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            if (i != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("v${i.versionName}")
                    Pill("Android ${sdkName(i.minSdk)}+")
                    Pill(Format.size(file.length()))
                }
                Spacer(Modifier.height(12.dp))
                InfoLine("Código de versión", i.versionCode.toString())
                InfoLine("SDK objetivo", "API ${i.targetSdk}")
                InfoLine("Estado", when {
                    i.installedVersion == null -> "No instalada"
                    i.installedVersion == i.versionName -> "Instalada (misma versión)"
                    else -> "Instalada: v${i.installedVersion}"
                })
                if (i.permissions.isNotEmpty()) InfoLine("Permisos (${i.permissions.size})", i.permissions.take(12).joinToString(", ") + if (i.permissions.size > 12) "…" else "")
            } else {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBrowse, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.FolderZip, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Ver contenido")
                }
                Button(onClick = { FileActions.installApk(context, file); onDismiss() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.InstallMobile, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(if (i?.installedVersion != null) "Actualizar" else "Instalar")
                }
            }
        }
    }
}

private fun sdkName(sdk: Int): String = when (sdk) {
    in 0..20 -> "4.x"; 21, 22 -> "5"; 23 -> "6"; 24, 25 -> "7"; 26, 27 -> "8"; 28 -> "9"; 29 -> "10"; 30 -> "11"; 31, 32 -> "12"
    33 -> "13"; 34 -> "14"; 35 -> "15"; else -> "16"
}

/** Floating panel listing running operations. */
@Composable
fun OperationsPanel(modifier: Modifier = Modifier) {
    val tasks by OperationManager.tasks.collectAsState()
    AnimatedVisibility(tasks.isNotEmpty(), modifier = modifier, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                tasks.forEach { t ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val detail = buildString {
                                if (t.progress >= 0) append("${(t.progress * 100).toInt()}%")
                                if (t.totalBytes > 0) append(" · ${Format.size(t.doneBytes)} de ${Format.size(t.totalBytes)}")
                                if (t.current.isNotEmpty()) append(" · ${t.current.substringAfterLast('/')}")
                            }
                            Text(detail.ifEmpty { "Preparando…" }, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.75f))
                        }
                        IconButton(onClick = { OperationManager.cancel(t.id) }) { Icon(Icons.Rounded.Close, "Cancelar") }
                    }
                    Spacer(Modifier.height(4.dp))
                    val pmod = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    if (t.progress < 0) LinearProgressIndicator(pmod, color = MaterialTheme.colorScheme.inversePrimary, trackColor = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.2f))
                    else LinearProgressIndicator(progress = { t.progress }, modifier = pmod, color = MaterialTheme.colorScheme.inversePrimary, trackColor = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.2f))
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

