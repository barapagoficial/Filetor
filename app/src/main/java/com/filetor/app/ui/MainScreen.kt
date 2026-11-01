package com.filetor.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding as pad
import com.filetor.app.data.OperationManager
import com.filetor.app.ui.components.ActionDialog
import com.filetor.app.ui.components.FileActionHost
import com.filetor.app.ui.components.OperationsPanel
import com.filetor.app.ui.components.PasswordDialog
import com.filetor.app.ui.components.rememberFileActions
import com.filetor.app.ui.screens.AnalyzerScreen
import com.filetor.app.ui.screens.ArchiveScreen
import com.filetor.app.ui.screens.BrowserScreen
import com.filetor.app.ui.screens.CategoryScreen
import com.filetor.app.ui.screens.DuplicatesScreen
import com.filetor.app.ui.screens.HomeScreen
import com.filetor.app.ui.screens.ImageViewerScreen
import com.filetor.app.ui.screens.RecentsScreen
import com.filetor.app.ui.screens.SearchScreen
import com.filetor.app.ui.screens.SettingsScreen
import com.filetor.app.ui.screens.TextEditorScreen
import com.filetor.app.ui.screens.ToolsScreen
import com.filetor.app.ui.screens.TrashScreen
import java.io.File

@Composable
fun MainScreen(vm: AppViewModel) {
    val snackbar = remember { SnackbarHostState() }
    val homeActions = rememberFileActions(vm)
    LaunchedEffect(Unit) {
        vm.messages.collect { m ->
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(m.text, m.action, duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) m.onAction?.invoke()
        }
    }
    BackHandler(enabled = vm.stack.isNotEmpty()) { vm.pop() }
    BackHandler(enabled = vm.stack.isEmpty() && vm.mainTab == 2) { vm.mainTab = 0 }

    val top = vm.stack.lastOrNull()
    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbar) { data -> Snackbar(data, shape = RoundedCornerShape(16.dp)) }
        },
        bottomBar = {
            if (top == null) Column {
                OperationsPanel()
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    val items = listOf(
                        Triple("Inicio", Icons.Rounded.Home, Icons.Outlined.Home),
                        Triple("Archivos", Icons.Rounded.Folder, Icons.Outlined.Folder),
                        Triple("Herramientas", Icons.Rounded.Handyman, Icons.Outlined.Handyman),
                    )
                    items.forEachIndexed { i, (label, sel, unsel) ->
                        NavigationBarItem(
                            selected = vm.mainTab == i,
                            onClick = {
                                if (vm.mainTab == i && i == 1) vm.load() else vm.mainTab = i
                            },
                            icon = { Icon(if (vm.mainTab == i) sel else unsel, label) },
                            label = { Text(label) },
                        )
                    }
                }
            } else Box(Modifier.navigationBarsPadding()) { OperationsPanel() }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(Modifier.fillMaxSize().pad(padding).consumeWindowInsets(padding)) {
            AnimatedContent(
                targetState = top ?: vm.mainTab,
                transitionSpec = {
                    val forward = targetState is Screen && (initialState !is Screen || vm.stack.size > 0)
                    if (targetState is Screen || initialState is Screen) {
                        if (forward && targetState is Screen) (slideInHorizontally(tween(260)) { it / 4 } + fadeIn(tween(220))) togetherWith fadeOut(tween(120))
                        else fadeIn(tween(200)) togetherWith (slideOutHorizontally(tween(260)) { it / 4 } + fadeOut(tween(200)))
                    } else fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                },
                label = "screens",
            ) { state ->
                when (state) {
                    0 -> HomeScreen(vm) { e -> homeActions.open(e) }
                    1 -> BrowserScreen(vm)
                    2 -> ToolsScreen(vm)
                    is Screen.CategoryView -> CategoryScreen(vm, state.category)
                    is Screen.ArchiveView -> ArchiveScreen(vm, state.path)
                    is Screen.TextEditor -> TextEditorScreen(vm, state.path)
                    is Screen.ImageViewer -> ImageViewerScreen(vm, state.path, state.siblings)
                    is Screen.Search -> SearchScreen(vm, state.root)
                    Screen.Analyzer -> AnalyzerScreen(vm)
                    Screen.Duplicates -> DuplicatesScreen(vm)
                    Screen.Trash -> TrashScreen(vm)
                    Screen.Settings -> SettingsScreen(vm)
                    Screen.Recents -> RecentsScreen(vm)
                    else -> {}
                }
            }
        }
    }
    FileActionHost(homeActions)

    vm.passwordRequest?.let { req ->
        PasswordDialog("Archivo protegido", "\"${File(req.archive).name}\" está cifrado. Introduce la contraseña para extraerlo.",
            onDismiss = { vm.passwordRequest = null }) { pw ->
            vm.passwordRequest = null
            OperationManager.extract(File(req.archive), File(req.dest), pw, req.only, req.strip)
        }
    }
}

@Composable
fun PermissionScreen(onGrant: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().navigationBarsPadding().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
            Box(
                Modifier.size(132.dp).clip(RoundedCornerShape(40.dp)).background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
                ),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(68.dp)) }
            Spacer(Modifier.height(32.dp))
            Text("Bienvenido a Filetor", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(
                "Para explorar, organizar, comprimir y extraer tus archivos, Filetor necesita permiso para acceder a todo el almacenamiento.",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(6.dp))
                Text("Tus archivos nunca salen del dispositivo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(36.dp))
            Button(onClick = onGrant, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) {
                Text("Conceder acceso", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
