package com.filetor.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.filetor.app.data.ArchiveFormat
import com.filetor.app.data.FileEntry
import com.filetor.app.data.FileRepository
import com.filetor.app.data.Prefs
import com.filetor.app.data.SortBy
import com.filetor.app.data.StorageVolumeInfo
import com.filetor.app.util.FileType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun TextInputDialog(
    title: String,
    initial: String = "",
    label: String = "Nombre",
    confirmText: String = "Aceptar",
    selectBaseName: Boolean = true,
    validate: (String) -> String? = { if (FileRepository.isValidName(it)) null else "Nombre no válido" },
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val end = if (selectBaseName && initial.lastIndexOf('.') > 0) initial.lastIndexOf('.') else initial.length
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, end))) }
    val error = if (value.text.isEmpty()) null else validate(value.text)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(150); runCatching { focus.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value, onValueChange = { value = it }, label = { Text(label) }, singleLine = true,
                isError = error != null, supportingText = { if (error != null) Text(error) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        },
        confirmButton = { Button(onClick = { onConfirm(value.text.trim()) }, enabled = value.text.isNotBlank() && error == null) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String = "Aceptar",
    destructive: Boolean = false,
    checkboxLabel: String? = null,
    checkboxInitial: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit,
) {
    var checked by remember { mutableStateOf(checkboxInitial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(text)
                if (checkboxLabel != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { checked = !checked }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked, { checked = it })
                        Text(checkboxLabel, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(checked) },
                colors = if (destructive) androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError) else androidx.compose.material3.ButtonDefaults.buttonColors(),
            ) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun NewItemDialog(onDismiss: () -> Unit, onConfirm: (name: String, folder: Boolean) -> Unit) {
    var folder by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(150); runCatching { focus.requestFocus() } }
    val valid = FileRepository.isValidName(name)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear nuevo") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(folder, { folder = true }, label = { Text("Carpeta") }, leadingIcon = { Icon(Icons.Rounded.CreateNewFolder, null, Modifier.size(18.dp)) })
                    FilterChip(!folder, { folder = false }, label = { Text("Archivo") }, leadingIcon = { Icon(Icons.Rounded.NoteAdd, null, Modifier.size(18.dp)) })
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    name, { name = it }, singleLine = true, shape = RoundedCornerShape(14.dp),
                    label = { Text(if (folder) "Nombre de la carpeta" else "Nombre del archivo") },
                    placeholder = { Text(if (folder) "Nueva carpeta" else "nota.txt") },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(name.trim(), folder) }, enabled = valid) { Text("Crear") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun PasswordDialog(title: String = "Contraseña", subtitle: String? = null, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pw by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(150); runCatching { focus.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (subtitle != null) { Text(subtitle, style = MaterialTheme.typography.bodyMedium); Spacer(Modifier.height(10.dp)) }
                OutlinedTextField(
                    pw, { pw = it }, singleLine = true, label = { Text("Contraseña") }, shape = RoundedCornerShape(14.dp),
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = { IconButton({ show = !show }) { Icon(if (show) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null) } },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(pw) }, enabled = pw.isNotEmpty()) { Text("Aceptar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text("Ordenar por", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            SortBy.entries.forEach { s ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        if (Prefs.sortBy == s) Prefs.setSort(s, !Prefs.sortAsc) else Prefs.setSort(s, s == SortBy.NAME || s == SortBy.TYPE)
                    }.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(Prefs.sortBy == s, null)
                    Spacer(Modifier.width(14.dp))
                    Text(s.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    if (Prefs.sortBy == s) {
                        Icon(if (Prefs.sortAsc) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(if (Prefs.sortAsc) "Ascendente" else "Descendente", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 24.dp))
            Row(Modifier.fillMaxWidth().clickable { Prefs.setFoldersFirstPref(!Prefs.foldersFirst) }.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(Prefs.foldersFirst, null); Spacer(Modifier.width(14.dp)); Text("Carpetas primero")
            }
            Row(Modifier.fillMaxWidth().clickable { Prefs.setHidden(!Prefs.showHidden) }.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(Prefs.showHidden, null); Spacer(Modifier.width(14.dp)); Text("Mostrar archivos ocultos")
            }
        }
    }
}

@Composable
fun CompressDialog(
    defaultName: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, format: ArchiveFormat, level: Int, password: String?) -> Unit,
) {
    var name by remember { mutableStateOf(defaultName) }
    var format by remember { mutableStateOf(ArchiveFormat.ZIP) }
    var level by remember { mutableIntStateOf(5) }
    var usePw by remember { mutableStateOf(false) }
    var pw by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Comprimir") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Nombre del archivo") }, shape = RoundedCornerShape(14.dp),
                    suffix = { Text("." + format.ext) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                Text("Formato", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                FlowRowCompat(ArchiveFormat.entries) { f ->
                    FilterChip(format == f, { format = f; if (!f.supportsPassword) usePw = false }, label = { Text(f.label) })
                }
                Spacer(Modifier.height(10.dp))
                val levelLabel = when (level) { 0 -> "Sin compresión"; in 1..2 -> "Más rápida"; in 3..4 -> "Rápida"; in 5..6 -> "Normal"; in 7..8 -> "Máxima"; else -> "Ultra" }
                Text("Nivel de compresión: $levelLabel", style = MaterialTheme.typography.labelLarge)
                Slider(level.toFloat(), { level = it.toInt() }, valueRange = 0f..9f, steps = 8)
                if (format.supportsPassword) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { usePw = !usePw }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(usePw, { usePw = it }); Text("Proteger con contraseña (AES-256)")
                    }
                    if (usePw) OutlinedTextField(
                        pw, { pw = it }, singleLine = true, label = { Text("Contraseña") }, shape = RoundedCornerShape(14.dp),
                        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = { IconButton({ show = !show }) { Icon(if (show) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text("El cifrado solo está disponible en ZIP.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim() + "." + format.ext, format, level, if (usePw && pw.isNotEmpty()) pw else null) },
                enabled = FileRepository.isValidName(name) && (!usePw || pw.isNotEmpty()),
            ) { Text("Comprimir") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun <T> FlowRowCompat(items: List<T>, content: @Composable (T) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { content(it) }
    }
}

@Composable
fun BatchRenameDialog(entries: List<FileEntry>, onDismiss: () -> Unit, onConfirm: (List<Pair<File, String>>) -> Unit) {
    var mode by remember { mutableIntStateOf(0) } // 0 = pattern, 1 = find/replace, 2 = case
    var pattern by remember { mutableStateOf("Archivo_{n}") }
    var start by remember { mutableStateOf("1") }
    var find by remember { mutableStateOf("") }
    var replace by remember { mutableStateOf("") }
    var upper by remember { mutableStateOf(false) }
    val sorted = remember(entries) { entries.sortedWith { a, b -> FileRepository.naturalCompare(a.name, b.name) } }
    val digits = (sorted.size + (start.toIntOrNull() ?: 1)).toString().length
    val preview = sorted.mapIndexed { i, e ->
        val base = if (e.isDirectory) e.name else e.name.substringBeforeLast('.', e.name)
        val ext = if (e.isDirectory || !e.name.contains('.')) "" else "." + e.name.substringAfterLast('.')
        val newName = when (mode) {
            0 -> pattern.replace("{n}", ((start.toIntOrNull() ?: 1) + i).toString().padStart(digits, '0'))
                .replace("{name}", base).replace("{ext}", ext.removePrefix(".")) + if (pattern.contains("{ext}")) "" else ext
            1 -> if (find.isEmpty()) e.name else e.name.replace(find, replace)
            else -> if (upper) base.uppercase() + ext else base.lowercase() + ext
        }
        e to newName
    }
    val valid = preview.all { FileRepository.isValidName(it.second) } && preview.map { it.second }.toSet().size == preview.size
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renombrar ${entries.size} elementos") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(mode == 0, { mode = 0 }, label = { Text("Patrón") })
                    FilterChip(mode == 1, { mode = 1 }, label = { Text("Reemplazar") })
                    FilterChip(mode == 2, { mode = 2 }, label = { Text("Mayús/minús") })
                }
                Spacer(Modifier.height(8.dp))
                when (mode) {
                    0 -> {
                        OutlinedTextField(pattern, { pattern = it }, singleLine = true, label = { Text("Patrón") }, shape = RoundedCornerShape(14.dp),
                            supportingText = { Text("{n} número · {name} nombre original · {ext} extensión") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(start, { start = it.filter { c -> c.isDigit() }.take(6) }, singleLine = true, label = { Text("Empezar en") },
                            shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    }
                    1 -> {
                        OutlinedTextField(find, { find = it }, singleLine = true, label = { Text("Buscar") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(replace, { replace = it }, singleLine = true, label = { Text("Reemplazar por") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    }
                    else -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(!upper, { upper = false }, label = { Text("minúsculas") })
                        FilterChip(upper, { upper = true }, label = { Text("MAYÚSCULAS") })
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Vista previa", style = MaterialTheme.typography.labelLarge)
                LazyColumn(Modifier.heightIn(max = 180.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(8.dp)) {
                    items(preview) { (e, n) ->
                        Text("${e.name}  →  $n", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(preview.map { it.first.file to it.second }) }, enabled = valid) { Text("Renombrar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Full-screen folder chooser used for "Copy to", "Move to" and "Extract to". */
@Composable
fun FolderPickerDialog(
    title: String,
    start: String,
    volumes: List<StorageVolumeInfo>,
    confirmText: String = "Seleccionar",
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    var path by remember { mutableStateOf(start) }
    var dirs by remember { mutableStateOf<List<File>>(emptyList()) }
    var creating by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(path, refresh) {
        dirs = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            File(path).listFiles()?.filter { it.isDirectory && (Prefs.showHidden || !it.name.startsWith(".")) }
                ?.sortedWith { a, b -> FileRepository.naturalCompare(a.name, b.name) } ?: emptyList()
        }
    }
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().padding(top = 12.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        val parent = File(path).parentFile
                        if (volumes.any { it.path == path } || parent == null) onDismiss() else path = parent.absolutePath
                    }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Atrás") }
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleLarge)
                        Text(FileRepository.displayPath(context, path), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { creating = true }) { Icon(Icons.Rounded.CreateNewFolder, "Nueva carpeta") }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    volumes.forEach { v ->
                        FilterChip(path.startsWith(v.path), { path = v.path }, label = { Text(if (v.isPrimary) "Interno" else v.label, maxLines = 1) },
                            leadingIcon = { Icon(if (v.isPrimary) Icons.Rounded.PhoneAndroid else Icons.Rounded.SdStorage, null, Modifier.size(18.dp)) })
                    }
                }
                LazyColumn(Modifier.weight(1f)) {
                    if (dirs.isEmpty()) item { EmptyState(Icons.Rounded.Folder, "Sin subcarpetas", "Puedes seleccionar esta carpeta o crear una nueva") }
                    items(dirs, key = { it.absolutePath }) { d ->
                        Row(
                            Modifier.fillMaxWidth().clickable { path = d.absolutePath }.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TypeIconBox(FileType.FOLDER, 40.dp)
                            Spacer(Modifier.width(14.dp))
                            Text(d.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(onClick = { onPick(path) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(confirmText)
                    }
                }
            }
        }
    }
    if (creating) TextInputDialog("Nueva carpeta", "", onDismiss = { creating = false }) { n ->
        creating = false
        scope.launch {
            val f = File(path, n)
            if (f.mkdirs()) path = f.absolutePath else refresh++
        }
    }
}

@Composable
fun HashLine(label: String, value: String?, onCompute: () -> Unit, onCopy: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value ?: "—", style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), fontWeight = FontWeight.Medium)
        }
        if (value == null) TextButton(onCompute) { Text("Calcular") }
        else if (value != "…") IconButton({ onCopy(value) }) { Icon(Icons.Rounded.ContentCopy, "Copiar", Modifier.size(18.dp)) }
    }
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}
