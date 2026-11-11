package com.example.widgets

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Movie
import com.example.data.WidgetConfigEntity
import com.example.utils.AlphaUtils
import com.example.utils.BitmapUtils
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

data class GifFrameResult(
    val frames: List<Bitmap>,
    val frameDelayMs: Int
)

object GifRenderer {

    /**
     * Extracts individual alpha-preserving ARGB_8888 frames from a GIF or Animated WebP file,
     * applying the widget's scale/background/opacity configuration to each frame.
     * Bounds frame count and resolution for battery-efficient RemoteViews playback.
     */
    @Suppress("DEPRECATION")
    fun decodeGifFrames(
        file: File,
        targetWidthPx: Int,
        targetHeightPx: Int,
        config: WidgetConfigEntity,
        maxFrames: Int = 8
    ): GifFrameResult {
        if (!file.exists()) {
            val fallback = AlphaUtils.createTransparentBitmap(targetWidthPx, targetHeightPx)
            return GifFrameResult(listOf(fallback), 150)
        }

        val rawFrames = extractRawGifFrames(file, maxFrames)
        val composedFrames = if (rawFrames.first.isNotEmpty()) {
            rawFrames.first.map { rawBmp ->
                BitmapUtils.composeWidgetBitmap(rawBmp, targetWidthPx, targetHeightPx, config)
            }
        } else {
            val single = BitmapFactory.decodeFile(file.absolutePath)
                ?: AlphaUtils.createTransparentBitmap(targetWidthPx, targetHeightPx)
            listOf(BitmapUtils.composeWidgetBitmap(single, targetWidthPx, targetHeightPx, config))
        }

        val speed = config.animationSpeedMultiplier.coerceIn(0.25f, 3.0f)
        val adjustedDelay = (rawFrames.second / speed).roundToInt().coerceIn(70, 2000)

        // If animation is disabled or user picked a single static frame, return just that frame
        if (!config.isAnimationEnabled || composedFrames.size <= 1) {
            val idx = config.selectedFrameIndex.coerceIn(0, composedFrames.lastIndex)
            return GifFrameResult(listOf(composedFrames[idx]), adjustedDelay)
        }

        // Rotate frame list so selectedFrameIndex is the starting frame
        val startIdx = config.selectedFrameIndex.coerceIn(0, composedFrames.lastIndex)
        val ordered = if (startIdx == 0) {
            composedFrames
        } else {
            composedFrames.subList(startIdx, composedFrames.size) + composedFrames.subList(0, startIdx)
        }

        return GifFrameResult(ordered, adjustedDelay)
    }

