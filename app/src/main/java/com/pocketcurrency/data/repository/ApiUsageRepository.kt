package com.pocketcurrency.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.pocketcurrency.util.Constants
import com.pocketcurrency.di.DefaultPrefs
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class ApiUsageState(
    val monthKey: String,
    val count: Int,
    val warn50: Boolean,
    val warn75: Boolean,
    val warn90: Boolean
)

class ApiUsageRepository @Inject constructor(
    @DefaultPrefs private val prefs: SharedPreferences
) {
    fun getUsageState(): ApiUsageState {
        val currentMonth = currentMonthKey()
        val storedMonth = prefs.getString(Constants.PREFS_USAGE_MONTH, currentMonth) ?: currentMonth
        if (storedMonth != currentMonth) {
            resetUsage(currentMonth)
        }
        return ApiUsageState(
            monthKey = prefs.getString(Constants.PREFS_USAGE_MONTH, currentMonth) ?: currentMonth,
            count = prefs.getInt(Constants.PREFS_USAGE_COUNT, 0),
            warn50 = prefs.getBoolean(Constants.PREFS_USAGE_WARN_50, false),
            warn75 = prefs.getBoolean(Constants.PREFS_USAGE_WARN_75, false),
            warn90 = prefs.getBoolean(Constants.PREFS_USAGE_WARN_90, false)
        )
    }

    fun recordApiRequest(): String? {
        val currentMonth = currentMonthKey()
        val storedMonth = prefs.getString(Constants.PREFS_USAGE_MONTH, currentMonth) ?: currentMonth
        if (storedMonth != currentMonth) {
            resetUsage(currentMonth)
        }

        val newCount = prefs.getInt(Constants.PREFS_USAGE_COUNT, 0) + 1
        prefs.edit {
            putInt(Constants.PREFS_USAGE_COUNT, newCount)
        }

        if (!prefs.getBoolean(Constants.PREFS_FREE_PLAN, true)) {
            return null
        }

        return when {
            newCount >= 90 && !prefs.getBoolean(Constants.PREFS_USAGE_WARN_90, false) -> {
                prefs.edit {
                    putBoolean(Constants.PREFS_USAGE_WARN_90, true)
                }
                "90% of your monthly API requests are used ($newCount/${Constants.FREE_PLAN_LIMIT})."
            }
            newCount >= 75 && !prefs.getBoolean(Constants.PREFS_USAGE_WARN_75, false) -> {
                prefs.edit {
                    putBoolean(Constants.PREFS_USAGE_WARN_75, true)
                }
                "75% of your monthly API requests are used ($newCount/${Constants.FREE_PLAN_LIMIT})."
            }
            newCount >= 50 && !prefs.getBoolean(Constants.PREFS_USAGE_WARN_50, false) -> {
                prefs.edit {
                    putBoolean(Constants.PREFS_USAGE_WARN_50, true)
                }
                "50% of your monthly API requests are used ($newCount/${Constants.FREE_PLAN_LIMIT})."
            }
            else -> null
        }
    }

    private fun resetUsage(currentMonth: String) {
        prefs.edit {
            putString(Constants.PREFS_USAGE_MONTH, currentMonth)
            putInt(Constants.PREFS_USAGE_COUNT, 0)
            putBoolean(Constants.PREFS_USAGE_WARN_50, false)
            putBoolean(Constants.PREFS_USAGE_WARN_75, false)
            putBoolean(Constants.PREFS_USAGE_WARN_90, false)
        }
    }

    private fun currentMonthKey(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return String.format(Locale.ROOT, "%04d-%02d", year, month)
    }
}
