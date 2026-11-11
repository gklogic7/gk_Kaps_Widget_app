package com.example.core

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.MediaFormat
import com.example.data.MediaItemEntity
import com.example.data.MediaStorage
import com.example.utils.AlphaUtils
import com.example.utils.ImageUtils
import com.example.widgets.GifRenderer
import com.example.widgets.SvgRenderer
import java.io.File
import java.io.FileOutputStream

class FileAccess(
    private val context: Context,
    private val mediaStorage: MediaStorage
) {

    /**
     * Takes a persistable URI permission via Android's Storage Access Framework (SAF)
     * and copies the selected media into KAPS's internal sandbox so widgets survive reboots
     * and external SD/document provider changes while also tracking original URI accessibility.
     */
    fun importFromSafUri(uri: Uri): MediaItemEntity? {
        val resolver = context.contentResolver
        runCatching {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        val displayName = queryDisplayName(uri) ?: "Imported_${System.currentTimeMillis()}"
        val mimeType = resolver.getType(uri) ?: guessMimeFromName(displayName)
        val extHint = displayName.substringAfterLast('.', "png")
        val localFile = mediaStorage.allocateMediaFile(extHint)

        val copiedOk = runCatching {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
                true
            } ?: false
        }.getOrDefault(false)

        if (!copiedOk || !localFile.exists() || localFile.length() == 0L) {
            return null
        }

        return inspectLocalFile(
            localFile = localFile,
            displayName = displayName,
            originalUri = uri.toString(),
            mimeTypeHint = mimeType
        )
    }

    fun inspectLocalFile(
        localFile: File,
        displayName: String,
        originalUri: String,
        mimeTypeHint: String = ""
    ): MediaItemEntity {
        val trueFormat = ImageUtils.inspectMediaFormat(localFile, displayName)
        var widthPx = 240
        var heightPx = 240
        var frameCount = 1
        var delayMs = 120
        var hasAlpha = true
        var svgNotes = ""

        when (trueFormat) {
            MediaFormat.SVG_STATIC, MediaFormat.SVG_ANIMATED -> {
                val report = SvgRenderer.analyzeSvg(localFile)
                widthPx = report.viewBoxWidth.toInt().coerceAtLeast(64)
                heightPx = report.viewBoxHeight.toInt().coerceAtLeast(64)
                frameCount = if (trueFormat == MediaFormat.SVG_ANIMATED) 8 else 1
                delayMs = 150
                hasAlpha = true
                svgNotes = report.toSummaryString()
            }
            MediaFormat.GIF_ANIMATED, MediaFormat.GIF_STATIC, MediaFormat.WEBP_ANIMATED -> {
                val extracted = GifRenderer.extractRawGifFrames(localFile, maxFrames = 10)
                val firstBmp = extracted.first.firstOrNull()
                if (firstBmp != null) {
                    widthPx = firstBmp.width
                    heightPx = firstBmp.height
                    hasAlpha = AlphaUtils.detectTransparency(firstBmp)
                }
                frameCount = extracted.first.size.coerceAtLeast(1)
                delayMs = extracted.second
            }
            else -> {
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                }
                val bmp = BitmapFactory.decodeFile(localFile.absolutePath, opts)
                if (bmp != null) {
                    widthPx = bmp.width
                    heightPx = bmp.height
                    hasAlpha = AlphaUtils.detectTransparency(bmp)
                }
            }
        }

        val resolvedMime = mimeTypeHint.ifBlank {
            when (trueFormat) {
                MediaFormat.SVG_STATIC, MediaFormat.SVG_ANIMATED -> "image/svg+xml"
                MediaFormat.GIF_ANIMATED, MediaFormat.GIF_STATIC -> "image/gif"
                MediaFormat.WEBP, MediaFormat.WEBP_ANIMATED -> "image/webp"
                MediaFormat.JPEG -> "image/jpeg"
                MediaFormat.BMP -> "image/bmp"
                MediaFormat.PNG -> "image/png"
            }
        }

        return MediaItemEntity(
            displayName = displayName,
            originalUri = originalUri,
            localFilePath = localFile.absolutePath,
            mimeType = resolvedMime,
            mediaFormat = trueFormat,
            widthPx = widthPx.coerceAtLeast(1),
            heightPx = heightPx.coerceAtLeast(1),
            fileSizeBytes = localFile.length(),
            frameCount = frameCount,
            defaultFrameDelayMs = delayMs,
            hasTransparency = hasAlpha,
            isBuiltInSample = false,
            isSourceAccessible = true,
            svgCompatibilityNotes = svgNotes
        )
    }

    /**
     * Checks whether the media source (either internal sandbox copy or original SAF URI) is still accessible.
     */
    fun verifyMediaAccessibility(item: MediaItemEntity): Boolean {
        val local = File(item.localFilePath)
        if (local.exists() && local.length() > 0L) return true
        if (item.originalUri.startsWith("internal://")) return local.exists()

        return runCatching {
            val uri = Uri.parse(item.originalUri)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                // Re-hydrate local sandbox copy if it was cleared
                FileOutputStream(local).use { out -> stream.copyTo(out) }
                true
            } ?: false
        }.getOrDefault(false)
    }

    private fun queryDisplayName(uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else {
                uri.lastPathSegment
            }
        } catch (_: Exception) {
            uri.lastPathSegment
        } finally {
            cursor?.close()
        }
    }

    private fun guessMimeFromName(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".svg") -> "image/svg+xml"
            lower.endsWith(".gif") -> "image/gif"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".bmp") -> "image/bmp"
            else -> "image/png"
        }
    }
}
