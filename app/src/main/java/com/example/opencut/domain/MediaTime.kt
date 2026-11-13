package com.example.opencut.domain

import kotlinx.serialization.Serializable
import kotlin.math.roundToLong

/**
 * Exact frame-accurate time representation ported from OpenCut's `rust/crates/time`.
 * 120,000 ticks per second divides evenly by every standard frame rate denominator
 * (23.976 -> 5,005 ticks/frame, 24 -> 5,000, 29.97 -> 4,004, 30 -> 4,000, 60 -> 2,000).
 */
object MediaTime {
    const val TICKS_PER_SECOND: Long = 120_000L
    const val TICKS_PER_MS: Long = 120L

    fun fromSeconds(seconds: Double): Long = (seconds * TICKS_PER_SECOND).roundToLong().coerceAtLeast(0L)

    fun fromMs(ms: Long): Long = (ms * TICKS_PER_MS).coerceAtLeast(0L)

    fun toMs(ticks: Long): Long = ticks / TICKS_PER_MS

    fun toSeconds(ticks: Long): Double = ticks.toDouble() / TICKS_PER_SECOND.toDouble()

    fun snapToFrame(ticks: Long, frameRate: FrameRate): Long {
        val tpf = frameRate.ticksPerFrame()
        if (tpf <= 0L) return ticks.coerceAtLeast(0L)
        return ((ticks + tpf / 2L) / tpf) * tpf
    }

    fun formatTimecode(ticks: Long, frameRate: FrameRate): String {
        val safeTicks = ticks.coerceAtLeast(0L)
        val totalFrames = (safeTicks / frameRate.ticksPerFrame().coerceAtLeast(1L)).toInt()
        val nominalFps = frameRate.nominalFps().coerceAtLeast(1)
        val frames = totalFrames % nominalFps
        val totalSeconds = totalFrames / nominalFps
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            String.format("%02d:%02d:%02d:%02d", hours, minutes, seconds, frames)
        } else {
            String.format("%02d:%02d:%02d", minutes, seconds, frames)
        }
    }

    fun formatDurationShort(ticks: Long): String {
        val totalSec = (ticks / TICKS_PER_SECOND).coerceAtLeast(0L)
        val mins = totalSec / 60
        val secs = totalSec % 60
        return String.format("%d:%02d", mins, secs)
    }
}

@Serializable
data class FrameRate(
    val numerator: Int,
    val denominator: Int = 1
) {
    fun ticksPerFrame(): Long =
        (MediaTime.TICKS_PER_SECOND * denominator) / numerator.coerceAtLeast(1)

    fun nominalFps(): Int = kotlin.math.round(numerator.toDouble() / denominator.toDouble()).toInt()

    fun label(): String = when {
        numerator == 24000 && denominator == 1001 -> "23.976 fps"
        numerator == 30000 && denominator == 1001 -> "29.97 fps"
        denominator == 1 -> "$numerator fps"
        else -> "${String.format("%.2f", numerator.toDouble() / denominator)} fps"
    }

    companion object {
        val FPS_24 = FrameRate(24, 1)
        val FPS_23_976 = FrameRate(24000, 1001)
        val FPS_29_97 = FrameRate(30000, 1001)
        val FPS_30 = FrameRate(30, 1)
        val FPS_60 = FrameRate(60, 1)

        val PRESETS = listOf(FPS_24, FPS_29_97, FPS_30, FPS_60)
    }
}
