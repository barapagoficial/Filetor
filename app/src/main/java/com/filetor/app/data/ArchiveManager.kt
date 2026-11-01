package com.filetor.app.data

import com.github.junrar.Archive
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import net.lingala.zip4j.progress.ProgressMonitor
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class PasswordRequiredException(msg: String = "Se requiere contraseña") : Exception(msg)

enum class ArchiveFormat(val label: String, val ext: String, val supportsPassword: Boolean) {
    ZIP("ZIP", "zip", true),
    SEVEN_Z("7z", "7z", false),
    TAR("TAR", "tar", false),
    TAR_GZ("TAR.GZ", "tar.gz", false),
    TAR_XZ("TAR.XZ", "tar.xz", false),
    TAR_BZ2("TAR.BZ2", "tar.bz2", false),
}

/** Read/extract/create archives. All functions are blocking: call from a background thread. */
object ArchiveManager {

    private enum class Kind { ZIP, SEVEN_Z, RAR, TAR, TAR_GZ, TAR_BZ2, TAR_XZ, GZ, BZ2, XZ }

    private fun kindOf(file: File): Kind? {
        val n = file.name.lowercase()
        return when {
            n.endsWith(".tar.gz") || n.endsWith(".tgz") -> Kind.TAR_GZ
            n.endsWith(".tar.bz2") || n.endsWith(".tbz2") || n.endsWith(".tbz") -> Kind.TAR_BZ2
            n.endsWith(".tar.xz") || n.endsWith(".txz") -> Kind.TAR_XZ
            n.endsWith(".tar") -> Kind.TAR
            n.endsWith(".zip") || n.endsWith(".jar") || n.endsWith(".apk") || n.endsWith(".apks") || n.endsWith(".xapk") ||
                n.endsWith(".cbz") || n.endsWith(".epub") || n.endsWith(".docx") || n.endsWith(".xlsx") || n.endsWith(".pptx") -> Kind.ZIP
            n.endsWith(".7z") || n.endsWith(".cb7") -> Kind.SEVEN_Z
            n.endsWith(".rar") || n.endsWith(".cbr") -> Kind.RAR
            n.endsWith(".gz") -> Kind.GZ
            n.endsWith(".bz2") -> Kind.BZ2
            n.endsWith(".xz") -> Kind.XZ
            else -> null
        }
    }

    fun isSupported(file: File): Boolean = kindOf(file) != null

    /** Whether we can browse the inside of this archive (APKs open as normal files). */
    fun isBrowsable(file: File): Boolean {
        val n = file.name.lowercase()
        if (n.endsWith(".apk") || n.endsWith(".docx") || n.endsWith(".xlsx") || n.endsWith(".pptx") || n.endsWith(".epub")) return false
        return isSupported(file)
    }

    fun isEncrypted(file: File): Boolean = when (kindOf(file)) {
        Kind.ZIP -> runCatching { ZipFile(file).isEncrypted }.getOrDefault(false)
        Kind.RAR -> runCatching { Archive(file).use { it.isEncrypted } }.getOrDefault(true)
        else -> false
    }

    private fun norm(p: String) = p.replace('\\', '/').trim('/')

    private fun singleName(file: File): String = file.name.substringBeforeLast('.')

    fun list(file: File, password: String? = null): List<ArchiveEntry> {
        val out = mutableListOf<ArchiveEntry>()
        when (val kind = kindOf(file)) {
            Kind.ZIP -> {
                val zf = ZipFile(file)
                if (password != null) zf.setPassword(password.toCharArray())
                zf.fileHeaders.forEach { h ->
                    out += ArchiveEntry(norm(h.fileName), h.isDirectory, h.uncompressedSize, h.compressedSize, h.lastModifiedTimeEpoch)
                }
            }
            Kind.SEVEN_Z -> {
                open7z(file, password).use { sz ->
                    for (e in sz.entries) {
                        out += ArchiveEntry(norm(e.name), e.isDirectory, e.size,
                            0L, if (e.hasLastModifiedDate) e.lastModifiedDate.time else 0L)
                    }
                }
            }
            Kind.RAR -> {
                openRar(file, password).use { ar ->
                    for (h in ar.fileHeaders) {
                        out += ArchiveEntry(norm(h.fileName), h.isDirectory, h.fullUnpackSize, h.fullPackSize, h.mTime?.time ?: 0L)
                    }
                }
            }
            Kind.TAR, Kind.TAR_GZ, Kind.TAR_BZ2, Kind.TAR_XZ -> {
                TarArchiveInputStream(decompressed(file, kind)).use { tin ->
                    var e: TarArchiveEntry? = tin.nextEntry
                    while (e != null) {
                        out += ArchiveEntry(norm(e.name), e.isDirectory, e.size, 0L, e.modTime.time)
                        e = tin.nextEntry
                    }
                }
            }
            Kind.GZ, Kind.BZ2, Kind.XZ -> out += ArchiveEntry(singleName(file), false, -1L, file.length(), file.lastModified())
            null -> throw IllegalArgumentException("Formato no soportado")
        }
        // Add implicit parent folders
        val dirs = out.filter { it.isDirectory }.map { it.path }.toMutableSet()
        val extra = mutableListOf<ArchiveEntry>()
        out.forEach { e ->
            var p = e.parent
            while (p.isNotEmpty() && dirs.add(p)) {
                extra += ArchiveEntry(p, true, 0, 0, 0)
                p = if (p.contains('/')) p.substringBeforeLast('/') else ""
            }
        }
        return (out + extra).filter { it.path.isNotEmpty() }
    }

