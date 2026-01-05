package com.pocketcurrency.di

import com.pocketcurrency.utils.SystemTimeProvider
import com.pocketcurrency.utils.TimeProvider
import com.pocketcurrency.viewmodel.ResourceStringProvider
import com.pocketcurrency.viewmodel.StringProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    abstract fun bindStringProvider(
        provider: ResourceStringProvider
    ): StringProvider

    @Binds
    abstract fun bindTimeProvider(
        provider: SystemTimeProvider
    ): TimeProvider
}
