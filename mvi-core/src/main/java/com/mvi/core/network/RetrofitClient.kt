package com.mvi.core.network

import com.google.gson.Gson
import com.mvi.core.R
import com.mvi.core.base.MviApplication
import com.mvi.core.network.interceptor.DataInterceptor
import com.mvi.core.network.interceptor.HeaderInterceptor
import com.mvi.core.network.interceptor.LoggingInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit客户端构建器
 *
 * 优化点:
 * 1. 单例模式管理Retrofit实例
 * 2. DSL风格配置
 * 3. 简化的初始化流程
 */
object RetrofitClient {

    private const val DEFAULT_TIMEOUT = 30L

    private var baseUrl: String = ""
    private var okHttpClient: OkHttpClient? = null
    private var retrofit: Retrofit? = null

    /**
     * 初始化配置
     */
    fun init(block: Builder.() -> Unit) {
        val builder = Builder()
        builder.block()
        builder.build()
    }

    /**
     * 创建API服务
     */
    fun <T> create(service: Class<T>): T {
        checkNotNull(retrofit) {
            MviApplication.instance.getString(R.string.retrofit_init_required)
        }
        return retrofit!!.create(service)
    }

    /**
     * 配置构建器
     */
    class Builder {
        var baseUrl: String = ""
        var connectTimeout: Long = DEFAULT_TIMEOUT
        var readTimeout: Long = DEFAULT_TIMEOUT
        var writeTimeout: Long = DEFAULT_TIMEOUT
        var enableLogging: Boolean = true
        var enableDataInterceptor: Boolean = false
        var headers: Map<String, String> = emptyMap()
        var interceptors: List<okhttp3.Interceptor> = emptyList()

        fun build() {
            require(baseUrl.isNotEmpty()) {
                MviApplication.instance.getString(R.string.retrofit_baseurl_required)
            }

            RetrofitClient.baseUrl = baseUrl

            // 构建OkHttpClient
            val okHttpBuilder = OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)

            // 添加日志拦截器
            if (enableLogging) {
                okHttpBuilder.addInterceptor(LoggingInterceptor.create())
            }

            // 添加 DataInterceptor
            if (enableDataInterceptor) {
                okHttpBuilder.addInterceptor(DataInterceptor())
            }

            // 添加Header拦截器
            if (headers.isNotEmpty()) {
                okHttpBuilder.addInterceptor(HeaderInterceptor(headers))
            }

            // 添加自定义拦截器
            interceptors.forEach { interceptor ->
                okHttpBuilder.addInterceptor(interceptor)
            }

            RetrofitClient.okHttpClient = okHttpBuilder.build()

            // 构建Retrofit
            RetrofitClient.retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(RetrofitClient.okHttpClient!!)
                .addConverterFactory(GsonConverterFactory.create(Gson()))
                .build()
        }
    }
}
