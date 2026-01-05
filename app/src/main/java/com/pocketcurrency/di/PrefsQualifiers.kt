package com.pocketcurrency.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultPrefs

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SecurePrefs
