package com.example.navigation


import androidx.navigation.NamedNavArgument

sealed class AppScreen(
    val route: String,
    navArguments: List<NamedNavArgument> = emptyList(),
) {

    val name: String = route.appendArguments(navArguments)

    data object WrongPassword : AppScreen(
        route = "wrong_password",
    )

    data object PasskeyRegistration : AppScreen(
        route = "passkey_registration",
    )

    data object PasskeyPrompts : AppScreen(
        route = "passkey_prompts",
    )

    data object FAQ : AppScreen(
        route = "faq",
    )

    data object PassKeysMockDemo : AppScreen(
        route = "passkeys_mock_demo",
    )
}

/**
 * Appends named navigation arguments to the current string route representation.
 *
 * @param navArguments The list of named navigation arguments to be appended.
 * @return The modified string route representation with appended navigation arguments.
 */
private fun String.appendArguments(navArguments: List<NamedNavArgument>): String {
    // Filter out the mandatory arguments that do not have a default value
    val mandatoryArguments = navArguments.filter { it.argument.defaultValue == null }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(separator = "/", prefix = "/") { "{${it.name}}" }
        .orEmpty()
    // Filter out the optional arguments that have a default value
    val optionalArguments = navArguments.filter { it.argument.defaultValue != null }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(separator = "&", prefix = "?") { "${it.name}={${it.name}}" }
        .orEmpty()
    // Concatenate the mandatory arguments and optional arguments to the route string
    return "$this$mandatoryArguments$optionalArguments"
}