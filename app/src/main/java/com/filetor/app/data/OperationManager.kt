package com.filetor.app.data

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import androidx.core.content.ContextCompat
import com.filetor.app.service.OperationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger

enum class OpKind(val verb: String, val doneMsg: String) {
    COPY("Copiando", "Copia completada"),
    MOVE("Moviendo", "Movimiento completado"),
    DELETE("Eliminando", "Eliminado"),
    TRASH("Moviendo a la papelera", "Movido a la papelera"),
    COMPRESS("Comprimiendo", "Archivo creado"),
    EXTRACT("Extrayendo", "Extracción completada"),
}

data class OpTask(
    val id: Int,
    val kind: OpKind,
    val title: String,
    val progress: Float = -1f, // -1 = indeterminate
    val current: String = "",
    val doneBytes: Long = 0,
    val totalBytes: Long = 0,
    val startedAt: Long = System.currentTimeMillis(),
)

sealed class OpEvent {
    data class Finished(val kind: OpKind, val message: String, val affectedDirs: Set<String>, val undo: (() -> Unit)? = null) : OpEvent()
    data class Failed(val kind: OpKind, val message: String, val affectedDirs: Set<String>) : OpEvent()
    data class NeedPassword(val archive: String, val dest: String, val only: Set<String>?, val strip: String) : OpEvent()
}

