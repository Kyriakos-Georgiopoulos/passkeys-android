package example.pathtrace

/*
 * Copyright 2025 Kyriakos Georgiopoulos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.xmlpull.v1.XmlPullParser
import kotlin.math.min

/**
 * Styled representation of an individual SVG path.
 *
 * @property d Raw path data in SVG pathData format.
 * @property fillColor Optional fill color.
 * @property strokeColor Optional stroke color.
 * @property strokeWidthPxAt1x Optional base stroke width in pixels for a 1x viewport.
 * @property fillAlpha Alpha multiplier applied to the fill color.
 * @property strokeAlpha Alpha multiplier applied to the stroke color.
 * @property cap Stroke cap style.
 * @property join Stroke join style.
 * @property fillTypeEvenOdd Whether the fill type is even-odd (otherwise non-zero).
 */
data class StyledPath(
    val d: String,
    val fillColor: Color? = null,
    val strokeColor: Color? = null,
    val strokeWidthPxAt1x: Float? = null,
    val fillAlpha: Float = 1f,
    val strokeAlpha: Float = 1f,
    val cap: StrokeCap = StrokeCap.Round,
    val join: StrokeJoin = StrokeJoin.Round,
    val fillTypeEvenOdd: Boolean = false
)

/**
 * Specification of an SVG path set, including viewport and paths.
 *
 * @property viewportWidth SVG viewport width.
 * @property viewportHeight SVG viewport height.
 * @property paths List of styled paths contained in the SVG.
 */
data class SvgPathSpec(
    val viewportWidth: Float,
    val viewportHeight: Float,
    val paths: List<StyledPath>
)

/**
 * Convenience constructor for a single-path [SvgPathSpec].
 *
 * @param viewportWidth SVG viewport width.
 * @param viewportHeight SVG viewport height.
 * @param d Raw path data in SVG pathData format.
 */
fun SvgPathSpec(
    viewportWidth: Float,
    viewportHeight: Float,
    d: String
): SvgPathSpec = SvgPathSpec(
    viewportWidth = viewportWidth,
    viewportHeight = viewportHeight,
    paths = listOf(StyledPath(d = d))
)

/**
 * Draws a styled SVG path specification with a progressive trace animation.
 *
 * The [progress] in [0f, 1f] controls how much of the combined path length is
 * revealed. Both fills and strokes are supported, with optional auto-thinning
 * of stroke widths as the SVG is scaled.
 *
 * @param spec SVG path specification.
 * @param progress Normalized progress in [0f, 1f] for the trace animation.
 * @param modifier Modifier applied to the [Canvas].
 * @param defaultStrokeColor Fallback stroke color when none is defined on the path.
 * @param defaultStrokeWidth Fallback stroke width when none is defined on the path.
 * @param autoThin Whether to automatically clamp stroke thickness relative to scale.
 */
