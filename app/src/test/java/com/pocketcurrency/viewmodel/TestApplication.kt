package com.pocketcurrency.viewmodel

import com.pocketcurrency.R
import java.util.Locale

class TestStringProvider : StringProvider {

    private val templates = mapOf(
        R.string.error_invalid_currency_codes to "Enter valid currency codes.",
        R.string.error_service_unavailable to "Service unavailable. Please try again.",
        R.string.error_invalid_api_key to "Enter a valid API key.",
        R.string.status_api_key_saved to "API key verified and saved.",
        R.string.error_api_key_verify_failed to "Unable to verify API key.",
        R.string.status_saved_rate_pair to "Saved rate for %1\$s/%2\$s.",
        R.string.error_fetch_rate_failed to "Failed to fetch rate.",
        R.string.status_no_saved_rates_to_refresh to "No saved rates to refresh.",
        R.string.status_refreshing_saved_rates to "Refreshing saved rates...",
        R.string.status_refreshed_saved_rates to "Refreshed %1\$d/%2\$d saved rates.",
        R.string.status_refreshed_saved_rates_with_error to
            "Refreshed %1\$d/%2\$d saved rates. %3\$s",
        R.string.status_manual_rate_saved to "Manual rate saved."
    )

    override fun get(resId: Int, vararg formatArgs: Any): String {
        val template = templates[resId]
            ?: error("Missing string mapping for resId=$resId")
        return if (formatArgs.isEmpty()) {
            template
        } else {
            String.format(Locale.getDefault(), template, *formatArgs)
        }
    }
}
