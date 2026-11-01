package com.filetor.app.data

import android.content.Context
import android.provider.MediaStore
import com.filetor.app.util.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class Category(val label: String, val type: FileType) {
    IMAGES("Imágenes", FileType.IMAGE),
    VIDEOS("Vídeos", FileType.VIDEO),
    AUDIO("Música", FileType.AUDIO),
    DOCUMENTS("Documentos", FileType.DOCUMENT),
    APKS("APKs", FileType.APK),
    ARCHIVES("Comprimidos", FileType.ARCHIVE),
    DOWNLOADS("Descargas", FileType.FOLDER),
    RECENT("Recientes", FileType.OTHER),
}

/** Fast category queries backed by MediaStore (falls back to a filesystem walk for some types). */
object MediaRepository {
    private val docExt = listOf("pdf", "doc", "docx", "odt", "rtf", "txt", "xls", "xlsx", "ods", "csv", "ppt", "pptx", "odp", "epub", "md")
    private val archiveExt = listOf("zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz", "txz")

    suspend fun query(context: Context, category: Category): List<FileEntry> = withContext(Dispatchers.IO) {
        when (category) {
            Category.DOWNLOADS -> {
                val dir = File(FileRepository.primaryRoot, "Download")
                return@withContext dir.listFiles()?.filter { !it.name.startsWith(".") }
                    ?.map { FileEntry.from(it) }?.sortedByDescending { it.lastModified } ?: emptyList()
            }
            else -> {}
        }
        val uri = MediaStore.Files.getContentUri("external")
        val (selection, args) = when (category) {
            Category.IMAGES -> "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?" to arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
            Category.VIDEOS -> "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?" to arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
            Category.AUDIO -> "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?" to arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO.toString())
            Category.DOCUMENTS -> docExt.joinToString(" OR ") { "${MediaStore.Files.FileColumns.DATA} LIKE ?" } to docExt.map { "%.$it" }.toTypedArray()
            Category.APKS -> "${MediaStore.Files.FileColumns.DATA} LIKE ?" to arrayOf("%.apk")
            Category.ARCHIVES -> archiveExt.joinToString(" OR ") { "${MediaStore.Files.FileColumns.DATA} LIKE ?" } to archiveExt.map { "%.$it" }.toTypedArray()
            Category.RECENT -> "${MediaStore.Files.FileColumns.DATE_MODIFIED}>? AND ${MediaStore.Files.FileColumns.MIME_TYPE} IS NOT NULL" to
                arrayOf(((System.currentTimeMillis() / 1000) - 7 * 86400).toString())
            Category.DOWNLOADS -> "" to emptyArray<String>()
        }
        val out = mutableListOf<FileEntry>()
        val proj = arrayOf(MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.DATE_MODIFIED)
        runCatching {
            context.contentResolver.query(uri, proj, selection, args, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")?.use { c ->
                val iData = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val iSize = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val iDate = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                while (c.moveToNext()) {
                    val path = c.getString(iData) ?: continue
                    val name = path.substringAfterLast('/')
                    if (category == Category.RECENT && (name.startsWith(".") || path.contains("/."))) continue
                    if (path.contains("/${TrashManager.DIR_NAME}/")) continue
                    val f = File(path)
                    if (!f.exists() || f.isDirectory) continue
                    out += FileEntry(path, name, false, c.getLong(iSize).takeIf { it > 0 } ?: f.length(), c.getLong(iDate) * 1000, name.startsWith("."))
                    if (category == Category.RECENT && out.size >= 300) break
                }
            }
        }
        out
    }
}
