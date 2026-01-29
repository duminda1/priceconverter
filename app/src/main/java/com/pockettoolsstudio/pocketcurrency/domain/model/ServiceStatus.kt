package com.pockettoolsstudio.pocketcurrency.domain.model

data class ServiceStatus(
    val type: ServiceStatusType,
    val lastUpdatedMillis: Long,
    val isStale: Boolean = false
) {
    val lastUpdatedAtMillis: Long
        get() = lastUpdatedMillis
}

enum class ServiceStatusType {
    LIVE,
    SAVED,
    MANUAL
}
