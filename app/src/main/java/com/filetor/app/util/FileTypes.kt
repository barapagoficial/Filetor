package com.filetor.app.util

import android.webkit.MimeTypeMap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.TextSnippet
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class FileType(val label: String, val color: Color, val icon: ImageVector) {
    FOLDER("Carpeta", Color(0xFF5B6CF0), Icons.Rounded.Folder),
    IMAGE("Imagen", Color(0xFFE5578A), Icons.Rounded.Image),
    VIDEO("Vídeo", Color(0xFF9466F0), Icons.Rounded.VideoFile),
    AUDIO("Audio", Color(0xFFF08A1C), Icons.Rounded.AudioFile),
    PDF("PDF", Color(0xFFE5484D), Icons.Rounded.PictureAsPdf),
    DOCUMENT("Documento", Color(0xFF2F80ED), Icons.Rounded.Description),
    SHEET("Hoja de cálculo", Color(0xFF1E9E5A), Icons.Rounded.TableChart),
    SLIDES("Presentación", Color(0xFFE0752D), Icons.Rounded.Slideshow),
    ARCHIVE("Archivo comprimido", Color(0xFFB7791F), Icons.Rounded.FolderZip),
    APK("Aplicación", Color(0xFF2EB872), Icons.Rounded.Android),
    CODE("Código", Color(0xFF14A3A3), Icons.Rounded.Code),
    TEXT("Texto", Color(0xFF64748B), Icons.Rounded.TextSnippet),
    OTHER("Archivo", Color(0xFF8792A8), Icons.Rounded.InsertDriveFile);

    companion object {
        private val image = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg", "ico", "tif", "tiff", "avif", "raw", "dng")
        private val video = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "m4v", "ts", "mpg", "mpeg")
        private val audio = setOf("mp3", "wav", "ogg", "flac", "aac", "m4a", "wma", "opus", "amr", "mid", "midi")
        private val doc = setOf("doc", "docx", "odt", "rtf", "epub", "pages")
        private val sheet = setOf("xls", "xlsx", "ods", "csv", "tsv")
        private val slides = setOf("ppt", "pptx", "odp", "key")
        val archiveExt = setOf("zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "tbz2", "xz", "txz", "jar", "zst", "lz4", "cab", "iso", "apks", "xapk")
        private val code = setOf("kt", "java", "py", "js", "ts", "c", "cpp", "h", "hpp", "cs", "go", "rs", "php", "rb", "swift", "sh", "html", "htm", "css", "xml", "json", "yml", "yaml", "gradle", "kts", "sql", "lua", "dart", "toml", "ini", "cfg", "conf", "smali", "prop")
        private val text = setOf("txt", "md", "log", "srt", "ass", "nfo", "readme")

        fun of(name: String, isDir: Boolean = false): FileType {
            if (isDir) return FOLDER
            val ext = name.substringAfterLast('.', "").lowercase()
            return when {
                ext.isEmpty() -> OTHER
                ext in image -> IMAGE
                ext in video -> VIDEO
                ext in audio -> AUDIO
                ext == "pdf" -> PDF
                ext in doc -> DOCUMENT
                ext in sheet -> SHEET
                ext in slides -> SLIDES
                ext == "apk" -> APK
                ext in archiveExt -> ARCHIVE
                ext in code -> CODE
                ext in text -> TEXT
                else -> OTHER
            }
        }

        fun isEditable(name: String): Boolean = of(name).let { it == TEXT || it == CODE } ||
                name.substringAfterLast('.', "").lowercase() in setOf("csv", "tsv")

        fun mime(name: String): String {
            val ext = name.substringAfterLast('.', "").lowercase()
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: when (of(name)) {
                IMAGE -> "image/*"
                VIDEO -> "video/*"
                AUDIO -> "audio/*"
                TEXT, CODE -> "text/plain"
                APK -> "application/vnd.android.package-archive"
                else -> "*/*"
            }
        }
    }
}
