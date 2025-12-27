package com.pocketcurrency.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketcurrency.data.network.NetworkMonitor
import com.pocketcurrency.data.provider.RateProviders
import com.pocketcurrency.data.repository.RateUpdatePolicyRegistry
import com.pocketcurrency.data.repository.RateUpdateRepository
import com.pocketcurrency.data.repository.SettingsRepository
import com.pocketcurrency.domain.usecase.GetRatesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RateViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val repository = RateUpdateRepository(
        settingsRepository = settingsRepository,
        providerRegistry = RateProviders.registry,
        networkMonitor = NetworkMonitor(application),
        policyRegistry = RateUpdatePolicyRegistry()
    )
    private val getRatesUseCase = GetRatesUseCase(repository)

    private val _rate = MutableStateFlow(0.0)
    val rate: StateFlow<Double> = _rate

    fun loadRate(baseCurrency: String, targetCurrency: String) {
        viewModelScope.launch {
            try {
                val result = getRatesUseCase.execute(baseCurrency, targetCurrency)
                _rate.value = result.rate?.rate ?: 0.0
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
