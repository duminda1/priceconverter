package com.pocketcurrency.ui

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Settings : Screen("settings")
    object Help : Screen("help")
}
