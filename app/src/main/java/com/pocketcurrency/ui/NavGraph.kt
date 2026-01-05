package com.pocketcurrency.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pocketcurrency.ui.screen.MainScreen
import com.pocketcurrency.ui.screen.SettingsScreen
import com.pocketcurrency.ui.screen.HelpScreen
import com.pocketcurrency.viewmodel.MainViewModel
import com.pocketcurrency.viewmodel.MainSettingsViewModel
import com.pocketcurrency.viewmodel.ScanViewModel
import com.pocketcurrency.viewmodel.SettingsViewModel

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Settings : Screen("settings")
    object Help : Screen("help")
}

@Composable
fun PocketCurrencyNavGraph(
    mainViewModel: MainViewModel,
    mainSettingsViewModel: MainSettingsViewModel,
    scanViewModel: ScanViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val navController: NavHostController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Main.route,
        modifier = modifier
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                conversionViewModel = mainViewModel,
                settingsViewModel = mainSettingsViewModel,
                scanViewModel = scanViewModel,
                navController = navController // pass navController
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                navController = navController // pass navController
            )
        }
        composable(Screen.Help.route) {
            HelpScreen(navController = navController)
        }
    }
}
