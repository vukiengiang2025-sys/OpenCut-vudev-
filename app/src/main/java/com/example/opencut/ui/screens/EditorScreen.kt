package com.example.opencut.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.opencut.R
import com.example.opencut.ui.components.OpenCutBadge
import com.example.opencut.ui.components.OpenCutBadgeVariant
import com.example.opencut.ui.components.OpenCutBrandMark
import com.example.opencut.ui.components.OpenCutPanelDivider
import com.example.opencut.ui.components.OpenCutPrimaryButton
import com.example.opencut.ui.panels.BrowserPanel
import com.example.opencut.ui.panels.ExportDialog
import com.example.opencut.ui.panels.InspectorPanel
import com.example.opencut.ui.panels.PreviewPanel
import com.example.opencut.ui.panels.TimelinePanel
import com.example.opencut.ui.theme.OpenCutTheme

private enum class CompactDrawerPane {
    BROWSER,
    INSPECTOR
}

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val colors = OpenCutTheme.colors

    val snackbarHostState = remember { SnackbarHostState() }
    var showExportDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameDraft by remember { mutableStateOf("") }
    var compactPane by remember { mutableStateOf(CompactDrawerPane.BROWSER) }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(state.selectedClipId) {
        if (state.selectedClipId != null) {
            compactPane = CompactDrawerPane.INSPECTOR
        }
    }

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.background(colors.sidebar)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.pausePlayback()
                                onBack()
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("editor_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                tint = colors.foreground
                            )
                        }

                        OpenCutBrandMark(size = 20.dp)

                        Text(
                            text = state.document.name,
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .clickable {
                                    renameDraft = state.document.name
                                    showRenameDialog = true
                                },
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.foreground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        OpenCutBadge(
                            text = state.document.canvas.resolutionLabel,
                            variant = OpenCutBadgeVariant.OUTLINE,
                            monospace = true
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = stringResource(R.string.action_toggle_theme),
                                tint = colors.foreground,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        OpenCutPrimaryButton(
                            text = stringResource(R.string.action_export),
                            modifier = Modifier.testTag("export_button"),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = { showExportDialog = true }
                        )
                    }
                }
                OpenCutPanelDivider()
            }
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val useDesktopShellLayout = maxWidth >= 700.dp || maxWidth > maxHeight

                if (useDesktopShellLayout) {
                    // Faithful 4-panel OpenCut Desktop Shell (`apps/desktop/src/shell.rs`)
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.62f)
                        ) {
                            BrowserPanel(
                                state = state,
                                viewModel = viewModel,
                                modifier = Modifier.weight(0.26f)
                            )
                            OpenCutPanelDivider(vertical = true)
                            PreviewPanel(
                                state = state,
                                viewModel = viewModel,
                                modifier = Modifier.weight(0.48f)
                            )
                            OpenCutPanelDivider(vertical = true)
                            InspectorPanel(
                                state = state,
                                viewModel = viewModel,
                                modifier = Modifier.weight(0.26f)
                            )
                        }
                        OpenCutPanelDivider()
                        TimelinePanel(
                            state = state,
                            viewModel = viewModel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.38f)
                        )
                    }
                } else {
                    // Compact Portrait Studio Layout
                    Column(modifier = Modifier.fillMaxSize()) {
                        PreviewPanel(
                            state = state,
                            viewModel = viewModel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.36f)
                        )
                        OpenCutPanelDivider()
                        TimelinePanel(
                            state = state,
                            viewModel = viewModel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.31f)
                        )
                        OpenCutPanelDivider()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.33f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.sidebar)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CompactPaneTab(
                                    label = stringResource(R.string.panel_browser),
                                    icon = Icons.Filled.VideoLibrary,
                                    selected = compactPane == CompactDrawerPane.BROWSER,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("compact_tab_browser"),
                                    onClick = { compactPane = CompactDrawerPane.BROWSER }
                                )
                                CompactPaneTab(
                                    label = if (state.selectedClip != null) {
                                        "${stringResource(R.string.panel_inspector)} · ${state.selectedClip?.title}"
                                    } else {
                                        "${stringResource(R.string.panel_inspector)} · Canvas"
                                    },
                                    icon = Icons.Filled.Tune,
                                    selected = compactPane == CompactDrawerPane.INSPECTOR,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("compact_tab_inspector"),
                                    onClick = { compactPane = CompactDrawerPane.INSPECTOR }
                                )
                            }
                            OpenCutPanelDivider()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                when (compactPane) {
                                    CompactDrawerPane.BROWSER -> BrowserPanel(
                                        state = state,
                                        viewModel = viewModel,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    CompactDrawerPane.INSPECTOR -> InspectorPanel(
                                        state = state,
                                        viewModel = viewModel,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        ExportDialog(
            exportState = exportState,
            projectName = state.document.name,
            onStartExport = { height -> viewModel.startExport(height) },
            onCancelExport = { viewModel.cancelExport() },
            onDismiss = {
                viewModel.resetExportState()
                showExportDialog = false
            }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = colors.popover,
            title = { Text("Rename Project", style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameProject(renameDraft)
                        showRenameDialog = false
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CompactPaneTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = OpenCutTheme.colors
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) colors.primary else colors.secondary,
        contentColor = if (selected) colors.primaryForeground else colors.mutedForeground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = " $label",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
