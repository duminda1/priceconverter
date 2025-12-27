package com.pocketcurrency.data.repository

import com.pocketcurrency.utils.Constants
import com.pocketcurrency.utils.FrankfurterSchedule

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
    override fun isStale(lastUpdatedMillis: Long, nowMillis: Long): Boolean = false
}

class RateUpdatePolicyRegistry {
    private val policies = mapOf(
        Constants.PROVIDER_FRANKFURTER to FrankfurterUpdatePolicy(),
        Constants.PROVIDER_EXCHANGE_RATES to DefaultUpdatePolicy()
    )

    fun getPolicy(providerId: String): RateUpdatePolicy {
        return policies[providerId] ?: DefaultUpdatePolicy()
    }
}