    private fun open7z(file: File, password: String?): SevenZFile {
        val b = SevenZFile.builder().setFile(file)
        if (password != null) b.setPassword(password.toCharArray())
        return b.get()
    }

    private fun openRar(file: File, password: String?): Archive {
        val a = if (password != null) Archive(file, password) else Archive(file)
        if (a.isEncrypted && password == null) { a.close(); throw PasswordRequiredException() }
        return a
    }

    private fun decompressed(file: File, kind: Kind): InputStream {
        val raw = BufferedInputStream(FileInputStream(file), 1 shl 16)
        return when (kind) {
            Kind.TAR_GZ, Kind.GZ -> GzipCompressorInputStream(raw, true)
            Kind.TAR_BZ2, Kind.BZ2 -> BZip2CompressorInputStream(raw, true)
            Kind.TAR_XZ, Kind.XZ -> XZCompressorInputStream(raw, true)
            else -> raw
        }
    }

    private fun safeTarget(destDir: File, entryPath: String): File {
        val f = File(destDir, entryPath)
        val canonDest = destDir.canonicalPath
        if (!f.canonicalPath.startsWith(canonDest)) throw SecurityException("Entrada inválida: $entryPath")
        return f
    }

    private fun copyStream(input: InputStream, output: OutputStream, onBytes: (Long) -> Unit, cancelled: () -> Boolean) {
        val buf = ByteArray(1 shl 16)
        while (true) {
            if (cancelled()) throw InterruptedException("Cancelado")
            val r = input.read(buf)
            if (r < 0) break
            output.write(buf, 0, r)
            onBytes(r.toLong())
        }
    }

