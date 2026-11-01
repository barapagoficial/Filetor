package com.filetor.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Recycle bin: items are moved into a hidden folder on the same volume, so trashing is instant. */
object TrashManager {
    const val DIR_NAME = ".FiletorTrash"
    val items = mutableStateListOf<TrashItem>()

    fun load() {
        items.clear()
        runCatching {
            val arr = JSONArray(Prefs.getString("trash_index", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val item = TrashItem(
                    id = o.getString("id"), name = o.getString("name"), originalPath = o.getString("orig"),
                    trashedPath = o.getString("path"), deletedAt = o.getLong("at"), size = o.optLong("size"),
                    isDirectory = o.optBoolean("dir"),
                )
                if (File(item.trashedPath).exists()) items.add(item)
            }
        }
        items.sortByDescending { it.deletedAt }
    }

    private fun save() {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name).put("orig", it.originalPath)
                .put("path", it.trashedPath).put("at", it.deletedAt).put("size", it.size).put("dir", it.isDirectory))
        }
        Prefs.putString("trash_index", arr.toString())
    }

    fun trashDirFor(context: Context, path: String): File {
        val root = FileRepository.volumeRootFor(context, path)
        return File(root, DIR_NAME).apply {
            if (!exists()) { mkdirs(); runCatching { File(this, ".nomedia").createNewFile() } }
        }
    }

    /** Returns true if moved to trash. */
    fun moveToTrash(context: Context, file: File, size: Long): Boolean {
        if (file.absolutePath.contains("/$DIR_NAME")) return false
        val dir = trashDirFor(context, file.absolutePath)
        val id = System.currentTimeMillis().toString(36) + (0..9999).random()
        val target = File(dir, id + "_" + file.name)
        val isDir = file.isDirectory
        if (!file.renameTo(target)) return false
        synchronized(items) {
            items.add(0, TrashItem(id, file.name, file.absolutePath, target.absolutePath, System.currentTimeMillis(), size, isDir))
            save()
        }
        return true
    }

    suspend fun restore(item: TrashItem): Boolean = withContext(Dispatchers.IO) {
        val src = File(item.trashedPath)
        val orig = File(item.originalPath)
        orig.parentFile?.mkdirs()
        val dest = if (orig.exists()) FileRepository.uniqueTarget(orig.parentFile!!, orig.name) else orig
        val ok = src.renameTo(dest)
        if (ok) synchronized(items) { items.remove(item); save() }
        ok
    }

    suspend fun delete(item: TrashItem) = withContext(Dispatchers.IO) {
        File(item.trashedPath).deleteRecursively()
        synchronized(items) { items.remove(item); save() }
    }

    suspend fun empty() = withContext(Dispatchers.IO) {
        items.toList().forEach { File(it.trashedPath).deleteRecursively() }
        synchronized(items) { items.clear(); save() }
    }

    suspend fun autoClean() = withContext(Dispatchers.IO) {
        val days = Prefs.trashDays
        if (days <= 0) return@withContext
        val limit = System.currentTimeMillis() - days * 86_400_000L
        items.filter { it.deletedAt < limit }.forEach { delete(it) }
    }

    val totalSize: Long get() = items.sumOf { it.size }
}
