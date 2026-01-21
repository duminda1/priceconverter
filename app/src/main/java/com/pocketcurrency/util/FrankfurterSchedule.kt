package com.pocketcurrency.util

import java.util.Calendar
import java.util.TimeZone

object FrankfurterSchedule {
    private const val UPDATE_HOUR_CET = 16
    private val timeZone = TimeZone.getTimeZone("Europe/Berlin")

    fun lastScheduledUpdateMillis(nowMillis: Long): Long {
        val now = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        val updateToday = Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, now.get(Calendar.YEAR))
            set(Calendar.MONTH, now.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, UPDATE_HOUR_CET)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return if (nowMillis >= updateToday.timeInMillis) {
            updateToday.timeInMillis
        } else {
            updateToday.add(Calendar.DAY_OF_MONTH, -1)
            updateToday.timeInMillis
        }
    }

    fun updateMillisForDate(date: String): Long? {
        val parts = date.split("-")
        if (parts.size != 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        val calendar = Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, UPDATE_HOUR_CET)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
