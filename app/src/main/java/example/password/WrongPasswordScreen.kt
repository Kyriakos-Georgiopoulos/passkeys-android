@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package example.password

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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import com.example.navigation.AppScreen
import kotlinx.coroutines.launch

/**
 * Full-screen local wrong-password demo flow.
 *
 * Features:
 * - Username and password fields centered on screen with keyboard insets handled via [imePadding].
 * - Toggle to show or hide password text (defaults to visible).
 * - Each failed submit triggers a shake animation, error styling, and a rotating witty quote.
 * - After 3 failed attempts, elements fall to the bottom and a restore CTA appears.
 * - Restore CTA shows a snackbar confirmation.
 * - Starting to type a new attempt clears the error and shows a rotating pep talk.
 *
 * @param modifier Optional [Modifier] for the root container.
 */
@Composable
fun WrongPasswordScreen(
    modifier: Modifier = Modifier,
    onNavigationEvent: (AppScreen) -> Unit
) {
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(true) }
    var attempts by remember { mutableStateOf(0) }
    var showError by remember { mutableStateOf(false) }
    var pepIndex by remember { mutableStateOf(0) }
    var pepChosenForAttempt by remember { mutableStateOf(false) }

    val lockedOut = attempts >= 3

    val shakeX = remember { Animatable(0f) }

    val titleDrop = remember { Animatable(0f) }
    val userDrop = remember { Animatable(0f) }
    val fieldDrop = remember { Animatable(0f) }
    val buttonDrop = remember { Animatable(0f) }
    val quoteDrop = remember { Animatable(0f) }

    val titleRotation = remember { Animatable(0f) }
    val userRotation = remember { Animatable(0f) }
    val fieldRotation = remember { Animatable(0f) }
    val buttonRotation = remember { Animatable(0f) }
    val quoteRotation = remember { Animatable(0f) }

    val quotes = listOf(
        "Nope. Not it.",
        "Close… in a parallel universe.",
        "That's not the droid you're looking for.",
        "Password rejected. The cake is a lie.",
        "Incorrect. But boldly done.",
        "Hint: there is no correct password here."
    )
    val pepTalks = listOf(
        "Come on, you can do it!",
        "This time you'll get it.",
        "You got this.",
        "Believe!",
        "Okay, now we're cooking."
    )
    val currentQuote = remember(attempts) {
        if (attempts == 0) "" else quotes[(attempts - 1) % quotes.size]
    }

    val isError = showError && !lockedOut
    val errorColor = MaterialTheme.colorScheme.error

    fun submit() {
        val next = attempts + 1
        if (next >= 3) focus.clearFocus()
        attempts = next
        if (next < 3) showError = true
        pepChosenForAttempt = false
    }

    LaunchedEffect(attempts) {
        if (attempts > 0 && !lockedOut) {
            shakeX.snapTo(0f)
            shakeX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 500
                    -24f at 60 with LinearOutSlowInEasing
                    24f at 120
                    -18f at 180
                    18f at 240
                    -10f at 300
                    10f at 360
                    0f at 500
                }
            )
        }
    }

    Scaffold { innerPadding ->
        ApplyStatusBar(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
            darkIcons = true
        )

        val bg = Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        )

        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(bg)
                .padding(innerPadding)
                .imePadding()
                .navigationBarsPadding()
        ) {
            val density = LocalDensity.current
            val groundPx = with(density) { (maxHeight - 224.dp).toPx() }

            LaunchedEffect(lockedOut) {
                if (lockedOut) {
                    fun bounceToGround(anim: Animatable<Float, AnimationVector1D>, target: Float) =
                        scope.launch {
                            anim.animateTo(
                                targetValue = target,
                                animationSpec = keyframes {
                                    durationMillis = 900
                                    (target + 80f) at 450
                                    (target - 18f) at 720
                                    target at 900
                                }
                            )
                        }
                    bounceToGround(titleDrop, groundPx)
                    scope.launch { titleRotation.animateTo(18f) }
                    scope.launch {
                        kotlinx.coroutines.delay(80)
                        bounceToGround(userDrop, groundPx)
                    }
                    scope.launch { userRotation.animateTo(-6f) }
                    scope.launch {
                        kotlinx.coroutines.delay(160)
                        bounceToGround(fieldDrop, groundPx)
                    }
                    scope.launch { fieldRotation.animateTo(-12f) }
                    scope.launch {
                        kotlinx.coroutines.delay(240)
                        bounceToGround(buttonDrop, groundPx)
                    }
                    scope.launch { buttonRotation.animateTo(24f) }
                    scope.launch {
                        kotlinx.coroutines.delay(320)
                        bounceToGround(quoteDrop, groundPx)
                    }
                    scope.launch { quoteRotation.animateTo(-28f) }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .offset(x = shakeX.value.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Welcome back",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.dropTransform(titleDrop.value, titleRotation.value)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Sign in to continue",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .dropTransform(titleDrop.value, titleRotation.value)
                        .alpha(0.8f)
                )
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .dropTransform(userDrop.value, userRotation.value)
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (!lockedOut) {
                            if (showError && it.isNotEmpty()) {
                                showError = false
                                if (attempts > 0 && !pepChosenForAttempt) {
                                    pepIndex = (pepIndex + 1) % pepTalks.size
                                    pepChosenForAttempt = true
                                }
                            }
                            if (it.isEmpty()) {
                                pepChosenForAttempt = false
                            }
                        }
                    },
                    label = { Text("Password") },
                    isError = isError,
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    supportingText = {
                        when {
                            lockedOut -> {}
                            isError -> Text("Wrong password.")
                            attempts > 0 && password.isNotEmpty() -> Text(pepTalks[pepIndex])
                        }
                    },
                    colors = if (isError) {
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = errorColor,
                            unfocusedBorderColor = errorColor.copy(alpha = 0.6f),
                            cursorColor = errorColor,
                            focusedLabelColor = errorColor
                        )
                    } else OutlinedTextFieldDefaults.colors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .dropTransform(fieldDrop.value, fieldRotation.value)
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { submit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .dropTransform(buttonDrop.value, buttonRotation.value),
                    enabled = !lockedOut
                ) { Text("Sign in") }

                if (attempts > 0 && (showError || lockedOut)) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = currentQuote,
                        color = if (!lockedOut) errorColor else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .dropTransform(quoteDrop.value, quoteRotation.value)
                            .padding(horizontal = 8.dp)
                    )
                }
                if (!lockedOut) {
                    Spacer(Modifier.height(64.dp))
                }
            }

            AnimatedVisibility(
                visible = lockedOut,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(top = 48.dp, start = 24.dp, end = 24.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Too many attempts",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Please restore your password via email.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.alpha(0.9f)
                    )
                    Spacer(Modifier.height(12.dp))

                    FilledTonalButton(onClick = {
                        onNavigationEvent(AppScreen.PasskeyRegistration)
                    }) {
                        Text("Restore via email")
                    }
                }
            }
        }
    }
}

