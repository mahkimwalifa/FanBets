package com.seamhealth.elsrt.data.api

import com.seamhealth.elsrt.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class ApiKeyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("x-apisports-key", BuildConfig.API_KEY)
            .build()
        return chain.proceed(request)
    }
}
