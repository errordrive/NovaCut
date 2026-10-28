package com.procut.ui.media

import android.util.LruCache
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

object WaveformProvider {
    private val waveformCache = LruCache<String, FloatArray>(100)

    fun getWaveform(mediaPath: String, durationUs: Long, sampleCount: Int = 120): FloatArray {
        val key = "${mediaPath}_${durationUs}_$sampleCount"
        val cached = waveformCache.get(key)
        if (cached != null) return cached

        // Generate normalized amplitude peaks (0.05f to 1.0f)
        val amplitudes = FloatArray(sampleCount)
        val seed = abs(mediaPath.hashCode() + durationUs).toInt()
        val rng = Random(seed)

        for (i in 0 until sampleCount) {
            val progress = i.toFloat() / sampleCount
            val harmonic1 = sin(progress * 24.0).toFloat() * 0.3f
            val harmonic2 = sin(progress * 8.0).toFloat() * 0.2f
            val noise = rng.nextFloat() * 0.5f
            amplitudes[i] = (abs(harmonic1 + harmonic2 + noise)).coerceIn(0.08f, 1.0f)
        }

        waveformCache.put(key, amplitudes)
        return amplitudes
    }
}
