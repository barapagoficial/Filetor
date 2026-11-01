package com.filetor.app.data

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import com.filetor.app.util.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.text.Collator
import java.util.ArrayDeque
import java.util.Locale
import kotlin.coroutines.coroutineContext

object FileRepository {

    val primaryRoot: String = Environment.getExternalStorageDirectory().absolutePath

    fun volumes(context: Context): List<StorageVolumeInfo> {
        val result = mutableListOf<StorageVolumeInfo>()
        val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        runCatching {
            for (v in sm.storageVolumes) {
                val dir: File? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) v.directory else runCatching {
                    v.javaClass.getMethod("getPathFile").invoke(v) as File
                }.getOrNull()
                if (dir == null || !dir.exists()) continue
                val stat = runCatching { StatFs(dir.absolutePath) }.getOrNull() ?: continue
                val label = when {
                    v.isPrimary -> "Almacenamiento interno"
                    else -> v.getDescription(context) ?: "Tarjeta SD"
                }
                result += StorageVolumeInfo(
                    path = dir.absolutePath,
                    label = label,
                    isPrimary = v.isPrimary,
                    isRemovable = v.isRemovable,
                    total = stat.totalBytes,
                    free = stat.availableBytes,
                )
            }
        }
        if (result.none { it.isPrimary }) {
            val stat = StatFs(primaryRoot)
            result.add(0, StorageVolumeInfo(primaryRoot, "Almacenamiento interno", true, false, stat.totalBytes, stat.availableBytes))
        }
        return result
    }

    fun volumeRootFor(context: Context, path: String): String =
        volumes(context).map { it.path }.filter { path.startsWith(it) }.maxByOrNull { it.length } ?: primaryRoot

    fun displayPath(context: Context, path: String): String {
        val vols = volumes(context)
        val v = vols.filter { path.startsWith(it.path) }.maxByOrNull { it.path.length } ?: return path
        return v.label + path.removePrefix(v.path)
    }

    private val collator: Collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }

    fun comparator(sortBy: SortBy = Prefs.sortBy, asc: Boolean = Prefs.sortAsc, foldersFirst: Boolean = Prefs.foldersFirst): Comparator<FileEntry> {
        val byName = Comparator<FileEntry> { a, b -> naturalCompare(a.name, b.name) }
        var base: Comparator<FileEntry> = when (sortBy) {
            SortBy.NAME -> byName
            SortBy.DATE -> compareBy<FileEntry> { it.lastModified }.then(byName)
            SortBy.SIZE -> compareBy<FileEntry> { if (it.isDirectory) it.childCount.toLong() else it.size }.then(byName)
            SortBy.TYPE -> compareBy<FileEntry> { it.extension }.then(byName)
        }
        if (!asc) base = base.reversed()
        return if (foldersFirst) compareBy<FileEntry> { !it.isDirectory }.then(base) else base
    }

    /** Natural ("file2" < "file10") and locale-aware comparison. */
    fun naturalCompare(a: String, b: String): Int {
        var i = 0; var j = 0
        while (i < a.length && j < b.length) {
            val ca = a[i]; val cb = b[j]
            if (ca.isDigit() && cb.isDigit()) {
                var ei = i; while (ei < a.length && a[ei].isDigit()) ei++
                var ej = j; while (ej < b.length && b[ej].isDigit()) ej++
                val na = a.substring(i, ei).trimStart('0'); val nb = b.substring(j, ej).trimStart('0')
                if (na.length != nb.length) return na.length - nb.length
                val c = na.compareTo(nb); if (c != 0) return c
                i = ei; j = ej
            } else {
                var ei = i; while (ei < a.length && !a[ei].isDigit()) ei++
                var ej = j; while (ej < b.length && !b[ej].isDigit()) ej++
                val c = collator.compare(a.substring(i, ei), b.substring(j, ej))
                if (c != 0) return c
                i = ei; j = ej
            }
        }
        return (a.length - i) - (b.length - j)
    }

    suspend fun list(path: String, showHidden: Boolean = Prefs.showHidden): List<FileEntry> = withContext(Dispatchers.IO) {
        val dir = File(path)
        val files = dir.listFiles() ?: throw SecurityException("No se puede leer esta carpeta")
        files.asSequence()
            .filter { showHidden || !it.name.startsWith(".") }
            .map { FileEntry.from(it, Prefs.showFileCount) }
            .sortedWith(comparator())
            .toList()
    }

    fun search(root: String, query: String, type: FileType?, includeHidden: Boolean): Flow<FileEntry> = flow {
        val q = query.trim().lowercase()
        val wildcard = if (q.contains('*') || q.contains('?')) {
            Regex(q.split('*').joinToString(".*") { part -> part.split('?').joinToString(".") { Regex.escape(it) } })
        } else null
        val stack = ArrayDeque<File>()
        stack.push(File(root))
        while (stack.isNotEmpty()) {
            coroutineContext.ensureActive()
            val dir = stack.pop()
            val children = dir.listFiles() ?: continue
            for (f in children) {
                if (!includeHidden && f.name.startsWith(".")) continue
                val isDir = f.isDirectory
                if (isDir) stack.push(f)
                val name = f.name.lowercase()
                val matchName = q.isEmpty() || (wildcard?.matches(name) ?: name.contains(q))
                val matchType = type == null || FileType.of(f.name, isDir) == type
                if (matchName && matchType) emit(FileEntry.from(f, false))
            }
        }
    }.flowOn(Dispatchers.IO)

    data class DirStats(val size: Long, val files: Int, val folders: Int)

    suspend fun dirStats(file: File, onProgress: (DirStats) -> Unit = {}): DirStats = withContext(Dispatchers.IO) {
        var size = 0L; var files = 0; var folders = 0
        val stack = ArrayDeque<File>(); stack.push(file)
        var tick = 0
        while (stack.isNotEmpty()) {
            coroutineContext.ensureActive()
            val f = stack.pop()
            if (f.isDirectory) {
                if (f != file) folders++
                f.listFiles()?.forEach { stack.push(it) }
            } else {
                files++; size += f.length()
            }
            if (++tick % 500 == 0) onProgress(DirStats(size, files, folders))
        }
        DirStats(size, files, folders)
    }

    fun uniqueTarget(dir: File, name: String): File {
        var target = File(dir, name)
        if (!target.exists()) return target
        val base: String; val ext: String
        val dot = name.lastIndexOf('.')
        if (dot > 0 && !File(dir, name).isDirectory) { base = name.substring(0, dot); ext = name.substring(dot) } else { base = name; ext = "" }
        var i = 1
        while (target.exists()) { target = File(dir, "$base ($i)$ext"); i++ }
        return target
    }

    fun isValidName(name: String): Boolean =
        name.isNotBlank() && name != "." && name != ".." && name.none { it in "/\u0000" }
}