    /**
     * Extract [file] into [destDir]. If [only] is non-null, only entries whose path equals one of them
     * (or is inside one of them, when it is a folder) are extracted. [stripPrefix] removes a leading folder
     * so that extracting "a/b" puts "b" at the destination root.
     */
    fun extract(
        file: File,
        destDir: File,
        password: String? = null,
        only: Set<String>? = null,
        stripPrefix: String = "",
        onProgress: (done: Long, total: Long, current: String) -> Unit = { _, _, _ -> },
        cancelled: () -> Boolean = { false },
    ) {
        destDir.mkdirs()
        val kind = kindOf(file) ?: throw IllegalArgumentException("Formato no soportado")
        fun wanted(p: String): Boolean = only == null || only.any { o -> p == o || p.startsWith("$o/") }
        fun outPath(p: String): String = if (stripPrefix.isNotEmpty() && p.startsWith("$stripPrefix/")) p.removePrefix("$stripPrefix/") else p
        val total = when (kind) {
            Kind.GZ, Kind.BZ2, Kind.XZ, Kind.TAR, Kind.TAR_GZ, Kind.TAR_BZ2, Kind.TAR_XZ -> file.length()
            else -> runCatching { list(file, password).filter { !it.isDirectory && wanted(it.path) }.sumOf { it.size } }.getOrDefault(0L)
        }
        var done = 0L
        val tick: (Long) -> Unit = { n -> done += n }

        when (kind) {
            Kind.ZIP -> {
                val zf = ZipFile(file)
                if (zf.isEncrypted) {
                    if (password == null) throw PasswordRequiredException()
                    zf.setPassword(password.toCharArray())
                }
                for (h in zf.fileHeaders) {
                    if (cancelled()) throw InterruptedException("Cancelado")
                    val p = norm(h.fileName)
                    if (p.isEmpty() || !wanted(p)) continue
                    val target = safeTarget(destDir, outPath(p))
                    if (h.isDirectory) { target.mkdirs(); continue }
                    target.parentFile?.mkdirs()
                    onProgress(done, total, p)
                    zf.getInputStream(h).use { ins ->
                        BufferedOutputStream(FileOutputStream(target)).use { os ->
                            copyStream(ins, os, { tick(it); onProgress(done, total, p) }, cancelled)
                        }
                    }
                    if (h.lastModifiedTimeEpoch > 0) target.setLastModified(h.lastModifiedTimeEpoch)
                }
            }
            Kind.SEVEN_Z -> {
                open7z(file, password).use { sz ->
                    var e: SevenZArchiveEntry? = sz.nextEntry
                    while (e != null) {
                        if (cancelled()) throw InterruptedException("Cancelado")
                        val p = norm(e.name)
                        if (p.isNotEmpty() && wanted(p)) {
                            val target = safeTarget(destDir, outPath(p))
                            if (e.isDirectory) target.mkdirs() else {
                                target.parentFile?.mkdirs()
                                BufferedOutputStream(FileOutputStream(target)).use { os ->
                                    val buf = ByteArray(1 shl 16)
                                    while (true) {
                                        if (cancelled()) throw InterruptedException("Cancelado")
                                        val r = sz.read(buf)
                                        if (r < 0) break
                                        os.write(buf, 0, r); tick(r.toLong()); onProgress(done, total, p)
                                    }
                                }
                                if (e.hasLastModifiedDate) target.setLastModified(e.lastModifiedDate.time)
                            }
                        }
                        e = sz.nextEntry
                    }
                }
            }
            Kind.RAR -> {
                openRar(file, password).use { ar ->
                    for (h in ar.fileHeaders) {
                        if (cancelled()) throw InterruptedException("Cancelado")
                        val p = norm(h.fileName)
                        if (p.isEmpty() || !wanted(p)) continue
                        val target = safeTarget(destDir, outPath(p))
                        if (h.isDirectory) { target.mkdirs(); continue }
                        target.parentFile?.mkdirs()
                        onProgress(done, total, p)
                        BufferedOutputStream(FileOutputStream(target)).use { os -> ar.extractFile(h, os) }
                        tick(h.fullUnpackSize); onProgress(done, total, p)
                    }
                }
            }
            Kind.TAR, Kind.TAR_GZ, Kind.TAR_BZ2, Kind.TAR_XZ -> {
                val counting = CountingInputStream(BufferedInputStream(FileInputStream(file), 1 shl 16))
                val decomp: InputStream = when (kind) {
                    Kind.TAR_GZ -> GzipCompressorInputStream(counting, true)
                    Kind.TAR_BZ2 -> BZip2CompressorInputStream(counting, true)
                    Kind.TAR_XZ -> XZCompressorInputStream(counting, true)
                    else -> counting
                }
                TarArchiveInputStream(decomp).use { tin ->
                    var e: TarArchiveEntry? = tin.nextEntry
                    while (e != null) {
                        if (cancelled()) throw InterruptedException("Cancelado")
                        val p = norm(e.name)
                        if (p.isNotEmpty() && wanted(p)) {
                            val target = safeTarget(destDir, outPath(p))
                            if (e.isDirectory) target.mkdirs()
                            else if (e.isFile) {
                                target.parentFile?.mkdirs()
                                BufferedOutputStream(FileOutputStream(target)).use { os ->
                                    copyStream(tin, os, { onProgress(counting.count, total, p) }, cancelled)
                                }
                                target.setLastModified(e.modTime.time)
                            }
                        }
                        onProgress(counting.count, total, p)
                        e = tin.nextEntry
                    }
                }
            }
            Kind.GZ, Kind.BZ2, Kind.XZ -> {
                val counting = CountingInputStream(BufferedInputStream(FileInputStream(file), 1 shl 16))
                val ins: InputStream = when (kind) {
                    Kind.GZ -> GzipCompressorInputStream(counting, true)
                    Kind.BZ2 -> BZip2CompressorInputStream(counting, true)
                    else -> XZCompressorInputStream(counting, true)
                }
                val target = FileRepository.uniqueTarget(destDir, singleName(file))
                ins.use { i ->
                    BufferedOutputStream(FileOutputStream(target)).use { os ->
                        copyStream(i, os, { onProgress(counting.count, total, target.name) }, cancelled)
                    }
                }
            }
        }
        onProgress(total, total, "")
    }

    private class CountingInputStream(private val inner: InputStream) : InputStream() {
        var count = 0L; private set
        override fun read(): Int = inner.read().also { if (it >= 0) count++ }
        override fun read(b: ByteArray, off: Int, len: Int): Int = inner.read(b, off, len).also { if (it > 0) count += it }
        override fun close() = inner.close()
        override fun available(): Int = inner.available()
    }

    private fun walk(files: List<File>): List<Pair<File, String>> {
        val out = mutableListOf<Pair<File, String>>()
        fun rec(f: File, rel: String) {
            out += f to rel
            if (f.isDirectory) f.listFiles()?.forEach { rec(it, "$rel/${it.name}") }
        }
        files.forEach { rec(it, it.name) }
        return out
    }

