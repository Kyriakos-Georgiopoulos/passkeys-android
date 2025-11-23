package example.faq

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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.navigation.AppScreen
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.abs
import kotlin.math.sign

/**
 * Color palette for the passkeys FAQ screen.
 */
private object Pro {
    val BgTop = Color(0xFFF0E9FF)
    val BgMid = Color(0xFFF6EAFE)
    val BgBottom = Color(0xFFFFF0F6)
    val BlobA = Color(0xFF8B90FF)
    val BlobB = Color(0xFFFA77D1)
    val Card = Color(0xFFF9FAFC)
    val Hairline = Color(0x1A1F2633)
    val Border = Color(0x141F2633)
    val SheenTop = Color(0x1AFFFFFF)
    val SheenMid = Color(0x0DFFFFFF)
    val ShadowTint = Color(0x22182434)
}

/**
 * FAQ item model used in the deck.
 *
 * @property q Question text.
 * @property a Answer text.
 */
private data class Faq(
    val q: String,
    val a: String
)

/**
 * Visual placement configuration for a card in the deck.
 *
 * @property dx Horizontal offset.
 * @property baseY Vertical base offset.
 * @property rot Rotation in degrees.
 * @property scale Scale factor.
 * @property elevation Card elevation.
 */
private data class Placement(
    val dx: Dp,
    val baseY: Dp,
    val rot: Float,
    val scale: Float,
    val elevation: Dp
)

private const val VISIBLE_STACK = 5
private val STACK_STEP_Y = 8.dp
private const val CARD_HEIGHT_FRACTION = 0.55f

/**
 * Static list of FAQs displayed in the passkeys deck.
 */
private val Faqs: List<Faq> = listOf(
    Faq(
        "Do I need backend changes to support passkeys?",
        "Yes. Your backend must support WebAuthn: create a challenge, store one or more public keys for each user, and verify the signed challenge during login."
    ),
    Faq(
        "What if the user loses or replaces their phone?",
        "If their passkeys are synced with Google Password Manager, they show up on the new device automatically. If not, the user signs in with a fallback method and creates a new passkey."
    ),
    Faq(
        "Do passkeys work across Android, iOS, and the web?",
        "Yes. Passkeys follow the WebAuthn standard and sync through Google Password Manager or iCloud. Cross-device sign-in also works using QR code and Bluetooth."
    ),
    Faq(
        "Can passkeys be phished?",
        "They’re extremely hard to phish. The private key never leaves the device and only works for the correct domain."
    ),
    Faq(
        "Where are passkeys stored on Android?",
        "In Google Password Manager, protected by the user’s screen lock and secure hardware like the device’s TEE (Trusted Execution Environment) or StrongBox."
    ),
    Faq(
        "Do I still need passwords in my app?",
        "For now, yes. Passkeys should be the default option, but you still need a 'Use password instead' fallback for older devices and recovery."
    ),
    Faq(
        "How do I migrate existing users to passkeys?",
        "Let them sign in with their current method, then offer a 'Create passkey' button. After that, prefer passkey sign-in and keep the old method as backup."
    ),
    Faq(
        "Can users have multiple passkeys?",
        "Yes. Each device creates its own key pair. Your backend should allow storing multiple public keys per user."
    ),
    Faq(
        "How do I revoke a passkey?",
        "Remove that device’s public key from your backend. The passkey will stop working for that device."
    ),
    Faq(
        "How do I test passkeys?",
        "Use a real device or emulator with HTTPS. Localhost doesn’t work—use a real domain or an ngrok tunnel."
    )
)

/**
 * Generates a stable set of random-looking placements for a FAQ deck.
 *
 * The placements are deterministic per [count] and are used to give
 * the stack a natural, slightly messy layout.
 *
 * @param count Number of cards in the deck.
 *
 * @return A list of [Placement] entries, one per card.
 */
