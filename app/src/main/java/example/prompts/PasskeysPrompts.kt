package example.prompts

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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.navigation.AppScreen
import example.passkeys.R
import example.pathtrace.PathTraceFromSvg
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Available modes for the passkey prompts sequence.
 */
private enum class Mode {
    Fingerprint,
    FaceId,
    Pattern,
    Pin,
    Final
}

/**
 * Animated passkey prompts sequence that cycles through several
 * authentication UI styles (fingerprint, PIN, face, pattern) and
 * finishes with a passkey hero animation.
 *
 * The sequence loops through the prompt modes until the user taps
 * anywhere on the screen, at which point it transitions to the final
 * hero animation. Tapping on the hero animation triggers [onNavigationEvent],
 * which is intended to navigate to the next screen.
 *
 * @param modifier Modifier applied to the root container.
 * @param accent Color accents used in various animations.
 * @param onNavigationEvent Callback invoked when the user taps on the final
 * hero animation to move to the next screen.
 */
@Composable
fun PasskeysPrompts(
    modifier: Modifier = Modifier,
    accent: List<Color> = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary
    ),
    onNavigationEvent: (AppScreen) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    var showFinal by remember { mutableStateOf(false) }
    var index by remember { mutableStateOf(0) }

    val sequence = remember {
        listOf(Mode.Fingerprint, Mode.Pin, Mode.FaceId, Mode.Pattern)
    }

    val sceneDurations = mapOf(
        Mode.Fingerprint to 3000L,
        Mode.Pattern to 3000L,
        Mode.FaceId to 3000L,
        Mode.Pin to 3000L
    )

    LaunchedEffect(showFinal) {
        var i = 0
        while (!showFinal) {
            index = i
            delay(sceneDurations[sequence[i]] ?: 3000L)
            i = (i + 1) % sequence.size
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxSize()
            .systemBarsPadding()
            .clickable(
                interactionSource = interaction,
                indication = null
            ) {
                showFinal = true
            }
    ) {
        val target = if (showFinal) Mode.Final else sequence[index]

        AnimatedContent(
            modifier = Modifier.fillMaxSize(),
            targetState = target,
            transitionSpec = {
                (fadeIn(animationSpec = tween(320)) + scaleIn(
                    initialScale = 0.97f,
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = 260f)
                )).togetherWith(
                    fadeOut(animationSpec = tween(300)) + scaleOut(
                        targetScale = 1.02f,
                        animationSpec = spring(dampingRatio = 0.95f, stiffness = 300f)
                    )
                )
            },
            label = "passkey_scene"
        ) { scene ->
            when (scene) {
                Mode.Fingerprint -> FingerprintPrompt(accent)
                Mode.FaceId -> FaceIdPrompt(accent)
                Mode.Pattern -> PatternPrompt(accent)
                Mode.Pin -> PinPrompt(accent)
                Mode.Final -> PasskeysFinalHero(accent = accent, onFinished = {
                    onNavigationEvent(AppScreen.FAQ)
                })
            }
        }
    }
}

/**
 * Fingerprint-style prompt with a progress ring and vertical scan animation.
 *
 * @param accent Color accents used in the animation.
 */
@Composable
private fun FingerprintPrompt(accent: List<Color>) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = accent.firstOrNull() ?: MaterialTheme.colorScheme.primary

    val totalMs = 4000
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = totalMs,
                easing = CubicBezierEasing(0.25f, 0.8f, 0.2f, 1.1f)
            )
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(Modifier.padding(bottom = 32.dp)) {
            val sizeDp = 132.dp
            Box(
                modifier = Modifier.size(sizeDp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val center = size.center
                    val radius = size.minDimension / 2.2f

                    drawCircle(
                        color = onSurface.copy(alpha = 0.12f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 10f)
                    )

                    val sweep = 360f * progress.value
                    val start = -90f
                    drawArc(
                        color = primary.copy(alpha = 0.95f),
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(2 * radius, 2 * radius),
                        style = Stroke(width = 10f, cap = StrokeCap.Round)
                    )

                    val t = progress.value
                    val tSmooth = t * t * (3f - 2f * t)
                    val cycles = 1f
                    val phase =
                        0.5f * (1f - cos((2f * Math.PI * cycles * tSmooth).toFloat()))
                    val scanRadius = radius * 0.96f
                    val circlePath = Path().apply {
                        addOval(androidx.compose.ui.geometry.Rect(center, scanRadius))
                    }
                    val bandHeight = size.minDimension * 0.13f
                    val top = (center.y - scanRadius) +
                            (2 * scanRadius - bandHeight) * phase
                    val bandRect = androidx.compose.ui.geometry.Rect(
                        left = center.x - scanRadius,
                        top = top,
                        right = center.x + scanRadius,
                        bottom = top + bandHeight
                    )
                    clipPath(circlePath) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    primary.copy(alpha = 0.26f),
                                    primary.copy(alpha = 0.10f),
                                    Color.Transparent
                                ),
                                startY = bandRect.top,
                                endY = bandRect.bottom
                            ),
                            topLeft = bandRect.topLeft,
                            size = bandRect.size
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = null,
                    tint = onSurface,
                    modifier = Modifier.size(86.dp)
                )
            }
        }
    }
}

