package example

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
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min

/**
 * Presentation-ready mocked passkey registration flow.
 *
 * This composable demonstrates a full passkey registration journey using
 * mocked steps:
 *
 * - Intro
 * - Account selection
 * - Biometric prompt
 * - Registration with optional error simulation
 * - Success / Error
 *
 * @param modifier Modifier applied to the root [Scaffold].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasskeyRegistrationMockScreen(
    modifier: Modifier = Modifier,
    onOpenPasskeysActivity: () -> Unit = {}
) {
    var step by remember { mutableStateOf(PasskeyStep.Intro) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showBiometricSheet by remember { mutableStateOf(false) }
    var forceErrorNext by remember { mutableStateOf(false) }

    val dark = isSystemInDarkTheme()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Passkey • Registration Demo") }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(gradientBackground(dark))
                .padding(padding)
                .padding(16.dp)
        ) {
            HeroCard(
                forceErrorNext = forceErrorNext,
                onToggleError = { forceErrorNext = it }
            )

            Spacer(Modifier.height(16.dp))

            Stepper(step = step)

            Spacer(Modifier.height(16.dp))

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 24.dp)
                ) {
                    when (step) {
                        PasskeyStep.Intro -> IntroStep(
                            onStart = { step = PasskeyStep.AccountSelection },
                            onOpenPasskeysActivity = onOpenPasskeysActivity
                        )

                        PasskeyStep.AccountSelection -> AccountSelectionStep(
                            onChoose = { showAccountSheet = true },
                            onBack = { step = PasskeyStep.Intro }
                        )

                        PasskeyStep.BiometricPrompt -> BiometricStep(
                            onTrigger = { showBiometricSheet = true },
                            onBack = { step = PasskeyStep.AccountSelection }
                        )

                        PasskeyStep.Registering -> RegisteringStep(
                            shouldError = forceErrorNext,
                            onDone = { success ->
                                step = if (success) PasskeyStep.Success else PasskeyStep.Error
                                forceErrorNext = false
                            },
                            onCancel = { step = PasskeyStep.AccountSelection }
                        )

                        PasskeyStep.Success -> SuccessStep(
                            onRestart = { step = PasskeyStep.Intro }
                        )

                        PasskeyStep.Error -> ErrorStep(
                            onRetry = { step = PasskeyStep.AccountSelection }
                        )
                    }
                }
            }
        }

        if (showAccountSheet) {
            MockAccountSheet(
                onDismiss = { showAccountSheet = false },
                onAccountPicked = {
                    showAccountSheet = false
                    step = PasskeyStep.BiometricPrompt
                }
            )
        }

        if (showBiometricSheet) {
            MockBiometricSheet(
                onDismiss = { showBiometricSheet = false },
                onSuccess = {
                    showBiometricSheet = false
                    step = PasskeyStep.Registering
                },
                onFail = {
                    showBiometricSheet = false
                    step = PasskeyStep.Registering
                    forceErrorNext = true
                }
            )
        }
    }
}

/**
 * Returns the gradient background used by the mock screen.
 *
 * @param dark Whether the system is in dark theme.
 */
@Composable
private fun gradientBackground(dark: Boolean): Brush = Brush.verticalGradient(
    colors = if (dark) {
        listOf(Color(0xFF0D1117), Color(0xFF0B1220))
    } else {
        listOf(Color(0xFFF7F9FD), Color(0xFFF0F4FF))
    }
)

/**
 * Header card describing the mock and exposing an error toggle switch.
 *
 * @param forceErrorNext Whether the next registration should force an error.
 * @param onToggleError Callback invoked when the error toggle changes.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeroCard(
    forceErrorNext: Boolean,
    onToggleError: (Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(32.dp)
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Security, contentDescription = null)
                }

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        "Secure, passwordless sign- in",
                        style = MaterialTheme.typography.titleLargeEmphasized
                    )
                    Text(
                        "This demo simulates the Passkey registration screens.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Simulate error on next run",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        if (forceErrorNext) {
                            "Errors are ON until you toggle them off"
                        } else {
                            "Toggle to force an error outcome"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = forceErrorNext,
                    onCheckedChange = onToggleError
                )
            }
        }
    }
}

/**
 * Circular icon with a subtle ring used in the step chrome.
 *
 * @param icon Icon to render in the center.
 * @param diameter Overall diameter of the ring.
 * @param stroke Stroke width of the ring.
 */
