package example.registration

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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.example.navigation.AppScreen
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Two-phase passkey registration animation.
 *
 * This composable visualizes how passkeys are created and used:
 *
 * 1. A private key is created on the device and moves to the bottom, staying local.
 * 2. A public key (lock) is derived and moves to the top, representing data sent to the server.
 *
 * Tapping anywhere on the surface cycles through the four animation beats:
 * private key creation, private key moving down, public key creation, and public key moving up.
 *
 * @param modifier [Modifier] applied to the root surface.
 */
@Composable
fun PasskeyRegistrationTwoPhase(
    modifier: Modifier = Modifier,
    onNavigationEvent: (AppScreen) -> Unit
) {
    var beat by remember { mutableStateOf(Beat.KeyCreate) }
    val interaction = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) {
                when (beat) {
                    Beat.LockUp -> onNavigationEvent(AppScreen.PasskeyPrompts)
                    else -> beat = beat.next()
                }
            },
        color = Color.Transparent
    ) {
        when (beat) {
            Beat.KeyCreate -> KeyCreate()
            Beat.KeyDown -> KeyDown()
            Beat.LockCreate -> LockCreate()
            Beat.LockUp -> LockUp()
        }
    }
}

/**
 * Animation beat for the two-phase passkey registration sequence.
 */
private enum class Beat {
    KeyCreate,
    KeyDown,
    LockCreate,
    LockUp;

    fun next(): Beat = entries[(ordinal + 1) % entries.size]
}

/**
 * First phase: create the private key at the center with an OS cue behind it.
 */
@Composable
private fun KeyCreate() {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(150)
        show = true
    }

    val scale by animateFloatAsState(
        targetValue = if (show) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "keyScale"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        OsMakerCue()
        Icon(
            imageVector = Icons.Rounded.VpnKey,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}

/**
 * Second phase: move the private key from the center to the bottom of the screen.
 */
@Composable
private fun KeyDown() {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val keySizePx = with(density) { 120.dp.toPx() }
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val centerX = width / 2f
        val centerY = height / 2f

        val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(this).toFloat() }
        val bottomMargin = with(density) { 24.dp.toPx() }
        val targetY = height - bottomInset - bottomMargin - keySizePx / 2f

        val progress = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
            )
        }

        val currentY = centerY + (targetY - centerY) * progress.value

        Icon(
            imageVector = Icons.Rounded.VpnKey,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer {
                    translationX = centerX - keySizePx / 2f
                    translationY = currentY - keySizePx / 2f
                }
        )
    }
}

/**
 * Third phase: create the public key (lock) at the center with a math cue.
 * The private key remains visible at the bottom.
 */
@Composable
private fun LockCreate() {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val keySizePx = with(density) { 120.dp.toPx() }
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val centerX = width / 2f

        val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(this).toFloat() }
        val bottomMargin = with(density) { 24.dp.toPx() }
        val keyBottomY = height - bottomInset - bottomMargin - keySizePx / 2f

        var show by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            show = true
        }

        val scale by animateFloatAsState(
            targetValue = if (show) 1f else 0.7f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "lockScale"
        )

        val alpha by animateFloatAsState(
            targetValue = if (show) 1f else 0.8f,
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            label = "lockAlpha"
        )

        Icon(
            imageVector = Icons.Rounded.VpnKey,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer {
                    translationX = centerX - keySizePx / 2f
                    translationY = keyBottomY - keySizePx / 2f
                }
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            MathMakerCue()
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = alpha),
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
    }
}

/**
 * Fourth phase: move the public key (lock) from the center to the top.
 * The private key remains visible at the bottom.
 */
@Composable
private fun LockUp() {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val keySizePx = with(density) { 120.dp.toPx() }
        val lockSizePx = with(density) { 96.dp.toPx() }
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val centerX = width / 2f
        val centerY = height / 2f

        val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(this).toFloat() }
        val bottomMargin = with(density) { 24.dp.toPx() }
        val keyBottomY = height - bottomInset - bottomMargin - keySizePx / 2f

        val topInset = with(density) { WindowInsets.statusBars.getTop(this).toFloat() }
        val topMargin = with(density) { 16.dp.toPx() }
        val lockTargetY = topInset + topMargin + lockSizePx / 2f

        val progress = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
            )
        }

        val lockY = centerY + (lockTargetY - centerY) * progress.value
        val lockAlpha = 1f - 0.4f * progress.value

        Icon(
            imageVector = Icons.Rounded.VpnKey,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer {
                    translationX = centerX - keySizePx / 2f
                    translationY = keyBottomY - keySizePx / 2f
                }
        )

        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary.copy(alpha = lockAlpha),
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer {
                    translationX = centerX - lockSizePx / 2f
                    translationY = lockY - lockSizePx / 2f
                }
        )
    }
}

/**
 * OS creation cue placed behind the private key.
 *
 * Draws a pulsing circular ring with gaps and three badges (Android, fingerprint,
 * and verified) distributed along the ring.
 *
 * @param badgeAnglesUp angles in degrees (0° is up, increasing clockwise) for badge placement.
 * @param gapDeg angular gap around each badge where the ring is not drawn.
 * @param iconSize size of each badge icon.
 */
