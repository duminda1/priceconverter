package com.pockettoolsstudio.pocketcurrency.di

import com.pockettoolsstudio.pocketcurrency.util.SystemTimeProvider
import com.pockettoolsstudio.pocketcurrency.util.TimeProvider
import com.pockettoolsstudio.pocketcurrency.viewmodel.ResourceStringProvider
import com.pockettoolsstudio.pocketcurrency.viewmodel.StringProvider
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
