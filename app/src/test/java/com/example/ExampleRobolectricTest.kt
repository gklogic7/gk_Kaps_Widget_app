package com.example

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.example.data.BackgroundMode
import com.example.data.MediaFormat
import com.example.data.ScaleMode
import com.example.data.WidgetConfigEntity
import com.example.utils.AlphaUtils
import com.example.utils.DimensionUtils
import com.example.utils.ImageUtils
import com.example.widgets.GifRenderer
import com.example.widgets.StaticImageRenderer
import com.example.widgets.SvgRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `verify app name is KAPS`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("KAPS", appName)
    }

    @Test
    fun `verify transparent PNG generation and alpha preservation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempPng = File(context.cacheDir, "test_transparent.png")
        ImageUtils.writeStarterTransparentPng(tempPng)

        assertEquals(MediaFormat.PNG, ImageUtils.inspectMediaFormat(tempPng))

        val config = WidgetConfigEntity(
            widgetId = 1,
            title = "Alpha Test",
            mediaId = 1L,
            sourceUri = tempPng.toURI().toString(),
            localFilePath = tempPng.absolutePath,
            mediaFormat = MediaFormat.PNG,
            backgroundMode = BackgroundMode.TRANSPARENT,
            preserveTransparency = true,
            scaleMode = ScaleMode.FIT_INSIDE
        )
        val rendered = StaticImageRenderer.renderStaticImage(tempPng, 256, 256, config)
        assertEquals(256, rendered.width)
        assertEquals(256, rendered.height)
        assertTrue(AlphaUtils.detectTransparency(rendered))
    }

    @Test
    fun `verify animated GIF89a generation and frame decoding`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempGif = File(context.cacheDir, "test_anim.gif")
        ImageUtils.writeStarterAnimatedGif(tempGif)

        assertEquals(MediaFormat.GIF_ANIMATED, ImageUtils.inspectMediaFormat(tempGif))
        assertTrue(ImageUtils.countGifFrames(tempGif) >= 4)

        val config = WidgetConfigEntity(
            widgetId = 2,
            title = "GIF Test",
            mediaId = 2L,
            sourceUri = tempGif.toURI().toString(),
            localFilePath = tempGif.absolutePath,
            mediaFormat = MediaFormat.GIF_ANIMATED,
            isAnimationEnabled = true
        )
        val result = GifRenderer.decodeGifFrames(tempGif, 128, 128, config, maxFrames = 6)
        assertTrue(result.frames.size >= 4)
        assertTrue(AlphaUtils.detectTransparency(result.frames.first()))
    }

    @Test
    fun `verify static and animated SVG vector rendering and resize options`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempSvg = File(context.cacheDir, "test_anim.svg")
        ImageUtils.writeStarterAnimatedSvg(tempSvg)

        assertEquals(MediaFormat.SVG_ANIMATED, ImageUtils.inspectMediaFormat(tempSvg))
        val report = SvgRenderer.analyzeSvg(tempSvg)
        assertTrue(report.isAnimated)
        assertTrue(report.supportedElementsCount >= 3)

        val frame0 = SvgRenderer.renderSvgToBitmap(tempSvg, 320, 320, normalizedPhase = 0f)
        val frameMid = SvgRenderer.renderSvgToBitmap(tempSvg, 320, 320, normalizedPhase = 0.5f)
        assertEquals(320, frame0.width)
        assertEquals(320, frameMid.height)
        assertTrue(AlphaUtils.detectTransparency(frame0))

        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 340)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 260)
        }
        val (resolvedW, resolvedH) = DimensionUtils.resolveWidgetDimensionsDp(options, 200, 200)
        assertEquals(340, resolvedW)
        assertEquals(260, resolvedH)
    }
}
