package com.example.opencut.ui.panels

import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.opencut.R
import com.example.opencut.domain.BackgroundMode
import com.example.opencut.domain.BlendModeKind
import com.example.opencut.domain.ClipType
import com.example.opencut.domain.MaskShape
import com.example.opencut.domain.MediaTime
import com.example.opencut.domain.ShapeKind
import com.example.opencut.domain.TimelineClip
import com.example.opencut.domain.TrackKind
import com.example.opencut.ui.components.OpenCutBadge
import com.example.opencut.ui.components.OpenCutBadgeVariant
import com.example.opencut.ui.components.OpenCutPanelDivider
import com.example.opencut.ui.screens.EditorUiState
import com.example.opencut.ui.screens.EditorViewModel
import com.example.opencut.ui.theme.OpenCutTheme
import com.example.opencut.ui.theme.RobotoMonoFontFamily
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@OptIn(UnstableApi::class)
@Composable
fun PreviewPanel(
    state: EditorUiState,
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val colors = OpenCutTheme.colors
    val context = LocalContext.current

    val videoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }
    val audioPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoPlayer.release()
            audioPlayer.release()
        }
    }

    val activeMainClip = state.document.clipsForTrack(TrackKind.MAIN)
        .firstOrNull { it.containsTick(state.playheadTick) }
    val activeAudioClip = state.document.clipsForTrack(TrackKind.AUDIO)
        .firstOrNull { it.containsTick(state.playheadTick) }
    val activeOverlays = state.document.clipsForTrack(TrackKind.OVERLAY)
        .filter { it.containsTick(state.playheadTick) }

    var loadedVideoClipId by remember { mutableStateOf<String?>(null) }
    var loadedAudioClipId by remember { mutableStateOf<String?>(null) }

    // Synchronize Video ExoPlayer
    LaunchedEffect(
        activeMainClip?.id,
        activeMainClip?.uriString,
        state.isPlaying,
        activeMainClip?.volume,
        activeMainClip?.speed
    ) {
        if (activeMainClip != null && activeMainClip.type == ClipType.VIDEO && !activeMainClip.uriString.isNullOrBlank()) {
            if (loadedVideoClipId != activeMainClip.id) {
                videoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(activeMainClip.uriString)))
                videoPlayer.prepare()
                loadedVideoClipId = activeMainClip.id
            }
            videoPlayer.volume = activeMainClip.volume.coerceIn(0f, 1f)
            videoPlayer.playbackParameters = PlaybackParameters(
                activeMainClip.speed.coerceIn(0.25f, 4f),
                if (activeMainClip.maintainPitch) 1f else activeMainClip.speed.coerceIn(0.25f, 4f)
            )
            val targetMs = MediaTime.toMs(
                activeMainClip.trimStartTick +
                    ((state.playheadTick - activeMainClip.startTick) * activeMainClip.speed).toLong()
            )
            if (abs(videoPlayer.currentPosition - targetMs) > 120L) {
                videoPlayer.seekTo(targetMs)
            }
            videoPlayer.playWhenReady = state.isPlaying
        } else {
            videoPlayer.playWhenReady = false
            loadedVideoClipId = null
        }
    }

    // Scrub seek when paused
    LaunchedEffect(state.playheadTick, state.isPlaying, activeMainClip?.id) {
        if (!state.isPlaying && activeMainClip != null && activeMainClip.type == ClipType.VIDEO) {
            val targetMs = MediaTime.toMs(
                activeMainClip.trimStartTick +
                    ((state.playheadTick - activeMainClip.startTick) * activeMainClip.speed).toLong()
            )
            if (abs(videoPlayer.currentPosition - targetMs) > 40L) {
                videoPlayer.seekTo(targetMs)
            }
        }
    }

    // Synchronize Audio Track ExoPlayer
    LaunchedEffect(
        activeAudioClip?.id,
        activeAudioClip?.uriString,
        state.isPlaying,
        activeAudioClip?.volume,
        activeAudioClip?.speed
    ) {
        if (activeAudioClip != null && activeAudioClip.type == ClipType.AUDIO && !activeAudioClip.uriString.isNullOrBlank()) {
            if (loadedAudioClipId != activeAudioClip.id) {
                audioPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(activeAudioClip.uriString)))
                audioPlayer.prepare()
                loadedAudioClipId = activeAudioClip.id
            }
            audioPlayer.volume = activeAudioClip.volume.coerceIn(0f, 1f)
            audioPlayer.playbackParameters = PlaybackParameters(
                activeAudioClip.speed.coerceIn(0.25f, 4f),
                if (activeAudioClip.maintainPitch) 1f else activeAudioClip.speed.coerceIn(0.25f, 4f)
            )
            val targetMs = MediaTime.toMs(activeAudioClip.trimStartTick + (state.playheadTick - activeAudioClip.startTick))
            if (abs(audioPlayer.currentPosition - targetMs) > 120L) {
                audioPlayer.seekTo(targetMs)
            }
            audioPlayer.playWhenReady = state.isPlaying
        } else {
            audioPlayer.playWhenReady = false
            loadedAudioClipId = null
        }
    }

    Column(
        modifier = modifier
            .background(colors.background)
            .fillMaxHeight()
    ) {
        // Preview Canvas Stage
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .background(if (colors.isDark) Color(0xFF050505) else Color(0xFFEAEAEA))
                .clickable(
                    interactionSource = null,
                    indication = null
                ) { viewModel.selectClip(null) },
            contentAlignment = Alignment.Center
        ) {
            val canvas = state.document.canvas
            val bgModifier = when (canvas.backgroundMode) {
                BackgroundMode.SOLID -> Modifier.background(Color(canvas.backgroundColorHex))
                BackgroundMode.GRADIENT -> Modifier.background(
                    Brush.linearGradient(
                        listOf(Color(canvas.backgroundColorHex), Color(canvas.gradientEndColorHex))
                    )
                )
                BackgroundMode.BLUR -> Modifier.background(
                    Brush.radialGradient(
                        listOf(
                            Color(canvas.backgroundColorHex).copy(alpha = 0.85f),
                            Color(canvas.gradientEndColorHex)
                        )
                    )
                )
            }

            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .aspectRatio(canvas.aspectRatio, matchHeightConstraintsFirst = true)
                    .graphicsLayer {
                        scaleX = state.previewZoom
                        scaleY = state.previewZoom
                    }
                    .clipToBounds()
                    .then(bgModifier)
                    .border(1.dp, colors.border)
            ) {
                // Global overlay blur if any EFFECT clip is active
                val activeBlur = activeOverlays
                    .filter { it.type == ClipType.EFFECT }
                    .maxOfOrNull { it.blurIntensity } ?: 0f

                // Main Track Layer
                if (activeMainClip != null) {
                    val eval = activeMainClip.evaluateAt(state.playheadTick)
                    val totalBlur = (activeMainClip.blurIntensity + activeBlur).coerceIn(0f, 1f)
                    val maskShape = maskToShape(activeMainClip.maskShape)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = eval.posX
                                translationY = eval.posY
                                scaleX = eval.scaleX
                                scaleY = eval.scaleY
                                rotationZ = eval.rotationDeg
                                alpha = eval.opacity
                            }
                            .clip(maskShape)
                            .then(if (totalBlur > 0.01f) Modifier.blur((totalBlur * 22f).dp) else Modifier)
                            .pointerInput(activeMainClip.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    viewModel.selectClip(activeMainClip.id)
                                    viewModel.updateSelectedClip { clip ->
                                        clip.copy(
                                            posX = clip.posX + dragAmount.x,
                                            posY = clip.posY + dragAmount.y
                                        )
                                    }
                                }
                            }
                    ) {
                        when (activeMainClip.type) {
                            ClipType.VIDEO -> {
                                AndroidView(
                                    factory = { ctx ->
                                        PlayerView(ctx).apply {
                                            useController = false
                                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            player = videoPlayer
                                        }
                                    },
                                    update = { view ->
                                        if (view.player !== videoPlayer) {
                                            view.player = videoPlayer
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            ClipType.IMAGE -> {
                                AsyncImage(
                                    model = activeMainClip.uriString,
                                    contentDescription = activeMainClip.title,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> Unit
                        }

                        if (state.selectedClipId == activeMainClip.id) {
                            DashedSelectionBox()
                        }
                    }
                } else if (activeOverlays.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Movie,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Canvas ${canvas.resolutionLabel}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = RobotoMonoFontFamily
                            ),
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                // Overlay Track Layers (Text & Shapes)
                activeOverlays.filter { it.type == ClipType.TEXT || it.type == ClipType.SHAPE }
                    .forEach { overlay ->
                        val eval = overlay.evaluateAt(state.playheadTick)
                        val isSelected = state.selectedClipId == overlay.id

                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .graphicsLayer {
                                    translationX = eval.posX
                                    translationY = eval.posY
                                    scaleX = eval.scaleX
                                    scaleY = eval.scaleY
                                    rotationZ = eval.rotationDeg
                                    alpha = eval.opacity
                                }
                                .pointerInput(overlay.id) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        viewModel.selectClip(overlay.id)
                                        viewModel.updateSelectedClip { clip ->
                                            clip.copy(
                                                posX = clip.posX + dragAmount.x,
                                                posY = clip.posY + dragAmount.y
                                            )
                                        }
                                    }
                                }
                                .clickable { viewModel.selectClip(overlay.id) }
                                .padding(6.dp)
                        ) {
                            when (overlay.type) {
                                ClipType.TEXT -> {
                                    val bg = Color(overlay.backgroundColorHex)
                                    Box(
                                        modifier = Modifier
                                            .then(
                                                if (bg.alpha > 0f) {
                                                    Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(bg)
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                } else Modifier
                                            )
                                    ) {
                                        Text(
                                            text = overlay.textContent,
                                            color = Color(overlay.textColorHex),
                                            fontSize = overlay.fontSizeSp.sp,
                                            letterSpacing = overlay.letterSpacingSp.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                ClipType.SHAPE -> {
                                    ShapeOverlayArtwork(
                                        shapeKind = overlay.shapeKind,
                                        color = Color(overlay.shapeColorHex),
                                        blendMode = overlay.blendMode
                                    )
                                }
                                else -> Unit
                            }

                            if (isSelected) {
                                DashedSelectionBox()
                            }
                        }
                    }
            }

            // Preview Zoom indicator
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .minimumInteractiveComponentSize()
                    .clickable {
                        val next = when {
                            state.previewZoom < 0.9f -> 1f
                            state.previewZoom < 1.4f -> 1.5f
                            state.previewZoom < 1.9f -> 2f
                            else -> 0.75f
                        }
                        viewModel.setPreviewZoom(next)
                    },
                shape = CircleShape,
                color = colors.card.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ZoomIn,
                        contentDescription = "Preview Zoom",
                        tint = colors.foreground,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${(state.previewZoom * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = RobotoMonoFontFamily
                        ),
                        color = colors.foreground
                    )
                }
            }
        }

        OpenCutPanelDivider()

        // Transport Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.card)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = MediaTime.formatTimecode(state.playheadTick, state.document.canvas.frameRate),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = RobotoMonoFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = colors.foreground
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { viewModel.seekTo(0L) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        Icons.Filled.FirstPage,
                        contentDescription = "Jump to start",
                        tint = colors.mutedForeground
                    )
                }
                IconButton(
                    onClick = { viewModel.stepFrames(-1) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "Previous frame",
                        tint = colors.foreground
                    )
                }
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .testTag("play_pause_button")
                        .clickable { viewModel.togglePlayPause() },
                    shape = CircleShape,
                    color = colors.primary,
                    contentColor = colors.primaryForeground
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(
                                if (state.isPlaying) R.string.action_pause else R.string.action_play
                            )
                        )
                    }
                }
                IconButton(
                    onClick = { viewModel.stepFrames(1) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "Next frame",
                        tint = colors.foreground
                    )
                }
            }

            OpenCutBadge(
                text = MediaTime.formatTimecode(
                    state.document.totalDurationTicks(),
                    state.document.canvas.frameRate
                ),
                variant = OpenCutBadgeVariant.OUTLINE,
                monospace = true
            )
        }
    }
}

