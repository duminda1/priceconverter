package com.pocketcurrency.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pocketcurrency.ui.screen.MainScreen
import com.pocketcurrency.ui.screen.SettingsScreen
import com.pocketcurrency.ui.screen.HelpScreen
import com.pocketcurrency.viewmodel.MainViewModel
import com.pocketcurrency.viewmodel.MainViewModelFactory
import com.pocketcurrency.viewmodel.SettingsViewModel
import com.pocketcurrency.viewmodel.SettingsViewModelFactory

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Settings : Screen("settings")
    object Help : Screen("help")
}

@Composable
fun PocketCurrencyNavGraph(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val mainViewModelFactory = remember(application) { MainViewModelFactory(application) }
    val settingsViewModelFactory = remember(application) { SettingsViewModelFactory(application) }
    val navController: NavHostController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel(factory = mainViewModelFactory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = settingsViewModelFactory)

    NavHost(
        navController = navController,
        startDestination = Screen.Main.route,
        modifier = modifier
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                viewModel = mainViewModel,
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