/**
 * Applies a vertical drop translation and z-rotation to the receiving [Modifier].
 *
 * @param drop Translation on the Y axis in pixels.
 * @param rotation Rotation around the Z axis in degrees.
 * @return A [Modifier] with the applied graphics transform.
 */
private fun Modifier.dropTransform(drop: Float, rotation: Float): Modifier =
    this.then(
        Modifier.graphicsLayer(
            translationY = drop,
            rotationZ = rotation
        )
    )

/**
 * Preview for [WrongPasswordScreen].
 */
@Preview(showBackground = true)
@Composable
private fun WrongPasswordScreenPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        WrongPasswordScreen(
            modifier = Modifier.fillMaxSize()
        ) { _ ->
            // no-op
        }
    }
}

/**
 * Applies a status bar color and light/dark icon appearance for the hosting [android.app.Activity].
 *
 * @param color The desired status bar color.
 * @param darkIcons Whether status bar icons should appear dark (true) or light (false).
 */
@Composable
private fun ApplyStatusBar(color: Color, darkIcons: Boolean) {
    val view = LocalView.current
    SideEffect {
        val activity = view.context as? android.app.Activity ?: return@SideEffect
        activity.window.statusBarColor = color.toArgb()
        WindowInsetsControllerCompat(activity.window, activity.window.decorView)
            .isAppearanceLightStatusBars = darkIcons
    }
}
