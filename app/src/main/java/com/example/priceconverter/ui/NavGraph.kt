package com.example.priceconverter.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.priceconverter.ui.screen.MainScreen
import com.example.priceconverter.ui.screen.SettingsScreen
import com.example.priceconverter.viewmodel.MainViewModel
import com.example.priceconverter.viewmodel.SettingsViewModel

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Settings : Screen("settings")
}

@Composable
fun PriceConverterNavGraph(
    modifier: Modifier = Modifier
) {
    val navController: NavHostController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

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
    }
}

