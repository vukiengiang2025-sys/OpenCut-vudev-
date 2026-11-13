package com.example.opencut.ui.screens

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.opencut.data.MediaImportHelper
import com.example.opencut.data.ProjectRepository
import com.example.opencut.domain.BackgroundMode
import com.example.opencut.domain.BlendModeKind
import com.example.opencut.domain.Bookmark
import com.example.opencut.domain.ClipType
import com.example.opencut.domain.FrameRate
import com.example.opencut.domain.Keyframe
import com.example.opencut.domain.KeyframeProperty
import com.example.opencut.domain.MaskShape
import com.example.opencut.domain.MediaAsset
import com.example.opencut.domain.MediaTime
import com.example.opencut.domain.ProjectDocument
import com.example.opencut.domain.ShapeKind
import com.example.opencut.domain.TimelineClip
import com.example.opencut.domain.TrackKind
import com.example.opencut.media.ExportState
import com.example.opencut.media.VideoExportManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class BrowserTab(val label: String) {
    MEDIA("Media"),
    TEXT("Text"),
    SHAPES("Shapes"),
    EFFECTS("Effects"),
    BOOKMARKS("Bookmarks")
}

enum class InspectorTab(val label: String) {
    TRANSFORM("Transform"),
    STYLE("Style"),
    AUDIO("Audio"),
    EFFECTS("Effects"),
    KEYFRAMES("Keyframes")
}

data class EditorUiState(
    val document: ProjectDocument = ProjectDocument(),
    val isLoading: Boolean = true,
    val selectedClipId: String? = null,
    val playheadTick: Long = 0L,
    val isPlaying: Boolean = false,
    val rippleMode: Boolean = false,
    val previewZoom: Float = 1f,
    val timelineZoomPxPerSec: Float = 44f,
    val activeBrowserTab: BrowserTab = BrowserTab.MEDIA,
    val activeInspectorTab: InspectorTab = InspectorTab.TRANSFORM,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isImporting: Boolean = false,
    val statusMessage: String? = null
) {
    val selectedClip: TimelineClip?
        get() = document.clips.firstOrNull { it.id == selectedClipId }
}

