package com.example.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.example.data.MediaFormat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

object ImageUtils {

    /**
     * Inspects actual file header bytes and XML content to determine the true media format.
     * Never trusts renamed extensions alone (e.g., an SVG renamed to .png is still detected as SVG).
     */
    fun inspectMediaFormat(file: File, fileNameHint: String = ""): MediaFormat {
        if (!file.exists() || file.length() == 0L) return MediaFormat.PNG
        val header = ByteArray(4096)
        val bytesRead = file.inputStream().use { it.read(header) }.coerceAtLeast(0)
        if (bytesRead >= 6) {
            // GIF87a or GIF89a signature
            val sig6 = String(header, 0, 6, StandardCharsets.US_ASCII)
            if (sig6 == "GIF87a" || sig6 == "GIF89a") {
                val frames = countGifFrames(file)
                return if (frames > 1) MediaFormat.GIF_ANIMATED else MediaFormat.GIF_STATIC
            }
        }
        if (bytesRead >= 12) {
            // RIFF....WEBP
            val riff = String(header, 0, 4, StandardCharsets.US_ASCII)
            val webp = String(header, 8, 4, StandardCharsets.US_ASCII)
            if (riff == "RIFF" && webp == "WEBP") {
                val headStr = String(header, 0, bytesRead, StandardCharsets.ISO_8859_1)
                return if (headStr.contains("ANIM") || headStr.contains("ANMF")) {
                    MediaFormat.WEBP_ANIMATED
                } else {
                    MediaFormat.WEBP
                }
            }
        }
        if (bytesRead >= 8) {
            // PNG signature: 89 50 4E 47 0D 0A 1A 0A
            if (header[0] == 0x89.toByte() &&
                header[1] == 0x50.toByte() &&
                header[2] == 0x4E.toByte() &&
                header[3] == 0x47.toByte()
            ) {
                return MediaFormat.PNG
            }
        }
        if (bytesRead >= 3) {
            // JPEG signature: FF D8 FF
            if (header[0] == 0xFF.toByte() &&
                header[1] == 0xD8.toByte() &&
                header[2] == 0xFF.toByte()
            ) {
                return MediaFormat.JPEG
            }
        }
        if (bytesRead >= 2) {
            // BMP signature: BM
            if (header[0] == 0x42.toByte() && header[1] == 0x4D.toByte()) {
                return MediaFormat.BMP
            }
        }

        // Check for SVG XML content
        val textSample = runCatching {
            file.bufferedReader().use { reader ->
                val buf = CharArray(8192)
                val count = reader.read(buf)
                if (count > 0) String(buf, 0, count) else ""
            }
        }.getOrDefault("")

        if (textSample.contains("<svg", ignoreCase = true) ||
            fileNameHint.endsWith(".svg", ignoreCase = true)
        ) {
            val hasAnim = textSample.contains("<animate", ignoreCase = true) ||
                textSample.contains("<animateTransform", ignoreCase = true) ||
                textSample.contains("@keyframes", ignoreCase = true) ||
                textSample.contains("animation:", ignoreCase = true)
            return if (hasAnim) MediaFormat.SVG_ANIMATED else MediaFormat.SVG_STATIC
        }

        return MediaFormat.PNG
    }

    /**
     * Fast binary scan of GIF89a image descriptor blocks (0x2C) to count frames accurately.
     */
    fun countGifFrames(file: File): Int {
        if (!file.exists()) return 1
        return runCatching {
            val bytes = file.readBytes()
            var count = 0
            for (i in 0 until bytes.size - 1) {
                // Image Separator byte 0x2C preceded by Graphic Control Extension or logical screen
                if (bytes[i] == 0x21.toByte() && bytes[i + 1] == 0xF9.toByte()) {
                    count++
                }
            }
            if (count == 0) 1 else count
        }.getOrDefault(1)
    }