@Composable
private fun rememberPlacements(count: Int): List<Placement> = remember(count) {
    val random = Random(2025L)

    fun randomFloat(min: Float, max: Float): Float =
        min + random.nextFloat() * (max - min)

    List(count) { index ->
        val depth = index
        val baseStep = STACK_STEP_Y * depth
        val dx = randomFloat(-10f, 10f).dp
        val dy = randomFloat(-6f, 6f).dp
        val rotation = randomFloat(-3.2f, 3.2f)
        val scaleBase = (1f - 0.025f * depth).coerceAtLeast(0.86f)
        val scaleJitter = randomFloat(-0.02f, 0f)
        val scale = (scaleBase + scaleJitter).coerceAtMost(1f)
        val elevation = (28 - depth * 4 + randomFloat(-2f, 2f))
            .toInt()
            .coerceIn(6, 28)
            .dp

        Placement(
            dx = dx,
            baseY = baseStep + dy,
            rot = rotation,
            scale = scale,
            elevation = elevation
        )
    }
}

/**
 * Full-screen background with a soft gradient, blobs and vignette
 * used behind the passkeys FAQ content.
 */
@Composable
private fun VividBackdrop() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val baseGradient = Brush.linearGradient(
                    0f to Pro.BgTop,
                    0.5f to Pro.BgMid,
                    1f to Pro.BgBottom,
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
                drawRect(baseGradient)

                fun blob(
                    centerX: Float,
                    centerY: Float,
                    color: Color,
                    radiusFraction: Float,
                    alpha: Float
                ) {
                    val radius = size.minDimension * radiusFraction
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                            center = Offset(centerX, centerY),
                            radius = radius
                        ),
                        radius = radius,
                        center = Offset(centerX, centerY)
                    )
                }

                blob(
                    size.width * 0.82f,
                    size.height * 0.18f,
                    Pro.BlobA,
                    0.55f,
                    0.20f
                )
                blob(
                    size.width * 0.18f,
                    size.height * 0.88f,
                    Pro.BlobB,
                    0.60f,
                    0.16f
                )

                val vignette = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.06f)
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.6f),
                    radius = size.maxDimension * 0.9f
                )
                drawRect(vignette, blendMode = BlendMode.Softlight)
            }
    )
}

/**
 * Passkeys FAQ screen that shows a stacked deck of FAQ cards.
 *
 * The user can swipe away cards one by one. Once the last card is
 * dismissed, [onNavigationEvent] is invoked so the caller can navigate to
 * the next screen.
 *
 * @param modifier Modifier applied to the root scaffold.
 * @param onNavigationEvent Callback invoked after the last card is removed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasskeysFaq(
    modifier: Modifier = Modifier,
    onNavigationEvent: (AppScreen) -> Unit = {}
) {
    Box(Modifier.fillMaxSize()) {
        VividBackdrop()

        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            topBar = {
                LargeTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = Color(0xFF121418)
                    ),
                    title = {
                        Text(
                            text = "Passkeys • FAQ",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            )
                        )
                    }
                )
            },
            modifier = modifier
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                FaqDeck(
                    faqs = Faqs,
                    onFinished = {
                        onNavigationEvent(AppScreen.PassKeysMockDemo)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 64.dp)
                        .zIndex(1f)
                )
            }
        }
    }
}

/**
 * Deck of FAQ cards with a stacked visual layout.
 *
 * The top card can be swiped left or right. Dismissing the last card
 * in the list triggers [onFinished].
 *
 * @param faqs List of FAQ entries to display.
 * @param onFinished Callback invoked when all cards have been dismissed.
 * @param modifier Modifier applied to the deck container.
 */
@Composable
private fun FaqDeck(
    faqs: List<Faq>,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var topIndex by rememberSaveable { mutableStateOf(0) }
    val haptics = LocalHapticFeedback.current
    val placements = rememberPlacements(faqs.size)
    val lastIndex = faqs.lastIndex

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (topIndex > lastIndex) {
            LaunchedEffect(Unit) {
                onFinished()
            }
            return@Box
        }

        val endIndex = (topIndex + VISIBLE_STACK - 1).coerceAtMost(lastIndex)

        for (index in endIndex downTo topIndex) {
            val placement = placements[index]

            if (index == topIndex) {
                key(index) {
                    SwipeableCard(
                        cardId = index,
                        faq = faqs[index],
                        baseScale = 1f,
                        baseTranslateY = 0.dp,
                        elevation = placement.elevation,
                        enterFromScale = placement.scale,
                        enterFromTranslateY = placement.baseY,
                        baseDx = placement.dx
                    ) { direction ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        val next = topIndex + 1
                        topIndex = next.coerceAtMost(faqs.size)
                    }
                }
            } else {
                StaticCard(
                    faq = faqs[index],
                    scale = placement.scale,
                    translateY = placement.baseY,
                    elevation = placement.elevation,
                    alpha = if (index == topIndex + 1) 1f else 0.92f,
                    jitterDx = placement.dx,
                    jitterRot = placement.rot
                )
            }
        }
    }
}

