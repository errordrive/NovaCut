package com.procut.export

import com.procut.model.AspectRatio
import com.procut.model.ExportConfig
import com.procut.model.ExportResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportConfigTest {

    @Test
    fun testMacroblockAlignmentFor1080p() {
        val config = ExportConfig(resolution = ExportResolution.RES_1080P)

        // 9:16 (1080 x 1920) -> 1088 x 1920 or 1080 aligned
        // 1080 / 16 = 67.5 -> 68 * 16 = 1088
        // 1920 / 16 = 120 -> 120 * 16 = 1920
        val (w916, h916) = config.calculateOutputDimensions(AspectRatio.RATIO_9_16)
        assertEquals(0, w916 % 16)
        assertEquals(0, h916 % 16)
        assertEquals(1088, w916)
        assertEquals(1920, h916)

        // 16:9 (1920 x 1080)
        val (w169, h169) = config.calculateOutputDimensions(AspectRatio.RATIO_16_9)
        assertEquals(0, w169 % 16)
        assertEquals(0, h169 % 16)
        assertEquals(1920, w169)
        assertEquals(1088, h169)

        // 1:1 (1080 x 1080) -> 1088 x 1088
        val (w11, h11) = config.calculateOutputDimensions(AspectRatio.RATIO_1_1)
        assertEquals(0, w11 % 16)
        assertEquals(0, h11 % 16)
        assertEquals(1088, w11)
        assertEquals(1088, h11)

        // 4:5
        val (w45, h45) = config.calculateOutputDimensions(AspectRatio.RATIO_4_5)
        assertEquals(0, w45 % 16)
        assertEquals(0, h45 % 16)

        // 2.35:1
        val (w235, h235) = config.calculateOutputDimensions(AspectRatio.RATIO_2_35_1)
        assertEquals(0, w235 % 16)
        assertEquals(0, h235 % 16)
    }

    @Test
    fun testMacroblockAlignmentAcrossAllResolutions() {
        val resolutions = ExportResolution.values()
        val aspectRatios = AspectRatio.values()

        for (res in resolutions) {
            val config = ExportConfig(resolution = res)
            for (ratio in aspectRatios) {
                val (w, h) = config.calculateOutputDimensions(ratio)
                assertTrue("Width $w must be aligned to 16 for $res and $ratio", w % 16 == 0)
                assertTrue("Height $h must be aligned to 16 for $res and $ratio", h % 16 == 0)
                assertTrue("Width $w must be positive", w > 0)
                assertTrue("Height $h must be positive", h > 0)
            }
        }
    }

    @Test
    fun testCustomBitrateOverride() {
        val config = ExportConfig(
            resolution = ExportResolution.RES_1080P,
            customBitrateBps = 25_000_000
        )
        assertEquals(25_000_000, config.effectiveVideoBitrateBps)
    }
}
