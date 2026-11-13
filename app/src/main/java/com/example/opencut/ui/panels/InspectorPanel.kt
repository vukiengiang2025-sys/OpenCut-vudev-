package com.example.opencut.ui.panels

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.opencut.domain.BackgroundMode
import com.example.opencut.domain.BlendModeKind
import com.example.opencut.domain.CanvasPreset
import com.example.opencut.domain.ClipType
import com.example.opencut.domain.FrameRate
import com.example.opencut.domain.KeyframeProperty
import com.example.opencut.domain.MaskShape
import com.example.opencut.domain.MediaTime
import com.example.opencut.domain.ShapeKind
import com.example.opencut.domain.TimelineClip
import com.example.opencut.ui.components.ColorSwatchRow
import com.example.opencut.ui.components.OpenCutBadge
import com.example.opencut.ui.components.OpenCutBadgeVariant
import com.example.opencut.ui.components.OpenCutPanelDivider
import com.example.opencut.ui.components.ScrubbableNumberRow
import com.example.opencut.ui.screens.EditorUiState
import com.example.opencut.ui.screens.EditorViewModel
import com.example.opencut.ui.screens.InspectorTab
import com.example.opencut.ui.theme.OpenCutTheme
import com.example.opencut.ui.theme.RobotoMonoFontFamily
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InspectorPanel(
    state: EditorUiState,
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val colors = OpenCutTheme.colors
    val selected = state.selectedClip

    Column(
        modifier = modifier
            .background(colors.card)
            .fillMaxHeight()
    ) {
        if (selected == null) {
            // Canvas Settings when no clip is selected
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Canvas",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.foreground
                    )
                    OpenCutBadge(
                        text = state.document.canvas.resolutionLabel,
                        variant = OpenCutBadgeVariant.OUTLINE,
                        monospace = true
                    )
                }

                Text(
                    text = "Dimensions",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.mutedForeground
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CanvasPreset.ALL.forEach { preset ->
                        val active = state.document.canvas.width == preset.width &&
                            state.document.canvas.height == preset.height
                        InspectorChip(
                            label = preset.title.substringBefore(' '),
                            selected = active,
                            onClick = {
                                viewModel.updateCanvasSettings(
                                    width = preset.width,
                                    height = preset.height
                                )
                            }
                        )
                    }
                }

                ScrubbableNumberRow(
                    label = "Width",
                    value = state.document.canvas.width.toFloat(),
                    valueRange = 240f..3840f,
                    step = 10f,
                    suffix = "px",
                    onValueChange = { viewModel.updateCanvasSettings(width = it.toInt()) }
                )
                ScrubbableNumberRow(
                    label = "Height",
                    value = state.document.canvas.height.toFloat(),
                    valueRange = 240f..3840f,
                    step = 10f,
                    suffix = "px",
                    onValueChange = { viewModel.updateCanvasSettings(height = it.toInt()) }
                )

                OpenCutPanelDivider()

                Text(
                    text = "Frame Rate",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.mutedForeground
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FrameRate.PRESETS.forEach { fps ->
                        InspectorChip(
                            label = fps.label(),
                            selected = state.document.canvas.frameRate == fps,
                            onClick = { viewModel.updateCanvasSettings(frameRate = fps) }
                        )
                    }
                }

                OpenCutPanelDivider()

                Text(
                    text = "Background",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.mutedForeground
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BackgroundMode.entries.forEach { mode ->
                        InspectorChip(
                            label = mode.label,
                            selected = state.document.canvas.backgroundMode == mode,
                            onClick = { viewModel.updateCanvasSettings(backgroundMode = mode) }
                        )
                    }
                }

                ColorSwatchRow(
                    colorsList = listOf(
                        0xFF000000,
                        0xFF0F172A,
                        0xFF18181B,
                        0xFFF4F1F3,
                        0xFF1D4ED8,
                        0xFF7C3AED,
                        0xFF059669
                    ),
                    selectedHex = state.document.canvas.backgroundColorHex,
                    onSelect = { viewModel.updateCanvasSettings(backgroundColorHex = it) }
                )
            }
        } else {
            // Clip Inspector Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selected.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                OpenCutBadge(
                    text = selected.trackKind.label,
                    variant = OpenCutBadgeVariant.SECONDARY
                )
            }

            // Tabbed Properties Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(InspectorTab.entries) { tab ->
                    val active = state.activeInspectorTab == tab
                    Surface(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("inspector_tab_${tab.name.lowercase()}")
                            .clickable { viewModel.setInspectorTab(tab) },
                        shape = MaterialTheme.shapes.small,
                        color = if (active) colors.primary else colors.secondary,
                        contentColor = if (active) colors.primaryForeground else colors.secondaryForeground
                    ) {
                        Text(
                            text = tab.label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                            )
                        )
                    }
                }
            }

            OpenCutPanelDivider()

            when (state.activeInspectorTab) {
                InspectorTab.TRANSFORM -> TransformTab(selected, state, viewModel)
                InspectorTab.STYLE -> StyleTab(selected, viewModel)
                InspectorTab.AUDIO -> AudioTab(selected, viewModel)
                InspectorTab.EFFECTS -> EffectsTab(selected, viewModel)
                InspectorTab.KEYFRAMES -> KeyframesTab(selected, state, viewModel)
            }
        }
    }
}

