package com.mvi.core.network.interceptor

import okhttp3.logging.HttpLoggingInterceptor

/**
 * 日志拦截器
 * 用于打印HTTP请求和响应日志
 */
object LoggingInterceptor {

    /**
     * 创建日志拦截器
     * @param level 日志级别，默认为BODY（打印完整请求和响应）
     * @return HttpLoggingInterceptor实例
     */
    fun create(level: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.BODY): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            this.level = level
        }
    }
}