/**
 * Soft oval shadow modifier used under cards in the stack.
 *
 * @param alpha Shadow opacity.
 * @param spread Horizontal spread factor.
 * @param lift Vertical lift factor applied to the shadow.
 */
private fun Modifier.softCardShadow(
    alpha: Float = 0.18f,
    spread: Float = 0.86f,
    lift: Float = 0.35f
): Modifier = drawBehind {
    val width = size.width * spread
    val height = size.height * 0.22f
    val left = (size.width - width) / 2f
    val top = size.height - height * (1f + lift)

    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black.copy(alpha = alpha), Color.Transparent),
            center = Offset(size.width / 2f, size.height - height * lift),
            radius = width
        ),
        topLeft = Offset(left, top),
        size = Size(width, height)
    )
}

/**
 * Draggable, dismissible card used as the top-most element in the deck.
 *
 * The card animates from its initial depth placement to the top position,
 * and can then be swiped horizontally to dismiss. When the card is
 * dismissed, [onDismiss] is invoked with the swipe direction.
 *
 * @param cardId Stable identifier for this card instance.
 * @param faq FAQ content displayed in the card.
 * @param baseScale Target scale when fully promoted to the top.
 * @param baseTranslateY Target vertical offset when fully promoted.
 * @param elevation Base card elevation.
 * @param enterFromScale Initial scale used for the enter animation.
 * @param enterFromTranslateY Initial vertical offset for the enter animation.
 * @param baseDx Static horizontal offset applied to this card in the deck.
 * @param onDismiss Callback invoked with the swipe direction (-1 or +1).
 */
@Composable
private fun SwipeableCard(
    cardId: Int,
    faq: Faq,
    baseScale: Float,
    baseTranslateY: Dp,
    elevation: Dp,
    enterFromScale: Float,
    enterFromTranslateY: Dp,
    baseDx: Dp,
    onDismiss: (direction: Int) -> Unit
) {
    val scope = rememberCoroutineScope()

    val offsetX = remember(cardId) { Animatable(0f) }
    val offsetY = remember(cardId) { Animatable(0f) }
    val rotationZ = remember(cardId) { Animatable(0f) }

    val scaleAnim = remember(cardId) { Animatable(enterFromScale) }
    val yAnim = remember(cardId) { Animatable(enterFromTranslateY.value) }
    val liftAnim = remember(cardId) { Animatable(0f) }

    LaunchedEffect(cardId, baseScale, baseTranslateY) {
        scaleAnim.snapTo(enterFromScale)
        yAnim.snapTo(enterFromTranslateY.value)
        liftAnim.snapTo(0f)

        scaleAnim.animateTo(
            targetValue = baseScale,
            animationSpec = spring(stiffness = 240f, dampingRatio = 0.88f)
        )
        yAnim.animateTo(
            targetValue = baseTranslateY.value,
            animationSpec = spring(stiffness = 260f, dampingRatio = 0.9f)
        )
        liftAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(stiffness = 200f, dampingRatio = 0.92f)
        )
    }

    val drag: suspend (dx: Float, dy: Float) -> Unit = { dx, dy ->
        offsetX.snapTo(offsetX.value + dx)
        offsetY.snapTo(offsetY.value + dy / 3f)
        rotationZ.snapTo((offsetX.value / 48f).coerceIn(-12f, 12f))
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()

        fun settleOrDismiss() {
            val threshold = widthPx * 0.25f
            val x = offsetX.value
            val direction = sign(x).toInt()

            if (abs(x) > threshold) {
                scope.launch {
                    offsetX.animateTo(
                        targetValue = direction * widthPx * 1.2f,
                        animationSpec = tween(durationMillis = 260)
                    )
                    onDismiss(direction)
                }
            } else {
                scope.launch {
                    offsetX.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = 0.9f
                        )
                    )
                    offsetY.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = 0.9f
                        )
                    )
                    rotationZ.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 240)
                    )
                }
            }
        }

        val dragScale by remember(cardId) {
            derivedStateOf {
                val fraction = (abs(offsetX.value) / (widthPx * 0.6f))
                    .coerceIn(0f, 1f)
                1f - 0.02f * fraction
            }
        }

        val downBias by remember(cardId) {
            derivedStateOf {
                val fraction = (abs(offsetX.value) / widthPx)
                    .coerceIn(0f, 1f)
                10f * fraction
            }
        }

        val liftedElevation = elevation * (0.85f + 0.15f * liftAnim.value)

        CardBody(
            faq = faq,
            elevation = liftedElevation,
            modifier = Modifier
                .offset(x = baseDx, y = Dp(yAnim.value))
                .graphicsLayer {
                    alpha = 1f
                    scaleX = scaleAnim.value * dragScale
                    scaleY = scaleAnim.value * dragScale
                    translationX = offsetX.value
                    translationY = offsetY.value + downBias
                    this.rotationZ = rotationZ.value
                    cameraDistance = 36_000f
                    transformOrigin = TransformOrigin(0.5f, 0.85f)
                }
                .pointerInput(cardId) {
                    detectDragGestures(
                        onDragEnd = { settleOrDismiss() },
                        onDragCancel = { settleOrDismiss() }
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            drag(dragAmount.x, dragAmount.y)
                        }
                    }
                }
                .zIndex(1f)
        )
    }
}

