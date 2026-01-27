package com.mvi.core.network

import android.content.Context
import com.google.gson.JsonParseException
import org.json.JSONException
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.ParseException

/**
 * 异常处理工具类
 *
 * 优化点:
 * 1. 提供默认错误消息，不强制依赖Context
 * 2. 支持国际化（初始化后使用Context）
 * 3. 空安全设计
 *
 * 使用示例:
 * ```kotlin
 * // 方式一：使用默认错误消息（无需初始化）
 * try {
 *     // 网络请求
 * } catch (e: Exception) {
 *     val errorData = e.handleException()
 *     showToast(errorData.message)
 * }
 *
 * // 方式二：使用国际化错误消息（需在Application初始化）
 * ExceptionHandle.init(context)
 * ```
 */
object ExceptionHandle {

    private var appContext: Context? = null

    /**
     * 自定义错误消息映射
     * 用于特殊业务场景覆盖默认错误消息
     */
    private var customMessageMap: Map<Int, String>? = null

    /**
     * 自定义错误消息映射（根据异常类型）
     */
    private var customClassMessageMap: Map<Class<out Throwable>, String>? = null

    /**
     * 初始化（在 Application 中调用，用于国际化支持）
     * 可选：不调用则使用默认英文错误消息
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * 设置自定义错误消息（根据错误码）
     * 用于特殊业务场景
     *
     * @param messages 错误码到消息的映射
     */
    fun setCustomMessages(messages: Map<Int, String>) {
        customMessageMap = messages
    }

    /**
     * 设置自定义错误消息（根据异常类型）
     *
     * @param messages 异常类型到消息的映射
     */
    fun setCustomClassMessages(messages: Map<Class<out Throwable>, String>) {
        customClassMessageMap = messages
    }

    /**
     * 清除自定义错误消息
     */
    fun clearCustomMessages() {
        customMessageMap = null
        customClassMessageMap = null
    }

    /**
     * 获取字符串资源
     * 如果未初始化Context，返回默认英文消息
     */
    private fun getString(defaultMessage: String, resId: Int? = null, vararg formatArgs: Any): String {
        if (resId != null) {
            return appContext?.getString(resId, *formatArgs) ?: defaultMessage
        }
        return appContext?.let {
            try {
                // 如果提供了resId且context可用，尝试获取字符串
                if (formatArgs.isNotEmpty()) {
                    String.format(defaultMessage, *formatArgs)
                } else {
                    defaultMessage
                }
            } catch (e: Exception) {
                defaultMessage
            }
        } ?: defaultMessage
    }

