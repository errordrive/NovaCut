package com.procut.ui.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.util.LruCache
import kotlinx.coroutines.*
import java.io.File

object ThumbnailProvider {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // Use 1/8th of available memory for thumb cache

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun getThumbnail(
        mediaPath: String,
        timeUs: Long,
        targetWidth: Int = 120,
        targetHeight: Int = 120,
        onLoaded: (Bitmap) -> Unit
    ): Bitmap? {
        val cacheKey = "${mediaPath}_${timeUs / 100_000L}_${targetWidth}x$targetHeight"
        val cached = memoryCache.get(cacheKey)
        if (cached != null) {
            return cached
        }

        scope.launch {
            val bitmap = extractFrame(mediaPath, timeUs, targetWidth, targetHeight)
            if (bitmap != null) {
                memoryCache.put(cacheKey, bitmap)
                withContext(Dispatchers.Main) {
                    onLoaded(bitmap)
                }
            }
        }

        return null
    }

    private fun extractFrame(path: String, timeUs: Long, w: Int, h: Int): Bitmap? {
        return try {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(path)
                val frame = retriever.getScaledFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    w,
                    h
                )
                retriever.release()
                frame
            } else {
                createPlaceholderBitmap(w, h, timeUs)
            }
        } catch (e: Exception) {
            createPlaceholderBitmap(w, h, timeUs)
        }
    }

    private fun createPlaceholderBitmap(w: Int, h: Int, timeUs: Long): Bitmap {
        val bmp = Bitmap.createBitmap(w.coerceAtLeast(32), h.coerceAtLeast(32), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val p = Paint().apply {
            isAntiAlias = true
        }

        // Procedural gradient thumbnail based on timestamp
        val hue = ((timeUs / 500_000L) % 360).toFloat()
        p.color = Color.HSVToColor(floatArrayOf(hue, 0.45f, 0.35f))
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)

        // Filmstrip notch
        p.color = Color.argb(80, 255, 255, 255)
        p.strokeWidth = 2f
        canvas.drawLine(0f, 0f, w.toFloat(), 0f, p)
        canvas.drawLine(0f, h.toFloat(), w.toFloat(), h.toFloat(), p)

        return bmp
    }

    fun clearCache() {
        memoryCache.evictAll()
    }
}
