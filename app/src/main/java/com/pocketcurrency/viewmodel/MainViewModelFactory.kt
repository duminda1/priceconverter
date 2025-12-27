package com.pocketcurrency.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pocketcurrency.data.network.NetworkMonitor
import com.pocketcurrency.data.provider.RateProviders
import com.pocketcurrency.data.repository.RateRepository
import com.pocketcurrency.data.repository.RateUpdatePolicyRegistry
import com.pocketcurrency.data.repository.RateUpdateRepository
import com.pocketcurrency.data.repository.SettingsRepository

class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val settingsRepository = SettingsRepository(application)
            val rateUpdateRepository = RateUpdateRepository(
                settingsRepository = settingsRepository,
                providerRegistry = RateProviders.registry,
                networkMonitor = NetworkMonitor(application),
                policyRegistry = RateUpdatePolicyRegistry()
            )
            val rateRepository = RateRepository(settingsRepository, rateUpdateRepository)
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                application,
                rateRepository,
                settingsRepository,
                rateUpdateRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
