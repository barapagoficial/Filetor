package com.filetor.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.WrapText
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.filetor.app.data.FileEntry
import com.filetor.app.ui.AppViewModel
import com.filetor.app.ui.components.ActionDialog
import com.filetor.app.ui.components.FileActionHost
import com.filetor.app.ui.components.SubTopBar
import com.filetor.app.ui.components.rememberFileActions
import com.filetor.app.util.FileActions
import com.filetor.app.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun TextEditorScreen(vm: AppViewModel, path: String) {
    val file = remember(path) { File(path) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var original by remember { mutableStateOf<String?>(null) }
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var fontSize by remember { mutableFloatStateOf(14f) }
    var wrap by remember { mutableStateOf(true) }
    var confirmExit by remember { mutableStateOf(false) }
    var findOpen by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    LaunchedEffect(path) {
        val r = withContext(Dispatchers.IO) { runCatching { file.readText() } }
        r.onSuccess { original = it; text = it }.onFailure { error = it.message }
    }
    val dirty = original != null && text != original
    val readOnly = !file.canWrite()
    fun save(after: () -> Unit = {}) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { file.writeText(text) }.isSuccess }
            if (ok) { original = text; vm.message("Guardado"); after() } else vm.message("No se pudo guardar")
        }
    }
    BackHandler { if (dirty) confirmExit = true else vm.pop() }
    Column(Modifier.fillMaxSize()) {
        SubTopBar(file.name + if (dirty) " •" else "", buildString {
            append(Format.size(file.length()))
            if (original != null) append(" · ${text.lines().size} líneas")
            if (readOnly) append(" · Solo lectura")
        }, onBack = { if (dirty) confirmExit = true else vm.pop() }) {
            IconButton({ findOpen = !findOpen }) { Icon(Icons.Rounded.Search, "Buscar") }
            IconButton({ wrap = !wrap }) { Icon(Icons.AutoMirrored.Rounded.WrapText, "Ajuste de línea", tint = if (wrap) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton({ fontSize = if (fontSize >= 22f) 11f else fontSize + 3f }) { Icon(Icons.Rounded.FormatSize, "Tamaño de letra") }
            if (!readOnly) IconButton({ save() }, enabled = dirty) { Icon(Icons.Rounded.Save, "Guardar") }
        }
        AnimatedVisibility(findOpen) {
            val count = if (findText.isEmpty()) 0 else Regex(Regex.escape(findText), RegexOption.IGNORE_CASE).findAll(text).count()
            OutlinedTextField(findText, { findText = it }, singleLine = true, placeholder = { Text("Buscar en el texto") },
                supportingText = { if (findText.isNotEmpty()) Text("$count coincidencias") }, shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        }
        when {
            error != null -> Text("No se pudo leer: $error", Modifier.padding(20.dp))
            original == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
            else -> {
                val hScroll = rememberScrollState()
                Box(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp).clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .verticalScroll(rememberScrollState())
                        .then(if (wrap) Modifier else Modifier.horizontalScroll(hScroll))
                        .padding(14.dp)
                ) {
                    BasicTextField(
                        value = text, onValueChange = { if (!readOnly) text = it },
                        readOnly = readOnly,
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = fontSize.sp, lineHeight = (fontSize * 1.45f).sp, color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = if (wrap) Modifier.fillMaxWidth() else Modifier,
                    )
                }
            }
        }
    }
    if (confirmExit) AlertDialog(
        onDismissRequest = { confirmExit = false },
        title = { Text("Cambios sin guardar") },
        text = { Text("¿Quieres guardar los cambios en \"${file.name}\"?") },
        confirmButton = { TextButton({ confirmExit = false; save { vm.pop() } }) { Text("Guardar") } },
        dismissButton = { TextButton({ confirmExit = false; vm.pop() }) { Text("Descartar") } },
    )
}

@Composable
fun ImageViewerScreen(vm: AppViewModel, path: String, siblings: List<String>) {
    val context = LocalContext.current
    val list = remember(siblings) { siblings.ifEmpty { listOf(path) } }
    val pager = rememberPagerState(initialPage = list.indexOf(path).coerceAtLeast(0)) { list.size }
    var chrome by remember { mutableStateOf(true) }
    val actions = rememberFileActions(vm)
    actions.onChanged = { vm.pop() }
    BackHandler { vm.pop() }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1, key = { list[it] }) { page ->
            ZoomableImage(File(list[page])) { chrome = !chrome }
        }
        val current = File(list[pager.currentPage.coerceIn(0, list.size - 1)])
        AnimatedVisibility(chrome, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter)) {
            Row(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.45f)).statusBarsPadding().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ vm.pop() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Atrás", tint = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text(current.name, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${pager.currentPage + 1} / ${list.size} · ${Format.size(current.length())}", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        AnimatedVisibility(chrome, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter)) {
            Row(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.45f)).navigationBarsPadding().padding(8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly) {
                IconButton({ FileActions.share(context, listOf(current)) }) { Icon(Icons.Rounded.Share, "Compartir", tint = Color.White) }
                IconButton({ FileActions.openExternal(context, current, forceChooser = true) }) { Icon(Icons.AutoMirrored.Rounded.OpenInNew, "Abrir con", tint = Color.White) }
                IconButton({ actions.dialog = ActionDialog.Properties(listOf(FileEntry.from(current, false))) }) { Icon(Icons.Rounded.Info, "Info", tint = Color.White) }
                IconButton({ actions.dialog = ActionDialog.Delete(listOf(FileEntry.from(current, false))) }) { Icon(Icons.Rounded.Delete, "Eliminar", tint = Color.White) }
            }
        }
    }
    FileActionHost(actions)
}

@Composable
private fun ZoomableImage(file: File, onTap: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier.fillMaxSize()
            .pointerInput(file) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f },
                )
            }
            .pointerInput(file) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.size > 1 || scale > 1f) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val newScale = (scale * zoom).coerceIn(1f, 6f)
                            scale = newScale
                            offset = if (newScale <= 1f) Offset.Zero else offset + pan
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = file, contentDescription = file.name, contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
        )
    }
}
