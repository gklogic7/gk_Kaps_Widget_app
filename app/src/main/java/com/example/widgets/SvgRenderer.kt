package com.example.widgets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.Xml
import com.example.data.SvgCompatibilityReport
import com.example.utils.AlphaUtils
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.StringReader
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object SvgRenderer {

    private data class GradientStop(val offset: Float, val color: Int)

    private sealed class SvgGradient(val id: String) {
        class Linear(
            id: String,
            val x1: Float,
            val y1: Float,
            val x2: Float,
            val y2: Float,
            val stops: List<GradientStop>
        ) : SvgGradient(id)

        class Radial(
            id: String,
            val cx: Float,
            val cy: Float,
            val r: Float,
            val stops: List<GradientStop>
        ) : SvgGradient(id)
    }

    private data class SvgAnimSpec(
        val tag: String, // "animate" or "animateTransform"
        val attributeName: String,
        val transformType: String, // "rotate", "scale", "translate"
        val from: String,
        val to: String,
        val values: List<String>,
        val durSeconds: Float
    )

    private data class SvgNode(
        val tag: String,
        val attrs: Map<String, String>,
        val children: MutableList<SvgNode> = mutableListOf(),
        val animations: MutableList<SvgAnimSpec> = mutableListOf()
    )

    private data class SvgDocument(
        val viewBoxX: Float,
        val viewBoxY: Float,
        val viewBoxWidth: Float,
        val viewBoxHeight: Float,
        val gradients: Map<String, SvgGradient>,
        val clipPaths: Map<String, Path>,
        val cssRules: Map<String, Map<String, String>>,
        val root: SvgNode,
        val report: SvgCompatibilityReport
    )

    fun analyzeSvg(file: File): SvgCompatibilityReport {
        val doc = parseSvgDocument(file) ?: return SvgCompatibilityReport(
            isAnimated = false,
            viewBoxWidth = 240f,
            viewBoxHeight = 240f,
            supportedElementsCount = 0,
            animationFeatures = emptyList(),
            unsupportedFeatures = listOf("Unable to parse XML structure")
        )
        return doc.report
    }

    /**
     * Rasterizes an SVG directly at the target widget pixel resolution (widthPx x heightPx)
     * with full alpha transparency and vector sharpness.
     * @param normalizedPhase Animation phase in [0.0f, 1.0f) for animated SVGs.
     */
    fun renderSvgToBitmap(
        file: File,
        widthPx: Int,
        heightPx: Int,
        normalizedPhase: Float = 0f
    ): Bitmap {
        val safeW = widthPx.coerceIn(32, 1024)
        val safeH = heightPx.coerceIn(32, 1024)
        val bitmap = AlphaUtils.createTransparentBitmap(safeW, safeH)
        val doc = parseSvgDocument(file) ?: return bitmap

        val canvas = Canvas(bitmap)
        val vbW = doc.viewBoxWidth.coerceAtLeast(1f)
        val vbH = doc.viewBoxHeight.coerceAtLeast(1f)

        // Preserve aspect ratio (xMidYMid meet)
        val scale = min(safeW.toFloat() / vbW, safeH.toFloat() / vbH)
        val dx = (safeW - vbW * scale) * 0.5f - doc.viewBoxX * scale
        val dy = (safeH - vbH * scale) * 0.5f - doc.viewBoxY * scale

        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)

        renderNode(
            canvas = canvas,
            node = doc.root,
            doc = doc,
            parentAttrs = emptyMap(),
            phase = normalizedPhase.coerceIn(0f, 1f)
        )

        canvas.restore()
        return bitmap
    }

    private fun parseSvgDocument(file: File): SvgDocument? {
        if (!file.exists()) return null
        val xmlContent = runCatching { file.readText() }.getOrNull() ?: return null
        if (!xmlContent.contains("<svg", ignoreCase = true)) return null

        return runCatching {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xmlContent))

            var vbX = 0f
            var vbY = 0f
            var vbW = 240f
            var vbH = 240f
            var rootNode: SvgNode? = null
            val nodeStack = ArrayDeque<SvgNode>()

            val gradients = mutableMapOf<String, SvgGradient>()
            var currentGradId: String? = null
            var currentGradIsRadial = false
            var currentGradAttrs: Map<String, String> = emptyMap()
            val currentGradStops = mutableListOf<GradientStop>()

            val clipPaths = mutableMapOf<String, Path>()
            var currentClipId: String? = null
            val currentClipPath = Path()

            val cssRules = mutableMapOf<String, Map<String, String>>()
            val supportedElements = mutableListOf<String>()
            val animFeatures = mutableSetOf<String>()
            val unsupportedFeatures = mutableSetOf<String>()

            val supportedTagSet = setOf(
                "svg", "g", "defs", "style", "lineargradient", "radialgradient", "stop",
                "clippath", "path", "rect", "circle", "ellipse", "line", "polyline", "polygon",
                "animate", "animatetransform", "title", "desc", "metadata"
            )

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val rawTag = parser.name ?: ""
                        val tag = rawTag.substringAfterLast(':').lowercase()
                        val attrs = mutableMapOf<String, String>()
                        for (i in 0 until parser.attributeCount) {
                            val attrName = parser.getAttributeName(i).substringAfterLast(':')
                            attrs[attrName] = parser.getAttributeValue(i)
                        }

                        // Expand inline style="fill:#fff;stroke:#000" into attrs
                        attrs["style"]?.let { styleStr ->
                            parseInlineStyle(styleStr).forEach { (k, v) ->
                                if (!attrs.containsKey(k)) attrs[k] = v
                            }
                            if (styleStr.contains("animation", ignoreCase = true)) {
                                animFeatures.add("CSS animation")
                            }
                        }

                        if (tag !in supportedTagSet) {
                            unsupportedFeatures.add("<$tag>")
                        } else if (tag in setOf("path", "rect", "circle", "ellipse", "line", "polyline", "polygon")) {
                            supportedElements.add(tag)
                        }

                        when (tag) {
                            "svg" -> {
                                val wAttr = parseNumber(attrs["width"], 240f)
                                val hAttr = parseNumber(attrs["height"], 240f)
                                vbW = wAttr
                                vbH = hAttr
                                attrs["viewBox"]?.let { vb ->
                                    val parts = vb.trim().split(Regex("[\\s,]+")).mapNotNull { it.toFloatOrNull() }
                                    if (parts.size == 4) {
                                        vbX = parts[0]
                                        vbY = parts[1]
                                        vbW = parts[2].coerceAtLeast(1f)
                                        vbH = parts[3].coerceAtLeast(1f)
                                    }
                                }
                                val node = SvgNode("svg", attrs)
                                rootNode = node
                                nodeStack.addLast(node)
                            }
                            "lineargradient", "radialgradient" -> {
                                currentGradId = attrs["id"]
                                currentGradIsRadial = (tag == "radialgradient")
                                currentGradAttrs = attrs
                                currentGradStops.clear()
                            }
                            "stop" -> {
                                if (currentGradId != null) {
                                    val offStr = attrs["offset"] ?: "0"
                                    val offset = if (offStr.endsWith("%")) {
                                        (offStr.removeSuffix("%").toFloatOrNull() ?: 0f) / 100f
                                    } else {
                                        offStr.toFloatOrNull() ?: 0f
                                    }
                                    val stopColorStr = attrs["stop-color"] ?: "#000000"
                                    val stopOpacity = attrs["stop-opacity"]?.toFloatOrNull() ?: 1f
                                    val baseColor = parseSvgColor(stopColorStr) ?: Color.BLACK
                                    val finalAlpha = (Color.alpha(baseColor) * stopOpacity.coerceIn(0f, 1f)).toInt()
                                    val finalColor = Color.argb(
                                        finalAlpha,
                                        Color.red(baseColor),
                                        Color.green(baseColor),
                                        Color.blue(baseColor)
                                    )
                                    currentGradStops.add(GradientStop(offset.coerceIn(0f, 1f), finalColor))
                                }
                            }
                            "clippath" -> {
                                currentClipId = attrs["id"]
                                currentClipPath.reset()
                            }
                            "animate", "animatetransform" -> {
                                val attrName = attrs["attributeName"] ?: ""
                                val trType = attrs["type"]?.lowercase() ?: "rotate"
                                val fromVal = attrs["from"] ?: ""
                                val toVal = attrs["to"] ?: ""
                                val valuesList = attrs["values"]?.split(";")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                                val durStr = attrs["dur"]?.lowercase()?.removeSuffix("s")?.removeSuffix("m") ?: "2"
                                val durSec = durStr.toFloatOrNull()?.coerceAtLeast(0.2f) ?: 2f
                                val spec = SvgAnimSpec(
                                    tag = tag,
                                    attributeName = attrName,
                                    transformType = trType,
                                    from = fromVal,
                                    to = toVal,
                                    values = valuesList,
                                    durSeconds = durSec
                                )
                                nodeStack.lastOrNull()?.animations?.add(spec)
                                if (tag == "animatetransform") {
                                    animFeatures.add("<animateTransform $trType>")
                                } else {
                                    animFeatures.add("<animate $attrName>")
                                }
                            }
                            "style" -> {
                                val cssText = parser.nextText()
                                parseCssBlock(cssText, cssRules)
                                if (cssText.contains("@keyframes", ignoreCase = true)) {
                                    animFeatures.add("@keyframes CSS")
                                }
                            }
                            else -> {
                                if (currentClipId != null) {
                                    buildShapePath(tag, attrs)?.let { currentClipPath.addPath(it) }
                                } else if (nodeStack.isNotEmpty()) {
                                    val child = SvgNode(tag, attrs)
                                    nodeStack.last().children.add(child)
                                    nodeStack.addLast(child)
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val tag = (parser.name ?: "").substringAfterLast(':').lowercase()
                        when (tag) {
                            "lineargradient", "radialgradient" -> {
                                val id = currentGradId
                                if (!id.isNullOrBlank() && currentGradStops.isNotEmpty()) {
                                    if (currentGradIsRadial) {
                                        val cx = parsePercentOrCoord(currentGradAttrs["cx"], vbW, vbW * 0.5f)
                                        val cy = parsePercentOrCoord(currentGradAttrs["cy"], vbH, vbH * 0.5f)
                                        val r = parsePercentOrCoord(currentGradAttrs["r"], max(vbW, vbH), max(vbW, vbH) * 0.5f)
                                        gradients[id] = SvgGradient.Radial(id, cx, cy, r, currentGradStops.toList())
                                    } else {
                                        val x1 = parsePercentOrCoord(currentGradAttrs["x1"], vbW, 0f)
                                        val y1 = parsePercentOrCoord(currentGradAttrs["y1"], vbH, 0f)
                                        val x2 = parsePercentOrCoord(currentGradAttrs["x2"], vbW, vbW)
                                        val y2 = parsePercentOrCoord(currentGradAttrs["y2"], vbH, vbH)
                                        gradients[id] = SvgGradient.Linear(id, x1, y1, x2, y2, currentGradStops.toList())
                                    }
                                }
                                currentGradId = null
                            }
                            "clippath" -> {
                                val id = currentClipId
                                if (!id.isNullOrBlank()) {
                                    clipPaths[id] = Path(currentClipPath)
                                }
                                currentClipId = null
                            }
                            "animate", "animatetransform", "stop", "style" -> {
                                // Handled on start
                            }
                            else -> {
                                if (currentClipId == null && nodeStack.isNotEmpty() && nodeStack.last().tag == tag) {
                                    nodeStack.removeLast()
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            val root = rootNode ?: SvgNode("svg", emptyMap())
            val report = SvgCompatibilityReport(
                isAnimated = animFeatures.isNotEmpty(),
                viewBoxWidth = vbW,
                viewBoxHeight = vbH,
                supportedElementsCount = supportedElements.size,
                animationFeatures = animFeatures.toList(),
                unsupportedFeatures = unsupportedFeatures.toList()
            )
            SvgDocument(
                viewBoxX = vbX,
                viewBoxY = vbY,
                viewBoxWidth = vbW,
                viewBoxHeight = vbH,
                gradients = gradients,
                clipPaths = clipPaths,
                cssRules = cssRules,
                root = root,
                report = report
            )
        }.getOrNull()
    }

    private fun renderNode(
        canvas: Canvas,
        node: SvgNode,
        doc: SvgDocument,
        parentAttrs: Map<String, String>,
        phase: Float
    ) {
        if (node.tag in setOf("defs", "title", "desc", "metadata")) return

        // Merge inherited presentation attributes, CSS class rules, and node attributes
        val effectiveAttrs = mutableMapOf<String, String>()
        for ((k, v) in parentAttrs) {
            if (k in setOf("fill", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin", "opacity", "fill-opacity", "stroke-opacity")) {
                effectiveAttrs[k] = v
            }
        }
        node.attrs["class"]?.split(Regex("\\s+"))?.forEach { cls ->
            doc.cssRules[".$cls"]?.forEach { (k, v) -> effectiveAttrs[k] = v }
        }
        node.attrs["id"]?.let { id ->
            doc.cssRules["#$id"]?.forEach { (k, v) -> effectiveAttrs[k] = v }
        }
        for ((k, v) in node.attrs) {
            if (k != "transform" && k != "clip-path") {
                effectiveAttrs[k] = v
            }
        }

        // Apply <animate> attribute interpolations at current phase
        for (anim in node.animations) {
            if (anim.tag == "animate" && anim.attributeName.isNotEmpty()) {
                val interpolated = interpolateScalarAnimation(anim, phase)
                if (interpolated != null) {
                    effectiveAttrs[anim.attributeName] = interpolated.toString()
                }
            }
        }

        canvas.save()

        // Apply static transform attribute if present
        node.attrs["transform"]?.let { trStr ->
            val matrix = parseTransformMatrix(trStr)
            canvas.concat(matrix)
        }

        // Apply <animateTransform> interpolations at current phase
        for (anim in node.animations) {
            if (anim.tag == "animatetransform") {
                applyAnimatedTransform(canvas, anim, phase)
            }
        }

        // Apply clip-path if present
        node.attrs["clip-path"]?.let { clipRef ->
            val clipId = extractUrlId(clipRef)
            doc.clipPaths[clipId]?.let { canvas.clipPath(it) }
        }

        // Draw geometry if shape tag
        val shapePath = buildShapePath(node.tag, effectiveAttrs)
        if (shapePath != null) {
            drawStyledPath(canvas, shapePath, effectiveAttrs, doc)
        }

        // Recurse into children
        for (child in node.children) {
            renderNode(canvas, child, doc, effectiveAttrs, phase)
        }

        canvas.restore()
    }

    private fun drawStyledPath(
        canvas: Canvas,
        path: Path,
        attrs: Map<String, String>,
        doc: SvgDocument
    ) {
        val globalOpacity = attrs["opacity"]?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f
        val fillStr = attrs["fill"]
        val strokeStr = attrs["stroke"]

        // 1. Fill pass (default in SVG is black unless fill="none")
        if (!fillStr.equals("none", ignoreCase = true)) {
            val fillOpacity = (attrs["fill-opacity"]?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f) * globalOpacity
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }
            if (fillStr != null && fillStr.startsWith("url(", ignoreCase = true)) {
                val gradId = extractUrlId(fillStr)
                val grad = doc.gradients[gradId]
                fillPaint.shader = buildShader(grad)
                fillPaint.alpha = (fillOpacity * 255f).toInt().coerceIn(0, 255)
                canvas.drawPath(path, fillPaint)
            } else {
                val color = parseSvgColor(fillStr ?: "#111216")
                if (color != null && Color.alpha(color) > 0) {
                    val combinedAlpha = ((Color.alpha(color) / 255f) * fillOpacity * 255f).toInt().coerceIn(0, 255)
                    fillPaint.color = Color.argb(combinedAlpha, Color.red(color), Color.green(color), Color.blue(color))
                    canvas.drawPath(path, fillPaint)
                }
            }
        }

        // 2. Stroke pass
        if (!strokeStr.isNullOrBlank() && !strokeStr.equals("none", ignoreCase = true)) {
            val strokeWidth = parseNumber(attrs["stroke-width"], 1f)
            if (strokeWidth > 0f) {
                val strokeOpacity = (attrs["stroke-opacity"]?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f) * globalOpacity
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    this.strokeWidth = strokeWidth
                    strokeCap = when (attrs["stroke-linecap"]?.lowercase()) {
                        "round" -> Paint.Cap.ROUND
                        "square" -> Paint.Cap.SQUARE
                        else -> Paint.Cap.BUTT
                    }
                    strokeJoin = when (attrs["stroke-linejoin"]?.lowercase()) {
                        "round" -> Paint.Join.ROUND
                        "bevel" -> Paint.Join.BEVEL
                        else -> Paint.Join.MITER
                    }
                }
                if (strokeStr.startsWith("url(", ignoreCase = true)) {
                    val gradId = extractUrlId(strokeStr)
                    strokePaint.shader = buildShader(doc.gradients[gradId])
                    strokePaint.alpha = (strokeOpacity * 255f).toInt().coerceIn(0, 255)
                    canvas.drawPath(path, strokePaint)
                } else {
                    val color = parseSvgColor(strokeStr)
                    if (color != null && Color.alpha(color) > 0) {
                        val combinedAlpha = ((Color.alpha(color) / 255f) * strokeOpacity * 255f).toInt().coerceIn(0, 255)
                        strokePaint.color = Color.argb(combinedAlpha, Color.red(color), Color.green(color), Color.blue(color))
                        canvas.drawPath(path, strokePaint)
                    }
                }
            }
        }
    }

    private fun buildShader(grad: SvgGradient?): Shader? {
        if (grad == null) return null
        return when (grad) {
            is SvgGradient.Linear -> {
                val stops = if (grad.stops.size >= 2) grad.stops else listOf(
                    GradientStop(0f, grad.stops.firstOrNull()?.color ?: Color.BLACK),
                    GradientStop(1f, grad.stops.lastOrNull()?.color ?: Color.WHITE)
                )
                LinearGradient(
                    grad.x1,
                    grad.y1,
                    grad.x2,
                    grad.y2,
                    stops.map { it.color }.toIntArray(),
                    stops.map { it.offset }.toFloatArray(),
                    Shader.TileMode.CLAMP
                )
            }
            is SvgGradient.Radial -> {
                val stops = if (grad.stops.size >= 2) grad.stops else listOf(
                    GradientStop(0f, grad.stops.firstOrNull()?.color ?: Color.WHITE),
                    GradientStop(1f, grad.stops.lastOrNull()?.color ?: Color.TRANSPARENT)
                )
                RadialGradient(
                    grad.cx,
                    grad.cy,
                    grad.r.coerceAtLeast(1f),
                    stops.map { it.color }.toIntArray(),
                    stops.map { it.offset }.toFloatArray(),
                    Shader.TileMode.CLAMP
                )
            }
        }
    }

    private fun interpolateScalarAnimation(anim: SvgAnimSpec, phase: Float): Float? {
        val keyframes: List<Float> = if (anim.values.isNotEmpty()) {
            anim.values.mapNotNull { parseNumber(it, Float.NaN).takeIf { v -> !v.isNaN() } }
        } else {
            listOfNotNull(
                parseNumber(anim.from, Float.NaN).takeIf { !it.isNaN() },
                parseNumber(anim.to, Float.NaN).takeIf { !it.isNaN() }
            )
        }
        if (keyframes.isEmpty()) return null
        if (keyframes.size == 1) return keyframes[0]
        val scaled = phase.coerceIn(0f, 1f) * (keyframes.size - 1)
        val idx = scaled.toInt().coerceIn(0, keyframes.size - 2)
        val frac = scaled - idx
        return keyframes[idx] + (keyframes[idx + 1] - keyframes[idx]) * frac
    }

    private fun applyAnimatedTransform(canvas: Canvas, anim: SvgAnimSpec, phase: Float) {
        val rawFrames = if (anim.values.isNotEmpty()) anim.values else listOf(anim.from, anim.to).filter { it.isNotBlank() }
        if (rawFrames.isEmpty()) return

        val parsedFrames = rawFrames.map { frameStr ->
            frameStr.trim().split(Regex("[\\s,]+")).mapNotNull { it.toFloatOrNull() }
        }.filter { it.isNotEmpty() }

        if (parsedFrames.isEmpty()) return
        val a = parsedFrames.first()
        val b = parsedFrames.last()
        val count = max(a.size, b.size)
        val interp = FloatArray(count) { i ->
            val va = a.getOrElse(i) { 0f }
            val vb = b.getOrElse(i) { va }
            va + (vb - va) * phase.coerceIn(0f, 1f)
        }

        when (anim.transformType) {
            "rotate" -> {
                val deg = interp.getOrElse(0) { 0f }
                val cx = interp.getOrElse(1) { 0f }
                val cy = interp.getOrElse(2) { 0f }
                canvas.rotate(deg, cx, cy)
            }
            "scale" -> {
                val sx = interp.getOrElse(0) { 1f }
                val sy = interp.getOrElse(1) { sx }
                canvas.scale(sx, sy)
            }
            "translate" -> {
                val tx = interp.getOrElse(0) { 0f }
                val ty = interp.getOrElse(1) { 0f }
                canvas.translate(tx, ty)
            }
        }
    }

    private fun buildShapePath(tag: String, attrs: Map<String, String>): Path? {
        return when (tag) {
            "path" -> {
                val d = attrs["d"] ?: return null
                parseSvgPathData(d)
            }
            "rect" -> {
                val x = parseNumber(attrs["x"], 0f)
                val y = parseNumber(attrs["y"], 0f)
                val w = parseNumber(attrs["width"], 0f)
                val h = parseNumber(attrs["height"], 0f)
                if (w <= 0f || h <= 0f) return null
                val rx = parseNumber(attrs["rx"], 0f)
                val ry = parseNumber(attrs["ry"], rx)
                Path().apply {
                    if (rx > 0f || ry > 0f) {
                        addRoundRect(RectF(x, y, x + w, y + h), rx, ry, Path.Direction.CW)
                    } else {
                        addRect(RectF(x, y, x + w, y + h), Path.Direction.CW)
                    }
                }
            }
            "circle" -> {
                val cx = parseNumber(attrs["cx"], 0f)
                val cy = parseNumber(attrs["cy"], 0f)
                val r = parseNumber(attrs["r"], 0f)
                if (r <= 0f) return null
                Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
            }
            "ellipse" -> {
                val cx = parseNumber(attrs["cx"], 0f)
                val cy = parseNumber(attrs["cy"], 0f)
                val rx = parseNumber(attrs["rx"], 0f)
                val ry = parseNumber(attrs["ry"], 0f)
                if (rx <= 0f || ry <= 0f) return null
                Path().apply { addOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), Path.Direction.CW) }
            }
            "line" -> {
                val x1 = parseNumber(attrs["x1"], 0f)
                val y1 = parseNumber(attrs["y1"], 0f)
                val x2 = parseNumber(attrs["x2"], 0f)
                val y2 = parseNumber(attrs["y2"], 0f)
                Path().apply {
                    moveTo(x1, y1)
                    lineTo(x2, y2)
                }
            }
            "polyline", "polygon" -> {
                val ptsStr = attrs["points"]?.trim() ?: return null
                val nums = Regex("[-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?")
                    .findAll(ptsStr)
                    .mapNotNull { it.value.toFloatOrNull() }
                    .toList()
                if (nums.size < 4) return null
                Path().apply {
                    moveTo(nums[0], nums[1])
                    var i = 2
                    while (i + 1 < nums.size) {
                        lineTo(nums[i], nums[i + 1])
                        i += 2
                    }
                    if (tag == "polygon") close()
                }
            }
            else -> null
        }
    }

    /**
     * Full SVG Path `d` attribute parser supporting M/m, L/l, H/h, V/v, C/c, S/s, Q/q, T/t, A/a, Z/z.
     */
    fun parseSvgPathData(d: String): Path {
        val path = Path()
        val tokenRegex = Regex("([MmLlHhVvCcSsQqTtAaZz])|([-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?)")
        val tokens = tokenRegex.findAll(d).map { it.value }.toList()

        var idx = 0
        var cmd = 'M'
        var curX = 0f
        var curY = 0f
        var startX = 0f
        var startY = 0f
        var lastCtrlX = 0f
        var lastCtrlY = 0f
        var prevCmd = ' '

        fun hasNextFloat(): Boolean = idx < tokens.size && tokens[idx][0].let { it.isDigit() || it == '-' || it == '+' || it == '.' }
        fun nextFloat(): Float = if (idx < tokens.size) tokens[idx++].toFloatOrNull() ?: 0f else 0f

        while (idx < tokens.size) {
            val t = tokens[idx]
            if (t.length == 1 && t[0].isLetter()) {
                cmd = t[0]
                idx++
            }

            when (cmd) {
                'M', 'm' -> {
                    val x = nextFloat()
                    val y = nextFloat()
                    curX = if (cmd == 'm') curX + x else x
                    curY = if (cmd == 'm') curY + y else y
                    startX = curX
                    startY = curY
                    path.moveTo(curX, curY)
                    lastCtrlX = curX
                    lastCtrlY = curY
                    cmd = if (cmd == 'm') 'l' else 'L'
                }
                'L', 'l' -> {
                    val x = nextFloat()
                    val y = nextFloat()
                    curX = if (cmd == 'l') curX + x else x
                    curY = if (cmd == 'l') curY + y else y
                    path.lineTo(curX, curY)
                    lastCtrlX = curX
                    lastCtrlY = curY
                }
                'H', 'h' -> {
                    val x = nextFloat()
                    curX = if (cmd == 'h') curX + x else x
                    path.lineTo(curX, curY)
                    lastCtrlX = curX
                    lastCtrlY = curY
                }
                'V', 'v' -> {
                    val y = nextFloat()
                    curY = if (cmd == 'v') curY + y else y
                    path.lineTo(curX, curY)
                    lastCtrlX = curX
                    lastCtrlY = curY
                }
                'C', 'c' -> {
                    val x1 = nextFloat()
                    val y1 = nextFloat()
                    val x2 = nextFloat()
                    val y2 = nextFloat()
                    val x = nextFloat()
                    val y = nextFloat()
                    val cx1 = if (cmd == 'c') curX + x1 else x1
                    val cy1 = if (cmd == 'c') curY + y1 else y1
                    val cx2 = if (cmd == 'c') curX + x2 else x2
                    val cy2 = if (cmd == 'c') curY + y2 else y2
                    val ex = if (cmd == 'c') curX + x else x
                    val ey = if (cmd == 'c') curY + y else y
                    path.cubicTo(cx1, cy1, cx2, cy2, ex, ey)
                    lastCtrlX = cx2
                    lastCtrlY = cy2
                    curX = ex
                    curY = ey
                }
                'S', 's' -> {
                    val x2 = nextFloat()
                    val y2 = nextFloat()
                    val x = nextFloat()
                    val y = nextFloat()
                    val cx1 = if (prevCmd in listOf('C', 'c', 'S', 's')) 2 * curX - lastCtrlX else curX
                    val cy1 = if (prevCmd in listOf('C', 'c', 'S', 's')) 2 * curY - lastCtrlY else curY
                    val cx2 = if (cmd == 's') curX + x2 else x2
                    val cy2 = if (cmd == 's') curY + y2 else y2
                    val ex = if (cmd == 's') curX + x else x
                    val ey = if (cmd == 's') curY + y else y
                    path.cubicTo(cx1, cy1, cx2, cy2, ex, ey)
                    lastCtrlX = cx2
                    lastCtrlY = cy2
                    curX = ex
                    curY = ey
                }
                'Q', 'q' -> {
                    val x1 = nextFloat()
                    val y1 = nextFloat()
                    val x = nextFloat()
                    val y = nextFloat()
                    val cx1 = if (cmd == 'q') curX + x1 else x1
                    val cy1 = if (cmd == 'q') curY + y1 else y1
                    val ex = if (cmd == 'q') curX + x else x
                    val ey = if (cmd == 'q') curY + y else y
                    path.quadTo(cx1, cy1, ex, ey)
                    lastCtrlX = cx1
                    lastCtrlY = cy1
                    curX = ex
                    curY = ey
                }
                'T', 't' -> {
                    val x = nextFloat()
                    val y = nextFloat()
                    val cx1 = if (prevCmd in listOf('Q', 'q', 'T', 't')) 2 * curX - lastCtrlX else curX
                    val cy1 = if (prevCmd in listOf('Q', 'q', 'T', 't')) 2 * curY - lastCtrlY else curY
                    val ex = if (cmd == 't') curX + x else x
                    val ey = if (cmd == 't') curY + y else y
                    path.quadTo(cx1, cy1, ex, ey)
                    lastCtrlX = cx1
                    lastCtrlY = cy1
                    curX = ex
                    curY = ey
                }
                'A', 'a' -> {
                    val rx = nextFloat()
                    val ry = nextFloat()
                    val xAxisRot = nextFloat()
                    val largeArcFlag = nextFloat() != 0f
                    val sweepFlag = nextFloat() != 0f
                    val x = nextFloat()
                    val y = nextFloat()
                    val ex = if (cmd == 'a') curX + x else x
                    val ey = if (cmd == 'a') curY + y else y
                    drawArcToPath(path, curX, curY, ex, ey, rx, ry, xAxisRot, largeArcFlag, sweepFlag)
                    curX = ex
                    curY = ey
                    lastCtrlX = curX
                    lastCtrlY = curY
                }
                'Z', 'z' -> {
                    path.close()
                    curX = startX
                    curY = startY
                    lastCtrlX = curX
                    lastCtrlY = curY
                }
                else -> {
                    if (!hasNextFloat()) idx++
                }
            }
            prevCmd = cmd
        }
        return path
    }

    private fun drawArcToPath(
        path: Path,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        rxIn: Float,
        ryIn: Float,
        angleDeg: Float,
        largeArc: Boolean,
        sweep: Boolean
    ) {
        var rx = abs(rxIn)
        var ry = abs(ryIn)
        if (rx == 0f || ry == 0f || (x0 == x1 && y0 == y1)) {
            path.lineTo(x1, y1)
            return
        }
        val phi = Math.toRadians(angleDeg.toDouble())
        val cosPhi = cos(phi)
        val sinPhi = sin(phi)
        val dx2 = (x0 - x1) / 2.0
        val dy2 = (y0 - y1) / 2.0
        val x1p = cosPhi * dx2 + sinPhi * dy2
        val y1p = -sinPhi * dx2 + cosPhi * dy2

        var rxSq = (rx * rx).toDouble()
        var rySq = (ry * ry).toDouble()
        val x1pSq = x1p * x1p
        val y1pSq = y1p * y1p

        val radicandCheck = x1pSq / rxSq + y1pSq / rySq
        if (radicandCheck > 1.0) {
            val s = sqrt(radicandCheck)
            rx = (rx * s).toFloat()
            ry = (ry * s).toFloat()
            rxSq = (rx * rx).toDouble()
            rySq = (ry * ry).toDouble()
        }

        val sign = if (largeArc == sweep) -1.0 else 1.0
        val sq = ((rxSq * rySq - rxSq * y1pSq - rySq * x1pSq) / (rxSq * y1pSq + rySq * x1pSq)).coerceAtLeast(0.0)
        val coef = sign * sqrt(sq)
        val cxp = coef * ((rx * y1p) / ry)
        val cyp = coef * -((ry * x1p) / rx)

        val cx = cosPhi * cxp - sinPhi * cyp + (x0 + x1) / 2.0
        val cy = sinPhi * cxp + cosPhi * cyp + (y0 + y1) / 2.0

        fun angle(ux: Double, uy: Double, vx: Double, vy: Double): Double {
            val dot = ux * vx + uy * vy
            val len = hypot(ux, uy) * hypot(vx, vy)
            val ang = kotlin.math.acos((dot / len).coerceIn(-1.0, 1.0))
            return if (ux * vy - uy * vx < 0) -ang else ang
        }

        val theta1 = angle(1.0, 0.0, (x1p - cxp) / rx, (y1p - cyp) / ry)
        var dTheta = angle(
            (x1p - cxp) / rx,
            (y1p - cyp) / ry,
            (-x1p - cxp) / rx,
            (-y1p - cyp) / ry
        )
        if (!sweep && dTheta > 0) dTheta -= 2.0 * PI
        else if (sweep && dTheta < 0) dTheta += 2.0 * PI

        val segments = ceil(abs(dTheta) / (PI / 2.0)).toInt().coerceAtLeast(1)
        val delta = dTheta / segments
        val t = (8.0 / 3.0) * sin(delta / 4.0) * sin(delta / 4.0) / sin(delta / 2.0)

        var curTheta = theta1
        for (i in 0 until segments) {
            val cos1 = cos(curTheta)
            val sin1 = sin(curTheta)
            val nextTheta = curTheta + delta
            val cos2 = cos(nextTheta)
            val sin2 = sin(nextTheta)

            val epX = cosPhi * rx * cos2 - sinPhi * ry * sin2 + cx
            val epY = sinPhi * rx * cos2 + cosPhi * ry * sin2 + cy

            val dx1 = -rx * sin1
            val dy1 = ry * cos1
            val cp1X = (cosPhi * rx * cos1 - sinPhi * ry * sin1 + cx) + t * (cosPhi * dx1 - sinPhi * dy1)
            val cp1Y = (sinPhi * rx * cos1 + cosPhi * ry * sin1 + cy) + t * (sinPhi * dx1 + cosPhi * dy1)

            val dx2Seg = -rx * sin2
            val dy2Seg = ry * cos2
            val cp2X = epX - t * (cosPhi * dx2Seg - sinPhi * dy2Seg)
            val cp2Y = epY - t * (sinPhi * dx2Seg + cosPhi * dy2Seg)

            path.cubicTo(
                cp1X.toFloat(),
                cp1Y.toFloat(),
                cp2X.toFloat(),
                cp2Y.toFloat(),
                epX.toFloat(),
                epY.toFloat()
            )
            curTheta = nextTheta
        }
    }

    private fun parseTransformMatrix(transformStr: String): Matrix {
        val result = Matrix()
        val regex = Regex("(translate|scale|rotate|skewX|skewY|matrix)\\s*\\(([^)]*)\\)")
        for (match in regex.findAll(transformStr)) {
            val fn = match.groupValues[1].lowercase()
            val args = match.groupValues[2].trim().split(Regex("[\\s,]+")).mapNotNull { it.toFloatOrNull() }
            val m = Matrix()
            when (fn) {
                "translate" -> {
                    val tx = args.getOrElse(0) { 0f }
                    val ty = args.getOrElse(1) { 0f }
                    m.setTranslate(tx, ty)
                }
                "scale" -> {
                    val sx = args.getOrElse(0) { 1f }
                    val sy = args.getOrElse(1) { sx }
                    m.setScale(sx, sy)
                }
                "rotate" -> {
                    val deg = args.getOrElse(0) { 0f }
                    val cx = args.getOrElse(1) { 0f }
                    val cy = args.getOrElse(2) { 0f }
                    m.setRotate(deg, cx, cy)
                }
                "skewx" -> {
                    val deg = args.getOrElse(0) { 0f }
                    m.setSkew(tan(Math.toRadians(deg.toDouble())).toFloat(), 0f)
                }
                "skewy" -> {
                    val deg = args.getOrElse(0) { 0f }
                    m.setSkew(0f, tan(Math.toRadians(deg.toDouble())).toFloat())
                }
                "matrix" -> {
                    if (args.size >= 6) {
                        m.setValues(
                            floatArrayOf(
                                args[0], args[2], args[4],
                                args[1], args[3], args[5],
                                0f, 0f, 1f
                            )
                        )
                    }
                }
            }
            result.preConcat(m)
        }
        return result
    }

    private fun parseInlineStyle(style: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        style.split(";").forEach { decl ->
            val idx = decl.indexOf(':')
            if (idx > 0) {
                val k = decl.substring(0, idx).trim().lowercase()
                val v = decl.substring(idx + 1).trim()
                if (k.isNotEmpty() && v.isNotEmpty()) map[k] = v
            }
        }
        return map
    }

    private fun parseCssBlock(css: String, outRules: MutableMap<String, Map<String, String>>) {
        val ruleRegex = Regex("([.#][a-zA-Z0-9_-]+)\\s*\\{([^}]*)\\}")
        for (match in ruleRegex.findAll(css)) {
            val selector = match.groupValues[1].trim()
            val body = match.groupValues[2]
            outRules[selector] = parseInlineStyle(body)
        }
    }

    private fun extractUrlId(urlRef: String): String {
        return urlRef.trim()
            .removePrefix("url(")
            .removeSuffix(")")
            .trim('"', '\'', ' ')
            .removePrefix("#")
    }

    private fun parsePercentOrCoord(raw: String?, total: Float, defaultVal: Float): Float {
        if (raw.isNullOrBlank()) return defaultVal
        val clean = raw.trim()
        return if (clean.endsWith("%")) {
            val pct = clean.removeSuffix("%").toFloatOrNull() ?: return defaultVal
            if (pct <= 1f && !raw.contains("%")) pct * total else (pct / 100f) * total
        } else {
            val v = parseNumber(clean, defaultVal)
            if (v in 0f..1f && total > 10f && (clean == "0" || clean == "1" || clean.startsWith("0."))) {
                v * total
            } else {
                v
            }
        }
    }

    private fun parseNumber(raw: String?, defaultVal: Float): Float {
        if (raw.isNullOrBlank()) return defaultVal
        val cleaned = raw.trim()
            .removeSuffix("px")
            .removeSuffix("dp")
            .removeSuffix("pt")
            .trim()
        return cleaned.toFloatOrNull() ?: defaultVal
    }

    private fun parseSvgColor(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val s = raw.trim().lowercase()
        if (s == "none" || s == "transparent") return Color.TRANSPARENT

        val namedColors = mapOf(
            "white" to "#ffffff",
            "black" to "#000000",
            "red" to "#e11d48",
            "blue" to "#2563eb",
            "green" to "#10b981",
            "yellow" to "#f59e0b",
            "cyan" to "#38bdf8",
            "magenta" to "#ec4899",
            "gray" to "#6b7280",
            "grey" to "#6b7280",
            "orange" to "#f97316",
            "purple" to "#8b5cf6"
        )
        val hex = namedColors[s] ?: s

        if (hex.startsWith("rgb(")) {
            val nums = hex.removePrefix("rgb(").removeSuffix(")").split(",").mapNotNull { it.trim().toIntOrNull() }
            if (nums.size >= 3) {
                return Color.rgb(
                    nums[0].coerceIn(0, 255),
                    nums[1].coerceIn(0, 255),
                    nums[2].coerceIn(0, 255)
                )
            }
        }
        if (hex.startsWith("rgba(")) {
            val parts = hex.removePrefix("rgba(").removeSuffix(")").split(",").map { it.trim() }
            if (parts.size >= 4) {
                val r = parts[0].toIntOrNull()?.coerceIn(0, 255) ?: 0
                val g = parts[1].toIntOrNull()?.coerceIn(0, 255) ?: 0
                val b = parts[2].toIntOrNull()?.coerceIn(0, 255) ?: 0
                val a = ((parts[3].toFloatOrNull() ?: 1f).coerceIn(0f, 1f) * 255f).toInt()
                return Color.argb(a, r, g, b)
            }
        }
        if (hex.startsWith("#")) {
            val expanded = if (hex.length == 4) {
                "#${hex[1]}${hex[1]}${hex[2]}${hex[2]}${hex[3]}${hex[3]}"
            } else {
                hex
            }
            return runCatching { Color.parseColor(expanded) }.getOrNull()
        }
        return null
    }
}
