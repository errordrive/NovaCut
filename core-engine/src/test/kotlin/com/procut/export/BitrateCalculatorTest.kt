package com.procut.export

import com.procut.model.BitrateCalculator
import com.procut.model.BitrateLevel
import com.procut.model.ExportResolution
import com.procut.model.VideoCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BitrateCalculatorTest {

    @Test
    fun testStandardBitrateLookups() {
        // 1080p @ 30 FPS baseline
        val b1080pLow = BitrateCalculator.calculateBitrate(
            ExportResolution.RES_1080P, fps = 30, BitrateLevel.LOW, VideoCodec.H264
        )
        assertEquals(8_000_000, b1080pLow)

        val b1080pRec = BitrateCalculator.calculateBitrate(
            ExportResolution.RES_1080P, fps = 30, BitrateLevel.RECOMMENDED, VideoCodec.H264
        )
        assertEquals(12_000_000, b1080pRec)

        val b1080pHigh = BitrateCalculator.calculateBitrate(
            ExportResolution.RES_1080P, fps = 30, BitrateLevel.HIGH, VideoCodec.H264
        )
        assertEquals(16_000_000, b1080pHigh)

        // 720p @ 60 FPS
        val b720p60fps = BitrateCalculator.calculateBitrate(
            ExportResolution.RES_720P, fps = 60, BitrateLevel.RECOMMENDED, VideoCodec.H264
        )
        assertEquals(8_500_000, b720p60fps)

        // 4K @ 60 FPS High
        val b4k60fps = BitrateCalculator.calculateBitrate(
            ExportResolution.RES_4K, fps = 60, BitrateLevel.HIGH, VideoCodec.H264
        )
        assertEquals(65_000_000, b4k60fps)
    }

    @Test
    fun testHevcDiscountMultiplier() {
        // 1080p @ 30 FPS Recommended H264 vs HEVC
        val h264Bitrate = BitrateCalculator.calculateBitrate(
            ExportResolution.RES_1080P, fps = 30, BitrateLevel.RECOMMENDED, VideoCodec.H264
        )
        val hevcBitrate = BitrateCalculator.calculateBitrate(
            ExportResolution.RES_1080P, fps = 30, BitrateLevel.RECOMMENDED, VideoCodec.HEVC
        )

        // HEVC should be 65% of H.264: 12,000,000 * 0.65 = 7,800,000
        assertEquals(7_800_000, hevcBitrate)
        assertTrue("HEVC must achieve smaller bitrate than H264", hevcBitrate < h264Bitrate)
    }

    @Test
    fun testFileSizeEstimation() {
        // 60 seconds (60_000_000 us) at 10 Mbps video + 128 kbps audio
        val durationUs = 60_000_000L
        val videoBitrate = 10_000_000
        val audioBitrate = 128_000

        val estimatedBytes = BitrateCalculator.estimateFileSize(durationUs, videoBitrate, audioBitrate)
        // (10_128_000 * 60) / 8 * 1.01 = 75,960,000 * 1.01 = ~76.7 MB
        val expectedMinBytes = 75_000_000L
        val expectedMaxBytes = 78_000_000L

        assertTrue("File size estimate in expected range", estimatedBytes in expectedMinBytes..expectedMaxBytes)
    }
}
