package com.filetor.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.filetor.app.data.OperationManager
import com.filetor.app.data.Prefs
import com.filetor.app.data.TrashManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FiletorApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        OperationManager.init(this)
        TrashManager.load()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { TrashManager.autoClean() }
            runCatching { java.io.File(cacheDir, "archive_preview").deleteRecursively() }
        }
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(VideoFrameDecoder.Factory()) }
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.2).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("thumbs")).maxSizeBytes(150L * 1024 * 1024).build() }
        .crossfade(true)
        .build()
}
