package com.procut.model

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
enum class VideoCodec(val mimeType: String, val label: String, val compressionRatio: Float) {
    H264("video/avc", "H.264 (Maximum Compatibility)", 1.0f),
    HEVC("video/hevc", "HEVC / H.265 (Smaller File Size)", 0.65f)
}

@Serializable
enum class BitrateLevel(val label: String, val multiplier: Float) {
    LOW("Low", 0.75f),
    RECOMMENDED("Recommended", 1.0f),
    HIGH("High", 1.4f)
}

@Serializable
enum class ExportResolution(
    val label: String,
    val standardWidth: Int,
    val standardHeight: Int
) {
    RES_480P("480p (SD)", 854, 480),
    RES_720P("720p (HD)", 1280, 720),
    RES_1080P("1080p (FHD)", 1920, 1080),
    RES_2K("2K (QHD)", 2560, 1440),
    RES_4K("4K (UHD)", 3840, 2160);

    val width: Int get() = standardWidth
    val height: Int get() = standardHeight

    /**
     * Calculates the width and height maintaining the project's AspectRatio,
     * ensuring dimensions are strictly aligned to 16-pixel macroblock boundaries
     * required by Android hardware video encoders (MediaCodec).
     */
    fun calculateDimensions(aspectRatio: AspectRatio): Pair<Int, Int> {
        val longSide = standardWidth
        val shortSide = standardHeight

        val (w, h) = when (aspectRatio) {
            AspectRatio.RATIO_9_16 -> Pair(shortSide, longSide)
            AspectRatio.RATIO_16_9 -> Pair(longSide, shortSide)
            AspectRatio.RATIO_1_1 -> Pair(shortSide, shortSide)
            AspectRatio.RATIO_4_5 -> {
                val calculatedH = (shortSide * 5f / 4f).roundToInt()
                Pair(shortSide, calculatedH)
            }
            AspectRatio.RATIO_2_35_1 -> {
                val calculatedH = (longSide / 2.35f).roundToInt()
                Pair(longSide, calculatedH)
            }
            AspectRatio.CUSTOM -> Pair(longSide, shortSide)
        }

        // Align to multiples of 16 for hardware encoder macroblocks
        val alignedW = ((w + 15) / 16) * 16
        val alignedH = ((h + 15) / 16) * 16
        return Pair(alignedW, alignedH)
    }
}

object BitrateCalculator {
    /**
     * Lookup table for baseline H.264 bitrates in bits per second (bps)
     * based on reverse-engineered specs from Technical Specification 04.
     */
    fun calculateBitrate(
        resolution: ExportResolution,
        fps: Int,
        bitrateLevel: BitrateLevel = BitrateLevel.RECOMMENDED,
        codec: VideoCodec = VideoCodec.H264
    ): Int {
        val isHighFps = fps >= 50

        val baseBitrateBps: Long = when (resolution) {
            ExportResolution.RES_480P -> {
                when (bitrateLevel) {
                    BitrateLevel.LOW -> 2_500_000L
                    BitrateLevel.RECOMMENDED -> 3_500_000L
                    BitrateLevel.HIGH -> 5_000_000L
                }
            }
            ExportResolution.RES_720P -> {
                if (!isHighFps) {
                    when (bitrateLevel) {
                        BitrateLevel.LOW -> 4_500_000L
                        BitrateLevel.RECOMMENDED -> 6_500_000L
                        BitrateLevel.HIGH -> 9_000_000L
                    }
                } else {
                    when (bitrateLevel) {
                        BitrateLevel.LOW -> 6_000_000L
                        BitrateLevel.RECOMMENDED -> 8_500_000L
                        BitrateLevel.HIGH -> 12_000_000L
                    }
                }
            }
            ExportResolution.RES_1080P -> {
                if (!isHighFps) {
                    when (bitrateLevel) {
                        BitrateLevel.LOW -> 8_000_000L
                        BitrateLevel.RECOMMENDED -> 12_000_000L
                        BitrateLevel.HIGH -> 16_000_000L
                    }
                } else {
                    when (bitrateLevel) {
                        BitrateLevel.LOW -> 11_000_000L
                        BitrateLevel.RECOMMENDED -> 16_000_000L
                        BitrateLevel.HIGH -> 22_000_000L
                    }
                }
            }
            ExportResolution.RES_2K -> {
                if (!isHighFps) {
                    when (bitrateLevel) {
                        BitrateLevel.LOW -> 15_000_000L
                        BitrateLevel.RECOMMENDED -> 20_000_000L
                        BitrateLevel.HIGH -> 28_000_000L
                    }
                } else {
                    when (bitrateLevel) {
                        BitrateLevel.LOW -> 20_000_000L
                        BitrateLevel.RECOMMENDED -> 28_000_000L
                        BitrateLevel.HIGH -> 38_000_000L
                    }
                }
            }
            ExportResolution.RES_4K -> {
                if (!isHighFps) {
                    when (bitrateLevel) {
                        BitrateLevel.LOW -> 25_000_000L
                        BitrateLevel.RECOMMENDED -> 35_000_000L
                        BitrateLevel.HIGH -> 45_000_000L
                    }
                } else {
                    when (bitrateLevel) {
                        BitrateLevel.LOW -> 35_000_000L
                        BitrateLevel.RECOMMENDED -> 50_000_000L
                        BitrateLevel.HIGH -> 65_000_000L
                    }
                }
            }
        }

        // Apply codec discount (HEVC provides ~35% bandwidth reduction at equivalent visual quality)
        val finalBitrate = (baseBitrateBps.toFloat() * codec.compressionRatio).roundToInt()
        return finalBitrate
    }

    /**
     * Estimates output file size in bytes given timeline duration and bitrates.
     * Includes 128 kbps audio overhead and container header tolerance.
     */
    fun estimateFileSize(
        durationUs: Long,
        videoBitrateBps: Int,
        audioBitrateBps: Int = 128_000
    ): Long {
        if (durationUs <= 0) return 0L
        val durationSeconds = durationUs.toDouble() / 1_000_000.0
        val totalBitrateBps = videoBitrateBps.toDouble() + audioBitrateBps.toDouble()
        val totalBits = durationSeconds * totalBitrateBps
        // 8 bits per byte + 1% container MP4 atom overhead
        val fileSizeBytes = ((totalBits / 8.0) * 1.01).toLong()
        return fileSizeBytes
    }
}

@Serializable
data class ExportConfig(
    val resolution: ExportResolution = ExportResolution.RES_1080P,
    val frameRate: Int = 30,
    val codec: VideoCodec = VideoCodec.H264,
    val bitrateLevel: BitrateLevel = BitrateLevel.RECOMMENDED,
    val customBitrateBps: Int? = null,
    val audioBitrateBps: Int = 128_000,
    val audioSampleRate: Int = 44_100,
    val audioChannelCount: Int = 2
) {
    val effectiveVideoBitrateBps: Int
        get() = customBitrateBps ?: BitrateCalculator.calculateBitrate(
            resolution = resolution,
            fps = frameRate,
            bitrateLevel = bitrateLevel,
            codec = codec
        )

    fun calculateOutputDimensions(aspectRatio: AspectRatio): Pair<Int, Int> {
        return resolution.calculateDimensions(aspectRatio)
    }

    fun estimateOutputSizeBytes(durationUs: Long): Long {
        return BitrateCalculator.estimateFileSize(
            durationUs = durationUs,
            videoBitrateBps = effectiveVideoBitrateBps,
            audioBitrateBps = audioBitrateBps
        )
    }
}