@Composable
private fun StepRingedIcon(
    icon: ImageVector,
    diameter: Dp = 132.dp,
    stroke: Dp = 6.dp
) {
    Box(contentAlignment = Alignment.Center) {
        val ringColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

        Canvas(Modifier.size(diameter)) {
            val strokePx = stroke.toPx()
            val s = size.minDimension
            val r = s / 2f

            val arcRect = androidx.compose.ui.geometry.Rect(
                left = center.x - r + strokePx,
                top = center.y - r + strokePx,
                right = center.x + r - strokePx,
                bottom = center.y + r - strokePx
            )

            drawArc(
                color = ringColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(arcRect.left, arcRect.top),
                size = Size(arcRect.width, arcRect.height),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(diameter * 0.45f)
        )
    }
}

/**
 * Common layout used by all steps in the flow.
 *
 * It renders:
 * - Optional back arrow
 * - Title and optional icon
 * - Center content
 * - Optional bottom actions
 *
 * @param title Step title.
 * @param icon Optional icon rendered under the title.
 * @param onBack Optional back action; if null, no back button is shown.
 * @param center Center content block.
 * @param bottom Optional content rendered at the bottom of the screen.
 */
@Composable
private fun StepChrome(
    title: String,
    icon: ImageVector?,
    onBack: (() -> Unit)? = null,
    center: @Composable () -> Unit,
    bottom: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Column(modifier = Modifier.padding(end = 4.dp)) {
                            Icon(
                                modifier = Modifier.size(30.dp),
                                imageVector = Icons.Outlined.ChevronLeft,
                                contentDescription = "Back"
                            )
                            Spacer(modifier = Modifier.height(15.dp))
                        }
                    }
                }

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
            }

            if (icon != null) {
                Spacer(Modifier.height(16.dp))
                StepRingedIcon(icon = icon)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            center()
        }

        if (bottom != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                bottom()
            }
        }
    }
}

/**
 * Horizontal stepper that visualizes the current [PasskeyStep].
 *
 * @param step Currently active step in the flow.
 */
@Composable
private fun Stepper(step: PasskeyStep) {
    val steps = listOf(
        PasskeyStep.Intro,
        PasskeyStep.AccountSelection,
        PasskeyStep.BiometricPrompt,
        PasskeyStep.Registering,
        PasskeyStep.Success
    )

    val targetIndex = when (step) {
        PasskeyStep.Error -> steps.lastIndex
        else -> steps.indexOf(step).coerceAtLeast(0)
    }

    val animatedIndex by animateFloatAsState(
        targetValue = targetIndex.toFloat(),
        animationSpec = tween(500),
        label = "stepperIndex"
    )

    val dotSize = 40.dp
    val dotRadius = dotSize / 2
    val trackHeight = 6.dp
    val containerHeight = 56.dp

    val reachedColor = MaterialTheme.colorScheme.primary
    val unreachedColor = MaterialTheme.colorScheme.surfaceVariant
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    val overallProgress =
        if (steps.size <= 1) 1f
        else (animatedIndex / steps.lastIndex.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(containerHeight)
            .drawBehind {
                val pxRadius = dotRadius.toPx()
                val startX = pxRadius
                val endX = size.width - pxRadius
                val span = (endX - startX).coerceAtLeast(0f)
                val progressX = startX + span * overallProgress

                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(
                        startX,
                        size.height / 2f - trackHeight.toPx() / 2f
                    ),
                    size = Size(span, trackHeight.toPx()),
                    cornerRadius = CornerRadius(
                        trackHeight.toPx() / 2f,
                        trackHeight.toPx() / 2f
                    )
                )

                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            reachedColor.copy(alpha = 0.85f),
                            reachedColor
                        )
                    ),
                    topLeft = Offset(
                        startX,
                        size.height / 2f - trackHeight.toPx() / 2f
                    ),
                    size = Size(
                        (progressX - startX).coerceAtLeast(0f),
                        trackHeight.toPx()
                    ),
                    cornerRadius = CornerRadius(
                        trackHeight.toPx() / 2f,
                        trackHeight.toPx() / 2f
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dotRadius),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, _ ->
                val reached = animatedIndex + 1e-3f >= index
                val isCurrent = abs(animatedIndex - index) < 0.5f

                val scale by animateFloatAsState(
                    targetValue = when {
                        isCurrent -> 1.35f
                        reached -> 1f
                        else -> 0.95f
                    },
                    animationSpec = tween(220),
                    label = "dotScale$index"
                )

                val bg = if (reached) reachedColor else unreachedColor
                val fg =
                    if (reached) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .clip(CircleShape)
                        .background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "${index + 1}", color = fg)
                }
            }
        }
    }
}

