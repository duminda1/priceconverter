package com.pocketcurrency.data.api

import com.pocketcurrency.utils.Constants

object FrankfurterRetrofitInstance {
    val api: FrankfurterApi by lazy {
        RetrofitClient.retrofit(Constants.FRANKFURTER_API_BASE_URL)
            .create(FrankfurterApi::class.java)
    }
}
