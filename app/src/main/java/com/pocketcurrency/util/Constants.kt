package com.pocketcurrency.util

object Constants {
    const val DEFAULT_BASE_CURRENCY = "USD"
    const val DEFAULT_TARGET_CURRENCY = "AUD"
    const val EXCHANGE_API_BASE_URL = "https://api.exchangerate.host/"
    const val FRANKFURTER_API_BASE_URL = "https://api.frankfurter.app/"
    const val PROVIDER_EXCHANGE_RATES = "exchangeratesapi"
    const val PROVIDER_FRANKFURTER = "frankfurter"
    const val DEFAULT_RATE_PROVIDER = PROVIDER_FRANKFURTER
    const val FREE_PLAN_LIMIT = 100
    const val PREFS_NAME = "price_converter_prefs"
    const val PREFS_SECURE_NAME = "price_converter_secure_prefs"
    const val PREFS_SELECTED_CURRENCIES = "selected_currencies"
    const val PREFS_API_KEY = "api_key"
    const val PREFS_SERVICE = "rate_service"
    const val PREFS_FREE_PLAN = "free_plan"
    const val PREFS_REALTIME_ENABLED = "realtime_enabled"
    const val PREFS_LIVE_SCAN_ENABLED = "live_scan_enabled"
    const val PREFS_USAGE_MONTH = "usage_month"
    const val PREFS_USAGE_COUNT = "usage_count"
    const val PREFS_USAGE_WARN_50 = "usage_warn_50"
    const val PREFS_USAGE_WARN_75 = "usage_warn_75"
    const val PREFS_USAGE_WARN_90 = "usage_warn_90"
    const val PREFS_SAVED_RATES = "saved_rates"
    const val PREFS_MANUAL_RATES = "manual_rates"
    const val PREFS_HOME_CURRENCY = "home_currency"
    const val PREFS_DESTINATION_CURRENCY = "destination_currency"
    const val PREFS_DESTINATION_AUTO = "destination_currency_auto"
    const val PREFS_RATE_REFRESH_CHECK_PREFIX = "rate_refresh_check_"
    // Pin rotation: update these prefs via remote config and bump PREFS_PIN_CONFIG_VERSION.
    const val PREFS_PIN_CONFIG_VERSION = "pin_config_version"
    const val PREFS_EXCHANGE_RATE_PINS = "exchange_rate_pins"
    const val PREFS_FRANKFURTER_PINS = "frankfurter_pins"
}
