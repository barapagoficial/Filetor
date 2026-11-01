package com.filetor.app

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.filetor.app.data.ArchiveManager
import com.filetor.app.ui.AppViewModel
import com.filetor.app.ui.MainScreen
import com.filetor.app.ui.PermissionScreen
import com.filetor.app.ui.Screen
import com.filetor.app.ui.theme.FiletorTheme
import com.filetor.app.util.FileActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    private var hasAccess by mutableStateOf(false)

    private val legacyPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { checkAccess() }
    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val manageAll = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { checkAccess() }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkAccess()
        handleIntent(intent)
        setContent {
            FiletorTheme {
                if (hasAccess) MainScreen(vm) else PermissionScreen { requestAccess() }
            }
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        val before = hasAccess
        checkAccess()
        if (hasAccess) {
            vm.refreshVolumes()
            if (!before || vm.tab.entries.isEmpty()) vm.load()
        }
    }

    private fun checkAccess() {
        hasAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                manageAll.launch(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
            } catch (e: Exception) {
                manageAll.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            legacyPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            Intent.ACTION_GET_CONTENT, Intent.ACTION_PICK -> {
                vm.pickMode = true
                vm.pickMultiple = intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                vm.mainTab = 1
                vm.onPick = { files -> returnPicked(files) }
                vm.message("Elige un archivo para compartir")
            }
            Intent.ACTION_VIEW -> {
                val uri = intent.data ?: return
                lifecycleScope.launch {
                    val file = withContext(Dispatchers.IO) { resolveToFile(uri) }
                    if (file == null) { vm.message("No se pudo abrir el archivo"); return@launch }
                    if (ArchiveManager.isSupported(file)) vm.push(Screen.ArchiveView(file.absolutePath))
                    else if (file.isDirectory) vm.openInFiles(file.absolutePath)
                    else vm.push(Screen.TextEditor(file.absolutePath))
                }
            }
        }
    }

    private fun resolveToFile(uri: Uri): File? {
        if (uri.scheme == "file") return uri.path?.let { File(it) }
        // Try to map well-known providers to a real path, else copy to cache
        runCatching {
            val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            } ?: (uri.lastPathSegment?.substringAfterLast('/') ?: "archivo")
            val dir = File(cacheDir, "incoming").apply { mkdirs() }
            val out = File(dir, name)
            contentResolver.openInputStream(uri)?.use { ins -> out.outputStream().use { ins.copyTo(it) } } ?: return null
            return out
        }
        return null
    }

    private fun returnPicked(files: List<File>) {
        if (files.isEmpty()) return
        val uris = files.map { FileActions.uri(this, it) }
        val result = Intent().apply {
            data = uris[0]
            if (uris.size > 1) {
                val clip = ClipData.newRawUri(null, uris[0])
                uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
                clipData = clip
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        setResult(Activity.RESULT_OK, result)
        finish()
    }
}
