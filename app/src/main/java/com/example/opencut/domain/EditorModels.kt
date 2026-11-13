package com.example.opencut.domain

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class TrackKind(val label: String) {
    OVERLAY("Overlay"),
    MAIN("Main"),
    AUDIO("Audio")
}

@Serializable
enum class ClipType {
    VIDEO,
    IMAGE,
    AUDIO,
    TEXT,
    SHAPE,
    EFFECT
}

@Serializable
enum class ShapeKind(val label: String) {
    RECTANGLE("Rectangle"),
    CIRCLE("Ellipse"),
    STAR("Star"),
    HEART("Heart"),
    DIAMOND("Diamond"),
    CINEMATIC_BARS("Cinema Bars")
}

@Serializable
enum class MaskShape(val label: String) {
    NONE("None"),
    RECTANGLE("Rectangle"),
    ELLIPSE("Ellipse"),
    SPLIT("Split"),
    CINEMATIC_BARS("Cinema Bars"),
    STAR("Star"),
    HEART("Heart"),
    DIAMOND("Diamond")
}

@Serializable
enum class BlendModeKind(val label: String) {
    NORMAL("Normal"),
    MULTIPLY("Multiply"),
    SCREEN("Screen"),
    OVERLAY("Overlay"),
    DARKEN("Darken"),
    LIGHTEN("Lighten")
}

@Serializable
enum class BackgroundMode(val label: String) {
    SOLID("Solid"),
    GRADIENT("Gradient"),
    BLUR("Blur")
}

@Serializable
enum class KeyframeProperty(val label: String) {
    OPACITY("Opacity"),
    SCALE_X("Scale X"),
    SCALE_Y("Scale Y"),
    POS_X("Position X"),
    POS_Y("Position Y"),
    ROTATION("Rotation")
}

@Serializable
data class Keyframe(
    val id: String = UUID.randomUUID().toString(),
    val tickOffset: Long,
    val property: KeyframeProperty,
    val value: Float
)

data class EvaluatedTransform(
    val posX: Float,
    val posY: Float,
    val scaleX: Float,
    val scaleY: Float,
    val rotationDeg: Float,
    val opacity: Float
)

@Serializable
data class MediaAsset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: ClipType,
    val uriString: String,
    val durationTicks: Long,
    val width: Int = 1920,
    val height: Int = 1080,
    val waveformSamples: List<Float> = emptyList()
)

@Serializable
data class TimelineClip(
    val id: String = UUID.randomUUID().toString(),
    val assetId: String? = null,
    val trackKind: TrackKind,
    val type: ClipType,
    val title: String,
    val uriString: String? = null,
    val startTick: Long,
    val durationTick: Long,
    val trimStartTick: Long = 0L,
    val sourceDurationTick: Long = durationTick,
    val posX: Float = 0f,
    val posY: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotationDeg: Float = 0f,
    val opacity: Float = 1f,
    val volume: Float = 1f,
    val speed: Float = 1f,
    val maintainPitch: Boolean = true,
    val blurIntensity: Float = 0f,
    val blendMode: BlendModeKind = BlendModeKind.NORMAL,
    val textContent: String = "OpenCut Title",
    val textColorHex: Long = 0xFFFFFFFF,
    val backgroundColorHex: Long = 0x00000000,
    val fontSizeSp: Float = 28f,
    val letterSpacingSp: Float = 0f,
    val shapeKind: ShapeKind = ShapeKind.RECTANGLE,
    val shapeColorHex: Long = 0xFF3B82F6,
    val maskShape: MaskShape = MaskShape.NONE,
    val maskFeather: Float = 0f,
    val keyframes: List<Keyframe> = emptyList(),
    val waveformSamples: List<Float> = emptyList()
) {
    val endTick: Long get() = startTick + durationTick

    fun containsTick(tick: Long): Boolean = tick >= startTick && tick < endTick

    fun evaluateAt(timelineTick: Long): EvaluatedTransform {
        val localTick = (timelineTick - startTick).coerceIn(0L, durationTick)
        return EvaluatedTransform(
            posX = interpolateProperty(KeyframeProperty.POS_X, posX, localTick),
            posY = interpolateProperty(KeyframeProperty.POS_Y, posY, localTick),
            scaleX = interpolateProperty(KeyframeProperty.SCALE_X, scaleX, localTick),
            scaleY = interpolateProperty(KeyframeProperty.SCALE_Y, scaleY, localTick),
            rotationDeg = interpolateProperty(KeyframeProperty.ROTATION, rotationDeg, localTick),
            opacity = interpolateProperty(KeyframeProperty.OPACITY, opacity, localTick).coerceIn(0f, 1f)
        )
    }

    private fun interpolateProperty(
        property: KeyframeProperty,
        baseValue: Float,
        localTick: Long
    ): Float {
        val points = keyframes.filter { it.property == property }.sortedBy { it.tickOffset }
        if (points.isEmpty()) return baseValue
        if (localTick <= points.first().tickOffset) return points.first().value
        if (localTick >= points.last().tickOffset) return points.last().value
        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            if (localTick in a.tickOffset..b.tickOffset) {
                val span = (b.tickOffset - a.tickOffset).coerceAtLeast(1L).toFloat()
                val t = ((localTick - a.tickOffset).toFloat() / span).coerceIn(0f, 1f)
                // Smooth ease-in-out curve
                val eased = t * t * (3f - 2f * t)
                return a.value + (b.value - a.value) * eased
            }
        }
        return baseValue
    }
}

@Serializable
data class Bookmark(
    val id: String = UUID.randomUUID().toString(),
    val tick: Long,
    val label: String,
    val colorHex: Long = 0xFFF59E0B,
    val note: String = ""
)

@Serializable
data class CanvasSettings(
    val width: Int = 1920,
    val height: Int = 1080,
    val frameRate: FrameRate = FrameRate.FPS_30,
    val backgroundMode: BackgroundMode = BackgroundMode.SOLID,
    val backgroundColorHex: Long = 0xFF000000,
    val gradientEndColorHex: Long = 0xFF1E293B
) {
    val aspectRatio: Float get() = width.coerceAtLeast(1).toFloat() / height.coerceAtLeast(1).toFloat()
    val resolutionLabel: String get() = "${width}×${height}"
}

@Serializable
data class ProjectDocument(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled Project",
    val updatedAt: Long = System.currentTimeMillis(),
    val canvas: CanvasSettings = CanvasSettings(),
    val assets: List<MediaAsset> = emptyList(),
    val clips: List<TimelineClip> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList()
) {
    fun totalDurationTicks(): Long {
        val maxClipEnd = clips.maxOfOrNull { it.endTick } ?: 0L
        return maxClipEnd.coerceAtLeast(MediaTime.fromSeconds(10.0))
    }

    fun clipsForTrack(kind: TrackKind): List<TimelineClip> =
        clips.filter { it.trackKind == kind }.sortedBy { it.startTick }
}

data class CanvasPreset(
    val title: String,
    val subtitle: String,
    val width: Int,
    val height: Int
) {
    companion object {
        val ALL = listOf(
            CanvasPreset("16:9 Landscape", "1920 × 1080 HD", 1920, 1080),
            CanvasPreset("9:16 Vertical", "1080 × 1920 Reels", 1080, 1920),
            CanvasPreset("1:1 Square", "1080 × 1080 Feed", 1080, 1080),
            CanvasPreset("4:5 Portrait", "1080 × 1350 Post", 1080, 1350)
        )
    }
}