/**
 * Face ID-style prompt with a circular progress ring and vertical scan effect.
 *
 * @param accent Color accents used in the animation.
 */
@Composable
private fun FaceIdPrompt(accent: List<Color>) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary

    val totalMs = 4000
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = totalMs,
                easing = CubicBezierEasing(0.25f, 0.8f, 0.2f, 1.1f)
            )
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val center = size.center
                val radius = size.minDimension / 2.25f

                val sweep = 360f * progress.value
                val start = -90f
                drawArc(
                    color = primary.copy(alpha = 0.95f),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(2 * radius, 2 * radius),
                    style = Stroke(width = 10f, cap = StrokeCap.Round)
                )

                val t = progress.value
                val tSmooth = t * t * (3f - 2f * t)
                val cycles = 1f
                val phase =
                    0.5f * (1f - cos((2f * Math.PI * cycles * tSmooth).toFloat()))
                val scanRadius = radius * 0.96f
                val circlePath = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(center, scanRadius))
                }
                val bandHeight = size.minDimension * 0.13f
                val top = (center.y - scanRadius) +
                        (2 * scanRadius - bandHeight) * phase
                val bandRect = androidx.compose.ui.geometry.Rect(
                    left = center.x - scanRadius,
                    top = top,
                    right = center.x + scanRadius,
                    bottom = top + bandHeight
                )
                clipPath(circlePath) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                primary.copy(alpha = 0.26f),
                                primary.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            startY = bandRect.top,
                            endY = bandRect.bottom
                        ),
                        topLeft = bandRect.topLeft,
                        size = bandRect.size
                    )
                }
            }

            Icon(
                painter = painterResource(R.drawable.frame_person),
                contentDescription = null,
                tint = onSurface,
                modifier = Modifier.size(88.dp)
            )
        }
    }
}

/**
 * Pattern-style prompt rendering a 3x3 grid with an animated unlock path.
 *
 * @param accent Color accents used in the path and glow.
 */
@Composable
private fun PatternPrompt(accent: List<Color>) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary

    val pathProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        pathProgress.snapTo(0f)
        pathProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 3800,
                easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
            )
        )
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val gridDp = minOf(maxWidth * 0.74f, maxHeight * 0.52f)
        val gridPx = with(density) { gridDp.toPx() }
        val stroke = with(density) { 6.dp.toPx() }
        val outerRadius = with(density) { 20.dp.toPx() }
        val innerRadius = with(density) { 8.dp.toPx() }

        Canvas(Modifier.size(gridDp)) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f
            val cell = gridPx / 3f

            val centers = (0..2).flatMap { row ->
                (0..2).map { column ->
                    Offset(
                        x = centerX + (column - 1) * cell,
                        y = centerY + (row - 1) * cell
                    )
                }
            }

            centers.forEach { center ->
                drawCircle(
                    color = onSurface.copy(alpha = 0.08f),
                    radius = outerRadius,
                    center = center,
                    style = Stroke(width = stroke)
                )
                drawCircle(
                    color = onSurface.copy(alpha = 0.60f),
                    radius = innerRadius,
                    center = center
                )
            }

            val pathIndices = listOf(0, 1, 2, 5, 8)
            val points = pathIndices.map { centers[it] }
            val steps = ceil(points.size * pathProgress.value)
                .toInt()
                .coerceIn(1, points.size)
            val drawPoints = points.take(steps)

            if (drawPoints.isNotEmpty()) {
                val path = Path().apply {
                    moveTo(drawPoints.first().x, drawPoints.first().y)
                    drawPoints.drop(1).forEachIndexed { i, point ->
                        val previous = drawPoints[i]
                        val mid = Offset(
                            x = (previous.x + point.x) / 2f,
                            y = (previous.y + point.y) / 2f
                        )
                        lineTo(mid.x, mid.y)
                        lineTo(point.x, point.y)
                    }
                }

                drawPath(
                    path = path,
                    color = primary,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )

                val head = drawPoints.last()
                val pulse =
                    1f + 0.12f * sin((pathProgress.value * 2f * Math.PI).toFloat())

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.26f),
                            Color.Transparent
                        ),
                        center = head,
                        radius = innerRadius * 3f
                    ),
                    radius = innerRadius * 3f,
                    center = head
                )
                drawCircle(
                    color = onSurface,
                    radius = innerRadius * pulse,
                    center = head
                )
            }
        }
    }
}