    /**
     * 扩展函数：处理异常
     * 空安全设计：即使未初始化Context也能工作
     */
    fun Throwable.handleException(): NetErrorData {
        this.printStackTrace()

        val errorData = NetErrorData()

        // 优先使用自定义异常类型消息
        val classMessage = customClassMessageMap?.get(this::class.java)
        if (classMessage != null) {
            errorData.code = ErrorStatus.CUSTOM_ERROR
            errorData.message = classMessage
            return errorData
        }

        when (this) {
            // HTTP 异常（Retrofit）
            is HttpException -> {
                errorData.code = this.code()
                errorData.message = when (this.code()) {
                    HttpURLConnection.HTTP_UNAUTHORIZED -> getString(
                        defaultMessage = "Unauthorized, please login again",
                        resId = com.mvi.core.R.string.error_http_unauthorized
                    )
                    HttpURLConnection.HTTP_FORBIDDEN -> getString(
                        defaultMessage = "Access denied",
                        resId = com.mvi.core.R.string.error_http_forbidden
                    )
                    HttpURLConnection.HTTP_NOT_FOUND -> getString(
                        defaultMessage = "Resource not found",
                        resId = com.mvi.core.R.string.error_http_not_found
                    )
                    HttpURLConnection.HTTP_CLIENT_TIMEOUT -> getString(
                        defaultMessage = "Request timeout, please try again later",
                        resId = com.mvi.core.R.string.error_http_timeout
                    )
                    HttpURLConnection.HTTP_INTERNAL_ERROR -> getString(
                        defaultMessage = "Internal server error",
                        resId = com.mvi.core.R.string.error_http_internal_error
                    )
                    HttpURLConnection.HTTP_BAD_GATEWAY -> getString(
                        defaultMessage = "Bad gateway",
                        resId = com.mvi.core.R.string.error_http_bad_gateway
                    )
                    HttpURLConnection.HTTP_UNAVAILABLE -> getString(
                        defaultMessage = "Service unavailable, please try again later",
                        resId = com.mvi.core.R.string.error_http_unavailable
                    )
                    HttpURLConnection.HTTP_GATEWAY_TIMEOUT -> getString(
                        defaultMessage = "Gateway timeout",
                        resId = com.mvi.core.R.string.error_http_gateway_timeout
                    )
                    else -> getString(
                        defaultMessage = "HTTP error (${this.code()})",
                        resId = com.mvi.core.R.string.error_http_unknown,
                        formatArgs = arrayOf(this.code())
                    )
                }
            }

            // 网络连接异常
            is ConnectException -> {
                errorData.code = ErrorStatus.NETWORK_ERROR
                errorData.message = getString(
                    defaultMessage = "Network connection failed, please check your network settings",
                    resId = com.mvi.core.R.string.error_network_connection
                )
            }

            // 未知主机异常（无网络或DNS解析失败）
            is UnknownHostException -> {
                errorData.code = ErrorStatus.NETWORK_ERROR
                errorData.message = getString(
                    defaultMessage = "Network connection failed, please check your network settings",
                    resId = com.mvi.core.R.string.error_network_connection
                )
            }

            // 网络超时异常
            is SocketTimeoutException -> {
                errorData.code = ErrorStatus.NETWORK_TIMEOUT
                errorData.message = getString(
                    defaultMessage = "Network connection timeout, please try again later",
                    resId = com.mvi.core.R.string.error_network_timeout
                )
            }

            // Socket 异常
            is SocketException -> {
                errorData.code = ErrorStatus.SOCKET_EXCEPTION
                errorData.message = getString(
                    defaultMessage = "Network connection exception",
                    resId = com.mvi.core.R.string.error_socket_exception
                )
            }

            // JSON 解析异常
            is JsonParseException, is JSONException, is ParseException -> {
                errorData.code = ErrorStatus.JSON_ERROR
                errorData.message = getString(
                    defaultMessage = "Data parsing failed",
                    resId = com.mvi.core.R.string.error_json_parse
                )
            }

            // API 自定义异常
            is ApiException -> {
                errorData.code = this.code
                errorData.message = this.message ?: "API Error"
            }

            // IO 异常
            is IOException -> {
                errorData.code = ErrorStatus.OTHER_ERROR
                val message = this.message
                errorData.message = if (message.isNullOrEmpty()) {
                    getString(
                        defaultMessage = "IO exception",
                        resId = com.mvi.core.R.string.error_io_exception
                    )
                } else {
                    message
                }
            }

            // 其他常见异常
            is NullPointerException -> {
                errorData.code = ErrorStatus.NULL_POINTER
                errorData.message = getString(
                    defaultMessage = "Null pointer exception",
                    resId = com.mvi.core.R.string.error_null_pointer
                )
            }

            is IndexOutOfBoundsException -> {
                errorData.code = ErrorStatus.INDEX_OUT_OF_BOUNDS
                errorData.message = getString(
                    defaultMessage = "Index out of bounds exception",
                    resId = com.mvi.core.R.string.error_index_out_of_bounds
                )
            }

            is ClassCastException -> {
                errorData.code = ErrorStatus.CLASS_CAST
                errorData.message = getString(
                    defaultMessage = "Class cast exception",
                    resId = com.mvi.core.R.string.error_class_cast
                )
            }

            is ArrayStoreException -> {
                errorData.code = ErrorStatus.ARRAY_STORE
                errorData.message = getString(
                    defaultMessage = "Array store exception",
                    resId = com.mvi.core.R.string.error_array_store
                )
            }

            is IllegalArgumentException -> {
                errorData.code = ErrorStatus.SERVER_ERROR
                val message = this.message
                errorData.message = if (message.isNullOrEmpty()) {
                    getString(
                        defaultMessage = "Invalid parameter",
                        resId = com.mvi.core.R.string.error_illegal_argument
                    )
                } else {
                    message
                }
            }

            // 未知错误
            else -> {
                errorData.code = ErrorStatus.UNKNOWN_ERROR
                val message = this.message
                errorData.message = if (message.isNullOrEmpty()) {
                    getString(
                        defaultMessage = "Unknown error",
                        resId = com.mvi.core.R.string.error_unknown
                    )
                } else {
                    message
                }
            }
        }

        // 检查自定义错误码消息（优先级最高）
        if (errorData.code != 0) {
            val customMessage = customMessageMap?.get(errorData.code)
            if (customMessage != null) {
                errorData.message = customMessage
            }
        }

        return errorData
    }

    /**
     * 判断是否为网络相关错误
     */
    fun NetErrorData.isNetworkError(): Boolean {
        return code == ErrorStatus.NETWORK_ERROR ||
                code == ErrorStatus.NETWORK_TIMEOUT ||
                code == ErrorStatus.SOCKET_EXCEPTION
    }

    /**
     * 判断是否为服务器错误
     */
    fun NetErrorData.isServerError(): Boolean {
        return code == ErrorStatus.SERVER_ERROR ||
                code in 500..599
    }

    /**
     * 判断是否需要重新登录
     */
    fun NetErrorData.needReLogin(): Boolean {
        return code == HttpURLConnection.HTTP_UNAUTHORIZED
    }
}