@Composable
fun PathTraceStyled(
    spec: SvgPathSpec,
    progress: Float,
    modifier: Modifier = Modifier,
    defaultStrokeColor: Color = Color(0x171717),
    defaultStrokeWidth: Dp = 2.dp,
    autoThin: Boolean = true
) {
    data class BuiltPath(
        val styled: StyledPath,
        val androidPath: android.graphics.Path,
        val length: Float
    )

    val built = remember(spec) {
        spec.paths.map { sp ->
            val composePath = PathParser()
                .parsePathString(sp.d)
                .toPath()
                .apply {
                    fillType = if (sp.fillTypeEvenOdd) {
                        androidx.compose.ui.graphics.PathFillType.EvenOdd
                    } else {
                        androidx.compose.ui.graphics.PathFillType.NonZero
                    }
                }

            val androidPath = composePath.asAndroidPath()
            val pm = android.graphics.PathMeasure(androidPath, false)

            var totalLength = 0f
            do {
                totalLength += pm.length
            } while (pm.nextContour())

            BuiltPath(
                styled = sp,
                androidPath = androidPath,
                length = if (totalLength <= 0f) 0.0001f else totalLength
            )
        }
    }

    val totalLength = remember(built) {
        built.sumOf { it.length.toDouble() }.toFloat()
    }

    Canvas(modifier = modifier) {
        if (totalLength <= 0f) return@Canvas

        val targetLength = progress.coerceIn(0f, 1f) * totalLength

        val scaleX = size.width / spec.viewportWidth
        val scaleY = size.height / spec.viewportHeight
        val scale = min(scaleX, scaleY)
        val translateX = (size.width - spec.viewportWidth * scale) / 2f
        val translateY = (size.height - spec.viewportHeight * scale) / 2f

        val baseMatrix = android.graphics.Matrix().apply {
            setScale(scale, scale)
            postTranslate(translateX, translateY)
        }

        fun strokePaintFor(sp: StyledPath, finalStrokePx: Float): android.graphics.Paint {
            return android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = finalStrokePx
                color = (sp.strokeColor ?: defaultStrokeColor).toArgb()
                alpha = ((sp.strokeColor?.alpha ?: 1f) * 255)
                    .toInt()
                    .coerceIn(0, 255)
                strokeCap = when (sp.cap) {
                    StrokeCap.Butt -> android.graphics.Paint.Cap.BUTT
                    StrokeCap.Round -> android.graphics.Paint.Cap.ROUND
                    StrokeCap.Square -> android.graphics.Paint.Cap.SQUARE
                    else -> android.graphics.Paint.Cap.ROUND
                }
                strokeJoin = when (sp.join) {
                    StrokeJoin.Round -> android.graphics.Paint.Join.ROUND
                    StrokeJoin.Miter -> android.graphics.Paint.Join.MITER
                    StrokeJoin.Bevel -> android.graphics.Paint.Join.BEVEL
                    else -> android.graphics.Paint.Join.ROUND
                }
                strokeMiter = 1f
            }
        }

        fun fillPaintFor(sp: StyledPath): android.graphics.Paint? {
            val color = sp.fillColor ?: return null
            return android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
                this.color = color.toArgb()
                alpha = (sp.fillAlpha * 255)
                    .toInt()
                    .coerceIn(0, 255)
            }
        }

        drawIntoCanvas { canvas ->
            var accumulated = 0f

            built.forEach { bp ->
                val completed = targetLength >= accumulated + bp.length - 1e-3f
                if (completed) {
                    val filled = android.graphics.Path(bp.androidPath)
                    filled.transform(baseMatrix)
                    fillPaintFor(bp.styled)?.let { paint ->
                        canvas.nativeCanvas.drawPath(filled, paint)
                    }
                }
                accumulated += bp.length
            }

            var remaining = targetLength

            built.forEach { bp ->
                if (remaining <= 0f) return@forEach

                val drawLength = remaining.coerceAtMost(bp.length)
                if (drawLength > 0f) {
                    val revealed = android.graphics.Path()
                    val pathMeasure = android.graphics.PathMeasure(bp.androidPath, false)
                    var left = drawLength

                    do {
                        val contourLength = pathMeasure.length
                        if (left <= 0f) break
                        val segmentLength = left.coerceAtMost(contourLength)
                        if (segmentLength > 0f) {
                            pathMeasure.getSegment(
                                0f,
                                segmentLength,
                                revealed,
                                true
                            )
                        }
                        left -= segmentLength
                    } while (pathMeasure.nextContour())

                    revealed.transform(baseMatrix)

                    val desiredPx =
                        bp.styled.strokeWidthPxAt1x ?: defaultStrokeWidth.toPx()
                    val maxAutoPx = 0.45f * scale
                    val finalStroke = if (autoThin) {
                        min(desiredPx, maxAutoPx).coerceAtLeast(0.75f)
                    } else {
                        desiredPx
                    }

                    val paint = strokePaintFor(bp.styled, finalStroke)
                    canvas.nativeCanvas.drawPath(revealed, paint)
                }

                remaining -= drawLength
            }
        }
    }
}

/**
 * Parses an Android vector drawable resource into an [SvgPathSpec].
 *
 * Only `<path>` elements with `android:pathData` are considered. The
 * vector's viewport width/height and basic styling attributes are mapped
 * to [StyledPath] instances.
 *
 * @param context Context used to resolve the drawable and resources.
 * @param drawableResId Resource ID of the vector drawable.
 *
 * @return Parsed [SvgPathSpec] for the provided drawable.
 *
 * @throws IllegalArgumentException If no path elements are found.
 */