@Composable
private fun TransformTab(
    clip: TimelineClip,
    state: EditorUiState,
    viewModel: EditorViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ScrubbableNumberRow(
            label = "Position X",
            value = clip.posX,
            valueRange = -400f..400f,
            step = 2f,
            suffix = "px",
            onValueChange = { x -> viewModel.updateSelectedClip { it.copy(posX = x) } },
            trailingAction = {
                KeyframeButton(clip, state, KeyframeProperty.POS_X, clip.posX, viewModel)
            }
        )
        ScrubbableNumberRow(
            label = "Position Y",
            value = clip.posY,
            valueRange = -400f..400f,
            step = 2f,
            suffix = "px",
            onValueChange = { y -> viewModel.updateSelectedClip { it.copy(posY = y) } },
            trailingAction = {
                KeyframeButton(clip, state, KeyframeProperty.POS_Y, clip.posY, viewModel)
            }
        )
        ScrubbableNumberRow(
            label = "Scale X",
            value = clip.scaleX * 100f,
            valueRange = 20f..300f,
            step = 2f,
            suffix = "%",
            onValueChange = { sx -> viewModel.updateSelectedClip { it.copy(scaleX = sx / 100f) } },
            trailingAction = {
                KeyframeButton(clip, state, KeyframeProperty.SCALE_X, clip.scaleX, viewModel)
            }
        )
        ScrubbableNumberRow(
            label = "Scale Y",
            value = clip.scaleY * 100f,
            valueRange = 20f..300f,
            step = 2f,
            suffix = "%",
            onValueChange = { sy -> viewModel.updateSelectedClip { it.copy(scaleY = sy / 100f) } },
            trailingAction = {
                KeyframeButton(clip, state, KeyframeProperty.SCALE_Y, clip.scaleY, viewModel)
            }
        )
        ScrubbableNumberRow(
            label = "Rotation",
            value = clip.rotationDeg,
            valueRange = -180f..180f,
            step = 1f,
            suffix = "°",
            onValueChange = { rot -> viewModel.updateSelectedClip { it.copy(rotationDeg = rot) } },
            trailingAction = {
                KeyframeButton(clip, state, KeyframeProperty.ROTATION, clip.rotationDeg, viewModel)
            }
        )
        ScrubbableNumberRow(
            label = "Opacity",
            value = clip.opacity * 100f,
            valueRange = 0f..100f,
            step = 2f,
            suffix = "%",
            onValueChange = { op -> viewModel.updateSelectedClip { it.copy(opacity = op / 100f) } },
            trailingAction = {
                KeyframeButton(clip, state, KeyframeProperty.OPACITY, clip.opacity, viewModel)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleTab(
    clip: TimelineClip,
    viewModel: EditorViewModel
) {
    val colors = OpenCutTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (clip.type == ClipType.TEXT) {
            OutlinedTextField(
                value = clip.textContent,
                onValueChange = { txt -> viewModel.updateSelectedClip { it.copy(textContent = txt) } },
                label = { Text("Text") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("text_content_input"),
                maxLines = 3
            )
            ScrubbableNumberRow(
                label = "Font Size",
                value = clip.fontSizeSp,
                valueRange = 12f..96f,
                step = 1f,
                suffix = "sp",
                onValueChange = { sz -> viewModel.updateSelectedClip { it.copy(fontSizeSp = sz) } }
            )
            ScrubbableNumberRow(
                label = "Letter Spacing",
                value = clip.letterSpacingSp,
                valueRange = -2f..16f,
                step = 0.5f,
                decimals = 1,
                suffix = "sp",
                onValueChange = { sp -> viewModel.updateSelectedClip { it.copy(letterSpacingSp = sp) } }
            )
            Text("Text Color", style = MaterialTheme.typography.labelMedium, color = colors.mutedForeground)
            ColorSwatchRow(
                colorsList = listOf(0xFFFFFFFF, 0xFF0A0A0A, 0xFFFACC15, 0xFFEF4444, 0xFF3B82F6, 0xFF10B981),
                selectedHex = clip.textColorHex,
                onSelect = { hex -> viewModel.updateSelectedClip(true) { it.copy(textColorHex = hex) } }
            )
            Text("Background Pill", style = MaterialTheme.typography.labelMedium, color = colors.mutedForeground)
            ColorSwatchRow(
                colorsList = listOf(0x00000000, 0xCC000000, 0xCCFFFFFF, 0xCC2563EB, 0xCCEF4444),
                selectedHex = clip.backgroundColorHex,
                onSelect = { hex -> viewModel.updateSelectedClip(true) { it.copy(backgroundColorHex = hex) } }
            )
        }

        if (clip.type == ClipType.SHAPE) {
            Text("Shape", style = MaterialTheme.typography.labelMedium, color = colors.mutedForeground)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ShapeKind.entries.forEach { shape ->
                    InspectorChip(
                        label = shape.label,
                        selected = clip.shapeKind == shape,
                        onClick = { viewModel.updateSelectedClip(true) { it.copy(shapeKind = shape) } }
                    )
                }
            }
            Text("Fill Color", style = MaterialTheme.typography.labelMedium, color = colors.mutedForeground)
            ColorSwatchRow(
                colorsList = listOf(0xFF3B82F6, 0xFF8B5CF6, 0xFF10B981, 0xFFF59E0B, 0xFFEF4444, 0xFFFFFFFF),
                selectedHex = clip.shapeColorHex,
                onSelect = { hex -> viewModel.updateSelectedClip(true) { it.copy(shapeColorHex = hex) } }
            )
        }

        Text("Blend Mode", style = MaterialTheme.typography.labelMedium, color = colors.mutedForeground)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BlendModeKind.entries.forEach { mode ->
                InspectorChip(
                    label = mode.label,
                    selected = clip.blendMode == mode,
                    onClick = { viewModel.updateSelectedClip(true) { it.copy(blendMode = mode) } }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudioTab(
    clip: TimelineClip,
    viewModel: EditorViewModel
) {
    val colors = OpenCutTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Volume", style = MaterialTheme.typography.labelMedium, color = colors.mutedForeground)
            Text(
                text = "${(clip.volume * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = RobotoMonoFontFamily),
                color = colors.foreground
            )
        }
        Slider(
            value = clip.volume,
            onValueChange = { v -> viewModel.updateSelectedClip { it.copy(volume = v) } },
            valueRange = 0f..2f
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Playback Speed", style = MaterialTheme.typography.labelMedium, color = colors.mutedForeground)
            Text(
                text = String.format("%.2fx", clip.speed),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = RobotoMonoFontFamily),
                color = colors.foreground
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { rate ->
                InspectorChip(
                    label = "${rate}x",
                    selected = abs(clip.speed - rate) < 0.05f,
                    onClick = { viewModel.updateSelectedClip(true) { it.copy(speed = rate) } }
                )
            }
        }
        Slider(
            value = clip.speed,
            onValueChange = { s -> viewModel.updateSelectedClip { it.copy(speed = s) } },
            valueRange = 0.25f..4f
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Maintain Pitch", style = MaterialTheme.typography.bodySmall, color = colors.foreground)
            Switch(
                checked = clip.maintainPitch,
                onCheckedChange = { mp -> viewModel.updateSelectedClip(true) { it.copy(maintainPitch = mp) } }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EffectsTab(
    clip: TimelineClip,
    viewModel: EditorViewModel
) {
    val colors = OpenCutTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Blur Intensity", style = MaterialTheme.typography.labelMedium, color = colors.mutedForeground)
            Text(
                text = "${(clip.blurIntensity * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = RobotoMonoFontFamily),
                color = colors.foreground
            )
        }
        Slider(
            value = clip.blurIntensity,
            onValueChange = { b -> viewModel.updateSelectedClip { it.copy(blurIntensity = b) } },
            valueRange = 0f..1f
        )

        OpenCutPanelDivider()

        Text("Mask Shape", style = MaterialTheme.typography.labelMedium, color = colors.mutedForeground)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MaskShape.entries.forEach { mask ->
                InspectorChip(
                    label = mask.label,
                    selected = clip.maskShape == mask,
                    onClick = { viewModel.updateSelectedClip(true) { it.copy(maskShape = mask) } }
                )
            }
        }
    }
}

@Composable
private fun KeyframesTab(
    clip: TimelineClip,
    state: EditorUiState,
    viewModel: EditorViewModel
) {
    val colors = OpenCutTheme.colors
    if (clip.keyframes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Tap the diamond icon next to any property in Transform to animate it at the playhead.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedForeground
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(clip.keyframes, key = { it.id }) { kf ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.seekTo(clip.startTick + kf.tickOffset) },
                    shape = MaterialTheme.shapes.small,
                    color = colors.secondary,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${kf.property.label}: ${String.format("%.2f", kf.value)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.foreground
                            )
                            Text(
                                text = MediaTime.formatTimecode(
                                    clip.startTick + kf.tickOffset,
                                    state.document.canvas.frameRate
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = RobotoMonoFontFamily
                                ),
                                color = colors.mutedForeground
                            )
                        }
                        IconButton(
                            onClick = { viewModel.removeKeyframe(kf.id) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Remove keyframe",
                                tint = colors.mutedForeground,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyframeButton(
    clip: TimelineClip,
    state: EditorUiState,
    property: KeyframeProperty,
    value: Float,
    viewModel: EditorViewModel
) {
    val colors = OpenCutTheme.colors
    val localTick = (state.playheadTick - clip.startTick).coerceIn(0L, clip.durationTick)
    val tol = state.document.canvas.frameRate.ticksPerFrame() * 2L
    val hasAtPlayhead = clip.keyframes.any {
        it.property == property && abs(it.tickOffset - localTick) <= tol
    }
    IconButton(
        onClick = { viewModel.toggleKeyframeAtPlayhead(property, value) },
        modifier = Modifier.size(34.dp)
    ) {
        Icon(
            imageVector = if (hasAtPlayhead) Icons.Filled.Diamond else Icons.Outlined.Diamond,
            contentDescription = "Toggle ${property.label} keyframe",
            tint = if (hasAtPlayhead) colors.sidebarPrimary else colors.mutedForeground,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun InspectorChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = OpenCutTheme.colors
    Surface(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) colors.primary else colors.secondary,
        contentColor = if (selected) colors.primaryForeground else colors.secondaryForeground,
        border = BorderStroke(1.dp, if (selected) colors.primary else colors.border)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
