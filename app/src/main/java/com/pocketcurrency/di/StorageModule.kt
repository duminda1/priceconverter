package com.pocketcurrency.di

import android.content.Context
import android.content.SharedPreferences
import com.pocketcurrency.data.repository.SecurePrefsProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideSecureSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return SecurePrefsProvider.create(context)
    }
}
