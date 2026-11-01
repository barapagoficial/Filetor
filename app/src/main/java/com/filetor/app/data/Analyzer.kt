package com.filetor.app.data

import com.filetor.app.util.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.ArrayDeque
import java.util.PriorityQueue

data class AnalysisResult(
    val totalSize: Long,
    val fileCount: Int,
    val byType: Map<FileType, Long>,
    val topFolders: List<Pair<FileEntry, Long>>,
    val largestFiles: List<FileEntry>,
    val emptyFolders: List<String>,
    val oldDownloads: List<FileEntry>,
)

data class DuplicateGroup(val size: Long, val files: List<FileEntry>) {
    val wasted: Long get() = size * (files.size - 1)
}

object Analyzer {

    suspend fun analyze(root: String, onProgress: (String, Int) -> Unit): AnalysisResult = withContext(Dispatchers.IO) {
        val rootFile = File(root)
        val byType = HashMap<FileType, Long>()
        val largest = PriorityQueue<FileEntry>(compareBy { it.size })
        val empty = mutableListOf<String>()
        var total = 0L; var count = 0
        val top = mutableListOf<Pair<FileEntry, Long>>()
        val children = rootFile.listFiles() ?: emptyArray()
        for (child in children) {
            var childSize = 0L
            val stack = ArrayDeque<File>(); stack.push(child)
            while (stack.isNotEmpty()) {
                ensureActive()
                val f = stack.pop()
                if (f.isDirectory) {
                    val list = f.listFiles()
                    if (list == null || list.isEmpty()) { if (!f.name.startsWith(".")) empty += f.absolutePath; continue }
                    list.forEach { stack.push(it) }
                } else {
                    val len = f.length()
                    childSize += len; total += len; count++
                    val t = FileType.of(f.name)
                    byType[t] = (byType[t] ?: 0L) + len
                    if (largest.size < 60 || len > (largest.peek()?.size ?: 0)) {
                        largest.add(FileEntry.from(f, false)); if (largest.size > 60) largest.poll()
                    }
                    if (count % 400 == 0) onProgress(f.parent ?: "", count)
                }
            }
            if (child.isDirectory) top += FileEntry.from(child, false) to childSize
        }
        val cutoff = System.currentTimeMillis() - 90L * 86_400_000L
        val downloads = File(root, "Download").listFiles()?.filter { it.isFile && it.lastModified() < cutoff }
            ?.map { FileEntry.from(it, false) }?.sortedByDescending { it.size } ?: emptyList()
        AnalysisResult(
            totalSize = total, fileCount = count, byType = byType,
            topFolders = top.sortedByDescending { it.second },
            largestFiles = largest.toList().sortedByDescending { it.size },
            emptyFolders = empty.filter { !it.contains("/Android/") }.take(500),
            oldDownloads = downloads,
        )
    }

    suspend fun findDuplicates(root: String, minSize: Long, onProgress: (String, Float) -> Unit): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        // 1. group by size
        val bySize = HashMap<Long, MutableList<File>>()
        val stack = ArrayDeque<File>(); stack.push(File(root))
        var scanned = 0
        while (stack.isNotEmpty()) {
            ensureActive()
            val d = stack.pop()
            for (f in d.listFiles() ?: continue) {
                if (f.name.startsWith(".")) continue
                if (f.isDirectory) { if (f.absolutePath != "$root/Android") stack.push(f) }
                else {
                    val len = f.length()
                    if (len >= minSize) bySize.getOrPut(len) { mutableListOf() }.add(f)
                    if (++scanned % 500 == 0) onProgress("Escaneando… $scanned archivos", -1f)
                }
            }
        }
        val candidates = bySize.filter { it.value.size > 1 }
        val totalCandidates = candidates.values.sumOf { it.size }.coerceAtLeast(1)
        var processed = 0
        val groups = mutableListOf<DuplicateGroup>()
        for ((size, files) in candidates) {
            ensureActive()
            // 2. partial hash then 3. full hash
            val partial = files.groupBy { hash(it, 64 * 1024) }.values.filter { it.size > 1 }
            for (pg in partial) {
                val full = if (size <= 64 * 1024) listOf(pg) else pg.groupBy { hash(it, Long.MAX_VALUE) }.values.filter { it.size > 1 }
                full.forEach { g -> groups += DuplicateGroup(size, g.map { FileEntry.from(it, false) }.sortedBy { it.lastModified }) }
            }
            processed += files.size
            onProgress("Comparando contenido…", processed.toFloat() / totalCandidates)
        }
        groups.sortedByDescending { it.wasted }
    }

    private fun hash(f: File, limit: Long): String = runCatching {
        val md = MessageDigest.getInstance("MD5")
        FileInputStream(f).use { ins ->
            val buf = ByteArray(1 shl 16); var read = 0L
            while (read < limit) {
                val r = ins.read(buf); if (r < 0) break
                md.update(buf, 0, r); read += r
            }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }.getOrDefault(f.absolutePath)

    suspend fun fileHash(f: File, algorithm: String): String = withContext(Dispatchers.IO) {
        val md = MessageDigest.getInstance(algorithm)
        FileInputStream(f).use { ins ->
            val buf = ByteArray(1 shl 16)
            while (true) { ensureActive(); val r = ins.read(buf); if (r < 0) break; md.update(buf, 0, r) }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }
}
