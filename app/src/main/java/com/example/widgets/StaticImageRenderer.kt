package com.example.widgets

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.data.WidgetConfigEntity
import com.example.utils.AlphaUtils
import com.example.utils.BitmapUtils
import java.io.File
import kotlin.math.max

object StaticImageRenderer {

    /**
     * Decodes a static image (PNG, JPG, WEBP, BMP) at the target widget resolution using
     * power-of-two subsampling for memory efficiency, preserving full ARGB_8888 alpha transparency.
     */
    fun renderStaticImage(
        file: File,
        targetWidthPx: Int,
        targetHeightPx: Int,
        config: WidgetConfigEntity
    ): Bitmap {
        if (!file.exists()) {
            return AlphaUtils.createTransparentBitmap(targetWidthPx, targetHeightPx)
        }

        // 1. Read bounds only without allocating pixel memory
        val boundsOpts = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, boundsOpts)

        val srcW = boundsOpts.outWidth.coerceAtLeast(1)
        val srcH = boundsOpts.outHeight.coerceAtLeast(1)

        // 2. Calculate optimal inSampleSize so large 4K+ photos never spike RAM
        val desiredMax = max(targetWidthPx, targetHeightPx) * 2
        var sampleSize = 1
        while ((srcW / (sampleSize * 2)) >= desiredMax && (srcH / (sampleSize * 2)) >= desiredMax) {
            sampleSize *= 2
        }

        // 3. Decode with ARGB_8888 to strictly preserve alpha channel and partial transparency
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = false
        }

        val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
            ?: return AlphaUtils.createTransparentBitmap(targetWidthPx, targetHeightPx)

        val composed = BitmapUtils.composeWidgetBitmap(
            source = decoded,
            targetWidthPx = targetWidthPx,
            targetHeightPx = targetHeightPx,
            config = config
        )

        if (decoded !== composed && !decoded.isRecycled) {
            // Allow GC of intermediate decoded bitmap if distinct
        }
        return composed
    }
}
