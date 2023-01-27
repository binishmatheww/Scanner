package com.binishmatheww.scanner.common

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class Scanner : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {

        return ImageLoader
            .Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(
                        cacheDir.resolve("coilCache")
                    )
                    .maxSizeBytes(10 * 1024 * 1024)
                    .build()
            }
            .build()

    }

}