/** Runs long file operations in the background with progress reporting. */
object OperationManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ids = AtomicInteger(1)
    private val jobs = mutableMapOf<Int, Job>()
    @Volatile private var cancelled = mutableSetOf<Int>()

    private val _tasks = MutableStateFlow<List<OpTask>>(emptyList())
    val tasks: StateFlow<List<OpTask>> = _tasks.asStateFlow()

    private val _events = MutableSharedFlow<OpEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<OpEvent> = _events.asSharedFlow()

    private lateinit var appContext: Context

    fun init(context: Context) { appContext = context.applicationContext }

    private fun update(id: Int, f: (OpTask) -> OpTask) {
        _tasks.update { list -> list.map { if (it.id == id) f(it) else it } }
    }

    fun cancel(id: Int) { synchronized(cancelled) { cancelled.add(id) }; jobs[id]?.cancel() }

    private fun isCancelled(id: Int) = synchronized(cancelled) { id in cancelled }

    private fun startService() {
        runCatching {
            ContextCompat.startForegroundService(appContext, Intent(appContext, OperationService::class.java))
        }
    }

    private fun run(kind: OpKind, title: String, affected: Set<String>, block: suspend (id: Int) -> String) {
        val id = ids.getAndIncrement()
        _tasks.update { it + OpTask(id, kind, title) }
        startService()
        val job = scope.launch {
            try {
                val msg = block(id)
                _events.emit(OpEvent.Finished(kind, msg, affected))
            } catch (t: Throwable) {
                val msg = if (isCancelled(id) || t is InterruptedException) "Operación cancelada" else (t.message ?: t.javaClass.simpleName)
                _events.emit(OpEvent.Failed(kind, msg, affected))
            } finally {
                _tasks.update { list -> list.filterNot { it.id == id } }
                jobs.remove(id)
                synchronized(cancelled) { cancelled.remove(id) }
                scan(affected)
            }
        }
        jobs[id] = job
    }

    private fun scan(dirs: Set<String>) {
        runCatching { MediaScannerConnection.scanFile(appContext, dirs.toTypedArray(), null, null) }
    }

    private fun progressFn(id: Int): (Long, Long, String) -> Unit {
        var last = 0L
        return { done, total, current ->
            val now = System.currentTimeMillis()
            if (now - last > 120 || done >= total) {
                last = now
                update(id) { it.copy(progress = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else -1f, current = current, doneBytes = done, totalBytes = total) }
            }
        }
    }

    // ---------- copy / move ----------
    fun copy(sources: List<File>, destDir: File, move: Boolean) {
        val kind = if (move) OpKind.MOVE else OpKind.COPY
        val title = "${kind.verb} ${if (sources.size == 1) "\"${sources[0].name}\"" else "${sources.size} elementos"}"
        val affected = sources.mapNotNull { it.parent }.toSet() + destDir.absolutePath
        run(kind, title, affected) { id ->
            val cancelledFn = { isCancelled(id) }
            for (s in sources) {
                if (destDir.absolutePath == s.absolutePath || destDir.absolutePath.startsWith(s.absolutePath + "/"))
                    throw IllegalArgumentException("No se puede ${if (move) "mover" else "copiar"} una carpeta dentro de sí misma")
            }
            // Fast path for moves on the same volume
            val remaining = mutableListOf<File>()
            if (move) {
                for (s in sources) {
                    if (s.parentFile?.absolutePath == destDir.absolutePath) continue
                    val target = FileRepository.uniqueTarget(destDir, s.name)
                    if (!s.renameTo(target)) remaining += s else Prefs.renamePath(s.absolutePath, target.absolutePath)
                }
            } else remaining += sources
            if (remaining.isNotEmpty()) {
                var total = 0L
                remaining.forEach { total += FileRepository.dirStats(it).size }
                var done = 0L
                val prog = progressFn(id)
                for (s in remaining) {
                    val target = FileRepository.uniqueTarget(destDir, s.name)
                    copyRecursive(s, target, { n -> done += n; prog(done, total, s.name) }, cancelledFn)
                    if (move) s.deleteRecursively()
                }
            }
            if (sources.size == 1) "${kind.doneMsg}: ${sources[0].name}" else "${kind.doneMsg} (${sources.size} elementos)"
        }
    }

    private fun copyRecursive(src: File, dst: File, onBytes: (Long) -> Unit, cancelled: () -> Boolean) {
        if (cancelled()) throw InterruptedException()
        if (src.isDirectory) {
            if (!dst.exists() && !dst.mkdirs()) throw java.io.IOException("No se pudo crear ${dst.name}")
            src.listFiles()?.forEach { copyRecursive(it, File(dst, it.name), onBytes, cancelled) }
            dst.setLastModified(src.lastModified())
        } else {
            try {
                FileInputStream(src).use { ins ->
                    FileOutputStream(dst).use { out ->
                        val buf = ByteArray(1 shl 17)
                        while (true) {
                            if (cancelled()) throw InterruptedException()
                            val r = ins.read(buf); if (r < 0) break
                            out.write(buf, 0, r); onBytes(r.toLong())
                        }
                    }
                }
                dst.setLastModified(src.lastModified())
            } catch (t: Throwable) {
                dst.delete(); throw t
            }
        }
    }

    // ---------- delete ----------
    fun delete(files: List<File>, toTrash: Boolean) {
        val kind = if (toTrash) OpKind.TRASH else OpKind.DELETE
        val title = "${kind.verb} ${if (files.size == 1) "\"${files[0].name}\"" else "${files.size} elementos"}"
        val affected = files.mapNotNull { it.parent }.toSet()
        run(kind, title, affected) { id ->
            var i = 0
            var failed = 0
            for (f in files) {
                if (isCancelled(id)) throw InterruptedException()
                update(id) { it.copy(progress = i.toFloat() / files.size, current = f.name) }
                val ok = if (toTrash) {
                    val size = if (f.isDirectory) FileRepository.dirStats(f).size else f.length()
                    TrashManager.moveToTrash(appContext, f, size) || f.deleteRecursively()
                } else f.deleteRecursively()
                if (!ok) failed++
                Prefs.removeRecent(f.absolutePath)
                i++
            }
            if (failed > 0) throw java.io.IOException("No se pudieron eliminar $failed elementos")
            if (files.size == 1) "${kind.doneMsg}: ${files[0].name}" else "${kind.doneMsg} (${files.size} elementos)"
        }
    }

    // ---------- archives ----------
    fun compress(sources: List<File>, dest: File, format: ArchiveFormat, level: Int, password: String?) {
        run(OpKind.COMPRESS, "Comprimiendo \"${dest.name}\"", setOf(dest.parent ?: "")) { id ->
            ArchiveManager.compress(sources, dest, format, level, password, progressFn(id)) { isCancelled(id) }
            "Creado ${dest.name}"
        }
    }

    fun extract(archive: File, destDir: File, password: String?, only: Set<String>? = null, strip: String = "") {
        val id = ids.getAndIncrement()
        val title = "Extrayendo \"${archive.name}\""
        _tasks.update { it + OpTask(id, OpKind.EXTRACT, title) }
        startService()
        val affected = setOf(destDir.absolutePath, destDir.parent ?: "")
        jobs[id] = scope.launch {
            try {
                ArchiveManager.extract(archive, destDir, password, only, strip, progressFn(id)) { isCancelled(id) }
                _events.emit(OpEvent.Finished(OpKind.EXTRACT, "Extraído en ${destDir.name}", affected))
            } catch (t: Throwable) {
                if (t.javaClass.simpleName == "PasswordRequiredException" || t.cause?.javaClass?.simpleName == "PasswordRequiredException") {
                    _events.emit(OpEvent.NeedPassword(archive.absolutePath, destDir.absolutePath, only, strip))
                    return@launch
                }
                val wrongPw = password != null && (t.message?.contains("password", true) == true || t.javaClass.simpleName.contains("Password") || t.message?.contains("Wrong", true) == true)
                val msg = when {
                    isCancelled(id) || t is InterruptedException -> "Operación cancelada"
                    wrongPw -> "Contraseña incorrecta"
                    else -> t.message ?: t.javaClass.simpleName
                }
                _events.emit(OpEvent.Failed(OpKind.EXTRACT, msg, affected))
            } finally {
                _tasks.update { list -> list.filterNot { it.id == id } }
                jobs.remove(id)
                synchronized(cancelled) { cancelled.remove(id) }
                scan(affected)
            }
        }
    }
}
