package com.woleapp.netpluscontactlesssdkimplementationsampleproject

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class PosClient {


    companion object {

        private fun getBaseOkhttpClientBuilder(): OkHttpClient.Builder {
            val okHttpClientBuilder = OkHttpClient.Builder()

            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            okHttpClientBuilder.addInterceptor(loggingInterceptor)

            return okHttpClientBuilder
        }

        private fun getOkHttpClient() =
            getBaseOkhttpClientBuilder()
                .addInterceptor(TokenInterceptor())
                .build()

        // private val BASE_URL = "http://192.168.32.20:5000/"
        private const val BASE_URL = "https://gateway.netpluspay.com/"
        private var INSTANCE: PosService? = null
        fun getInstance(): PosService = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(getOkHttpClient())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PosService::class.java)
                .also {
                    INSTANCE = it
                }
        }

    }}