package com.example.opencut.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.opencut.R
import com.example.opencut.data.ProjectSummary
import com.example.opencut.domain.CanvasPreset
import com.example.opencut.domain.MediaTime
import com.example.opencut.ui.components.OpenCutBadge
import com.example.opencut.ui.components.OpenCutBadgeVariant
import com.example.opencut.ui.components.OpenCutBrandMark
import com.example.opencut.ui.components.OpenCutPanelDivider
import com.example.opencut.ui.components.OpenCutPrimaryButton
import com.example.opencut.ui.theme.OpenCutTheme
import com.example.opencut.ui.theme.RobotoMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    isDarkTheme: Boolean,
    isExpandedWidth: Boolean,
    onToggleTheme: () -> Unit,
    onOpenProject: (String) -> Unit
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val colors = OpenCutTheme.colors

    var renameTarget by remember { mutableStateOf<ProjectSummary?>(null) }
    var renameText by remember { mutableStateOf("") }

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column(modifier = Modifier.background(colors.background)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OpenCutBrandMark(size = 26.dp)
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.foreground
                        )
                        OpenCutBadge(
                            text = "0.3.0",
                            variant = OpenCutBadgeVariant.OUTLINE,
                            monospace = true
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = stringResource(R.string.action_toggle_theme),
                                tint = colors.foreground
                            )
                        }
                        OpenCutPrimaryButton(
                            text = stringResource(R.string.new_project),
                            modifier = Modifier.testTag("new_project_button"),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                val index = projects.size + 1
                                viewModel.createProject(
                                    name = "Cut $index",
                                    width = 1920,
                                    height = 1080,
                                    onCreated = onOpenProject
                                )
                            }
                        )
                    }
                }
                OpenCutPanelDivider()
            }
        }
    ) { innerPadding ->
        val gridLineColor = colors.border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isExpandedWidth) 3 else 1),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1100.dp),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = colors.card,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    val step = 28.dp.toPx()
                                    var x = step
                                    while (x < size.width) {
                                        drawLine(
                                            color = gridLineColor.copy(alpha = 0.45f),
                                            start = Offset(x, 0f),
                                            end = Offset(x, size.height),
                                            strokeWidth = 1f
                                        )
                                        x += step
                                    }
                                    var y = step
                                    while (y < size.height) {
                                        drawLine(
                                            color = gridLineColor.copy(alpha = 0.45f),
                                            start = Offset(0f, y),
                                            end = Offset(size.width, y),
                                            strokeWidth = 1f
                                        )
                                        y += step
                                    }
                                }
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Frame-accurate mobile editing",
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.foreground
                            )
                            Text(
                                text = "Multi-track timeline with 120kHz tick precision, keyframes, overlays, and hardware MP4 export.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.mutedForeground
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(CanvasPreset.ALL) { preset ->
                                    Surface(
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("preset_${preset.width}x${preset.height}")
                                            .clickable {
                                                val idx = projects.size + 1
                                                viewModel.createProject(
                                                    name = "${preset.title.substringBefore(' ')} Cut $idx",
                                                    width = preset.width,
                                                    height = preset.height,
                                                    onCreated = onOpenProject
                                                )
                                            },
                                        shape = MaterialTheme.shapes.medium,
                                        color = colors.secondary,
                                        border = BorderStroke(1.dp, colors.border)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AspectRatio,
                                                contentDescription = null,
                                                tint = colors.foreground,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = preset.title,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = colors.foreground
                                                )
                                                Text(
                                                    text = preset.subtitle,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = RobotoMonoFontFamily
                                                    ),
                                                    color = colors.mutedForeground
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.projects_heading),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.foreground
                        )
                        OpenCutBadge(
                            text = "${projects.size} saved",
                            variant = OpenCutBadgeVariant.OUTLINE,
                            monospace = true
                        )
                    }
                }

                if (projects.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = colors.sidebar,
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MovieCreation,
                                    contentDescription = null,
                                    tint = colors.mutedForeground,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = stringResource(R.string.empty_projects_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.foreground
                                )
                                Text(
                                    text = stringResource(R.string.empty_projects_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.mutedForeground
                                )
                                Spacer(Modifier.height(4.dp))
                                OpenCutPrimaryButton(
                                    text = stringResource(R.string.new_project),
                                    onClick = {
                                        viewModel.createProject("Cut 1", 1920, 1080, onOpenProject)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    items(projects, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            onOpen = { onOpenProject(project.id) },
                            onRename = {
                                renameTarget = project
                                renameText = project.name
                            },
                            onDuplicate = { viewModel.duplicateProject(project.id) },
                            onDelete = { viewModel.deleteProject(project.id) }
                        )
                    }
                }
            }
        }
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = colors.popover,
            titleContentColor = colors.popoverForeground,
            textContentColor = colors.popoverForeground,
            title = { Text("Rename Project", style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("Project name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameProject(target.id, renameText)
                        renameTarget = null
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProjectCard(
    project: ProjectSummary,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = OpenCutTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }
    val dateLabel = remember(project.updatedAt) {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(project.updatedAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_card_${project.id}")
            .clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.foreground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Updated $dateLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedForeground
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Project options",
                            tint = colors.mutedForeground
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = colors.destructive) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = colors.destructive
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OpenCutBadge(
                    text = "${project.width}×${project.height}",
                    variant = OpenCutBadgeVariant.SECONDARY,
                    monospace = true
                )
                OpenCutBadge(
                    text = MediaTime.formatDurationShort(project.durationTicks),
                    variant = OpenCutBadgeVariant.OUTLINE,
                    monospace = true
                )
                OpenCutBadge(
                    text = "${project.clipCount} clips",
                    variant = OpenCutBadgeVariant.OUTLINE,
                    monospace = true
                )
            }
        }
    }
}
