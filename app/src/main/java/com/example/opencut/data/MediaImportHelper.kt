package com.example.opencut.data

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.opencut.domain.ClipType
import com.example.opencut.domain.MediaAsset
import com.example.opencut.domain.MediaTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.abs

class MediaImportHelper(private val context: Context) {

    suspend fun importMedia(uri: Uri, forcedType: ClipType? = null): MediaAsset? =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val mimeType = resolver.getType(uri).orEmpty()
                val detectedType = forcedType ?: when {
                    mimeType.startsWith("video/") -> ClipType.VIDEO
                    mimeType.startsWith("image/") -> ClipType.IMAGE
                    mimeType.startsWith("audio/") -> ClipType.AUDIO
                    else -> ClipType.VIDEO
                }

                val displayName = queryDisplayName(uri) ?: when (detectedType) {
                    ClipType.VIDEO -> "Video_${System.currentTimeMillis() % 10000}"
                    ClipType.IMAGE -> "Image_${System.currentTimeMillis() % 10000}"
                    ClipType.AUDIO -> "Audio_${System.currentTimeMillis() % 10000}"
                    else -> "Media_${System.currentTimeMillis() % 10000}"
                }

                val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                    ?: displayName.substringAfterLast('.', "").ifBlank {
                        when (detectedType) {
                            ClipType.VIDEO -> "mp4"
                            ClipType.IMAGE -> "jpg"
                            ClipType.AUDIO -> "mp3"
                            else -> "bin"
                        }
                    }

                val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
                val destFile = File(mediaDir, "${UUID.randomUUID()}.$ext")
                resolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@runCatching null

                val localUri = Uri.fromFile(destFile).toString()

                when (detectedType) {
                    ClipType.VIDEO -> extractVideoAsset(destFile, displayName, localUri)
                    ClipType.IMAGE -> extractImageAsset(destFile, displayName, localUri)
                    ClipType.AUDIO -> extractAudioAsset(destFile, displayName, localUri)
                    else -> null
                }
            }.getOrNull()
        }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        }.getOrNull()
    }

    private fun extractVideoAsset(file: File, name: String, uriString: String): MediaAsset {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.coerceAtLeast(500L) ?: 5_000L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull()?.coerceAtLeast(1) ?: 1920
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull()?.coerceAtLeast(1) ?: 1080
            MediaAsset(
                name = name,
                type = ClipType.VIDEO,
                uriString = uriString,
                durationTicks = MediaTime.fromMs(durationMs),
                width = width,
                height = height,
                waveformSamples = sampleFileWaveform(file, 48)
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun extractImageAsset(file: File, name: String, uriString: String): MediaAsset {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        val w = options.outWidth.coerceAtLeast(1)
        val h = options.outHeight.coerceAtLeast(1)
        return MediaAsset(
            name = name,
            type = ClipType.IMAGE,
            uriString = uriString,
            durationTicks = MediaTime.fromSeconds(4.0),
            width = w,
            height = h
        )
    }

    private fun extractAudioAsset(file: File, name: String, uriString: String): MediaAsset {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.coerceAtLeast(500L) ?: 10_000L
            MediaAsset(
                name = name,
                type = ClipType.AUDIO,
                uriString = uriString,
                durationTicks = MediaTime.fromMs(durationMs),
                width = 0,
                height = 0,
                waveformSamples = sampleFileWaveform(file, 64)
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun sampleFileWaveform(file: File, buckets: Int): List<Float> {
        val length = file.length()
        if (length <= 0L) return List(buckets) { 0.4f }
        val result = ArrayList<Float>(buckets)
        runCatching {
            file.inputStream().use { stream ->
                val chunkSize = (length / buckets).coerceAtLeast(64L).toInt().coerceAtMost(4096)
                val buf = ByteArray(chunkSize)
                for (i in 0 until buckets) {
                    val read = stream.read(buf)
                    if (read <= 0) {
                        result.add(0.25f)
                    } else {
                        var sum = 0L
                        for (j in 0 until read) {
                            sum += abs(buf[j].toInt())
                        }
                        val avg = (sum.toFloat() / read.toFloat()) / 128f
                        result.add((0.2f + avg * 0.8f).coerceIn(0.15f, 1f))
                    }
                }
            }
        }
        return if (result.size == buckets) result else List(buckets) { 0.45f }
    }
}