/**
 * Non-interactive card used for the lower layers of the stack.
 *
 * @param faq FAQ content displayed in the card.
 * @param scale Scale factor applied to the card.
 * @param translateY Vertical offset applied to the card.
 * @param elevation Elevation of the card.
 * @param alpha Overall alpha used to fade the card slightly.
 * @param jitterDx Horizontal offset jitter for natural layout.
 * @param jitterRot Rotation jitter for natural layout.
 */
@Composable
private fun StaticCard(
    faq: Faq,
    scale: Float,
    translateY: Dp,
    elevation: Dp,
    alpha: Float,
    jitterDx: Dp,
    jitterRot: Float
) {
    CardBody(
        faq = faq,
        elevation = elevation,
        modifier = Modifier
            .offset(x = jitterDx, y = translateY)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
                rotationZ = jitterRot
            }
            .zIndex(0f)
    )
}

/**
 * Card layout used for both interactive and static FAQ cards.
 *
 * @param faq FAQ content to render.
 * @param elevation Elevation applied to the surrounding shadow.
 * @param modifier Modifier applied to the card container.
 */
@Composable
private fun CardBody(
    faq: Faq,
    elevation: Dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(CARD_HEIGHT_FRACTION)
            .shadow(
                elevation = elevation / 7,
                shape = shape,
                clip = false,
                ambientColor = Pro.ShadowTint,
                spotColor = Pro.ShadowTint
            )
            .softCardShadow(alpha = 0.18f)
            .clip(shape)
            .background(Pro.Card)
            .border(width = 1.dp, color = Pro.Border, shape = shape)
            .drawBehind {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(1f, 1f),
                    size = Size(size.width - 2f, size.height - 2f),
                    cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx()),
                    style = Stroke(width = 1f)
                )

                val sheen = Brush.verticalGradient(
                    0f to Pro.SheenTop,
                    0.35f to Pro.SheenMid,
                    1f to Color.Transparent
                )
                drawRect(sheen)
            }
            .padding(horizontal = 24.dp, vertical = 22.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Text(
                text = faq.q,
                color = Color(0xFF1E222B),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 34.sp,
                    letterSpacing = (-0.3).sp
                )
            )

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Pro.Hairline)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = faq.a,
                color = Color(0xFF2A3039),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                )
            )
        }
    }
}