    fun compress(
        sources: List<File>,
        dest: File,
        format: ArchiveFormat,
        level: Int, // 0..9
        password: String?,
        onProgress: (done: Long, total: Long, current: String) -> Unit,
        cancelled: () -> Boolean,
    ) {
        val entries = walk(sources)
        val total = entries.filter { it.first.isFile }.sumOf { it.first.length() }
        var done = 0L
        try {
            when (format) {
                ArchiveFormat.ZIP -> {
                    val zf = ZipFile(dest)
                    val params = ZipParameters().apply {
                        compressionMethod = if (level == 0) CompressionMethod.STORE else CompressionMethod.DEFLATE
                        compressionLevel = when {
                            level <= 1 -> CompressionLevel.FASTEST
                            level <= 3 -> CompressionLevel.FAST
                            level <= 6 -> CompressionLevel.NORMAL
                            level <= 8 -> CompressionLevel.MAXIMUM
                            else -> CompressionLevel.ULTRA
                        }
                        if (!password.isNullOrEmpty()) {
                            isEncryptFiles = true
                            encryptionMethod = EncryptionMethod.AES
                            aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                        }
                    }
                    if (!password.isNullOrEmpty()) zf.setPassword(password.toCharArray())
                    zf.isRunInThread = true
                    val pm = zf.progressMonitor
                    for (src in sources) {
                        if (cancelled()) throw InterruptedException("Cancelado")
                        if (src.isDirectory) zf.addFolder(src, params) else zf.addFile(src, params)
                        val base = done
                        while (pm.state == ProgressMonitor.State.BUSY) {
                            if (cancelled()) { pm.isCancelAllTasks = true }
                            onProgress(base + pm.workCompleted, total, pm.fileName?.substringAfterLast('/') ?: src.name)
                            Thread.sleep(80)
                        }
                        if (pm.result == ProgressMonitor.Result.ERROR) throw pm.exception ?: Exception("Error al comprimir")
                        if (pm.result == ProgressMonitor.Result.CANCELLED) throw InterruptedException("Cancelado")
                        done = base + if (src.isDirectory) entries.filter { it.second.startsWith(src.name + "/") && it.first.isFile }.sumOf { it.first.length() } else src.length()
                    }
                }
                ArchiveFormat.SEVEN_Z -> {
                    SevenZOutputFile(dest).use { out ->
                        for ((f, rel) in entries) {
                            if (cancelled()) throw InterruptedException("Cancelado")
                            val e = out.createArchiveEntry(f, rel)
                            out.putArchiveEntry(e)
                            if (f.isFile) FileInputStream(f).use { ins ->
                                val buf = ByteArray(1 shl 16)
                                while (true) {
                                    if (cancelled()) throw InterruptedException("Cancelado")
                                    val r = ins.read(buf); if (r < 0) break
                                    out.write(buf, 0, r); done += r; onProgress(done, total, f.name)
                                }
                            }
                            out.closeArchiveEntry()
                        }
                    }
                }
                ArchiveFormat.TAR, ArchiveFormat.TAR_GZ, ArchiveFormat.TAR_XZ, ArchiveFormat.TAR_BZ2 -> {
                    val raw = BufferedOutputStream(FileOutputStream(dest), 1 shl 16)
                    val lv = level.coerceIn(1, 9)
                    val comp: OutputStream = when (format) {
                        ArchiveFormat.TAR_GZ -> GzipCompressorOutputStream(raw, org.apache.commons.compress.compressors.gzip.GzipParameters().apply { compressionLevel = lv })
                        ArchiveFormat.TAR_XZ -> XZCompressorOutputStream(raw, lv.coerceAtMost(9))
                        ArchiveFormat.TAR_BZ2 -> BZip2CompressorOutputStream(raw, lv)
                        else -> raw
                    }
                    TarArchiveOutputStream(comp).use { tout ->
                        tout.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
                        tout.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX)
                        for ((f, rel) in entries) {
                            if (cancelled()) throw InterruptedException("Cancelado")
                            val e = TarArchiveEntry(f, if (f.isDirectory) "$rel/" else rel)
                            tout.putArchiveEntry(e)
                            if (f.isFile) FileInputStream(f).use { ins ->
                                copyStream(ins, tout, { done += it; onProgress(done, total, f.name) }, cancelled)
                            }
                            tout.closeArchiveEntry()
                        }
                        tout.finish()
                    }
                }
            }
        } catch (t: Throwable) {
            dest.delete()
            throw t
        }
        onProgress(total, total, "")
    }
}
