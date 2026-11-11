package com.example.core

import android.graphics.Bitmap
import android.util.LruCache

object ImageCache {

    // Bounded 16MB LRU Bitmap cache to avoid repeated decoding while keeping RAM footprint tiny
    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSizeKb = (maxMemoryKb / 12).coerceIn(4096, 16384)

    private val bitmapCache = object : LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.allocationByteCount / 1024).coerceAtLeast(1)
        }
    }

    fun get(key: String): Bitmap? {
        val bmp = bitmapCache.get(key)
        return if (bmp != null && !bmp.isRecycled) bmp else null
    }

    fun put(key: String, bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            bitmapCache.put(key, bitmap)
        }
    }

    fun evictAll() {
        bitmapCache.evictAll()
    }

    fun currentSizeBytes(): Long {
        return bitmapCache.size().toLong() * 1024L
    }
}
