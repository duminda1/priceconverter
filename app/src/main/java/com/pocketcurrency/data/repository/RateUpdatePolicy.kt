package com.pocketcurrency.data.repository

import com.pocketcurrency.util.Constants
import com.pocketcurrency.util.FrankfurterSchedule
import java.util.concurrent.TimeUnit
import javax.inject.Inject

interface RateUpdatePolicy {
    fun isStale(lastUpdatedMillis: Long, nowMillis: Long): Boolean
}

class FrankfurterUpdatePolicy : RateUpdatePolicy {
    override fun isStale(lastUpdatedMillis: Long, nowMillis: Long): Boolean {
        if (lastUpdatedMillis <= 0L) return true
        val lastScheduled = FrankfurterSchedule.lastScheduledUpdateMillis(nowMillis)
        return lastUpdatedMillis < lastScheduled
    }
}

class DefaultUpdatePolicy : RateUpdatePolicy {
    private val maxAgeMillis = TimeUnit.DAYS.toMillis(1)

    override fun isStale(lastUpdatedMillis: Long, nowMillis: Long): Boolean {
        if (lastUpdatedMillis <= 0L) return true
        return nowMillis - lastUpdatedMillis >= maxAgeMillis
    }
}

class RateUpdatePolicyRegistry @Inject constructor() {
    private val policies = mapOf(
        Constants.PROVIDER_FRANKFURTER to FrankfurterUpdatePolicy(),
        Constants.PROVIDER_EXCHANGE_RATES to DefaultUpdatePolicy()
    )

    fun getPolicy(providerId: String): RateUpdatePolicy {
        return policies[providerId] ?: DefaultUpdatePolicy()
    }
}
