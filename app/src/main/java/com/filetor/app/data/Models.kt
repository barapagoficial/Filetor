package com.filetor.app.data

import com.filetor.app.util.FileType
import java.io.File

data class FileEntry(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val isHidden: Boolean,
    val childCount: Int = -1,
) {
    val file: File get() = File(path)
    val extension: String get() = if (isDirectory) "" else name.substringAfterLast('.', "").lowercase()
    val type: FileType by lazy { FileType.of(name, isDirectory) }

    companion object {
        fun from(f: File, countChildren: Boolean = true): FileEntry {
            val dir = f.isDirectory
            return FileEntry(
                path = f.absolutePath,
                name = f.name,
                isDirectory = dir,
                size = if (dir) 0L else f.length(),
                lastModified = f.lastModified(),
                isHidden = f.name.startsWith("."),
                childCount = if (dir && countChildren) (f.list()?.size ?: 0) else -1,
            )
        }
    }
}

enum class SortBy(val label: String) { NAME("Nombre"), DATE("Fecha"), SIZE("Tamaño"), TYPE("Tipo") }

enum class ViewMode { LIST, GRID }

enum class ThemeMode(val label: String) { SYSTEM("Sistema"), LIGHT("Claro"), DARK("Oscuro") }

data class Clipboard(val paths: List<String>, val cut: Boolean)

data class StorageVolumeInfo(
    val path: String,
    val label: String,
    val isPrimary: Boolean,
    val isRemovable: Boolean,
    val total: Long,
    val free: Long,
) {
    val used: Long get() = total - free
}

data class TrashItem(
    val id: String,
    val name: String,
    val originalPath: String,
    val trashedPath: String,
    val deletedAt: Long,
    val size: Long,
    val isDirectory: Boolean,
)

data class ArchiveEntry(
    val path: String,          // normalized, no leading or trailing slash
    val isDirectory: Boolean,
    val size: Long,
    val compressedSize: Long,
    val time: Long,
) {
    val name: String get() = path.substringAfterLast('/')
    val parent: String get() = if (path.contains('/')) path.substringBeforeLast('/') else ""
}
