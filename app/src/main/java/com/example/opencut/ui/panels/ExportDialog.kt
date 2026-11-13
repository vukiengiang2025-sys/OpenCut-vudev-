package com.example.opencut.ui.panels

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.opencut.R
import com.example.opencut.media.ExportState
import com.example.opencut.ui.components.OpenCutBadge
import com.example.opencut.ui.components.OpenCutBadgeVariant
import com.example.opencut.ui.components.OpenCutPrimaryButton
import com.example.opencut.ui.theme.OpenCutTheme
import com.example.opencut.ui.theme.RobotoMonoFontFamily

@Composable
fun ExportDialog(
    exportState: ExportState,
    projectName: String,
    onStartExport: (Int) -> Unit,
    onCancelExport: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = OpenCutTheme.colors
    val context = LocalContext.current
    var selectedHeight by remember { mutableIntStateOf(720) }

    AlertDialog(
        onDismissRequest = {
            if (exportState !is ExportState.Exporting) {
                onDismiss()
            }
        },
        containerColor = colors.popover,
        titleContentColor = colors.popoverForeground,
        textContentColor = colors.popoverForeground,
        title = {
            Text(
                text = stringResource(R.string.export_title),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (exportState) {
                    ExportState.Idle -> {
                        Text(
                            text = "Render \"$projectName\" to H.264 MP4 using Media3 Transformer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedForeground
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(720 to "720p HD", 1080 to "1080p Full HD").forEach { (h, label) ->
                                val active = selectedHeight == h
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedHeight = h },
                                    shape = MaterialTheme.shapes.medium,
                                    color = if (active) colors.primary else colors.secondary,
                                    contentColor = if (active) colors.primaryForeground else colors.secondaryForeground,
                                    border = BorderStroke(1.dp, colors.border)
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }

                    is ExportState.Exporting -> {
                        Text(
                            text = "Encoding MP4… ${exportState.progressPercent}%",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = RobotoMonoFontFamily
                            ),
                            color = colors.foreground
                        )
                        LinearProgressIndicator(
                            progress = { exportState.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    is ExportState.Completed -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = colors.trackAudio,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = stringResource(R.string.export_completed),
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.foreground
                            )
                        }
                        OpenCutBadge(
                            text = "${exportState.fileSizeBytes / 1024} KB · MP4",
                            variant = OpenCutBadgeVariant.OUTLINE,
                            monospace = true
                        )
                        OpenCutPrimaryButton(
                            text = stringResource(R.string.export_share),
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    exportState.outputFile
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "video/mp4"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share MP4"))
                            }
                        )
                    }

                    is ExportState.Error -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = colors.destructive,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = exportState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.destructive
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (exportState) {
                ExportState.Idle -> {
                    TextButton(onClick = { onStartExport(selectedHeight) }) {
                        Text("Start Export", fontWeight = FontWeight.SemiBold)
                    }
                }
                is ExportState.Exporting -> {
                    TextButton(onClick = onCancelExport) {
                        Text(stringResource(R.string.export_cancel), color = colors.destructive)
                    }
                }
                else -> {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        },
        dismissButton = {
            if (exportState is ExportState.Idle) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
