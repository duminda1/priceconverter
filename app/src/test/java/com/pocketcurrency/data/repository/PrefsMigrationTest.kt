package com.pocketcurrency.data.repository

import com.pocketcurrency.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefsMigrationTest {

    @Test
    fun migrateNonSecure_movesNonSensitiveKeys_andKeepsApiKeyEncrypted() {
        val securePrefs = FakeSharedPreferences(
            mutableMapOf(
                Constants.PREFS_API_KEY to "SECRET",
                Constants.PREFS_SERVICE to Constants.PROVIDER_EXCHANGE_RATES,
                Constants.PREFS_FREE_PLAN to false,
                Constants.PREFS_USAGE_COUNT to 12,
                Constants.PREFS_SAVED_RATES to "[{\"from\":\"USD\",\"to\":\"AUD\"}]",
                Constants.PREFS_RATE_REFRESH_CHECK_PREFIX + "provider" to 123L
            )
        )
        val defaultPrefs = FakeSharedPreferences()

        PrefsMigration.migrateNonSecure(defaultPrefs, securePrefs)

        assertEquals(
            Constants.PROVIDER_EXCHANGE_RATES,
            defaultPrefs.getString(Constants.PREFS_SERVICE, null)
        )
        assertEquals(false, defaultPrefs.getBoolean(Constants.PREFS_FREE_PLAN, true))
        assertEquals(12, defaultPrefs.getInt(Constants.PREFS_USAGE_COUNT, 0))
        assertEquals(
            "[{\"from\":\"USD\",\"to\":\"AUD\"}]",
            defaultPrefs.getString(Constants.PREFS_SAVED_RATES, null)
        )
        assertEquals(
            123L,
            defaultPrefs.getLong(Constants.PREFS_RATE_REFRESH_CHECK_PREFIX + "provider", 0L)
        )
        assertTrue(defaultPrefs.getBoolean(PrefsMigration.PREFS_MIGRATION_KEY, false))

        assertTrue(securePrefs.contains(Constants.PREFS_API_KEY))
        assertFalse(securePrefs.contains(Constants.PREFS_SERVICE))
        assertFalse(securePrefs.contains(Constants.PREFS_FREE_PLAN))
    }

    @Test
    fun migrateNonSecure_prefersSecureValues_whenDefaultAlreadySet() {
        val securePrefs = FakeSharedPreferences(
            mutableMapOf(Constants.PREFS_SERVICE to Constants.PROVIDER_EXCHANGE_RATES)
        )
        val defaultPrefs = FakeSharedPreferences(
            mutableMapOf(Constants.PREFS_SERVICE to Constants.PROVIDER_FRANKFURTER)
        )

        PrefsMigration.migrateNonSecure(defaultPrefs, securePrefs)

        assertEquals(
            Constants.PROVIDER_EXCHANGE_RATES,
            defaultPrefs.getString(Constants.PREFS_SERVICE, null)
        )
        assertFalse(securePrefs.contains(Constants.PREFS_SERVICE))
    }
}
