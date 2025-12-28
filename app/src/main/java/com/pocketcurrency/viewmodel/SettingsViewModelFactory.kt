package com.pocketcurrency.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pocketcurrency.data.network.NetworkMonitor
import com.pocketcurrency.data.provider.RateProviders
import com.pocketcurrency.data.repository.RateUpdatePolicyRegistry
import com.pocketcurrency.data.repository.RateUpdateRepository
import com.pocketcurrency.data.repository.SettingsRepository

class SettingsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val settingsRepository = SettingsRepository(application)
            val rateUpdateRepository = RateUpdateRepository(
                settingsRepository = settingsRepository,
                providerRegistry = RateProviders.registry,
                networkMonitor = NetworkMonitor(application),
                policyRegistry = RateUpdatePolicyRegistry()
            )
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(
                application,
                settingsRepository,
                rateUpdateRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
