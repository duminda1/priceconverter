package com.pocketcurrency.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pocketcurrency.data.repository.ExchangeRateRepository
import com.pocketcurrency.data.repository.RateRepository
import com.pocketcurrency.data.repository.SettingsRepository

class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val settingsRepository = SettingsRepository(application)
            val exchangeRepository = ExchangeRateRepository(settingsRepository)
            val rateRepository = RateRepository(settingsRepository, exchangeRepository)
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, rateRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
