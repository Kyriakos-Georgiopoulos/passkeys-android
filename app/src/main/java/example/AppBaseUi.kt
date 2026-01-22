package example

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.example.navigation.AppComposeNavigator
import example.navigation.AppNavigationHost

@Composable
fun AppBaseUi(
    composeNavigator: AppComposeNavigator,
) {
    MaterialTheme {
        val navHostController = rememberNavController()

        LaunchedEffect(Unit) {
            composeNavigator.handleNavigationCommands(navHostController)
        }

        AppNavigationHost(
            navHostController = navHostController,
            composeNavigator = composeNavigator,
        )
    }
}