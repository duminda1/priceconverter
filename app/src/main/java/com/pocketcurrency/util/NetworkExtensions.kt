package com.pocketcurrency.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

internal const val DEFAULT_MAX_RETRY_ATTEMPTS = 2
internal const val DEFAULT_RETRY_DELAY_MILLIS = 350L

internal suspend fun <T> executeWithRetry(
    maxRetryAttempts: Int = DEFAULT_MAX_RETRY_ATTEMPTS,
    initialDelayMillis: Long = DEFAULT_RETRY_DELAY_MILLIS,
    shouldRetry: (Exception) -> Boolean = ::shouldRetryNetworkException,
    block: suspend () -> T
): T {
    var lastError: Exception? = null
    var delayMillis = initialDelayMillis
    repeat(maxRetryAttempts) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            lastError = e
            val shouldRetryAttempt = shouldRetry(e) && attempt < maxRetryAttempts - 1
            if (!shouldRetryAttempt) {
                throw e
            }
            delay(delayMillis)
            delayMillis *= 2
        }
    }
    throw lastError ?: IOException("Service unavailable.")
}

internal fun shouldRetryNetworkException(e: Exception): Boolean {
    return when (e) {
        is SocketTimeoutException,
        is UnknownHostException,
        is ConnectException,
        is IOException -> true
        is HttpException -> e.code() >= 500
        else -> false
    }
}

internal fun mapNetworkExceptionToMessage(e: Exception): String {
    return when (e) {
        is UnknownHostException,
        is ConnectException -> "No internet connection. Please try again."
        is SocketTimeoutException -> "The service is taking too long. Please try again."
        is HttpException -> {
            if (e.code() >= 500) {
                "Service unavailable. Please try again."
            } else {
                "Unable to reach the service. Please try again."
            }
        }
        is IOException -> "No internet connection. Please try again."
        else -> e.message ?: "Service unavailable. Please try again."
    }
}
