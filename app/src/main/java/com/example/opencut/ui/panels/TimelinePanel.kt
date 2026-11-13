package com.example.opencut.ui.panels

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.opencut.R
import com.example.opencut.domain.ClipType
import com.example.opencut.domain.MediaTime
import com.example.opencut.domain.TimelineClip
import com.example.opencut.domain.TrackKind
import com.example.opencut.ui.components.OpenCutPanelDivider
import com.example.opencut.ui.screens.EditorUiState
import com.example.opencut.ui.screens.EditorViewModel
import com.example.opencut.ui.theme.OpenCutTheme
import com.example.opencut.ui.theme.RobotoMonoFontFamily
import kotlin.math.roundToInt

@Composable
fun TimelinePanel(
    state: EditorUiState,
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val colors = OpenCutTheme.colors
    val density = LocalDensity.current
    val pxPerSec = state.timelineZoomPxPerSec
    val pxPerTick = pxPerSec / MediaTime.TICKS_PER_SECOND.toFloat()

    val totalTicks = (state.document.totalDurationTicks() + MediaTime.fromSeconds(4.0))
    val totalTimelineWidthDp = with(density) { (totalTicks * pxPerTick).toDp().coerceAtLeast(600.dp) }
    val scrollState = rememberScrollState()
    val headerWidth = 74.dp

    Column(
        modifier = modifier
            .background(colors.card)
            .fillMaxSize()
    ) {
        // Timeline Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.sidebar)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = state.canUndo,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("undo_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = stringResource(R.string.action_undo),
                        tint = if (state.canUndo) colors.foreground else colors.mutedForeground.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = state.canRedo,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("redo_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = stringResource(R.string.action_redo),
                        tint = if (state.canRedo) colors.foreground else colors.mutedForeground.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.splitSelectedOrPlayheadClip() },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("split_button")
                ) {
                    Icon(
                        Icons.Filled.ContentCut,
                        contentDescription = stringResource(R.string.action_split),
                        tint = colors.foreground,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Surface(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("ripple_toggle")
                        .clickable { viewModel.toggleRippleMode() },
                    shape = MaterialTheme.shapes.small,
                    color = if (state.rippleMode) colors.sidebarPrimary else colors.secondary,
                    contentColor = if (state.rippleMode) Color.White else colors.mutedForeground
                ) {
                    Text(
                        text = stringResource(R.string.action_ripple),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                IconButton(
                    onClick = { viewModel.addBookmarkAtPlayhead() },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        Icons.Filled.BookmarkAdd,
                        contentDescription = stringResource(R.string.action_add_bookmark),
                        tint = colors.mutedForeground,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (state.selectedClip != null) {
                    IconButton(
                        onClick = { viewModel.duplicateSelectedClip() },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = stringResource(R.string.action_duplicate),
                            tint = colors.foreground,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.deleteSelectedClip() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_clip_button")
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = colors.destructive,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.setTimelineZoom(state.timelineZoomPxPerSec - 12f) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.ZoomOut,
                        contentDescription = "Zoom out timeline",
                        tint = colors.mutedForeground,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.setTimelineZoom(state.timelineZoomPxPerSec + 12f) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.ZoomIn,
                        contentDescription = "Zoom in timeline",
                        tint = colors.mutedForeground,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        OpenCutPanelDivider()

        // Fixed Top Ruler + Scrollable Tracks Body
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Track Labels Column (aligned with ruler + tracks)
            Column(
                modifier = Modifier
                    .width(headerWidth)
                    .fillMaxHeight()
                    .background(colors.sidebar)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = state.document.canvas.frameRate.label(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = 9.sp
                        ),
                        color = colors.mutedForeground
                    )
                }
                OpenCutPanelDivider()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Spacer(Modifier.height(2.dp))
                    TrackKind.entries.forEach { track ->
                        val hasSelected = state.selectedClip?.trackKind == track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(
                                    if (hasSelected) colors.sidebarPrimary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (track) {
                                                TrackKind.OVERLAY -> colors.trackOverlay
                                                TrackKind.MAIN -> colors.trackMain
                                                TrackKind.AUDIO -> colors.trackAudio
                                            }
                                        )
                                )
                                Text(
                                    text = track.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (hasSelected) FontWeight.SemiBold else FontWeight.Medium
                                    ),
                                    color = if (hasSelected) colors.foreground else colors.mutedForeground
                                )
                            }
                        }
                    }
                }
            }

            OpenCutPanelDivider(vertical = true)

            // Right Scrollable Timeline Canvas (Ruler + Typed SceneTracks + Playhead)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(scrollState)
            ) {
                Box(modifier = Modifier.width(totalTimelineWidthDp).fillMaxHeight()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Interactive Time Ruler
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .background(colors.sidebar)
                                .pointerInput(pxPerTick) {
                                    detectTapGestures { offset ->
                                        viewModel.seekTo((offset.x / pxPerTick).toLong())
                                    }
                                }
                                .pointerInput(pxPerTick) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        viewModel.seekTo((change.position.x / pxPerTick).toLong())
                                    }
                                }
                        ) {
                            val tickColor = colors.mutedForeground.copy(alpha = 0.45f)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val secPx = pxPerSec
                                val totalSeconds = (size.width / secPx).toInt() + 1
                                for (s in 0..totalSeconds) {
                                    val x = s * secPx
                                    val major = s % 5 == 0
                                    drawLine(
                                        color = tickColor,
                                        start = Offset(x, size.height - if (major) 12.dp.toPx() else 6.dp.toPx()),
                                        end = Offset(x, size.height),
                                        strokeWidth = if (major) 1.5.dp.toPx() else 1.dp.toPx()
                                    )
                                }
                                // Bookmark flags
                                state.document.bookmarks.forEach { bm ->
                                    val bx = bm.tick * pxPerTick
                                    drawCircle(
                                        color = Color(bm.colorHex),
                                        radius = 4.dp.toPx(),
                                        center = Offset(bx, 8.dp.toPx())
                                    )
                                }
                            }
                            // Major second labels
                            val maxSec = (totalTicks / MediaTime.TICKS_PER_SECOND).toInt()
                            for (s in 0..maxSec step 5) {
                                val xDp = with(density) { (s * pxPerSec).toDp() }
                                Text(
                                    text = "${s}s",
                                    modifier = Modifier.offset(x = xDp + 3.dp, y = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = RobotoMonoFontFamily,
                                        fontSize = 9.sp
                                    ),
                                    color = colors.mutedForeground
                                )
                            }
                        }

                        OpenCutPanelDivider()

                        // Tracks Body (tapping empty area seeks playhead per changelog/0.3.0.md)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(pxPerTick) {
                                    detectTapGestures { offset ->
                                        viewModel.selectClip(null)
                                        viewModel.seekTo((offset.x / pxPerTick).toLong())
                                    }
                                },
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Spacer(Modifier.height(2.dp))
                            TrackKind.entries.forEach { track ->
                                val trackClips = state.document.clipsForTrack(track)
                                val hasSelected = state.selectedClip?.trackKind == track
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .background(
                                            if (hasSelected) colors.sidebarPrimary.copy(alpha = 0.06f)
                                            else colors.background.copy(alpha = 0.4f)
                                        )
                                ) {
                                    trackClips.forEach { clip ->
                                        TimelineClipBlock(
                                            clip = clip,
                                            isSelected = state.selectedClipId == clip.id,
                                            pxPerTick = pxPerTick,
                                            onSelect = { viewModel.selectClip(clip.id) },
                                            onDragMoveTicks = { delta ->
                                                viewModel.moveClip(clip.id, clip.startTick + delta)
                                            },
                                            onTrimStartTicks = { delta ->
                                                viewModel.trimClipStart(clip.id, delta)
                                            },
                                            onTrimEndTicks = { delta ->
                                                viewModel.trimClipEnd(clip.id, delta)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Playhead Indicator Line
                    val playheadX = (state.playheadTick * pxPerTick).roundToInt()
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(playheadX - with(density) { 1.dp.roundToPx() }, 0) }
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(colors.playhead)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .offset(x = (-4).dp, y = 0.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(colors.playhead)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineClipBlock(
    clip: TimelineClip,
    isSelected: Boolean,
    pxPerTick: Float,
    onSelect: () -> Unit,
    onDragMoveTicks: (Long) -> Unit,
    onTrimStartTicks: (Long) -> Unit,
    onTrimEndTicks: (Long) -> Unit
) {
    val colors = OpenCutTheme.colors
    val density = LocalDensity.current

    val clipColor = when (clip.trackKind) {
        TrackKind.MAIN -> colors.trackMain
        TrackKind.OVERLAY -> colors.trackOverlay
        TrackKind.AUDIO -> colors.trackAudio
    }

    val xOffsetPx = (clip.startTick * pxPerTick).roundToInt()
    val widthDp = with(density) { (clip.durationTick * pxPerTick).toDp().coerceAtLeast(28.dp) }

    Box(
        modifier = Modifier
            .offset { IntOffset(xOffsetPx, 0) }
            .width(widthDp)
            .fillMaxHeight()
            .padding(vertical = 3.dp, horizontal = 1.dp)
    ) {
        // Clip body
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .pointerInput(clip.id, pxPerTick) {
                    detectTapGestures { onSelect() }
                }
                .pointerInput(clip.id, pxPerTick) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onSelect()
                        onDragMoveTicks((dragAmount.x / pxPerTick).toLong())
                    }
                },
            shape = RoundedCornerShape(6.dp),
            color = clipColor.copy(alpha = if (isSelected) 0.34f else 0.22f),
            border = BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color.White else clipColor.copy(alpha = 0.7f)
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Waveform preview for video/audio clips
                if ((clip.type == ClipType.AUDIO || clip.type == ClipType.VIDEO) && clip.waveformSamples.isNotEmpty()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        val count = clip.waveformSamples.size
                        val barW = size.width / count.toFloat()
                        clip.waveformSamples.forEachIndexed { idx, amp ->
                            val barH = size.height * amp
                            drawRect(
                                color = clipColor.copy(alpha = 0.55f),
                                topLeft = Offset(idx * barW, size.height - barH),
                                size = Size((barW * 0.7f).coerceAtLeast(1f), barH)
                            )
                        }
                    }
                }

                // Keyframe dots
                clip.keyframes.forEach { kf ->
                    val kfX = with(density) { (kf.tickOffset * pxPerTick).toDp() }
                    Box(
                        modifier = Modifier
                            .offset(x = kfX, y = 4.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFACC15))
                    )
                }

                Text(
                    text = clip.title,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    ),
                    color = colors.foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // External Left/Right Trim Handles when selected (per changelog/0.2.0.md)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(12.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                    .background(Color.White)
                    .pointerInput(clip.id, pxPerTick) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onTrimStartTicks((dragAmount.x / pxPerTick).toLong())
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(16.dp)
                        .background(Color.Black)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(12.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                    .background(Color.White)
                    .pointerInput(clip.id, pxPerTick) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onTrimEndTicks((dragAmount.x / pxPerTick).toLong())
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(16.dp)
                        .background(Color.Black)
                )
            }
        }
    }
}
