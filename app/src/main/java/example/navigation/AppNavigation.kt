package example.navigation

import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.navigation.AppComposeNavigator
import com.example.navigation.AppScreen
import example.PasskeyRegistrationMockScreen
import example.faq.PasskeysFaq
import example.passkeys.PasskeysActivity
import example.password.WrongPasswordScreen
import example.prompts.PasskeysPrompts
import example.registration.PasskeyRegistrationTwoPhase

fun NavGraphBuilder.appNavigation(
    composeNavigator: AppComposeNavigator,
) {
    composable(
        route = AppScreen.WrongPassword.name,
    ) {
        WrongPasswordScreen { screen ->
            composeNavigator.navigate(screen.name)
        }
    }
    composable(
        route = AppScreen.PasskeyRegistration.name,
    ) {
        PasskeyRegistrationTwoPhase { screen ->
            composeNavigator.navigate(screen.name)
        }
    }
    composable(
        route = AppScreen.PasskeyPrompts.name,
    ) {
        PasskeysPrompts { screen ->
            composeNavigator.navigate(screen.name)
        }
    }
    composable(
        route = AppScreen.FAQ.name,
    ) {
        PasskeysFaq { screen ->
            composeNavigator.navigate(screen.name)
        }
    }
    composable(
        route = AppScreen.PassKeysMockDemo.name,
    ) {
        val context = LocalContext.current

        PasskeyRegistrationMockScreen(
            onOpenPasskeysActivity = {
                val intent = Intent(context, PasskeysActivity::class.java)
                context.startActivity(intent)
            }
        )
    }
}