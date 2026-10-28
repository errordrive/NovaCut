package com.procut.model

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/**
 * Microsecond-precise time range representation.
 * All timeline and media timestamps in ProCut are measured in integer microseconds (1 s = 1,000,000 us).
 */
@Serializable
data class TimeRangeUs(
    val startUs: Long,
    val durationUs: Long
) {
    init {
        require(durationUs >= 0) { "durationUs must be non-negative: $durationUs" }
    }

    val endUs: Long
        get() = startUs + durationUs

    fun contains(timestampUs: Long): Boolean {
        return timestampUs in startUs until endUs
    }

    fun containsInclusive(timestampUs: Long): Boolean {
        return timestampUs in startUs..endUs
    }

    fun overlaps(other: TimeRangeUs): Boolean {
        return startUs < other.endUs && other.startUs < endUs
    }

    fun intersect(other: TimeRangeUs): TimeRangeUs? {
        val maxStart = max(startUs, other.startUs)
        val minEnd = min(endUs, other.endUs)
        val duration = minEnd - maxStart
        return if (duration > 0) {
            TimeRangeUs(startUs = maxStart, durationUs = duration)
        } else {
            null
        }
    }

    fun shift(offsetUs: Long): TimeRangeUs {
        return copy(startUs = startUs + offsetUs)
    }

    fun withStart(newStartUs: Long): TimeRangeUs {
        return copy(startUs = newStartUs)
    }

    fun withDuration(newDurationUs: Long): TimeRangeUs {
        require(newDurationUs >= 0) { "newDurationUs must be non-negative: $newDurationUs" }
        return copy(durationUs = newDurationUs)
    }

    fun clamp(timestampUs: Long): Long {
        return timestampUs.coerceIn(startUs, endUs)
    }

    companion object {
        val ZERO = TimeRangeUs(0L, 0L)

        fun fromStartEnd(startUs: Long, endUs: Long): TimeRangeUs {
            require(endUs >= startUs) { "endUs ($endUs) must be >= startUs ($startUs)" }
            return TimeRangeUs(startUs = startUs, durationUs = endUs - startUs)
        }
    }
}
