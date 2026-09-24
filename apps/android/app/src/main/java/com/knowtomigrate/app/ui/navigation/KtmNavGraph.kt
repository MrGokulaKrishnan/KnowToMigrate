package com.knowtomigrate.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
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
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.Home.route) { HomeScreen(navController, sharedUris) }
        composable(Screen.Send.route) { SendScreen(navController) }
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