    /**
     * Generates a high-resolution transparent PNG character sticker ("KAPS Bot Companion")
     * with soft anti-aliased edges and alpha shadow so users can immediately test transparent PNG widgets.
     */
    fun writeStarterTransparentPng(targetFile: File) {
        val size = 512
        val bmp = AlphaUtils.createTransparentBitmap(size, size)
        val canvas = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // Subtle floating drop aura (semi-transparent)
        p.color = Color.argb(45, 17, 18, 22)
        canvas.drawOval(RectF(136f, 430f, 376f, 472f), p)

        // Antenna
        p.color = Color.parseColor("#111216")
        p.strokeWidth = 14f
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(256f, 118f, 256f, 64f, p)

        p.style = Paint.Style.FILL
        p.color = Color.parseColor("#E11D48")
        canvas.drawCircle(256f, 54f, 22f, p)
        // Gloss highlight on antenna orb
        p.color = Color.argb(180, 255, 255, 255)
        canvas.drawCircle(249f, 47f, 6f, p)

        // Side ear pods
        p.color = Color.parseColor("#E11D48")
        canvas.drawRoundRect(RectF(68f, 196f, 112f, 292f), 20f, 20f, p)
        canvas.drawRoundRect(RectF(400f, 196f, 444f, 292f), 20f, 20f, p)

        // Outer robot head chassis (Alabaster metallic gradient)
        val chassisRect = RectF(96f, 114f, 416f, 384f)
        p.shader = LinearGradient(
            96f, 114f, 416f, 384f,
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#DCE0EC"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(chassisRect, 72f, 72f, p)
        p.shader = null

        // Crisp dark outline
        p.style = Paint.Style.STROKE
        p.strokeWidth = 12f
        p.color = Color.parseColor("#111216")
        canvas.drawRoundRect(chassisRect, 72f, 72f, p)

        // Inner obsidian visor screen
        p.style = Paint.Style.FILL
        p.color = Color.parseColor("#111216")
        val visorRect = RectF(134f, 158f, 378f, 322f)
        canvas.drawRoundRect(visorRect, 44f, 44f, p)

        // Glowing cyan/crimson expressive eyes
        p.color = Color.parseColor("#38BDF8")
        canvas.drawRoundRect(RectF(172f, 202f, 220f, 258f), 24f, 24f, p)
        canvas.drawRoundRect(RectF(292f, 202f, 340f, 258f), 24f, 24f, p)

        p.color = Color.WHITE
        canvas.drawCircle(206f, 218f, 8f, p)
        canvas.drawCircle(326f, 218f, 8f, p)

        // Happy curved smile arc
        p.style = Paint.Style.STROKE
        p.strokeWidth = 8f
        p.color = Color.parseColor("#38BDF8")
        canvas.drawArc(RectF(226f, 256f, 286f, 294f), 15f, 150f, false, p)

        // Nothing-style dot matrix badge on cheek
        p.style = Paint.Style.FILL
        p.color = Color.parseColor("#E11D48")
        for (dx in 0..2) {
            for (dy in 0..1) {
                canvas.drawCircle(326f + dx * 12f, 348f + dy * 12f, 3.5f, p)
            }
        }

        FileOutputStream(targetFile).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    /**
     * Generates a valid multi-frame animated GIF89a binary file with transparent background
     * (index 0 transparent) and 6 animated frames so users can test true GIF playback immediately.
     */
    fun writeStarterAnimatedGif(targetFile: File) {
        val width = 128
        val height = 128
        val out = ByteArrayOutputStream()

        // 1. Header: GIF89a
        out.write("GIF89a".toByteArray(StandardCharsets.US_ASCII))

        // 2. Logical Screen Descriptor (128x128, Global Color Table flag = 1, 8 colors -> 2^(2+1)=8)
        writeShortLE(out, width)
        writeShortLE(out, height)
        out.write(0x92) // GCT present (1), color res (001), sort (0), size of GCT = 2 (8 colors)
        out.write(0x00) // Background color index 0 (transparent)
        out.write(0x00) // Pixel aspect ratio

        // 3. Global Color Table (8 RGB entries = 24 bytes)
        val palette = intArrayOf(
            0x000000, // 0: Transparent key
            0x111216, // 1: Obsidian Ink
            0xE11D48, // 2: Signal Crimson
            0xFFFFFF, // 3: Pure White
            0x2563EB, // 4: Blueprint Cobalt
            0x38BDF8, // 5: Sky Cyan
            0xF59E0B, // 6: Amber Gold
            0x10B981  // 7: Emerald
        )
        for (rgb in palette) {
            out.write((rgb shr 16) and 0xFF)
            out.write((rgb shr 8) and 0xFF)
            out.write(rgb and 0xFF)
        }

        // 4. Netscape 2.0 Looping Application Extension
        out.write(
            byteArrayOf(
                0x21, 0xFF.toByte(), 0x0B,
                'N'.code.toByte(), 'E'.code.toByte(), 'T'.code.toByte(), 'S'.code.toByte(),
                'C'.code.toByte(), 'A'.code.toByte(), 'P'.code.toByte(), 'E'.code.toByte(),
                '2'.code.toByte(), '.'.code.toByte(), '0'.code.toByte(),
                0x03, 0x01, 0x00, 0x00, 0x00
            )
        )

        // 5. Write 6 animated frames showing an orbiting pulse ring + rotating satellite node
        val totalFrames = 6
        for (frameIdx in 0 until totalFrames) {
            // Graphic Control Extension: Disposal = 2 (Restore to background/transparent), Transparent Index = 0, Delay = 14 (140ms)
            out.write(
                byteArrayOf(
                    0x21, 0xF9.toByte(), 0x04,
                    0x09, // Disposal method 2 (restore to bg) + transparent color flag 1
                    0x0E, 0x00, // 140ms frame delay
                    0x00, // Transparent color index 0
                    0x00
                )
            )

            // Image Descriptor
            out.write(0x2C)
            writeShortLE(out, 0)
            writeShortLE(out, 0)
            writeShortLE(out, width)
            writeShortLE(out, height)
            out.write(0x00) // No local color table

            // Build indexed pixel array (values 0..7)
            val pixels = ByteArray(width * height)
            val cx = width / 2.0
            val cy = height / 2.0
            val angle = (frameIdx.toDouble() / totalFrames) * Math.PI * 2.0
            val satX = cx + Math.cos(angle) * 40.0
            val satY = cy + Math.sin(angle) * 40.0
            val pulseRadius = 16.0 + (frameIdx % 3) * 4.0

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val dx = x - cx
                    val dy = y - cy
                    val dist = Math.sqrt(dx * dx + dy * dy)

                    val sdx = x - satX
                    val sdy = y - satY
                    val satDist = Math.sqrt(sdx * sdx + sdy * sdy)

                    val colorIdx: Byte = when {
                        satDist <= 9.0 -> 2 // Signal Crimson orbiting satellite
                        satDist <= 12.0 -> 3 // White halo on satellite
                        dist >= 37.5 && dist <= 42.5 -> 1 // Obsidian orbit track ring
                        dist <= pulseRadius -> 4 // Blueprint Cobalt pulsing core
                        dist <= pulseRadius + 3.5 -> 5 // Cyan core rim
                        else -> 0 // Transparent
                    }
                    pixels[y * width + x] = colorIdx
                }
            }

            // Encode using Uncompressed GIF LZW (minCodeSize = 3, clearCode = 8, eoiCode = 9)
            writeGifUncompressedLzw(out, pixels)
        }

        // 6. GIF Trailer
        out.write(0x3B)

        FileOutputStream(targetFile).use { it.write(out.toByteArray()) }
    }

