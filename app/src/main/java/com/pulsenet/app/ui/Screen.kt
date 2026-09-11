package com.pulsenet.app.ui

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object SOS : Screen("sos")
    data object Messages : Screen("messages")
    data object Map : Screen("map")
}