@Composable
private fun OsMakerCue(
    badgeAnglesUp: List<Float> = listOf(-135f, 0f, 120f),
    gapDeg: Float = 18f,
    iconSize: Dp = 52.dp
) {
    val pulse = rememberInfiniteTransition(label = "osPulse")
    val ringAlpha by pulse.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "osRingAlpha"
    )

    val ringStroke = 7f

    fun up0ToCanvasStart(deg: Float): Float = deg - 90f

    fun up0ToXY(deg: Float): Pair<Float, Float> {
        val radians = Math.toRadians((deg - 90f).toDouble())
        return cos(radians).toFloat() to sin(radians).toFloat()
    }

    BoxWithConstraints(
        modifier = Modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val density = LocalDensity.current
        val minSidePx = with(density) { min(maxWidth, maxHeight).toPx() }
        val ringRadiusPx = minSidePx * 0.44f

        Canvas(Modifier.matchParentSize()) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            val sorted = badgeAnglesUp.sorted()
            val extended = sorted + (sorted.first() + 360f)

            for (i in sorted.indices) {
                val startUp = sorted[i] + gapDeg
                val endUp = extended[i + 1] - gapDeg
                val sweep = endUp - startUp

                if (sweep > 0f) {
                    drawArc(
                        color = primaryColor.copy(alpha = 0.12f * ringAlpha),
                        startAngle = up0ToCanvasStart(startUp),
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            center.x - ringRadiusPx,
                            center.y - ringRadiusPx
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            ringRadiusPx * 2f,
                            ringRadiusPx * 2f
                        ),
                        style = Stroke(width = ringStroke)
                    )
                }
            }
        }

        val tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)

        @Composable
        fun Place(angleUp: Float, content: @Composable () -> Unit) {
            val (cx, sy) = up0ToXY(angleUp)
            val dx = ringRadiusPx * cx
            val dy = ringRadiusPx * sy
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = dx
                        translationY = dy
                    }
                    .align(Alignment.Center)
            ) {
                content()
            }
        }

        Place(badgeAnglesUp.getOrElse(0) { -135f }) {
            Icon(
                imageVector = Icons.Rounded.Android,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }

        Place(badgeAnglesUp.getOrElse(1) { 0f }) {
            Icon(
                imageVector = Icons.Rounded.Fingerprint,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }

        Place(badgeAnglesUp.getOrElse(2) { 120f }) {
            Icon(
                imageVector = Icons.Rounded.Verified,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Math cue placed behind the public key (lock).
 *
 * Draws a faint grid and a set of math symbols around the center,
 * leaving an empty area where the lock icon is rendered.
 */
@Composable
private fun MathMakerCue() {
    val pulse = rememberInfiniteTransition(label = "mathPulse")
    val alphaFactor by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mathAlpha"
    )

    Canvas(Modifier.size(240.dp)) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val minDim = size.minDimension
        val step = minDim / 12f

        repeat(13) { x ->
            drawLine(
                color = Color.Black.copy(alpha = 0.05f),
                start = androidx.compose.ui.geometry.Offset(
                    center.x - 6 * step + x * step,
                    center.y - 6 * step
                ),
                end = androidx.compose.ui.geometry.Offset(
                    center.x - 6 * step + x * step,
                    center.y + 6 * step
                ),
                strokeWidth = 2f
            )
        }

        repeat(13) { y ->
            drawLine(
                color = Color.Black.copy(alpha = 0.05f),
                start = androidx.compose.ui.geometry.Offset(
                    center.x - 6 * step,
                    center.y - 6 * step + y * step
                ),
                end = androidx.compose.ui.geometry.Offset(
                    center.x + 6 * step,
                    center.y - 6 * step + y * step
                ),
                strokeWidth = 2f
            )
        }

        val lockBox = android.graphics.RectF(
            center.x - step * 2.2f,
            center.y - step * 2.3f,
            center.x + step * 2.2f,
            center.y + step * 2.6f
        )

        val big = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = minDim * 0.22f
            color = android.graphics.Color.argb((190 * alphaFactor).toInt(), 0, 0, 0)
            textAlign = android.graphics.Paint.Align.CENTER
        }

        val medium = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = minDim * 0.16f
            color = android.graphics.Color.argb((165 * alphaFactor).toInt(), 0, 0, 0)
            textAlign = android.graphics.Paint.Align.CENTER
        }

        val small = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = minDim * 0.13f
            color = android.graphics.Color.argb((145 * alphaFactor).toInt(), 0, 0, 0)
            textAlign = android.graphics.Paint.Align.CENTER
        }

        data class SymbolSpec(
            val text: String,
            val dx: Float,
            val dy: Float,
            val paint: android.graphics.Paint
        )

        val symbols = listOf(
            SymbolSpec("π", -4.8f, -1.2f, medium),
            SymbolSpec("∮", -3.3f, -3.3f, medium),
            SymbolSpec("+", 0.0f, -3.6f, small),
            SymbolSpec("−", 2.8f, -3.6f, small),
            SymbolSpec("9", 3.2f, -0.7f, small),
            SymbolSpec("√", 4.2f, -0.1f, medium),
            SymbolSpec("×", 4.0f, 1.6f, small),
            SymbolSpec("÷", -3.5f, 0.8f, small),
            SymbolSpec("2", -4.3f, 2.2f, small),
            SymbolSpec("∑", -2.4f, 4.2f, big),
            SymbolSpec("≈", 0.8f, 3.8f, small),
            SymbolSpec("13", 2.6f, 5.1f, small),
            SymbolSpec("∞", 4.6f, 4.1f, big)
        )

        val nativeCanvas = drawContext.canvas.nativeCanvas

        symbols.forEach { symbol ->
            val x = center.x + symbol.dx * step
            val y = center.y + symbol.dy * step
            if (!lockBox.contains(x, y)) {
                nativeCanvas.drawText(symbol.text, x, y, symbol.paint)
            }
        }
    }
}

/**
 * Preview of the two-phase passkey registration animation.
 */
@Preview(showBackground = true)
@Composable
private fun PasskeyRegistrationTwoPhasePreview() {
    MaterialTheme(colorScheme = androidx.compose.material3.lightColorScheme()) {
        PasskeyRegistrationTwoPhase() {
            // No-op
        }
    }
}
