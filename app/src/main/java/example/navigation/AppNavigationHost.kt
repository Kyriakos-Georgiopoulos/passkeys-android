package example.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.navigation.AppComposeNavigator
import com.example.navigation.AppScreen

@Composable
fun AppNavigationHost(
    navHostController: NavHostController,
    composeNavigator: AppComposeNavigator,
) {
    NavHost(
        navController = navHostController,
        startDestination = AppScreen.WrongPassword.name,
    ) {
        appNavigation(
            composeNavigator = composeNavigator,
        )
    }
}