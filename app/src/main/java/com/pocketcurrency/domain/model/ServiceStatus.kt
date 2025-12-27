package com.pocketcurrency.domain.model

data class ServiceStatus(
    val type: ServiceStatusType,
    val lastUpdatedMillis: Long
)

enum class ServiceStatusType {
    LIVE,
    SAVED,
    MANUAL
}