    private fun writeShortLE(out: ByteArrayOutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
    }

    /**
     * Encodes 3-bit indexed pixels (0..7) using valid LZW with periodic Clear codes (8)
     * so every standard GIF decoder and Android ImageDecoder/BitmapFactory reads it cleanly.
     */
    private fun writeGifUncompressedLzw(out: ByteArrayOutputStream, pixels: ByteArray) {
        val minCodeSize = 3
        out.write(minCodeSize)

        val clearCode = 8
        val eoiCode = 9

        val bitStream = ByteArrayOutputStream()
        var bitBuffer = 0
        var bitCount = 0

        fun emitCode(code: Int, bits: Int = 4) {
            bitBuffer = bitBuffer or ((code and ((1 shl bits) - 1)) shl bitCount)
            bitCount += bits
            while (bitCount >= 8) {
                bitStream.write(bitBuffer and 0xFF)
                bitBuffer = bitBuffer ushr 8
                bitCount -= 8
            }
        }

        emitCode(clearCode)
        var runCount = 0
        for (px in pixels) {
            emitCode(px.toInt() and 0x07)
            runCount++
            // Reset LZW table before 4-bit codes overflow (16 - 10 = 6 entries max)
            if (runCount == 4) {
                emitCode(clearCode)
                runCount = 0
            }
        }
        emitCode(eoiCode)
        if (bitCount > 0) {
            bitStream.write(bitBuffer and 0xFF)
        }

        val encodedBytes = bitStream.toByteArray()
        var offset = 0
        while (offset < encodedBytes.size) {
            val blockLen = minOf(255, encodedBytes.size - offset)
            out.write(blockLen)
            out.write(encodedBytes, offset, blockLen)
            offset += blockLen
        }
        out.write(0x00) // Block terminator
    }