@SuppressLint("ResourceType")
fun loadPathSpecFromVectorDrawable(
    context: Context,
    @DrawableRes drawableResId: Int
): SvgPathSpec {
    val parser = context.resources.getXml(drawableResId)
    val ANDROID = "http://schemas.android.com/apk/res/android"

    fun parseColorAttr(name: String): Color? {
        val resId = parser.getAttributeResourceValue(ANDROID, name, 0)
        if (resId != 0) {
            return Color(context.getColor(resId))
        }
        val raw = parser.getAttributeValue(ANDROID, name) ?: return null
        return try {
            Color(android.graphics.Color.parseColor(raw))
        } catch (_: Throwable) {
            null
        }
    }

    fun parseFloatAttr(name: String, fallback: Float? = null): Float? {
        val raw = parser.getAttributeValue(ANDROID, name) ?: return fallback
        return raw.toFloatOrNull() ?: fallback
    }

    var viewportWidth = 24f
    var viewportHeight = 24f
    val paths = mutableListOf<StyledPath>()

    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        if (event == XmlPullParser.START_TAG) {
            when (parser.name) {
                "vector" -> {
                    viewportWidth = parser
                        .getAttributeValue(ANDROID, "viewportWidth")
                        ?.toFloatOrNull()
                        ?: viewportWidth
                    viewportHeight = parser
                        .getAttributeValue(ANDROID, "viewportHeight")
                        ?.toFloatOrNull()
                        ?: viewportHeight
                }

                "path" -> {
                    val d = parser.getAttributeValue(ANDROID, "pathData")
                    if (!d.isNullOrBlank()) {
                        val fill = parseColorAttr("fillColor")
                        val stroke = parseColorAttr("strokeColor")
                        val strokeWidth = parseFloatAttr("strokeWidth")
                        val fillAlpha = parseFloatAttr("fillAlpha", 1f) ?: 1f
                        val strokeAlpha = parseFloatAttr("strokeAlpha", 1f) ?: 1f

                        val cap = when (parser.getAttributeValue(ANDROID, "strokeLineCap")) {
                            "butt" -> StrokeCap.Butt
                            "square" -> StrokeCap.Square
                            else -> StrokeCap.Round
                        }

                        val join = when (parser.getAttributeValue(ANDROID, "strokeLineJoin")) {
                            "miter" -> StrokeJoin.Miter
                            "bevel" -> StrokeJoin.Bevel
                            else -> StrokeJoin.Round
                        }

                        val fillTypeEvenOdd =
                            parser.getAttributeValue(ANDROID, "fillType") == "evenOdd"

                        paths += StyledPath(
                            d = d,
                            fillColor = fill?.copy(alpha = fillAlpha),
                            strokeColor = stroke?.copy(alpha = strokeAlpha),
                            strokeWidthPxAt1x = strokeWidth,
                            fillAlpha = fillAlpha,
                            strokeAlpha = strokeAlpha,
                            cap = cap,
                            join = join,
                            fillTypeEvenOdd = fillTypeEvenOdd
                        )
                    }
                }
            }
        }
        event = parser.next()
    }

    require(paths.isNotEmpty()) {
        "No <path android:pathData=\"...\"> found in vector drawable #$drawableResId"
    }

    return SvgPathSpec(
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        paths = paths
    )
}

/**
 * Demonstration composable that traces a vector drawable's paths in a loop.
 *
 * The animation runs cycles from 0f to 1f progress using [PathTraceStyled].
 * When [stopSignal] is set to true and [stopAtEndOfCurrentCycle] is true,
 * the composable stops after finishing the current cycle, optionally
 * honoring [pauseMs]. The latest value of [stopSignal] is always used.
 *
 * @param drawableId Vector drawable resource to trace.
 * @param modifier Modifier applied to the drawing area.
 * @param speedMs Duration of one trace cycle in milliseconds.
 * @param pauseMs Pause duration at 100% progress between cycles.
 * @param easing Easing used for the trace animation.
 * @param stopSignal Flag indicating that the animation should stop.
 * @param stopAtEndOfCurrentCycle If true, stop after completing the current cycle.
 * @param onCycle Optional callback invoked after each completed cycle.
 */
@Composable
fun PathTraceFromSvg(
    @DrawableRes drawableId: Int,
    modifier: Modifier = Modifier.size(380.dp),
    speedMs: Int = 3800,
    pauseMs: Int = 1000,
    easing: Easing = LinearEasing,
    stopSignal: Boolean = false,
    stopAtEndOfCurrentCycle: Boolean = true,
    onCycle: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val spec = remember(drawableId) {
        loadPathSpecFromVectorDrawable(context, drawableId)
    }

    val progress = remember { Animatable(0f) }
    val stopRef = rememberUpdatedState(stopSignal)

    LaunchedEffect(drawableId, speedMs, pauseMs, easing) {
        while (true) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = speedMs, easing = easing)
            )

            onCycle?.invoke()

            if (stopRef.value && stopAtEndOfCurrentCycle) {
                if (pauseMs > 0) {
                    kotlinx.coroutines.delay(pauseMs.toLong())
                }
                progress.snapTo(1f)
                break
            }

            if (pauseMs > 0) {
                kotlinx.coroutines.delay(pauseMs.toLong())
            }

            if (stopRef.value) {
                progress.snapTo(1f)
                break
            }
        }
    }

    PathTraceStyled(
        spec = spec,
        progress = progress.value,
        modifier = modifier
    )
}
