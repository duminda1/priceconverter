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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.onFocusChanged
import androidx.navigation.NavHostController
import com.pocketcurrency.R
import com.pocketcurrency.data.model.CurrencyPairRate
import com.pocketcurrency.ui.Screen
import com.pocketcurrency.util.Constants
import com.pocketcurrency.viewmodel.SettingsViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.lifecycle.compose.LocalLifecycleOwner


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
    val manualFromError = remember { mutableStateOf<String?>(null) }
    val manualToError = remember { mutableStateOf<String?>(null) }
    val manualRateError = remember { mutableStateOf<String?>(null) }
    val editingManual = remember { mutableStateOf<CurrencyPairRate?>(null) }
    val homeInput = remember(uiState.homeCurrency) { mutableStateOf(uiState.homeCurrency) }
    val destinationInput = remember(uiState.destinationCurrency, uiState.destinationAuto) {
        mutableStateOf(uiState.destinationCurrency)
    }
    val uriHandler = LocalUriHandler.current
    val selectedProvider = uiState.providers.firstOrNull { it.id == uiState.service }
    val frankfurterLabel = stringResource(R.string.settings_provider_frankfurter_label)
    val advancedLabel = stringResource(R.string.settings_provider_advanced_label)
    val advancedSubtitle = stringResource(R.string.settings_provider_advanced_subtitle)
    val apiSignupUrl = stringResource(R.string.settings_api_signup_url)
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
                title = { Text(stringResource(R.string.action_settings)) },
                navigationIcon = {
                    TextButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(R.string.action_back))
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
                        Text(
                            stringResource(R.string.settings_help_support_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            stringResource(R.string.settings_help_support_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        stringResource(R.string.settings_open),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                stringResource(R.string.settings_currency_defaults_title),
                style = MaterialTheme.typography.titleLarge
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
                        stringResource(R.string.settings_home_destination_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        stringResource(R.string.settings_home_destination_description),
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
                        label = { Text(stringResource(R.string.settings_home_currency_label)) },
                        placeholder = { Text(stringResource(R.string.settings_home_currency_placeholder)) },
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
                            Text(
                                stringResource(R.string.settings_auto_destination_title),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                stringResource(R.string.settings_auto_destination_description),
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
                        label = { Text(stringResource(R.string.settings_destination_currency_label)) },
                        placeholder = { Text(stringResource(R.string.settings_destination_currency_placeholder)) },
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

            Text(
                stringResource(R.string.settings_rate_service_title),
                style = MaterialTheme.typography.titleLarge
            )

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
                        label = { Text(stringResource(R.string.settings_provider_label)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                    stringResource(R.string.settings_provider_description_advanced)
                } else {
                    stringResource(R.string.settings_provider_description_default)
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
                    stringResource(R.string.settings_advanced_title),
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
                            stringResource(R.string.settings_api_access_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!uiState.hasSavedApiKey) {
                            Text(
                                stringResource(R.string.settings_no_api_key_saved),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = { uriHandler.openUri(apiSignupUrl) },
                                modifier = Modifier.heightIn(min = 40.dp)
                            ) {
                                Text(apiSignupUrl)
                            }
                        }
                        OutlinedTextField(
                            value = uiState.apiKeyInput,
                            onValueChange = viewModel::onApiKeyChanged,
                            label = { Text(stringResource(R.string.settings_api_key_label)) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (uiState.isApiKeyVerified) {
                                    stringResource(R.string.settings_api_key_verified)
                                } else {
                                    stringResource(R.string.settings_api_key_not_verified)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = viewModel::verifyAndSaveApiKey,
                                enabled = !uiState.isVerifying,
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(
                                    if (uiState.isVerifying) {
                                        stringResource(R.string.settings_api_key_verifying)
                                    } else {
                                        stringResource(R.string.settings_api_key_verify_button)
                                    }
                                )
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
                            stringResource(R.string.settings_plan_usage_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    stringResource(
                                        R.string.settings_free_plan_label,
                                        Constants.FREE_PLAN_LIMIT.toString()
                                    )
                                )
                                Text(
                                    if (uiState.isFreePlan) {
                                        stringResource(R.string.settings_alerts_enabled)
                                    } else {
                                        stringResource(R.string.settings_alerts_disabled)
                                    },
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
                            progress = { usageProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            stringResource(
                                R.string.settings_usage_format,
                                uiState.usageCount.toString(),
                                Constants.FREE_PLAN_LIMIT.toString(),
                                uiState.usageMonth
                            ),
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
                                    Text(
                                        stringResource(R.string.settings_usage_alert_title),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        warning,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(
                                        onClick = viewModel::clearUsageWarning,
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Text(stringResource(R.string.settings_dismiss))
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
                        Text(
                            stringResource(R.string.settings_scan_camera_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            stringResource(R.string.settings_scan_camera_subtitle),
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
                Text(
                    stringResource(R.string.settings_saved_offline_title),
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(
                    onClick = viewModel::refreshSavedRates,
                    enabled = uiState.savedRates.isNotEmpty() && !uiState.isRefreshingSavedRates,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        if (uiState.isRefreshingSavedRates) {
                            stringResource(R.string.settings_refreshing)
                        } else {
                            stringResource(R.string.settings_refresh_all)
                        }
                    )
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
                    Text(
                        stringResource(R.string.settings_saved_pair_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = savedFrom.value,
                            onValueChange = { savedFrom.value = it.uppercase() },
                            label = { Text(stringResource(R.string.main_currency_from_label)) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = savedTo.value,
                            onValueChange = { savedTo.value = it.uppercase() },
                            label = { Text(stringResource(R.string.main_currency_to_label)) },
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
                        Text(
                            if (editingSaved.value == null) {
                                stringResource(R.string.settings_fetch_save)
                            } else {
                                stringResource(R.string.settings_update_pair)
                            }
                        )
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
                        stringResource(R.string.settings_no_saved_pairs),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                sortedSavedRates.forEach { rate ->
                    RateRow(
                        title = stringResource(
                            R.string.settings_rate_row_title,
                            rate.from,
                            rate.to
                        ),
                        subtitle = stringResource(
                            R.string.settings_rate_row_subtitle,
                            rate.rate,
                            formatTimestamp(rate.lastUpdatedMillis)
                        ),
                        onPrimary = {
                            savedFrom.value = rate.from
                            savedTo.value = rate.to
                            editingSaved.value = rate
                        },
                        primaryLabel = stringResource(R.string.settings_rate_row_edit),
                        onSecondary = { viewModel.removeSavedRate(rate.from, rate.to) },
                        secondaryLabel = stringResource(R.string.settings_rate_row_remove)
                    )
                }
            }

            val manualFromNeedsHelp = shouldShowCurrencyCodeHelp(manualFrom.value)
            val manualToNeedsHelp = shouldShowCurrencyCodeHelp(manualTo.value)
            val invalidCurrencyMessage = stringResource(R.string.error_invalid_currency_codes)
            val currencyHelpMessage = stringResource(R.string.settings_currency_code_help)
            val invalidManualRateMessage = stringResource(R.string.error_invalid_manual_rate)

            Text(
                stringResource(R.string.settings_offline_rates_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                stringResource(R.string.settings_offline_rates_description),
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
                        if (editingManual.value == null) {
                            stringResource(R.string.settings_add_offline_rate)
                        } else {
                            stringResource(R.string.settings_edit_offline_rate)
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = manualFrom.value,
                            onValueChange = {
                                manualFrom.value = it.uppercase()
                                if (manualFromError.value != null) {
                                    manualFromError.value = null
                                }
                            },
                            label = { Text(stringResource(R.string.main_currency_from_label)) },
                            modifier = Modifier.weight(1f),
                            isError = manualFromError.value != null,
                            supportingText = {
                                val error = manualFromError.value
                                when {
                                    error != null -> Text(error)
                                    manualFromNeedsHelp -> Text(currencyHelpMessage)
                                }
                            }
                        )
                        OutlinedTextField(
                            value = manualTo.value,
                            onValueChange = {
                                manualTo.value = it.uppercase()
                                if (manualToError.value != null) {
                                    manualToError.value = null
                                }
                            },
                            label = { Text(stringResource(R.string.main_currency_to_label)) },
                            modifier = Modifier.weight(1f),
                            isError = manualToError.value != null,
                            supportingText = {
                                val error = manualToError.value
                                when {
                                    error != null -> Text(error)
                                    manualToNeedsHelp -> Text(currencyHelpMessage)
                                }
                            }
                        )
                    }
                    OutlinedTextField(
                        value = manualRate.value,
                        onValueChange = {
                            manualRate.value = it
                            if (manualRateError.value != null) {
                                manualRateError.value = null
                            }
                        },
                        label = { Text(stringResource(R.string.settings_rate_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = manualRateError.value != null,
                        supportingText = {
                            manualRateError.value?.let { Text(it) }
                        }
                    )
                    Button(
                        onClick = {
                            val validation = validateManualRateInput(
                                manualFrom.value,
                                manualTo.value,
                                manualRate.value,
                                invalidCurrencyMessage,
                                currencyHelpMessage,
                                invalidManualRateMessage
                            )

                            manualFromError.value = validation.fromError
                            manualToError.value = validation.toError
                            manualRateError.value = validation.rateError

                            if (!validation.isValid) {
                                return@Button
                            }

                            editingManual.value?.let { original ->
                                if (shouldRemoveOriginalManualRate(
                                        original,
                                        validation.normalizedFrom,
                                        validation.normalizedTo
                                    )
                                ) {
                                    viewModel.removeManualRate(original.from, original.to)
                                }
                            }
                            val validatedRate = validation.rateValue ?: return@Button
                            viewModel.upsertManualRate(
                                validation.normalizedFrom,
                                validation.normalizedTo,
                                validatedRate
                            )
                            manualFrom.value = ""
                            manualTo.value = ""
                            manualRate.value = ""
                            manualFromError.value = null
                            manualToError.value = null
                            manualRateError.value = null
                            editingManual.value = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(
                            if (editingManual.value == null) {
                                stringResource(R.string.settings_save_rate)
                            } else {
                                stringResource(R.string.settings_update_rate)
                            }
                        )
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
                        stringResource(R.string.settings_no_offline_rates),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                sortedManualRates.forEach { rate ->
                    RateRow(
                        title = stringResource(
                            R.string.settings_rate_row_title,
                            rate.from,
                            rate.to
                        ),
                        subtitle = stringResource(
                            R.string.settings_rate_row_subtitle,
                            rate.rate,
                            formatTimestamp(rate.lastUpdatedMillis)
                        ),
                        onPrimary = {
                            manualFrom.value = rate.from
                            manualTo.value = rate.to
                            manualRate.value = rate.rate.toString()
                            editingManual.value = rate
                            manualFromError.value = null
                            manualToError.value = null
                            manualRateError.value = null
                        },
                        primaryLabel = stringResource(R.string.settings_rate_row_edit),
                        onSecondary = { viewModel.removeManualRate(rate.from, rate.to) },
                        secondaryLabel = stringResource(R.string.settings_rate_row_remove)
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

internal fun validateManualRateInput(
    from: String,
    to: String,
    rateInput: String,
    invalidCurrencyMessage: String,
    currencyHelpMessage: String,
    invalidManualRateMessage: String
): ManualRateValidationResult {
    val normalizedFrom = from.trim().uppercase()
    val normalizedTo = to.trim().uppercase()
    val rateValue = rateInput.trim().toDoubleOrNull()
    val fromError = when {
        normalizedFrom.isBlank() -> invalidCurrencyMessage
        shouldShowCurrencyCodeHelp(normalizedFrom) -> currencyHelpMessage
        else -> null
    }
    val toError = when {
        normalizedTo.isBlank() -> invalidCurrencyMessage
        shouldShowCurrencyCodeHelp(normalizedTo) -> currencyHelpMessage
        else -> null
    }
    val rateError = if (rateValue == null || rateValue <= 0.0) {
        invalidManualRateMessage
    } else {
        null
    }

    return ManualRateValidationResult(
        normalizedFrom = normalizedFrom,
        normalizedTo = normalizedTo,
        rateValue = rateValue,
        fromError = fromError,
        toError = toError,
        rateError = rateError
    )
}

internal fun shouldRemoveOriginalManualRate(
    original: CurrencyPairRate?,
    normalizedFrom: String,
    normalizedTo: String
): Boolean {
    if (original == null) return false
    val originalFrom = original.from.trim().uppercase()
    val originalTo = original.to.trim().uppercase()
    return originalFrom != normalizedFrom || originalTo != normalizedTo
}

private fun formatTimestamp(timestampMillis: Long): String {
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(timestampMillis))
}
