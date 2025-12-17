package com.mvi.core.network.interceptor

import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.security.MessageDigest

/**
 * 数据拦截器
 * 用于向所有请求添加通用参数，如 gSign、randCode、ts 等
 *
 * 使用示例:
 * ```kotlin
 * // 简单模式（使用默认签名算法）
 * okHttpBuilder.addInterceptor(DataInterceptor())
 *
 * // 自定义模式
 * okHttpBuilder.addInterceptor(DataInterceptor(
 *     gSignProvider = { ts, randCode -> "custom_sign" },
 *     randCodeProvider = { "custom_rand_code" },
 *     additionalParams = mapOf("key" to "value")
 * ))
 * ```
 */
class DataInterceptor(
    private val gSignProvider: ((ts: String, randCode: String) -> String)? = null,
    private val randCodeProvider: (() -> String)? = null,
    private val additionalParams: Map<String, String> = emptyMap()
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 生成参数
        val ts = System.currentTimeMillis().toString()
        val randCode = randCodeProvider?.invoke() ?: generateRandCode()
        val gSign = gSignProvider?.invoke(ts, randCode) ?: generateSign(ts, randCode)

        // 根据请求方法处理
        val newRequest = when (originalRequest.method) {
            "GET" -> addParamsToGetRequest(originalRequest, ts, randCode, gSign)
            "POST" -> addParamsToPostRequest(originalRequest, ts, randCode, gSign)
            else -> originalRequest
        }

        return chain.proceed(newRequest)
    }

    /**
     * 为 GET 请求添加参数
     */
    private fun addParamsToGetRequest(
        request: Request,
        ts: String,
        randCode: String,
        gSign: String
    ): Request {
        val originalUrl = request.url
        val newUrlBuilder = originalUrl.newBuilder()
            .addQueryParameter("ts", ts)
            .addQueryParameter("randCode", randCode)
            .addQueryParameter("gSign", gSign)

        // 添加额外参数
        additionalParams.forEach { (key, value) ->
            newUrlBuilder.addQueryParameter(key, value)
        }

        return request.newBuilder()
            .url(newUrlBuilder.build())
            .build()
    }

    /**
     * 为 POST 请求添加参数
     */
    private fun addParamsToPostRequest(
        request: Request,
        ts: String,
        randCode: String,
        gSign: String
    ): Request {
        val body = request.body ?: return request

        // 只处理 FormBody
        if (body is FormBody) {
            val newBodyBuilder = FormBody.Builder()

            // 复制原有参数
            for (i in 0 until body.size) {
                newBodyBuilder.add(body.name(i), body.value(i))
            }

            // 添加参数
            newBodyBuilder.add("ts", ts)
            newBodyBuilder.add("randCode", randCode)
            newBodyBuilder.add("gSign", gSign)

            // 添加额外参数
            additionalParams.forEach { (key, value) ->
                newBodyBuilder.add(key, value)
            }

            return request.newBuilder()
                .post(newBodyBuilder.build())
                .build()
        }

        return request
    }

    /**
     * 生成随机码
     */
    private fun generateRandCode(): String {
        return System.nanoTime().toString() + (0..9999).random()
    }

    /**
     * 生成签名
     * 可以根据实际业务需求修改签名算法
     */
    private fun generateSign(ts: String, randCode: String): String {
        // 示例签名算法：md5(ts + randCode + secret_key)
        val secretKey = "your_secret_key"  // 可以从配置文件或其他地方获取
        val signStr = "$ts$randCode$secretKey"
        return md5(signStr)
    }

    /**
     * MD5 加密工具方法
     */
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        /**
         * 创建简单的 DataInterceptor（使用默认签名算法）
         */
        fun createSimple(): DataInterceptor {
            return DataInterceptor()
        }

        /**
         * 创建带自定义签名的 DataInterceptor
         * @param gSignProvider 签名生成器，接收 ts 和 randCode 参数
         * @param randCodeProvider 随机码生成器（可选）
         * @param additionalParams 额外参数（可选）
         */
        fun createWithSign(
            gSignProvider: (ts: String, randCode: String) -> String,
            randCodeProvider: (() -> String)? = null,
            additionalParams: Map<String, String> = emptyMap()
        ): DataInterceptor {
            return DataInterceptor(gSignProvider, randCodeProvider, additionalParams)
        }

        /**
         * MD5 加密工具方法（供外部使用）
         */
        fun md5(input: String): String {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
