package com.example.opencut.ui.panels

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.opencut.R
import com.example.opencut.domain.ClipType
import com.example.opencut.domain.MediaTime
import com.example.opencut.domain.ShapeKind
import com.example.opencut.ui.components.OpenCutBadge
import com.example.opencut.ui.components.OpenCutBadgeVariant
import com.example.opencut.ui.components.OpenCutOutlineButton
import com.example.opencut.ui.components.OpenCutPanelDivider
import com.example.opencut.ui.components.OpenCutPrimaryButton
import com.example.opencut.ui.screens.BrowserTab
import com.example.opencut.ui.screens.EditorUiState
import com.example.opencut.ui.screens.EditorViewModel
import com.example.opencut.ui.theme.OpenCutTheme
import com.example.opencut.ui.theme.RobotoMonoFontFamily

@Composable
fun BrowserPanel(
    state: EditorUiState,
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val colors = OpenCutTheme.colors

    val visualMediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importMediaUri(uri)
        }
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importMediaUri(uri, ClipType.AUDIO)
        }
    }

    Column(
        modifier = modifier
            .background(colors.sidebar)
            .fillMaxHeight()
    ) {
        // Tab bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(BrowserTab.entries) { tab ->
                val selected = state.activeBrowserTab == tab
                Surface(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("browser_tab_${tab.name.lowercase()}")
                        .clickable { viewModel.setBrowserTab(tab) },
                    shape = MaterialTheme.shapes.small,
                    color = if (selected) colors.primary else colors.secondary,
                    contentColor = if (selected) colors.primaryForeground else colors.secondaryForeground
                ) {
                    Text(
                        text = tab.label,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    )
                }
            }
        }

        OpenCutPanelDivider()

        when (state.activeBrowserTab) {
            BrowserTab.MEDIA -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OpenCutPrimaryButton(
                            text = stringResource(R.string.action_import_media),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_media_button"),
                            enabled = !state.isImporting,
                            leadingIcon = {
                                if (state.isImporting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = colors.primaryForeground
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Videocam,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            onClick = {
                                visualMediaPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            }
                        )
                        OpenCutOutlineButton(
                            text = "Audio",
                            modifier = Modifier.testTag("import_audio_button"),
                            enabled = !state.isImporting,
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.AudioFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                audioPicker.launch(arrayOf("audio/*"))
                            }
                        )
                    }

                    if (state.document.assets.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.medium)
                                .border(1.dp, colors.border, MaterialTheme.shapes.medium)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Movie,
                                    contentDescription = null,
                                    tint = colors.mutedForeground,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "No media imported",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.foreground
                                )
                                Text(
                                    text = "Pick videos, photos, or audio tracks from your device to place on the timeline.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.mutedForeground
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(state.document.assets, key = { it.id }) { asset ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.addAssetToTimeline(asset) },
                                    shape = MaterialTheme.shapes.medium,
                                    color = colors.card,
                                    border = BorderStroke(1.dp, colors.border)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.secondary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (asset.type == ClipType.IMAGE || asset.type == ClipType.VIDEO) {
                                                AsyncImage(
                                                    model = asset.uriString,
                                                    contentDescription = asset.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Filled.AudioFile,
                                                    contentDescription = null,
                                                    tint = colors.trackAudio
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = asset.name,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = colors.foreground,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OpenCutBadge(
                                                    text = asset.type.name,
                                                    variant = OpenCutBadgeVariant.SECONDARY
                                                )
                                                Text(
                                                    text = MediaTime.formatDurationShort(asset.durationTicks),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = RobotoMonoFontFamily
                                                    ),
                                                    color = colors.mutedForeground
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { viewModel.addAssetToTimeline(asset) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = "Add to timeline",
                                                tint = colors.foreground
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            BrowserTab.TEXT -> {
                val presets = listOf(
                    Triple("Title", "OpenCut Studio", 36f),
                    Triple("Subtitle", "Scene Caption", 22f),
                    Triple("Lower Third", "Director's Cut", 20f),
                    Triple("Minimal Tag", "CHAPTER 01", 16f)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presets) { (label, sample, sizeSp) ->
                        BrowserActionCard(
                            title = label,
                            subtitle = "\"$sample\" · ${sizeSp.toInt()}sp",
                            icon = {
                                Icon(
                                    Icons.Filled.TextFields,
                                    contentDescription = null,
                                    tint = colors.trackOverlay
                                )
                            },
                            onClick = { viewModel.addTextOverlay(label, sample, sizeSp) }
                        )
                    }
                }
            }

            BrowserTab.SHAPES -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ShapeKind.entries) { shape ->
                        BrowserActionCard(
                            title = shape.label,
                            subtitle = "Vector overlay sticker",
                            icon = {
                                Icon(
                                    Icons.Filled.Category,
                                    contentDescription = null,
                                    tint = colors.sidebarPrimary
                                )
                            },
                            onClick = { viewModel.addShapeOverlay(shape) }
                        )
                    }
                }
            }

            BrowserTab.EFFECTS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        BrowserActionCard(
                            title = "Gaussian Blur",
                            subtitle = "Adjustable overlay blur layer",
                            icon = {
                                Icon(
                                    Icons.Filled.BlurOn,
                                    contentDescription = null,
                                    tint = colors.trackOverlay
                                )
                            },
                            onClick = { viewModel.addBlurEffectClip() }
                        )
                    }
                    item {
                        BrowserActionCard(
                            title = "Cinema Bars",
                            subtitle = "2.39:1 letterbox overlay",
                            icon = {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = colors.trackMain
                                )
                            },
                            onClick = { viewModel.addShapeOverlay(ShapeKind.CINEMATIC_BARS) }
                        )
                    }
                }
            }

            BrowserTab.BOOKMARKS -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OpenCutPrimaryButton(
                        text = "Add Marker at Playhead",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                Icons.Filled.BookmarkAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        onClick = { viewModel.addBookmarkAtPlayhead() }
                    )

                    if (state.document.bookmarks.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No bookmarks yet. Mark key beats along the timeline.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.mutedForeground
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(state.document.bookmarks, key = { it.id }) { bm ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.seekTo(bm.tick) },
                                    shape = MaterialTheme.shapes.medium,
                                    color = colors.card,
                                    border = BorderStroke(1.dp, colors.border)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(Color(bm.colorHex))
                                            )
                                            Column {
                                                Text(
                                                    text = bm.label,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = colors.foreground
                                                )
                                                Text(
                                                    text = MediaTime.formatTimecode(
                                                        bm.tick,
                                                        state.document.canvas.frameRate
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = RobotoMonoFontFamily
                                                    ),
                                                    color = colors.mutedForeground
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteBookmark(bm.id) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Delete bookmark",
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
            }
        }
    }
}

@Composable
private fun BrowserActionCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val colors = OpenCutTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = colors.card,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.secondary),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.foreground
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedForeground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add $title",
                tint = colors.mutedForeground,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
