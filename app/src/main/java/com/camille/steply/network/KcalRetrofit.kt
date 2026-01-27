
package com.camille.steply.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object KcalRetrofit {
    private const val BASE_URL = "https://sellero.pythonanywhere.com/"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    val api: KcalApi by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // keep for debugging
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logger)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "SteplyAppClient")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(KcalApi::class.java)
    }
}
