package com.mvi.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Header拦截器
 * 用于向所有请求添加通用Header
 */
class HeaderInterceptor(private val headers: Map<String, String>) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        return chain.proceed(requestBuilder.build())
    }
}
