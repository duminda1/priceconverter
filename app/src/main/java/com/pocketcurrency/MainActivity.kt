package com.pocketcurrency

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.pocketcurrency.ui.PocketCurrencyNavGraph
import com.pocketcurrency.ui.theme.PocketCurrencyTheme
import com.pocketcurrency.viewmodel.MainViewModel
import com.pocketcurrency.viewmodel.MainSettingsViewModel
import com.pocketcurrency.viewmodel.ScanViewModel
import com.pocketcurrency.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val mainSettingsViewModel: MainSettingsViewModel by viewModels()
    private val scanViewModel: ScanViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PocketCurrencyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PocketCurrencyNavGraph(
                        mainViewModel = mainViewModel,
                        mainSettingsViewModel = mainSettingsViewModel,
                        scanViewModel = scanViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}