/**
 * PIN-style prompt with animated keypad presses and indicator dots.
 *
 * @param accent Color accents used in the keypad effects.
 */
@Composable
private fun PinPrompt(accent: List<Color>) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val presses = remember { listOf(0, 4, 7, 10) }
    val pressProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        pressProgress.snapTo(0f)
        pressProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 3800,
                easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
            )
        )
    }

    val raw = presses.size * pressProgress.value
    val count = ceil(raw.toDouble()).toInt().coerceIn(0, presses.size)
    val activeIndex = if (count in 1..presses.size) presses[count - 1] else null
    val localT = if (count == 0) 0f else (raw - floor(raw)).toFloat()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { index ->
                    val filled = index < count
                    Dot(
                        size = 20.dp,
                        scale = if (filled) 1.1f else 1f,
                        color = if (filled) accent.firstOrNull()
                            ?: MaterialTheme.colorScheme.primary
                        else onSurface.copy(alpha = 0.22f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            KeypadPro(
                presses = presses,
                progress = pressProgress.value,
                accent = accent,
                onSurface = onSurface,
                activeIndex = activeIndex,
                activeLocalT = localT
            )
        }
    }
}

/**
 * Animated numeric keypad for the PIN prompt.
 *
 * @param presses Indices of keys that will be animated as pressed.
 * @param progress Overall animation progress in [0f, 1f].
 * @param accent Color accents for the ripple and glow.
 * @param onSurface Base color derived from the surface content color.
 * @param activeIndex Index of the currently active key, if any.
 * @param activeLocalT Local progress of the active key animation.
 */
@Composable
private fun KeypadPro(
    presses: List<Int>,
    progress: Float,
    accent: List<Color>,
    onSurface: Color,
    activeIndex: Int?,
    activeLocalT: Float
) {
    val rows = 4
    val cols = 3

    val gradientStops: List<Color> = when {
        accent.size >= 2 -> accent + accent.first()
        else -> {
            val base = accent.firstOrNull() ?: MaterialTheme.colorScheme.primary
            listOf(
                base.copy(alpha = 0.95f),
                base.copy(alpha = 0.75f),
                base.copy(alpha = 0.95f)
            )
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(rows) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                repeat(cols) { column ->
                    val index = row * cols + column
                    val isActive = index == activeIndex
                    val t = if (isActive) activeLocalT else 0f

                    val scale by animateFloatAsState(
                        targetValue = if (isActive) 0.94f else 1f,
                        animationSpec = spring(
                            dampingRatio = 0.9f,
                            stiffness = 180f
                        ),
                        label = "keyScale$index"
                    )

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .scale(scale),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val radius = size.minDimension / 2f
                            val center = Offset(size.width / 2f, size.height / 2f)

                            drawCircle(
                                color = onSurface.copy(alpha = 0.10f),
                                radius = radius,
                                center = center
                            )

                            if (isActive) {
                                val ringBrush =
                                    Brush.sweepGradient(colors = gradientStops, center = center)
                                drawCircle(
                                    brush = ringBrush,
                                    radius = radius * (1f + 0.24f * t),
                                    style = Stroke(width = 6f)
                                )

                                val innerBrush = Brush.radialGradient(
                                    colors = listOf(
                                        gradientStops.first().copy(alpha = 0.12f * (1f - t)),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = radius * (0.9f + 0.3f * t)
                                )
                                drawCircle(
                                    brush = innerBrush,
                                    radius = radius * (0.75f + 0.20f * t),
                                    center = center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Simple circular dot used as a PIN indicator.
 *
 * @param size Diameter of the dot.
 * @param scale Scale factor for the dot's size.
 * @param color Fill color of the dot.
 * @param modifier Modifier applied to the dot container.
 */
@Composable
private fun Dot(
    size: Dp,
    scale: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .background(color, CircleShape)
    )
}

/**
 * Final passkey hero animation, driven by an outline drawable.
 *
 * Tapping on this hero both requests the trace animation to stop
 * gracefully at the end of the current cycle and invokes [onFinished]
 * so that the caller can navigate to the next screen.
 *
 * @param accent Color accents potentially used by the hero (kept for
 *               API symmetry and future customization).
 * @param onFinished Callback invoked when the hero is tapped.
 */
@Composable
private fun PasskeysFinalHero(
    accent: List<Color>,
    onFinished: () -> Unit
) {
    var stop by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interaction,
                indication = null
            ) {
                stop = true
                onFinished()
            },
        contentAlignment = Alignment.Center
    ) {
        PathTraceFromSvg(
            drawableId = R.drawable.passkey_outline,
            modifier = Modifier.size(260.dp),
            speedMs = 2800,
            pauseMs = 800,
            easing = LinearEasing,
            stopSignal = stop,
            stopAtEndOfCurrentCycle = true
        )
    }
}
