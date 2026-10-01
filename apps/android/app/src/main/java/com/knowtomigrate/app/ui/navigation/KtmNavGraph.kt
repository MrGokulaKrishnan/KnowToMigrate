package com.knowtomigrate.app.ui.navigation

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.*
import androidx.navigation.compose.*
import com.knowtomigrate.app.ui.screens.*

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Send : Screen("send")
    object Receive : Screen("receive")
    object Transfer : Screen("transfer/{sessionId}") {
        fun withSession(id: String) = "transfer/$id"
    }
    object Migration : Screen("migration")
    object History : Screen("history")
    object Settings : Screen("settings")
}

@Composable
fun KtmNavGraph(sharedUris: List<Uri> = emptyList()) {
    val navController = rememberNavController()

    // When launched via Android Sharesheet with shared files, open immediately into Launch Transfer screen
    val startDest = if (sharedUris.isNotEmpty()) Screen.Send.route else Screen.Splash.route

    // If shared files arrive while app is already running, navigate immediately to Launch Transfer
    LaunchedEffect(sharedUris) {
        if (sharedUris.isNotEmpty()) {
            navController.navigate(Screen.Send.route) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        composable(
            route = Screen.Splash.route,
            exitTransition = {
                fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing))
            }
        ) {
            SplashScreen(navController)
        }
        composable(
            route = Screen.Home.route,
            enterTransition = {
                fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(280, easing = EaseOutCubic)
                )
            }
        ) {
            HomeScreen(navController, sharedUris)
        }
        composable(Screen.Send.route) {
            SendScreen(navController, initialUris = sharedUris)
        }
        composable(Screen.Receive.route) { ReceiveScreen(navController) }
        composable(Screen.Transfer.route) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            TransferScreen(navController, sessionId)
        }
        composable(Screen.Migration.route) { MigrationScreen(navController) }
        composable(Screen.History.route) { HistoryScreen(navController) }
        composable(Screen.Settings.route) { SettingsScreen(navController) }
    }
}
