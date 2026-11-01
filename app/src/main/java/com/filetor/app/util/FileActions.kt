package com.filetor.app.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object FileActions {
    fun uri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, context.packageName + ".provider", file)

    fun openExternal(context: Context, file: File, forceChooser: Boolean = false, mimeOverride: String? = null) {
        val mime = mimeOverride ?: FileType.mime(file.name)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri(context, file), mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(if (forceChooser) Intent.createChooser(intent, "Abrir con").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) else intent)
        } catch (e: Exception) {
            try {
                context.startActivity(Intent.createChooser(intent.setDataAndType(uri(context, file), "*/*"), "Abrir con")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e2: Exception) {
                Toast.makeText(context, "No hay ninguna app para abrir este archivo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun share(context: Context, files: List<File>) {
        val real = files.filter { it.isFile }
        if (real.isEmpty()) { Toast.makeText(context, "Solo se pueden compartir archivos", Toast.LENGTH_SHORT).show(); return }
        val intent = if (real.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = FileType.mime(real[0].name)
                putExtra(Intent.EXTRA_STREAM, uri(context, real[0]))
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(real.map { uri(context, it) }))
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, "Compartir").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun installApk(context: Context, file: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri(context, file), "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try { context.startActivity(intent) } catch (e: Exception) {
            Toast.makeText(context, "No se pudo iniciar la instalación", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyText(context: Context, label: String, text: String) {
        val cm = context.getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show()
    }

    data class ApkInfo(val label: String, val packageName: String, val versionName: String, val versionCode: Long,
                       val minSdk: Int, val targetSdk: Int, val icon: Drawable?, val installedVersion: String?, val permissions: List<String>)

    @Suppress("DEPRECATION")
    fun apkInfo(context: Context, file: File): ApkInfo? {
        val pm = context.packageManager
        val info: PackageInfo = pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_PERMISSIONS) ?: return null
        val ai = info.applicationInfo ?: return null
        ai.sourceDir = file.absolutePath
        ai.publicSourceDir = file.absolutePath
        val installed = runCatching { pm.getPackageInfo(info.packageName, 0).versionName }.getOrNull()
        return ApkInfo(
            label = ai.loadLabel(pm).toString(),
            packageName = info.packageName,
            versionName = info.versionName ?: "?",
            versionCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong(),
            minSdk = if (Build.VERSION.SDK_INT >= 24) ai.minSdkVersion else 0,
            targetSdk = ai.targetSdkVersion,
            icon = runCatching { ai.loadIcon(pm) }.getOrNull(),
            installedVersion = installed,
            permissions = info.requestedPermissions?.map { it.substringAfterLast('.') } ?: emptyList(),
        )
    }

    fun apkIcon(context: Context, file: File): Drawable? = runCatching {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val info = pm.getPackageArchiveInfo(file.absolutePath, 0) ?: return null
        val ai = info.applicationInfo ?: return null
        ai.sourceDir = file.absolutePath; ai.publicSourceDir = file.absolutePath
        ai.loadIcon(pm)
    }.getOrNull()
}
