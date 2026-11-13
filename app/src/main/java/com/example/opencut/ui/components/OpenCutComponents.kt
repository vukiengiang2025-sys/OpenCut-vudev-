package com.example.opencut.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.opencut.ui.theme.OpenCutTheme
import com.example.opencut.ui.theme.RobotoMonoFontFamily
import kotlin.math.roundToInt

/**
 * Exact geometric octagonal hollow-square mark from `brand/marks/symbol-dark.svg`.
 */
@Composable
fun OpenCutBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    tint: Color = OpenCutTheme.colors.foreground
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cut = 0.2929f
        val innerMin = 0.25f
        val innerMax = 0.75f

        val path = Path().apply {
            fillType = PathFillType.EvenOdd
            moveTo(w * cut, 0f)
            lineTo(w * (1f - cut), 0f)
            lineTo(w, h * cut)
            lineTo(w, h * (1f - cut))
            lineTo(w * (1f - cut), h)
            lineTo(w * cut, h)
            lineTo(0f, h * (1f - cut))
            lineTo(0f, h * cut)
            close()

            moveTo(w * innerMin, h * innerMin)
            lineTo(w * innerMax, h * innerMin)
            lineTo(w * innerMax, h * innerMax)
            lineTo(w * innerMin, h * innerMax)
            close()
        }
        drawPath(path = path, color = tint)
    }
}

enum class OpenCutBadgeVariant {
    DEFAULT,
    SECONDARY,
    OUTLINE,
    DESTRUCTIVE
}

@Composable
fun OpenCutBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: OpenCutBadgeVariant = OpenCutBadgeVariant.SECONDARY,
    monospace: Boolean = false
) {
    val colors = OpenCutTheme.colors
    val (bg, fg, border) = when (variant) {
        OpenCutBadgeVariant.DEFAULT -> Triple(colors.primary, colors.primaryForeground, null)
        OpenCutBadgeVariant.SECONDARY -> Triple(colors.secondary, colors.secondaryForeground, null)
        OpenCutBadgeVariant.OUTLINE -> Triple(Color.Transparent, colors.foreground, BorderStroke(1.dp, colors.border))
        OpenCutBadgeVariant.DESTRUCTIVE -> Triple(colors.destructive.copy(alpha = 0.14f), colors.destructive, null)
    }
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = bg,
        contentColor = fg,
        border = border
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = if (monospace) RobotoMonoFontFamily else MaterialTheme.typography.labelSmall.fontFamily,
                fontSize = 10.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun OpenCutPanelDivider(vertical: Boolean = false) {
    val color = OpenCutTheme.colors.border
    if (vertical) {
        VerticalDivider(thickness = 1.dp, color = color)
    } else {
        HorizontalDivider(thickness = 1.dp, color = color)
    }
}

@Composable
fun OpenCutPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val colors = OpenCutTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(40.dp),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.primaryForeground,
            disabledContainerColor = colors.muted,
            disabledContentColor = colors.mutedForeground
        )
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1
        )
    }
}

@Composable
fun OpenCutOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val colors = OpenCutTheme.colors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(38.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, colors.border),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = colors.foreground,
            disabledContentColor = colors.mutedForeground
        )
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

/**
 * Click-drag scrubbable numeric control inspired by OpenCut's Properties Panel (`changelog/0.1.0.md`).
 * Dragging horizontally adjusts the numeric value with real-time precision feedback.
 */
@Composable
fun ScrubbableNumberRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float = 1f,
    suffix: String = "",
    decimals: Int = 0,
    onValueChange: (Float) -> Unit,
    trailingAction: (@Composable () -> Unit)? = null
) {
    val colors = OpenCutTheme.colors
    val formatted = if (decimals == 0) {
        "${value.roundToInt()}$suffix"
    } else {
        String.format("%.${decimals}f%s", value, suffix)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.mutedForeground,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.secondary)
                    .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                    .minimumInteractiveComponentSize()
                    .pointerInput(value, valueRange, step) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            val span = (valueRange.endInclusive - valueRange.start)
                            val delta = (dragAmount / 180f) * span
                            val raw = (value + delta).coerceIn(valueRange.start, valueRange.endInclusive)
                            val snapped = if (step > 0f) {
                                (raw / step).roundToInt() * step
                            } else raw
                            onValueChange(snapped.coerceIn(valueRange.start, valueRange.endInclusive))
                        }
                    }
                    .clickable {
                        val next = (value + step).let {
                            if (it > valueRange.endInclusive) valueRange.start else it
                        }
                        onValueChange(next)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = formatted,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = RobotoMonoFontFamily,
                        color = colors.foreground
                    )
                )
            }

            if (trailingAction != null) {
                trailingAction()
            }
        }
    }
}

@Composable
fun ColorSwatchRow(
    colorsList: List<Long>,
    selectedHex: Long,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = OpenCutTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colorsList.forEach { hex ->
            val isSelected = hex == selectedHex
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(hex))
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) themeColors.sidebarPrimary else themeColors.border,
                        shape = CircleShape
                    )
                    .clickable { onSelect(hex) }
            )
        }
    }
}