    /**
     * Decodes multi-frame GIF streams using android.graphics.Movie (with transparent canvas clear
     * per time step) and falls back to native GIF89a LZW frame extraction or BitmapFactory.
     */
    @Suppress("DEPRECATION")
    fun extractRawGifFrames(file: File, maxFrames: Int = 8): Pair<List<Bitmap>, Int> {
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return emptyList<Bitmap>() to 140

        // First try our native GIF89a indexed decoder for crisp exact frame extraction
        val nativeDecoded = runCatching { decodeGif89aIndexedFrames(bytes, maxFrames) }.getOrNull()
        if (nativeDecoded != null && nativeDecoded.first.isNotEmpty()) {
            return nativeDecoded
        }

        // Fallback to android.graphics.Movie time-sampling
        val movie = runCatching { Movie.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
        if (movie != null && movie.width() > 0 && movie.height() > 0) {
            val duration = movie.duration()
            val mw = movie.width()
            val mh = movie.height()
            if (duration > 0) {
                val frameCount = (duration / 120).coerceIn(2, maxFrames)
                val stepMs = max(duration / frameCount, 80)
                val list = mutableListOf<Bitmap>()
                for (i in 0 until frameCount) {
                    val bmp = AlphaUtils.createTransparentBitmap(mw, mh)
                    val canvas = Canvas(bmp)
                    canvas.drawColor(Color.TRANSPARENT)
                    movie.setTime((i * stepMs) % duration)
                    movie.draw(canvas, 0f, 0f)
                    list.add(bmp)
                }
                return list to stepMs
            }
        }

        val staticBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        return if (staticBmp != null) listOf(staticBmp) to 150 else emptyList<Bitmap>() to 150
    }

    /**
     * Lightweight GIF89a LZW frame decoder that preserves exact palette alpha transparency.
     */
    private fun decodeGif89aIndexedFrames(bytes: ByteArray, maxFrames: Int): Pair<List<Bitmap>, Int>? {
        if (bytes.size < 14) return null
        val sig = String(bytes, 0, 6, Charsets.US_ASCII)
        if (sig != "GIF87a" && sig != "GIF89a") return null

        var pos = 6
        fun readU8(): Int = if (pos < bytes.size) bytes[pos++].toInt() and 0xFF else 0
        fun readU16(): Int {
            val lo = readU8()
            val hi = readU8()
            return lo or (hi shl 8)
        }

        val logicalW = readU16()
        val logicalH = readU16()
        if (logicalW <= 0 || logicalH <= 0 || logicalW > 2048 || logicalH > 2048) return null

        val packed = readU8()
        val gctFlag = (packed and 0x80) != 0
        val gctSize = 1 shl ((packed and 0x07) + 1)
        readU8() // bgColor
        readU8() // aspect

        val globalPalette = IntArray(256)
        if (gctFlag) {
            for (i in 0 until gctSize) {
                val r = readU8()
                val g = readU8()
                val b = readU8()
                globalPalette[i] = Color.argb(255, r, g, b)
            }
        }

        val frames = mutableListOf<Bitmap>()
        var currentCanvas = AlphaUtils.createTransparentBitmap(logicalW, logicalH)
        var transparentIndex = -1
        var disposalMethod = 0
        var delayMs = 140

        while (pos < bytes.size && frames.size < maxFrames) {
            when (val blockId = readU8()) {
                0x21 -> { // Extension
                    val extLabel = readU8()
                    if (extLabel == 0xF9) { // Graphic Control Extension
                        readU8() // block size (4)
                        val gcePacked = readU8()
                        disposalMethod = (gcePacked shr 2) and 0x07
                        val transFlag = (gcePacked and 0x01) != 0
                        val delayCs = readU16()
                        val transIdx = readU8()
                        readU8() // terminator
                        if (delayCs > 0) delayMs = (delayCs * 10).coerceIn(60, 2000)
                        transparentIndex = if (transFlag) transIdx else -1
                    } else {
                        // Skip sub-blocks
                        while (pos < bytes.size) {
                            val subSize = readU8()
                            if (subSize == 0) break
                            pos = (pos + subSize).coerceAtMost(bytes.size)
                        }
                    }
                }
                0x2C -> { // Image Descriptor
                    val left = readU16()
                    val top = readU16()
                    val iw = readU16()
                    val ih = readU16()
                    val imgPacked = readU8()
                    val lctFlag = (imgPacked and 0x80) != 0
                    val interlaceFlag = (imgPacked and 0x40) != 0
                    val lctSize = 1 shl ((imgPacked and 0x07) + 1)

                    val activePalette = globalPalette.clone()
                    if (lctFlag) {
                        for (i in 0 until lctSize) {
                            val r = readU8()
                            val g = readU8()
                            val b = readU8()
                            activePalette[i] = Color.argb(255, r, g, b)
                        }
                    }

                    val minCodeSize = readU8()
                    val lzwBlock = ByteArrayOutputStream()
                    while (pos < bytes.size) {
                        val subSize = readU8()
                        if (subSize == 0) break
                        val safeLen = subSize.coerceAtMost(bytes.size - pos)
                        lzwBlock.write(bytes, pos, safeLen)
                        pos += safeLen
                    }

                    val pixelCount = iw * ih
                    val indices = decodeLzwStream(lzwBlock.toByteArray(), minCodeSize, pixelCount)
                    if (indices != null) {
                        val prevCanvas = currentCanvas.copy(Bitmap.Config.ARGB_8888, true)
                        for (py in 0 until ih) {
                            val srcY = if (!interlaceFlag) py else deinterlaceRow(py, ih)
                            val dstY = top + srcY
                            if (dstY !in 0 until logicalH) continue
                            for (px in 0 until iw) {
                                val dstX = left + px
                                if (dstX !in 0 until logicalW) continue
                                val colorIdx = indices[py * iw + px].toInt() and 0xFF
                                if (colorIdx != transparentIndex) {
                                    currentCanvas.setPixel(dstX, dstY, activePalette[colorIdx])
                                }
                            }
                        }
                        frames.add(currentCanvas.copy(Bitmap.Config.ARGB_8888, false))

                        if (disposalMethod == 2) {
                            currentCanvas = AlphaUtils.createTransparentBitmap(logicalW, logicalH)
                        } else if (disposalMethod == 3) {
                            currentCanvas = prevCanvas
                        }
                    }
                    transparentIndex = -1
                }
                0x3B -> break // Trailer
                else -> if (blockId == 0) continue else break
            }
        }

        return if (frames.isNotEmpty()) frames to delayMs else null
    }

    private fun deinterlaceRow(row: Int, height: Int): Int {
        var r = row
        val pass1 = (height + 7) / 8
        if (r < pass1) return r * 8
        r -= pass1
        val pass2 = (height + 3) / 8
        if (r < pass2) return r * 8 + 4
        r -= pass2
        val pass3 = (height + 1) / 4
        if (r < pass3) return r * 4 + 2
        r -= pass3
        return r * 2 + 1
    }

    private fun decodeLzwStream(data: ByteArray, minCodeSize: Int, expectedPixels: Int): ByteArray? {
        if (minCodeSize < 2 || minCodeSize > 8 || expectedPixels <= 0) return null
        val maxStack = 4096
        val prefix = IntArray(maxStack)
        val suffix = ByteArray(maxStack)
        val pixelStack = ByteArray(maxStack + 1)
        val output = ByteArray(expectedPixels)

        val clearCode = 1 shl minCodeSize
        val endOfInfo = clearCode + 1
        var available = clearCode + 2
        var oldCode = -1
        var codeSize = minCodeSize + 1
        var codeMask = (1 shl codeSize) - 1

        for (code in 0 until clearCode) {
            prefix[code] = 0
            suffix[code] = code.toByte()
        }

        var datum = 0
        var bits = 0
        var first = 0
        var top = 0
        var bi = 0
        var outIdx = 0

        while (outIdx < expectedPixels) {
            if (top == 0) {
                if (bits < codeSize) {
                    if (bi >= data.size) break
                    datum += (data[bi++].toInt() and 0xFF) shl bits
                    bits += 8
                    continue
                }
                var code = datum and codeMask
                datum = datum ushr codeSize
                bits -= codeSize

                if (code > available || code == endOfInfo) break
                if (code == clearCode) {
                    codeSize = minCodeSize + 1
                    codeMask = (1 shl codeSize) - 1
                    available = clearCode + 2
                    oldCode = -1
                    continue
                }
                if (oldCode == -1) {
                    pixelStack[top++] = suffix[code]
                    oldCode = code
                    first = code
                    continue
                }
                val inCode = code
                if (code == available) {
                    pixelStack[top++] = first.toByte()
                    code = oldCode
                }
                while (code > clearCode) {
                    if (top >= maxStack) break
                    pixelStack[top++] = suffix[code]
                    code = prefix[code]
                }
                first = suffix[code].toInt() and 0xFF
                if (top < maxStack) {
                    pixelStack[top++] = first.toByte()
                }
                if (available < maxStack) {
                    prefix[available] = oldCode
                    suffix[available] = first.toByte()
                    available++
                    if ((available and codeMask) == 0 && available < maxStack) {
                        codeSize++
                        codeMask += available
                    }
                }
                oldCode = inCode
            }
            top--
            output[outIdx++] = pixelStack[top]
        }
        return output
    }
}
