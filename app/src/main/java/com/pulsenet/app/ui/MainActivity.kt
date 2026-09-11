package com.pulsenet.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pulsenet.app.ui.home.HomeScreen
import com.pulsenet.app.ui.map.MeshMapScreen
import com.pulsenet.app.ui.messages.MessagesScreen
import com.pulsenet.app.ui.onboarding.OnboardingScreen
import com.pulsenet.app.ui.onboarding.OnboardingViewModel
import com.pulsenet.app.ui.sos.SOSScreen
import com.pulsenet.app.ui.theme.PulseNetTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            PulseNetTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PulseNetNavHost()
                }
            }
        }
    }
}

@Composable
private fun PulseNetNavHost(navController: NavHostController = rememberNavController()) {
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val isOnboardingComplete by onboardingViewModel.isOnboardingComplete.collectAsState(initial = null)

    // Wait for the DataStore-backed flag before picking a start destination —
    // NavHost's startDestination is fixed at first composition, so starting on a
    // stale default and correcting it later wouldn't actually navigate anywhere.
    val startDestination = isOnboardingComplete ?: return

    NavHost(
        navController = navController,
        startDestination = if (startDestination) Screen.Home.route else Screen.Onboarding.route
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                viewModel = onboardingViewModel,
                onFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSOS = { navController.navigate(Screen.SOS.route) },
                onNavigateToMessages = { navController.navigate(Screen.Messages.route) },
                onNavigateToMap = { navController.navigate(Screen.Map.route) }
            )
        }
        composable(Screen.SOS.route) {
            SOSScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Messages.route) {
            MessagesScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Map.route) {
            MeshMapScreen(onBack = { navController.popBackStack() })
        }
    }
}