@Composable
private fun DashedSelectionBox() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            color = Color(0xFF5B6CFA),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
            )
        )
    }
}

@Composable
private fun ShapeOverlayArtwork(
    shapeKind: ShapeKind,
    color: Color,
    blendMode: BlendModeKind
) {
    val composeBlend = when (blendMode) {
        BlendModeKind.NORMAL -> BlendMode.SrcOver
        BlendModeKind.MULTIPLY -> BlendMode.Multiply
        BlendModeKind.SCREEN -> BlendMode.Screen
        BlendModeKind.OVERLAY -> BlendMode.Overlay
        BlendModeKind.DARKEN -> BlendMode.Darken
        BlendModeKind.LIGHTEN -> BlendMode.Lighten
    }

    Canvas(modifier = Modifier.size(110.dp)) {
        val w = size.width
        val h = size.height
        when (shapeKind) {
            ShapeKind.RECTANGLE -> drawRect(color = color, blendMode = composeBlend)
            ShapeKind.CIRCLE -> drawOval(color = color, blendMode = composeBlend)
            ShapeKind.DIAMOND -> {
                val p = Path().apply {
                    moveTo(w / 2f, 0f)
                    lineTo(w, h / 2f)
                    lineTo(w / 2f, h)
                    lineTo(0f, h / 2f)
                    close()
                }
                drawPath(p, color = color, blendMode = composeBlend)
            }
            ShapeKind.STAR -> {
                drawPath(createStarPath(w, h), color = color, blendMode = composeBlend)
            }
            ShapeKind.HEART -> {
                drawPath(createHeartPath(w, h), color = color, blendMode = composeBlend)
            }
            ShapeKind.CINEMATIC_BARS -> {
                drawRect(color = Color.Black, topLeft = Offset.Zero, size = Size(w, h * 0.18f))
                drawRect(color = Color.Black, topLeft = Offset(0f, h * 0.82f), size = Size(w, h * 0.18f))
            }
        }
    }
}

