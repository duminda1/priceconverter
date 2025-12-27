package com.pocketcurrency.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.onFocusChanged
import androidx.navigation.NavHostController
import com.pocketcurrency.data.model.CurrencyPairRate
import com.pocketcurrency.ui.Screen
import com.pocketcurrency.utils.Constants
import com.pocketcurrency.viewmodel.SettingsViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    navController: NavHostController
) {
    val uiState = viewModel.uiState.collectAsState().value
    val serviceExpanded = remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val savedFrom = remember { mutableStateOf("") }
    val savedTo = remember { mutableStateOf("") }
    val editingSaved = remember { mutableStateOf<CurrencyPairRate?>(null) }
    val manualFrom = remember { mutableStateOf("") }
    val manualTo = remember { mutableStateOf("") }
    val manualRate = remember { mutableStateOf("") }
    val editingManual = remember { mutableStateOf<CurrencyPairRate?>(null) }
    val homeInput = remember(uiState.homeCurrency) { mutableStateOf(uiState.homeCurrency) }
    val destinationInput = remember(uiState.destinationCurrency, uiState.destinationAuto) {
        mutableStateOf(uiState.destinationCurrency)
    }
    val selectedProvider = uiState.providers.firstOrNull { it.id == uiState.service }
    val frankfurterLabel = "Daily updates (recommended)"
    val advancedLabel = "Advanced: Custom API"
    val advancedSubtitle = "For advanced users who need real-time updates"
    val selectedProviderLabel = selectedProvider?.let { provider ->
        when (provider.id) {
            Constants.PROVIDER_FRANKFURTER -> frankfurterLabel
            Constants.PROVIDER_EXCHANGE_RATES -> advancedLabel
            else -> provider.displayName
        }
    } ?: uiState.service
    val sortedSavedRates = remember(uiState.savedRates) {
        uiState.savedRates.sortedWith(
            compareByDescending<CurrencyPairRate> { it.lastUpdatedMillis }
                .thenBy { "${it.from}-${it.to}" }
        )
    }
    val sortedManualRates = remember(uiState.manualRates) {
        uiState.manualRates.sortedWith(
            compareByDescending<CurrencyPairRate> { it.lastUpdatedMillis }
                .thenBy { "${it.from}-${it.to}" }
        )
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.refreshState()
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.background
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    TextButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Screen.Help.route) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Help & Support", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Learn how PocketCurrency works and troubleshoot common issues.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "Open",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text("Currency defaults", style = MaterialTheme.typography.titleLarge)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Home & destination", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Home currency is used for the default \"To\" value. Destination currency is the default \"From\" value.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = homeInput.value,
                        onValueChange = { value ->
                            homeInput.value = value.uppercase()
                            if (homeInput.value.isNotBlank()) {
                                viewModel.setHomeCurrency(homeInput.value)
                            }
                        },
                        label = { Text("Home currency") },
                        placeholder = { Text("e.g. USD") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { state ->
                                if (!state.isFocused && homeInput.value.isBlank()) {
                                    homeInput.value = uiState.homeCurrency
                                }
                            },
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-detect destination", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Uses your current country when enabled.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.destinationAuto,
                            onCheckedChange = viewModel::setDestinationAuto
                        )
                    }
                    OutlinedTextField(
                        value = destinationInput.value,
                        onValueChange = { value ->
                            destinationInput.value = value.uppercase()
                            if (!uiState.destinationAuto && destinationInput.value.isNotBlank()) {
                                viewModel.setDestinationCurrency(destinationInput.value)
                            }
                        },
                        label = { Text("Destination currency") },
                        placeholder = { Text("e.g. EUR") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { state ->
                                if (!state.isFocused && destinationInput.value.isBlank()) {
                                    destinationInput.value = uiState.destinationCurrency
                                }
                            },
                        singleLine = true,
                        enabled = !uiState.destinationAuto
                    )
                }
            }

            Text("Rate service", style = MaterialTheme.typography.titleLarge)

            var expanded by remember { mutableStateOf(false) }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedProviderLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Provider") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        uiState.providers.forEach { provider ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            when (provider.id) {
                                                Constants.PROVIDER_FRANKFURTER -> frankfurterLabel
                                                Constants.PROVIDER_EXCHANGE_RATES -> advancedLabel
                                                else -> provider.displayName
                                            }
                                        )
                                        if (provider.id == Constants.PROVIDER_EXCHANGE_RATES) {
                                            Text(
                                                advancedSubtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.setService(provider.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                // Friendly provider summary keeps choices simple for travellers.
                val providerDescription = if (uiState.service == Constants.PROVIDER_EXCHANGE_RATES) {
                    "For advanced users who need real-time updates"
                } else {
                    "Daily rates · No setup · Works offline"
                }
                Text(
                    providerDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Advanced options stay tucked away unless explicitly chosen.
            if (uiState.service == Constants.PROVIDER_EXCHANGE_RATES) {
                Text(
                    "Advanced",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "API access",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = uiState.apiKeyInput,
                            onValueChange = viewModel::onApiKeyChanged,
                            label = { Text("API key") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (uiState.isApiKeyVerified) "Key verified" else "Key not verified",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = viewModel::verifyAndSaveApiKey,
                                enabled = !uiState.isVerifying,
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(if (uiState.isVerifying) "Verifying..." else "Verify & Save")
                            }
                        }
                        uiState.apiKeyStatus?.let { message ->
                            Text(
                                message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Plan & usage",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Free plan (100 requests/month)")
                                Text(
                                    if (uiState.isFreePlan) "Alerts enabled" else "Alerts disabled",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.isFreePlan,
                                onCheckedChange = viewModel::setFreePlan
                            )
                        }
                        val usageProgress =
                            (uiState.usageCount.coerceAtMost(Constants.FREE_PLAN_LIMIT)).toFloat() /
                                Constants.FREE_PLAN_LIMIT
                        LinearProgressIndicator(
                            progress = usageProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Usage ${uiState.usageCount}/${Constants.FREE_PLAN_LIMIT} • ${uiState.usageMonth}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        uiState.usageWarning?.let { warning ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Usage alert", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        warning,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(
                                        onClick = viewModel::clearUsageWarning,
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Scan prices with camera", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Disable scanning and use manual entry only.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isLiveScanEnabled,
                        onCheckedChange = viewModel::setLiveScanEnabled
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Saved for offline use", style = MaterialTheme.typography.titleLarge)
                TextButton(
                    onClick = viewModel::refreshSavedRates,
                    enabled = uiState.savedRates.isNotEmpty() && !uiState.isRefreshingSavedRates,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(if (uiState.isRefreshingSavedRates) "Refreshing..." else "Refresh all")
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Add or refresh a pair", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = savedFrom.value,
                            onValueChange = { savedFrom.value = it },
                            label = { Text("From") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = savedTo.value,
                            onValueChange = { savedTo.value = it },
                            label = { Text("To") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.fetchAndReplaceRate(
                                editingSaved.value,
                                savedFrom.value,
                                savedTo.value
                            )
                            savedFrom.value = ""
                            savedTo.value = ""
                            editingSaved.value = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(if (editingSaved.value == null) "Fetch & Save" else "Update pair")
                    }
                }
            }

            uiState.actionStatus?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.savedRates.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        "No saved offline pairs yet.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                sortedSavedRates.forEach { rate ->
                    RateRow(
                        title = "${rate.from} → ${rate.to}",
                        subtitle = "Rate ${rate.rate} • ${formatTimestamp(rate.lastUpdatedMillis)}",
                        onPrimary = {
                            savedFrom.value = rate.from
                            savedTo.value = rate.to
                            editingSaved.value = rate
                        },
                        primaryLabel = "Edit",
                        onSecondary = { viewModel.removeSavedRate(rate.from, rate.to) },
                        secondaryLabel = "Remove"
                    )
                }
            }

            val manualFromNeedsHelp = shouldShowCurrencyCodeHelp(manualFrom.value)
            val manualToNeedsHelp = shouldShowCurrencyCodeHelp(manualTo.value)

            Text("Offline rates", style = MaterialTheme.typography.titleLarge)
            Text(
                "Offline rates are ideal when travelling without internet access.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        if (editingManual.value == null) "Add offline rate" else "Edit offline rate",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = manualFrom.value,
                            onValueChange = { manualFrom.value = it.uppercase() },
                            label = { Text("From") },
                            modifier = Modifier.weight(1f),
                            supportingText = {
                                if (manualFromNeedsHelp) {
                                    Text("Use a 3-letter currency code (e.g. USD)")
                                }
                            }
                        )
                        OutlinedTextField(
                            value = manualTo.value,
                            onValueChange = { manualTo.value = it.uppercase() },
                            label = { Text("To") },
                            modifier = Modifier.weight(1f),
                            supportingText = {
                                if (manualToNeedsHelp) {
                                    Text("Use a 3-letter currency code (e.g. USD)")
                                }
                            }
                        )
                    }
                    OutlinedTextField(
                        value = manualRate.value,
                        onValueChange = { manualRate.value = it },
                        label = { Text("Rate") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val rateValue = manualRate.value.toDoubleOrNull()
                            if (rateValue != null) {
                                viewModel.upsertManualRate(
                                    manualFrom.value,
                                    manualTo.value,
                                    rateValue
                                )
                                manualFrom.value = ""
                                manualTo.value = ""
                                manualRate.value = ""
                                editingManual.value = null
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(if (editingManual.value == null) "Save rate" else "Update rate")
                    }
                }
            }

            if (uiState.manualRates.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        "No offline rates yet.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                sortedManualRates.forEach { rate ->
                    RateRow(
                        title = "${rate.from} → ${rate.to}",
                        subtitle = "Rate ${rate.rate} • ${formatTimestamp(rate.lastUpdatedMillis)}",
                        onPrimary = {
                            manualFrom.value = rate.from
                            manualTo.value = rate.to
                            manualRate.value = rate.rate.toString()
                            editingManual.value = rate
                        },
                        primaryLabel = "Edit",
                        onSecondary = { viewModel.removeManualRate(rate.from, rate.to) },
                        secondaryLabel = "Remove"
                    )
                }
            }
        }
    }
}

@Composable
private fun RateRow(
    title: String,
    subtitle: String,
    onPrimary: () -> Unit,
    primaryLabel: String,
    onSecondary: () -> Unit,
    secondaryLabel: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onPrimary,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(primaryLabel)
                }
                TextButton(
                    onClick = onSecondary,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(secondaryLabel)
                }
            }
        }
    }
}

private fun shouldShowCurrencyCodeHelp(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return false
    if (trimmed.length != 3) return true
    return trimmed.any { !it.isLetter() }
}

private fun formatTimestamp(timestampMillis: Long): String {
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(timestampMillis))
}