/**
 * Intro step that explains the flow and provides a start button.
 *
 * @param onStart Callback invoked when the user presses "Start".
 */
@Composable
private fun IntroStep(
    onStart: () -> Unit,
    onOpenPasskeysActivity: () -> Unit
) {
    StepChrome(
        title = "Register a Passkey",
        icon = Icons.Outlined.Key,
        onBack = null,
        center = {
            Text(
                "Follow the flow: choose account → biometric → registering → result.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        },
        bottom = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .height(56.dp)
                        .widthIn(min = 220.dp)
                ) {
                    Text("Start mock flow")
                }

                OutlinedButton(
                    onClick = onOpenPasskeysActivity,
                    modifier = Modifier
                        .height(48.dp)
                        .widthIn(min = 220.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Key,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Open real Passkeys demo")
                }
            }
        }
    )
}


/**
 * Step that mimics an account picker.
 *
 * @param onChoose Callback invoked when the user chooses to open the picker.
 * @param onBack Callback invoked when the back button is pressed.
 */
@Composable
private fun AccountSelectionStep(
    onChoose: () -> Unit,
    onBack: () -> Unit
) {
    StepChrome(
        title = "Choose an account",
        icon = Icons.Outlined.Smartphone,
        onBack = onBack,
        center = {
            Text(
                "Mimics the Credential Manager picker.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        },
        bottom = {
            Button(
                onClick = onChoose,
                modifier = Modifier
                    .height(56.dp)
                    .widthIn(min = 240.dp)
            ) {
                Text("Open account picker")
            }
        }
    )
}

/**
 * Step that leads to the biometric prompt mock.
 *
 * @param onTrigger Callback invoked when the user requests the biometric prompt.
 * @param onBack Callback invoked when the back button is pressed.
 */
@Composable
private fun BiometricStep(
    onTrigger: () -> Unit,
    onBack: () -> Unit
) {
    StepChrome(
        title = "Biometric verification",
        icon = Icons.Outlined.Fingerprint,
        onBack = onBack,
        center = {
            Text(
                "Simulated fingerprint / face prompt.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        },
        bottom = {
            Button(
                onClick = onTrigger,
                modifier = Modifier
                    .height(56.dp)
                    .widthIn(min = 240.dp)
            ) {
                Text("Show biometric prompt")
            }
        }
    )
}

/**
 * Step that simulates the passkey registration progress.
 *
 * @param shouldError Whether this registration should end with an error.
 * @param onDone Callback invoked when registration completes, with a success flag.
 * @param onCancel Callback invoked when the user taps the back button.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RegisteringStep(
    shouldError: Boolean,
    onDone: (Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        val duration = 1400
        val steps = 20
        repeat(steps) { index ->
            progress = (index + 1) / steps.toFloat()
            delay((duration / steps).toLong())
        }
        onDone(!shouldError)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = 800,
            easing = CubicBezierEasing(0.0f, 0.0f, 1.0f, 1.0f)
        ),
        label = "wavyProgress"
    )

    StepChrome(
        title = "Registering your passkey…",
        icon = null,
        onBack = onCancel,
        center = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularWavyProgressIndicator(
                    stroke = Stroke(width = 6.dp.toPx()),
                    progress = { animatedProgress },
                    modifier = Modifier.size(132.dp)
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    if (shouldError) {
                        "(Demo will end with an error)"
                    } else {
                        "(Demo will end with success)"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        bottom = null
    )
}

/**
 * Step representing successful registration.
 *
 * @param onRestart Callback invoked when the user restarts the demo.
 */
@Composable
private fun SuccessStep(onRestart: () -> Unit) {
    StepChrome(
        title = "Passkey registered",
        icon = null,
        onBack = null,
        center = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedCheckmark(diameter = 132.dp)
                Spacer(Modifier.height(10.dp))
                Text(
                    "You're all set. Next login will be passwordless.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        bottom = {
            Button(
                onClick = onRestart,
                modifier = Modifier
                    .height(56.dp)
                    .widthIn(min = 220.dp)
            ) {
                Text("Restart demo")
            }
        }
    )
}

/**
 * Step representing a registration error.
 *
 * @param onRetry Callback invoked when the user retries registration.
 */
@Composable
private fun ErrorStep(onRetry: () -> Unit) {
    StepChrome(
        title = "Something went wrong",
        icon = null,
        onBack = null,
        center = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedCross(diameter = 132.dp)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Triggered by demo error mode.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        bottom = {
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .height(56.dp)
                    .widthIn(min = 180.dp)
            ) {
                Text("Retry")
            }
        }
    )
}

/**
 * Bottom sheet that mimics a credential account picker.
 *
 * @param onDismiss Callback invoked when the sheet is dismissed.
 * @param onAccountPicked Callback invoked when an account is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MockAccountSheet(
    onDismiss: () -> Unit,
    onAccountPicked: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("Choose an account", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            AccountRow(
                name = "Kyriakos Georgiopoulos",
                email = "kyriakos@example.com"
            ) {
                onAccountPicked("kyriakos@example.com")
            }
            AccountRow(
                name = "Demo User",
                email = "user@example.com"
            ) {
                onAccountPicked("user@example.com")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "This sheet mimics the Credential Manager account picker.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * Single account row used inside [MockAccountSheet].
 *
 * @param name Display name of the account.
 * @param email Email address of the account.
 * @param onClick Callback invoked when the row or chip is clicked.
 */
@Composable
private fun AccountRow(
    name: String,
    email: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(name.first().uppercase(), fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.SemiBold)
            Text(
                email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AssistChip(onClick = onClick, label = { Text("Select") })
    }
}

/**
 * Bottom sheet that mimics a biometric authentication prompt.
 *
 * @param onDismiss Callback invoked when the sheet is dismissed.
 * @param onSuccess Callback invoked when the user verifies successfully.
 * @param onFail Callback invoked when the user forces a failure.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MockBiometricSheet(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onFail: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                tonalElevation = 3.dp,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text("Touch sensor", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Simulated biometric auth sheet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.Center) {
                Button(onClick = onSuccess) { Text("Verify") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onFail) { Text("Fail") }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Animated checkmark icon used in the success step.
 *
 * @param diameter Size of the circular container.
 * @param stroke Stroke width of the checkmark and ring.
 */
@Composable
private fun AnimatedCheckmark(
    diameter: Dp,
    stroke: Dp = 6.dp
) {
    val successColor = Color(0xFF2ecc71)
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 630,
                easing = FastOutSlowInEasing
            )
        )
    }

    val ringColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

    Box(
        modifier = Modifier.size(diameter),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val s = size.minDimension
            val r = s / 2f

            val arcRect = androidx.compose.ui.geometry.Rect(
                left = center.x - r + strokePx,
                top = center.y - r + strokePx,
                right = center.x + r - strokePx,
                bottom = center.y + r - strokePx
            )

            drawArc(
                color = ringColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(arcRect.left, arcRect.top),
                size = Size(arcRect.width, arcRect.height),
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Round
                )
            )

            val path = Path().apply {
                val w = arcRect.width
                val h = arcRect.height
                val l = arcRect.left
                val t = arcRect.top
                moveTo(l + 0.28f * w, t + 0.55f * h)
                lineTo(l + 0.45f * w, t + 0.72f * h)
                lineTo(l + 0.74f * w, t + 0.36f * h)
            }

            val measure = PathMeasure().apply { setPath(path, false) }
            val segment = Path()
            val eased = progress.value * progress.value * (3f - 2f * progress.value)
            val ok = measure.getSegment(
                0f,
                measure.length * eased,
                segment,
                true
            )

            if (ok) {
                drawPath(
                    path = segment,
                    color = successColor,
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}

/**
 * Animated cross icon used in the error step.
 *
 * @param diameter Size of the circular container.
 * @param stroke Stroke width of the cross and ring.
 */
@Composable
private fun AnimatedCross(
    diameter: Dp,
    stroke: Dp = 6.dp
) {
    val errorColor = Color(0xFFe74c3c)
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1030,
                easing = FastOutSlowInEasing
            )
        )
    }

    val ringColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

    Box(
        modifier = Modifier.size(diameter),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val s = size.minDimension
            val r = s / 2f

            val arcRect = androidx.compose.ui.geometry.Rect(
                left = center.x - r + strokePx,
                top = center.y - r + strokePx,
                right = center.x + r - strokePx,
                bottom = center.y + r - strokePx
            )

            drawArc(
                color = ringColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(arcRect.left, arcRect.top),
                size = Size(arcRect.width, arcRect.height),
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Round
                )
            )

            val path1 = Path().apply {
                val w = arcRect.width
                val h = arcRect.height
                val l = arcRect.left
                val t = arcRect.top
                moveTo(l + 0.32f * w, t + 0.32f * h)
                lineTo(l + 0.68f * w, t + 0.68f * h)
            }

            val path2 = Path().apply {
                val w = arcRect.width
                val h = arcRect.height
                val l = arcRect.left
                val t = arcRect.top
                moveTo(l + 0.68f * w, t + 0.32f * h)
                lineTo(l + 0.32f * w, t + 0.68f * h)
            }

            val measure1 = PathMeasure().apply { setPath(path1, false) }
            val measure2 = PathMeasure().apply { setPath(path2, false) }

            val tRaw = progress.value.coerceIn(0f, 1f)
            val t = tRaw * tRaw * (3f - 2f * tRaw)

            if (t > 0f) {
                val seg1 = Path()
                val part1 = (min(t, 0.5f) * 2f)
                val ok1 = measure1.getSegment(
                    0f,
                    measure1.length * part1,
                    seg1,
                    true
                )
                if (ok1 && part1 > 0f) {
                    drawPath(
                        path = seg1,
                        color = errorColor,
                        style = Stroke(
                            width = strokePx,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }

            if (t > 0.5f) {
                val seg2 = Path()
                val part2 = ((t - 0.5f).coerceIn(0f, 0.5f) * 2f)
                val ok2 = measure2.getSegment(
                    0f,
                    measure2.length * part2,
                    seg2,
                    true
                )
                if (ok2 && part2 > 0f) {
                    drawPath(
                        path = seg2,
                        color = errorColor,
                        style = Stroke(
                            width = strokePx,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun Dp.toPx(): Float {
    return with(LocalDensity.current) { this@toPx.toPx() }
}

/**
 * Steps used by [PasskeyRegistrationMockScreen].
 */
enum class PasskeyStep {
    Intro,
    AccountSelection,
    BiometricPrompt,
    Registering,
    Success,
    Error
}

/**
 * Preview for the passkey registration mock screen.
 */
@Preview(showBackground = true)
@Composable
private fun PasskeyRegistrationMockPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        PasskeyRegistrationMockScreen()
    }
}