class EditorViewModel(
    private val projectId: String,
    private val repository: ProjectRepository,
    appContext: Context
) : ViewModel() {

    private val importHelper = MediaImportHelper(appContext)
    private val exportManager = VideoExportManager(appContext)

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    val exportState: StateFlow<ExportState> = exportManager.state

    private val undoStack = ArrayDeque<ProjectDocument>()
    private val redoStack = ArrayDeque<ProjectDocument>()

    private var playbackJob: Job? = null
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val loaded = repository.getProject(projectId) ?: repository.createProject("Untitled Cut", 1920, 1080)
            _uiState.update {
                it.copy(
                    document = loaded,
                    isLoading = false
                )
            }
        }
    }

    private fun mutateDocument(
        recordUndo: Boolean = true,
        transform: (ProjectDocument) -> ProjectDocument
    ) {
        val current = _uiState.value.document
        val updated = transform(current)
        if (updated == current) return

        if (recordUndo) {
            undoStack.addLast(current)
            if (undoStack.size > 40) undoStack.removeFirst()
            redoStack.clear()
        }

        _uiState.update {
            it.copy(
                document = updated,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        scheduleSave(updated)
    }

    private fun scheduleSave(doc: ProjectDocument) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(250L)
            repository.saveProject(doc)
        }
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(_uiState.value.document)
        _uiState.update {
            it.copy(
                document = previous,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                selectedClipId = previous.clips.firstOrNull { c -> c.id == it.selectedClipId }?.id
            )
        }
        scheduleSave(previous)
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(_uiState.value.document)
        _uiState.update {
            it.copy(
                document = next,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        scheduleSave(next)
    }

    fun setBrowserTab(tab: BrowserTab) {
        _uiState.update { it.copy(activeBrowserTab = tab) }
    }

    fun setInspectorTab(tab: InspectorTab) {
        _uiState.update { it.copy(activeInspectorTab = tab) }
    }

    fun selectClip(clipId: String?) {
        _uiState.update { state ->
            val clip = state.document.clips.firstOrNull { it.id == clipId }
            val suggestedTab = when (clip?.type) {
                ClipType.TEXT, ClipType.SHAPE -> InspectorTab.STYLE
                ClipType.AUDIO -> InspectorTab.AUDIO
                ClipType.EFFECT -> InspectorTab.EFFECTS
                else -> state.activeInspectorTab
            }
            state.copy(selectedClipId = clipId, activeInspectorTab = suggestedTab)
        }
    }

    fun seekTo(tick: Long) {
        val doc = _uiState.value.document
        val clamped = MediaTime.snapToFrame(
            tick.coerceIn(0L, doc.totalDurationTicks()),
            doc.canvas.frameRate
        )
        _uiState.update { it.copy(playheadTick = clamped) }
    }

    fun stepFrames(deltaFrames: Int) {
        val state = _uiState.value
        val step = state.document.canvas.frameRate.ticksPerFrame() * deltaFrames
        seekTo(state.playheadTick + step)
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    fun pausePlayback() {
        playbackJob?.cancel()
        playbackJob = null
        _uiState.update { it.copy(isPlaying = false) }
    }

    private fun startPlayback() {
        playbackJob?.cancel()
        val maxTick = _uiState.value.document.clips.maxOfOrNull { it.endTick }
            ?: _uiState.value.document.totalDurationTicks()
        if (_uiState.value.playheadTick >= maxTick) {
            _uiState.update { it.copy(playheadTick = 0L) }
        }
        _uiState.update { it.copy(isPlaying = true) }

        playbackJob = viewModelScope.launch {
            var lastTimeMs = System.currentTimeMillis()
            while (isActive && _uiState.value.isPlaying) {
                delay(16L)
                val now = System.currentTimeMillis()
                val elapsedMs = (now - lastTimeMs).coerceIn(0L, 100L)
                lastTimeMs = now
                val current = _uiState.value
                val endBoundary = current.document.clips.maxOfOrNull { it.endTick }
                    ?: current.document.totalDurationTicks()
                val nextTick = current.playheadTick + MediaTime.fromMs(elapsedMs)
                if (nextTick >= endBoundary) {
                    _uiState.update { it.copy(playheadTick = endBoundary, isPlaying = false) }
                    break
                } else {
                    _uiState.update { it.copy(playheadTick = nextTick) }
                }
            }
        }
    }

    fun toggleRippleMode() {
        _uiState.update { it.copy(rippleMode = !it.rippleMode) }
    }

    fun setTimelineZoom(pxPerSec: Float) {
        _uiState.update { it.copy(timelineZoomPxPerSec = pxPerSec.coerceIn(16f, 160f)) }
    }

    fun setPreviewZoom(zoom: Float) {
        _uiState.update { it.copy(previewZoom = zoom.coerceIn(0.5f, 3.0f)) }
    }

    fun importMediaUri(uri: Uri, forcedType: ClipType? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, statusMessage = null) }
            val asset = importHelper.importMedia(uri, forcedType)
            if (asset != null) {
                mutateDocument { doc ->
                    doc.copy(assets = listOf(asset) + doc.assets)
                }
                addAssetToTimeline(asset)
                _uiState.update { it.copy(isImporting = false, statusMessage = "Added ${asset.name}") }
            } else {
                _uiState.update {
                    it.copy(isImporting = false, statusMessage = "Could not import selected media")
                }
            }
        }
    }

    fun addAssetToTimeline(asset: MediaAsset) {
        val targetTrack = when (asset.type) {
            ClipType.AUDIO -> TrackKind.AUDIO
            else -> TrackKind.MAIN
        }
        mutateDocument { doc ->
            val trackEnd = doc.clipsForTrack(targetTrack).maxOfOrNull { it.endTick } ?: 0L
            val clip = TimelineClip(
                assetId = asset.id,
                trackKind = targetTrack,
                type = asset.type,
                title = asset.name,
                uriString = asset.uriString,
                startTick = trackEnd,
                durationTick = asset.durationTicks,
                sourceDurationTick = asset.durationTicks,
                waveformSamples = asset.waveformSamples
            )
            doc.copy(clips = doc.clips + clip)
        }
        val newestId = _uiState.value.document.clips.lastOrNull()?.id
        selectClip(newestId)
    }

    fun addTextOverlay(title: String = "Title", text: String = "OpenCut Studio", fontSizeSp: Float = 32f) {
        val playhead = _uiState.value.playheadTick
        val duration = MediaTime.fromSeconds(4.0)
        val newClip = TimelineClip(
            trackKind = TrackKind.OVERLAY,
            type = ClipType.TEXT,
            title = title,
            startTick = findNonOverlappingStart(TrackKind.OVERLAY, playhead, duration),
            durationTick = duration,
            sourceDurationTick = MediaTime.fromSeconds(30.0),
            textContent = text,
            fontSizeSp = fontSizeSp
        )
        mutateDocument { doc -> doc.copy(clips = doc.clips + newClip) }
        selectClip(newClip.id)
    }

    fun addShapeOverlay(shapeKind: ShapeKind) {
        val playhead = _uiState.value.playheadTick
        val duration = MediaTime.fromSeconds(4.0)
        val newClip = TimelineClip(
            trackKind = TrackKind.OVERLAY,
            type = ClipType.SHAPE,
            title = shapeKind.label,
            startTick = findNonOverlappingStart(TrackKind.OVERLAY, playhead, duration),
            durationTick = duration,
            sourceDurationTick = MediaTime.fromSeconds(30.0),
            shapeKind = shapeKind
        )
        mutateDocument { doc -> doc.copy(clips = doc.clips + newClip) }
        selectClip(newClip.id)
    }

    fun addBlurEffectClip() {
        val playhead = _uiState.value.playheadTick
        val duration = MediaTime.fromSeconds(4.0)
        val newClip = TimelineClip(
            trackKind = TrackKind.OVERLAY,
            type = ClipType.EFFECT,
            title = "Blur Effect",
            startTick = findNonOverlappingStart(TrackKind.OVERLAY, playhead, duration),
            durationTick = duration,
            sourceDurationTick = MediaTime.fromSeconds(30.0),
            blurIntensity = 0.45f
        )
        mutateDocument { doc -> doc.copy(clips = doc.clips + newClip) }
        selectClip(newClip.id)
    }

    private fun findNonOverlappingStart(track: TrackKind, preferred: Long, duration: Long): Long {
        val existing = _uiState.value.document.clipsForTrack(track)
        var candidate = preferred.coerceAtLeast(0L)
        for (clip in existing) {
            if (candidate < clip.endTick && (candidate + duration) > clip.startTick) {
                candidate = clip.endTick
            }
        }
        return candidate
    }

    fun moveClip(clipId: String, newStartTick: Long) {
        mutateDocument(recordUndo = true) { doc ->
            val target = doc.clips.firstOrNull { it.id == clipId } ?: return@mutateDocument doc
            val snapped = MediaTime.snapToFrame(newStartTick.coerceAtLeast(0L), doc.canvas.frameRate)
            val siblings = doc.clips.filter { it.trackKind == target.trackKind && it.id != clipId }
            val clamped = clampWithoutOverlap(snapped, target.durationTick, siblings)
            doc.copy(
                clips = doc.clips.map { if (it.id == clipId) it.copy(startTick = clamped) else it }
            )
        }
    }

    fun trimClipStart(clipId: String, deltaTicks: Long) {
        mutateDocument(recordUndo = true) { doc ->
            val target = doc.clips.firstOrNull { it.id == clipId } ?: return@mutateDocument doc
            val minDuration = doc.canvas.frameRate.ticksPerFrame() * 3L
            val maxTrimAdvance = (target.durationTick - minDuration).coerceAtLeast(0L)
            val minDelta = maxOf(-target.startTick, -target.trimStartTick)
            val clampedDelta = deltaTicks.coerceIn(minDelta, maxTrimAdvance)
            val newStart = MediaTime.snapToFrame(target.startTick + clampedDelta, doc.canvas.frameRate)
            val actualDelta = newStart - target.startTick
            val updated = target.copy(
                startTick = newStart,
                durationTick = (target.durationTick - actualDelta).coerceAtLeast(minDuration),
                trimStartTick = (target.trimStartTick + actualDelta).coerceAtLeast(0L)
            )
            doc.copy(clips = doc.clips.map { if (it.id == clipId) updated else it })
        }
    }

    fun trimClipEnd(clipId: String, deltaTicks: Long) {
        mutateDocument(recordUndo = true) { doc ->
            val target = doc.clips.firstOrNull { it.id == clipId } ?: return@mutateDocument doc
            val minDuration = doc.canvas.frameRate.ticksPerFrame() * 3L
            val nextNeighborStart = doc.clips
                .filter { it.trackKind == target.trackKind && it.id != clipId && it.startTick >= target.endTick }
                .minOfOrNull { it.startTick } ?: Long.MAX_VALUE
            val maxEndBySource = if (target.type == ClipType.VIDEO || target.type == ClipType.AUDIO) {
                target.startTick + (target.sourceDurationTick - target.trimStartTick)
            } else Long.MAX_VALUE
            val maxAllowedDuration = (minOf(nextNeighborStart, maxEndBySource) - target.startTick)
                .coerceAtLeast(minDuration)
            val newDuration = MediaTime.snapToFrame(
                (target.durationTick + deltaTicks).coerceIn(minDuration, maxAllowedDuration),
                doc.canvas.frameRate
            )
            doc.copy(
                clips = doc.clips.map { if (it.id == clipId) it.copy(durationTick = newDuration) else it }
            )
        }
    }

    private fun clampWithoutOverlap(
        desiredStart: Long,
        duration: Long,
        siblings: List<TimelineClip>
    ): Long {
        var candidate = desiredStart.coerceAtLeast(0L)
        for (other in siblings.sortedBy { it.startTick }) {
            if (candidate < other.endTick && candidate + duration > other.startTick) {
                val distBefore = kotlin.math.abs(candidate - (other.startTick - duration))
                val distAfter = kotlin.math.abs(candidate - other.endTick)
                candidate = if (other.startTick >= duration && distBefore < distAfter) {
                    other.startTick - duration
                } else {
                    other.endTick
                }
            }
        }
        return candidate.coerceAtLeast(0L)
    }

    fun splitSelectedOrPlayheadClip() {
        val state = _uiState.value
        val playhead = state.playheadTick
        val target = state.selectedClip?.takeIf {
            playhead > it.startTick && playhead < it.endTick
        } ?: state.document.clips.firstOrNull {
            playhead > it.startTick && playhead < it.endTick
        } ?: return

        val minDuration = state.document.canvas.frameRate.ticksPerFrame() * 2L
        val leftDuration = playhead - target.startTick
        val rightDuration = target.endTick - playhead
        if (leftDuration < minDuration || rightDuration < minDuration) return

        val leftClip = target.copy(
            durationTick = leftDuration,
            keyframes = target.keyframes.filter { it.tickOffset <= leftDuration }
        )
        val rightClip = target.copy(
            id = java.util.UUID.randomUUID().toString(),
            startTick = playhead,
            durationTick = rightDuration,
            trimStartTick = target.trimStartTick + leftDuration,
            keyframes = target.keyframes
                .filter { it.tickOffset >= leftDuration }
                .map { it.copy(tickOffset = it.tickOffset - leftDuration) }
        )

        mutateDocument { doc ->
            doc.copy(
                clips = doc.clips.flatMap { if (it.id == target.id) listOf(leftClip, rightClip) else listOf(it) }
            )
        }
        selectClip(rightClip.id)
    }

    fun duplicateSelectedClip() {
        val target = _uiState.value.selectedClip ?: return
        val copy = target.copy(
            id = java.util.UUID.randomUUID().toString(),
            title = "${target.title} Copy",
            startTick = findNonOverlappingStart(target.trackKind, target.endTick, target.durationTick)
        )
        mutateDocument { doc -> doc.copy(clips = doc.clips + copy) }
        selectClip(copy.id)
    }

    fun deleteSelectedClip() {
        val target = _uiState.value.selectedClip ?: return
        val ripple = _uiState.value.rippleMode
        mutateDocument { doc ->
            val remaining = doc.clips.filterNot { it.id == target.id }
            val adjusted = if (ripple) {
                remaining.map { clip ->
                    if (clip.trackKind == target.trackKind && clip.startTick >= target.endTick) {
                        clip.copy(startTick = (clip.startTick - target.durationTick).coerceAtLeast(0L))
                    } else clip
                }
            } else remaining
            doc.copy(clips = adjusted)
        }
        selectClip(null)
    }

    fun updateSelectedClip(recordUndo: Boolean = false, updater: (TimelineClip) -> TimelineClip) {
        val id = _uiState.value.selectedClipId ?: return
        mutateDocument(recordUndo = recordUndo) { doc ->
            doc.copy(clips = doc.clips.map { if (it.id == id) updater(it) else it })
        }
    }

    fun toggleKeyframeAtPlayhead(property: KeyframeProperty, value: Float) {
        val state = _uiState.value
        val clip = state.selectedClip ?: return
        val localTick = (state.playheadTick - clip.startTick).coerceIn(0L, clip.durationTick)
        val tolerance = state.document.canvas.frameRate.ticksPerFrame() * 2L

        updateSelectedClip(recordUndo = true) { target ->
            val existing = target.keyframes.firstOrNull {
                it.property == property && kotlin.math.abs(it.tickOffset - localTick) <= tolerance
            }
            val nextKeyframes = if (existing != null) {
                target.keyframes.filterNot { it.id == existing.id }
            } else {
                target.keyframes + Keyframe(
                    tickOffset = localTick,
                    property = property,
                    value = value
                )
            }
            target.copy(keyframes = nextKeyframes.sortedBy { it.tickOffset })
        }
    }

    fun removeKeyframe(keyframeId: String) {
        updateSelectedClip(recordUndo = true) { clip ->
            clip.copy(keyframes = clip.keyframes.filterNot { it.id == keyframeId })
        }
    }

    fun addBookmarkAtPlayhead(label: String = "Marker", note: String = "") {
        val tick = _uiState.value.playheadTick
        mutateDocument { doc ->
            val count = doc.bookmarks.size + 1
            doc.copy(
                bookmarks = (doc.bookmarks + Bookmark(
                    tick = tick,
                    label = if (label == "Marker") "Marker $count" else label,
                    note = note
                )).sortedBy { it.tick }
            )
        }
    }

    fun deleteBookmark(id: String) {
        mutateDocument { doc ->
            doc.copy(bookmarks = doc.bookmarks.filterNot { it.id == id })
        }
    }

    fun updateCanvasSettings(
        width: Int? = null,
        height: Int? = null,
        frameRate: FrameRate? = null,
        backgroundMode: BackgroundMode? = null,
        backgroundColorHex: Long? = null,
        gradientEndColorHex: Long? = null
    ) {
        mutateDocument(recordUndo = true) { doc ->
            val c = doc.canvas
            doc.copy(
                canvas = c.copy(
                    width = width?.coerceIn(100, 3840) ?: c.width,
                    height = height?.coerceIn(100, 3840) ?: c.height,
                    frameRate = frameRate ?: c.frameRate,
                    backgroundMode = backgroundMode ?: c.backgroundMode,
                    backgroundColorHex = backgroundColorHex ?: c.backgroundColorHex,
                    gradientEndColorHex = gradientEndColorHex ?: c.gradientEndColorHex
                )
            )
        }
    }

    fun renameProject(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        mutateDocument(recordUndo = false) { doc -> doc.copy(name = trimmed) }
    }

    fun startExport(targetHeight: Int = 720) {
        pausePlayback()
        exportManager.exportProject(_uiState.value.document, targetHeight)
    }

    fun cancelExport() {
        exportManager.cancelExport()
    }

    fun resetExportState() {
        exportManager.resetState()
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        exportManager.cancelExport()
    }

    class Factory(
        private val projectId: String,
        private val repository: ProjectRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditorViewModel(projectId, repository, context.applicationContext) as T
        }
    }
}