private fun maskToShape(mask: MaskShape): Shape = when (mask) {
    MaskShape.NONE, MaskShape.RECTANGLE -> RectangleShape
    MaskShape.ELLIPSE -> CircleShape
    MaskShape.SPLIT -> GenericShape { size, _ ->
        addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width * 0.5f, size.height))
    }
    MaskShape.CINEMATIC_BARS -> GenericShape { size, _ ->
        addRect(androidx.compose.ui.geometry.Rect(0f, size.height * 0.14f, size.width, size.height * 0.86f))
    }
    MaskShape.DIAMOND -> GenericShape { size, _ ->
        moveTo(size.width / 2f, 0f)
        lineTo(size.width, size.height / 2f)
        lineTo(size.width / 2f, size.height)
        lineTo(0f, size.height / 2f)
        close()
    }
    MaskShape.STAR -> GenericShape { size, _ ->
        addPath(createStarPath(size.width, size.height))
    }
    MaskShape.HEART -> GenericShape { size, _ ->
        addPath(createHeartPath(size.width, size.height))
    }
}

private fun createStarPath(w: Float, h: Float): Path {
    val path = Path()
    val cx = w / 2f
    val cy = h / 2f
    val outer = minOf(w, h) * 0.48f
    val inner = outer * 0.42f
    for (i in 0 until 10) {
        val angle = -PI / 2.0 + i * PI / 5.0
        val r = if (i % 2 == 0) outer else inner
        val x = cx + (cos(angle) * r).toFloat()
        val y = cy + (sin(angle) * r).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun createHeartPath(w: Float, h: Float): Path {
    return Path().apply {
        moveTo(w * 0.5f, h * 0.88f)
        cubicTo(w * 0.1f, h * 0.62f, w * 0.05f, h * 0.22f, w * 0.3f, h * 0.16f)
        cubicTo(w * 0.44f, h * 0.12f, w * 0.5f, h * 0.28f, w * 0.5f, h * 0.34f)
        cubicTo(w * 0.5f, h * 0.28f, w * 0.56f, h * 0.12f, w * 0.7f, h * 0.16f)
        cubicTo(w * 0.95f, h * 0.22f, w * 0.9f, h * 0.62f, w * 0.5f, h * 0.88f)
        close()
    }
}
