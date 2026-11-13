package com.example.opencut.media

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.TextOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.opencut.domain.ClipType
import com.example.opencut.domain.MediaTime
import com.example.opencut.domain.ProjectDocument
import com.example.opencut.domain.TrackKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.math.abs

sealed interface ExportState {
    data object Idle : ExportState
    data class Exporting(val progressPercent: Int) : ExportState
    data class Completed(val outputFile: File, val fileSizeBytes: Long) : ExportState
    data class Error(val message: String) : ExportState
}

@OptIn(UnstableApi::class)
class VideoExportManager(private val context: Context) {

    private val _state = MutableStateFlow<ExportState>(ExportState.Idle)
    val state: StateFlow<ExportState> = _state.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeTransformer: Transformer? = null
    private var progressRunnable: Runnable? = null

    fun exportProject(document: ProjectDocument, targetHeight: Int = 720) {
        val mainClips = document.clipsForTrack(TrackKind.MAIN)
            .filter { (it.type == ClipType.VIDEO || it.type == ClipType.IMAGE) && !it.uriString.isNullOrBlank() }

        if (mainClips.isEmpty()) {
            _state.value = ExportState.Error("Add at least one video or image clip to the Main track before exporting.")
            return
        }

        cancelExport()

        val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
        val safeName = document.name.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "opencut_export" }
        val outputFile = File(exportsDir, "${safeName}_${System.currentTimeMillis()}.mp4")

        val aspect = document.canvas.aspectRatio
        val exportHeight = targetHeight.coerceAtLeast(360)
        val exportWidth = ((exportHeight * aspect).toInt() / 2) * 2

        val textOverlays = document.clipsForTrack(TrackKind.OVERLAY)
            .filter { it.type == ClipType.TEXT && it.textContent.isNotBlank() }

        val editedMainItems = mainClips.map { clip ->
            val startMs = MediaTime.toMs(clip.trimStartTick)
            val clipDurationMs = MediaTime.toMs(clip.durationTick).coerceAtLeast(250L)
            val mediaItemBuilder = MediaItem.Builder()
                .setUri(Uri.parse(clip.uriString))

            if (clip.type == ClipType.VIDEO) {
                mediaItemBuilder.setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(startMs + clipDurationMs)
                        .build()
                )
            } else {
                mediaItemBuilder.setImageDurationMs(clipDurationMs)
            }

            val videoEffects = mutableListOf<Effect>()
            if (clip.scaleX != 1f || clip.scaleY != 1f || clip.rotationDeg != 0f) {
                videoEffects.add(
                    ScaleAndRotateTransformation.Builder()
                        .setScale(clip.scaleX.coerceIn(0.2f, 4f), clip.scaleY.coerceIn(0.2f, 4f))
                        .setRotationDegrees(-clip.rotationDeg)
                        .build()
                )
            }

            val overlappingText = textOverlays.firstOrNull { overlay ->
                overlay.startTick < clip.endTick && overlay.endTick > clip.startTick
            }
            if (overlappingText != null) {
                val span = SpannableString(overlappingText.textContent).apply {
                    setSpan(
                        ForegroundColorSpan(overlappingText.textColorHex.toInt()),
                        0,
                        length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    setSpan(
                        RelativeSizeSpan((overlappingText.fontSizeSp / 24f).coerceIn(0.6f, 2.5f)),
                        0,
                        length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                videoEffects.add(OverlayEffect(listOf(TextOverlay.createStaticTextOverlay(span))))
            }

            videoEffects.add(
                Presentation.createForWidthAndHeight(
                    exportWidth.coerceAtLeast(360),
                    exportHeight,
                    Presentation.LAYOUT_SCALE_TO_FIT
                )
            )

            val audioProcessors = mutableListOf<AudioProcessor>()
            if (abs(clip.speed - 1f) > 0.02f) {
                audioProcessors.add(
                    SonicAudioProcessor().apply {
                        setSpeed(clip.speed.coerceIn(0.25f, 4f))
                        setPitch(if (clip.maintainPitch) 1f else clip.speed.coerceIn(0.25f, 4f))
                    }
                )
            }

            val itemBuilder = EditedMediaItem.Builder(mediaItemBuilder.build())
                .setEffects(Effects(audioProcessors, videoEffects))

            if (clip.type == ClipType.IMAGE) {
                itemBuilder.setFrameRate(document.canvas.frameRate.nominalFps().coerceIn(24, 60))
            }
            if (clip.volume <= 0.01f) {
                itemBuilder.setRemoveAudio(true)
            }

            itemBuilder.build()
        }

        val mainSequence = EditedMediaItemSequence(editedMainItems)

        val audioClips = document.clipsForTrack(TrackKind.AUDIO)
            .filter { it.type == ClipType.AUDIO && !it.uriString.isNullOrBlank() && it.volume > 0.01f }

        val sequences = mutableListOf(mainSequence)
        if (audioClips.isNotEmpty()) {
            val editedAudioItems = audioClips.map { clip ->
                val startMs = MediaTime.toMs(clip.trimStartTick)
                val durationMs = MediaTime.toMs(clip.durationTick).coerceAtLeast(250L)
                val item = MediaItem.Builder()
                    .setUri(Uri.parse(clip.uriString))
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(startMs)
                            .setEndPositionMs(startMs + durationMs)
                            .build()
                    )
                    .build()
                EditedMediaItem.Builder(item)
                    .setRemoveVideo(true)
                    .build()
            }
            sequences.add(EditedMediaItemSequence(editedAudioItems))
        }

        val composition = Composition.Builder(sequences).build()
        _state.value = ExportState.Exporting(0)

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                stopProgressPolling()
                activeTransformer = null
                _state.value = ExportState.Completed(
                    outputFile = outputFile,
                    fileSizeBytes = exportResult.fileSizeBytes
                )
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException
            ) {
                stopProgressPolling()
                activeTransformer = null
                _state.value = ExportState.Error(
                    exportException.localizedMessage ?: "Export failed (${exportException.errorCodeName})"
                )
            }
        }

        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(listener)
            .build()

        activeTransformer = transformer
        transformer.start(composition, outputFile.absolutePath)
        startProgressPolling(transformer)
    }

    private fun startProgressPolling(transformer: Transformer) {
        val progressHolder = ProgressHolder()
        val runnable = object : Runnable {
            override fun run() {
                val current = activeTransformer ?: return
                val progressState = current.getProgress(progressHolder)
                if (progressState == Transformer.PROGRESS_STATE_AVAILABLE) {
                    _state.value = ExportState.Exporting(progressHolder.progress.coerceIn(0, 99))
                }
                if (progressState != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    mainHandler.postDelayed(this, 250L)
                }
            }
        }
        progressRunnable = runnable
        mainHandler.postDelayed(runnable, 250L)
    }

    private fun stopProgressPolling() {
        progressRunnable?.let { mainHandler.removeCallbacks(it) }
        progressRunnable = null
    }

    fun cancelExport() {
        stopProgressPolling()
        activeTransformer?.cancel()
        activeTransformer = null
        if (_state.value is ExportState.Exporting) {
            _state.value = ExportState.Idle
        }
    }

    fun resetState() {
        _state.value = ExportState.Idle
    }
}
