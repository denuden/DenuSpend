package com.gmail.vondenuelle.denuspend.utils

import android.content.Context
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import dagger.hilt.android.qualifiers.ApplicationContext


/**
 * Coil Image Loader with cache
 */
class ImageLoader(@ApplicationContext val applicationContext : Context): SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): coil3.ImageLoader {
        return coil3.ImageLoader.Builder(context)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context = context, percent = 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(applicationContext.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .build()
    }
}