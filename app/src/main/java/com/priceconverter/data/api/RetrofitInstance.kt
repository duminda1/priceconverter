package com.priceconverter.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.priceconverter.utils.Constants

object RetrofitInstance {
    val api: ExchangeRateApi by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.EXCHANGE_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ExchangeRateApi::class.java)
    }
}
