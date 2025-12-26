package com.example.priceconverter.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.priceconverter.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel,
                   navController: NavHostController
) {
    val currencies by viewModel.selectedCurrencies.collectAsState()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
        Text("Selected Currencies", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        currencies.forEach { currency ->
            Text(currency)
        }
    }

    Button(onClick = { navController.popBackStack() }) {
    Text("Back")
    }
}