    /**
     * Writes a crisp, resolution-independent Static Vector SVG starter graphic
     * featuring linear gradients, complex bezier paths, strokes, and transparent background.
     */
    fun writeStarterStaticSvg(targetFile: File) {
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 240 240" width="240" height="240">
              <defs>
                <linearGradient id="crimsonGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#FB7185" />
                  <stop offset="100%" stop-color="#E11D48" />
                </linearGradient>
                <linearGradient id="obsidianGrad" x1="0%" y1="0%" x2="0%" y2="100%">
                  <stop offset="0%" stop-color="#272A36" />
                  <stop offset="100%" stop-color="#111216" />
                </linearGradient>
              </defs>
              <!-- Outer Geometric Hex Frame -->
              <polygon points="120,16 210,68 210,172 120,224 30,172 30,68" fill="url(#obsidianGrad)" stroke="#FFFFFF" stroke-width="4" />
              <!-- Inner Isometric Prism -->
              <path d="M 120 48 L 182 84 L 182 156 L 120 192 L 58 156 L 58 84 Z" fill="none" stroke="#E11D48" stroke-width="5" />
              <!-- Top Facet -->
              <path d="M 120 48 L 182 84 L 120 120 L 58 84 Z" fill="url(#crimsonGrad)" opacity="0.92" />
              <!-- Left Facet -->
              <path d="M 58 84 L 120 120 L 120 192 L 58 156 Z" fill="#FFFFFF" opacity="0.18" />
              <!-- Center Precision Node -->
              <circle cx="120" cy="120" r="14" fill="#FFFFFF" stroke="#111216" stroke-width="4" />
              <circle cx="120" cy="120" r="5" fill="#E11D48" />
              <!-- Dot Matrix Corner Accents -->
              <circle cx="120" cy="30" r="3" fill="#E11D48" />
              <circle cx="132" cy="37" r="3" fill="#FFFFFF" />
              <circle cx="108" cy="37" r="3" fill="#FFFFFF" />
            </svg>
        """.trimIndent()
        targetFile.writeText(svg)
    }

    /**
     * Writes an Animated Vector SVG starter graphic using supported <animateTransform>
     * (rotation & scaling) and <animate> (opacity) elements.
     */
    fun writeStarterAnimatedSvg(targetFile: File) {
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 240 240" width="240" height="240">
              <defs>
                <radialGradient id="coreGlow" cx="50%" cy="50%" r="50%">
                  <stop offset="0%" stop-color="#38BDF8" stop-opacity="0.9" />
                  <stop offset="100%" stop-color="#2563EB" stop-opacity="0.1" />
                </radialGradient>
              </defs>
              <!-- Outer Compass Bezel -->
              <circle cx="120" cy="120" r="96" fill="#111216" stroke="#2B2F3C" stroke-width="6" />
              <circle cx="120" cy="120" r="76" fill="url(#coreGlow)" stroke="#38BDF8" stroke-width="2" />
              <!-- Pulsing Radar Halo -->
              <circle cx="120" cy="120" r="52" fill="none" stroke="#E11D48" stroke-width="4">
                <animate attributeName="opacity" values="0.25;1.0;0.25" dur="2s" repeatCount="indefinite" />
              </circle>
              <!-- Rotating Chrono Sweep Hand Group -->
              <g>
                <animateTransform attributeName="transform" type="rotate" from="0 120 120" to="360 120 120" dur="4s" repeatCount="indefinite" />
                <line x1="120" y1="120" x2="120" y2="34" stroke="#E11D48" stroke-width="6" />
                <circle cx="120" cy="42" r="9" fill="#E11D48" stroke="#FFFFFF" stroke-width="3" />
                <path d="M 120 120 L 154 154" stroke="#FFFFFF" stroke-width="4" />
              </g>
              <!-- Center Anchor Cap -->
              <circle cx="120" cy="120" r="12" fill="#FFFFFF" stroke="#111216" stroke-width="4" />
            </svg>
        """.trimIndent()
        targetFile.writeText(svg)
    }
}
