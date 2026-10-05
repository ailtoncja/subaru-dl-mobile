package com.subarudl.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.subarudl.app.data.SettingsStore
import com.subarudl.app.service.DownloadManager
import com.subarudl.app.service.Notifier

class SubaruApp : Application(), SingletonImageLoader.Factory {
    lateinit var manager: DownloadManager
        private set

    override fun onCreate() {
        super.onCreate()
        SettingsStore.init(this)
        Notifier.createChannels(this)
        manager = DownloadManager(this)
        manager.start()
    }

    /** Miniaturas: cache de disco para a lista de vídeos não baixar a mesma capa duas vezes. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .crossfade(true)
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.2).build() }
            .diskCache { DiskCache.Builder().directory(cacheDir.resolve("capas")).maxSizeBytes(150L * 1024 * 1024).build() }
            .build()
}
