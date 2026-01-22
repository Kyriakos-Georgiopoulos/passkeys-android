package com.example.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onSubscription

/**
 * Abstract base class representing a navigator for navigation within the application.
 *
 * Subclasses of this navigator can handle navigation commands and provide navigation functionality.
 */
abstract class Navigator {

    /**
     * A shared flow that emits navigation commands to be processed by the navigator.
     *
     * ViewModels or other components can emit navigation commands to trigger navigation actions.
     */
    val navigationCommands = MutableSharedFlow<NavigationCommand>(extraBufferCapacity = Int.MAX_VALUE)

    /**
     * A state flow that holds the current instance of the NavController associated with the navigator.
     *
     * It allows ViewModels to observe navigation results and access the NavController instance.
     */
    val navControllerFlow = MutableStateFlow<NavController?>(null)

    /**
     * Navigates up in the navigation hierarchy.
     *
     * Emits a [NavigationCommand.NavigateUp] command to trigger the navigation action.
     */
    fun navigateUp() {
        navigationCommands.tryEmit(NavigationCommand.NavigateUp)
    }
}

/**
 * Abstract base class representing a navigator for navigation within the application using Jetpack Compose.
 *
 * Subclasses of this navigator can handle navigation commands and provide navigation functionality specific to Compose.
 */
abstract class AppComposeNavigator : Navigator() {

    /**
     * Navigates to the specified route with optional navigation options.
     *
     * @param route The destination route to navigate to.
     * @param optionsBuilder Optional builder function to configure additional navigation options.
     */
    abstract fun navigate(
        route: String,
        optionsBuilder: (NavOptionsBuilder.() -> Unit)? = null,
    )

    /**
     * Navigates back to a previous destination with a result.
     *
     * @param key The key to associate with the navigation result.
     * @param result The result value to be passed back to the previous destination.
     * @param route Optional route specifying the destination to navigate back to. If null, the default behavior
     * is to navigate up in the navigation hierarchy.
     */
    abstract fun <T> navigateBackWithResult(
        key: String,
        result: T,
        route: String?,
    )

    /**
     * Pops the back stack up to the specified route.
     *
     * @param route The destination route to pop the back stack up to.
     * @param inclusive Specifies whether the destination route should be included in the pop operation.
     */
    abstract fun popUpTo(
        route: String,
        inclusive: Boolean,
    )

    /**
     * Navigates to the specified route and clears the back stack, removing all previous destinations.
     *
     * @param route The destination route to navigate to and clear the back stack.
     */
    abstract fun navigateAndClearBackStack(route: String)

    /**
     * Handles the navigation commands emitted by the [navigationCommands] flow.
     *
     * @param navController The NavController instance to handle the navigation commands.
     */
    suspend fun handleNavigationCommands(navController: NavController) {
        navigationCommands
            .onSubscription { this@AppComposeNavigator.navControllerFlow.value = navController }
            .onCompletion { this@AppComposeNavigator.navControllerFlow.value = null }
            .collect { navController.handleComposeNavigationCommand(it) }
    }

    /**
     * Handles a Compose-specific navigation command.
     *
     * @param navigationCommand The navigation command to handle.
     */
    private fun NavController.handleComposeNavigationCommand(navigationCommand: NavigationCommand) {
        when (navigationCommand) {
            is ComposeNavigationCommand.NavigateToRoute -> {
                navigate(navigationCommand.route, navigationCommand.options)
            }

            NavigationCommand.NavigateUp -> navigateUp()
            is ComposeNavigationCommand.PopUpToRoute -> popBackStack(
                navigationCommand.route,
                navigationCommand.inclusive
            )

            is ComposeNavigationCommand.NavigateUpWithResult<*> -> {
                navUpWithResult(navigationCommand)
            }
        }
    }

    /**
     * Handles the navigation command to navigate up with a result.
     *
     * @param navigationCommand The navigation command to handle.
     */
    private fun NavController.navUpWithResult(
        navigationCommand: ComposeNavigationCommand.NavigateUpWithResult<*>,
    ) {
        val backStackEntry =
            navigationCommand.route?.let { getBackStackEntry(it) }
                ?: previousBackStackEntry
        backStackEntry?.savedStateHandle?.set(
            navigationCommand.key,
            navigationCommand.result
        )

        navigationCommand.route?.let {
            popBackStack(it, false)
        } ?: run {
            navigateUp()
        }
    }
}