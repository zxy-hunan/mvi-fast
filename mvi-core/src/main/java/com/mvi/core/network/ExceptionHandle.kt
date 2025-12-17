package com.mvi.core.network

import android.content.Context
import com.google.gson.JsonParseException
import com.mvi.core.R
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
 * 用于将各种异常转换为统一的 NetErrorData
 *
 * 使用示例:
 * ```kotlin
 * try {
 *     // 网络请求
 * } catch (e: Exception) {
 *     val errorData = e.handleException(context)
 *     showToast(errorData.message)
 * }
 * ```
 */
object ExceptionHandle {

    private var appContext: Context? = null

    /**
     * 初始化（在 Application 中调用）
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * 获取字符串资源
     */
    private fun getString(resId: Int, vararg formatArgs: Any): String {
        return appContext?.getString(resId, *formatArgs) ?: "Error"
    }

    /**
     * 扩展函数：处理异常
     */
    fun Throwable.handleException(): NetErrorData {
        this.printStackTrace()

        val errorData = NetErrorData()

        when (this) {
            // HTTP 异常（Retrofit）
            is HttpException -> {
                errorData.code = this.code()
                errorData.message = when (this.code()) {
                    HttpURLConnection.HTTP_UNAUTHORIZED -> getString(R.string.error_http_unauthorized)
                    HttpURLConnection.HTTP_FORBIDDEN -> getString(R.string.error_http_forbidden)
                    HttpURLConnection.HTTP_NOT_FOUND -> getString(R.string.error_http_not_found)
                    HttpURLConnection.HTTP_CLIENT_TIMEOUT -> getString(R.string.error_http_timeout)
                    HttpURLConnection.HTTP_INTERNAL_ERROR -> getString(R.string.error_http_internal_error)
                    HttpURLConnection.HTTP_BAD_GATEWAY -> getString(R.string.error_http_bad_gateway)
                    HttpURLConnection.HTTP_UNAVAILABLE -> getString(R.string.error_http_unavailable)
                    HttpURLConnection.HTTP_GATEWAY_TIMEOUT -> getString(R.string.error_http_gateway_timeout)
                    else -> getString(R.string.error_http_unknown, this.code())
                }
            }

            // 网络连接异常
            is ConnectException -> {
                errorData.code = ErrorStatus.NETWORK_ERROR
                errorData.message = getString(R.string.error_network_connection)
            }

            // 未知主机异常（无网络或DNS解析失败）
            is UnknownHostException -> {
                errorData.code = ErrorStatus.NETWORK_ERROR
                errorData.message = getString(R.string.error_network_connection)
            }

            // 网络超时异常
            is SocketTimeoutException -> {
                errorData.code = ErrorStatus.NETWORK_TIMEOUT
                errorData.message = getString(R.string.error_network_timeout)
            }

            // Socket 异常
            is SocketException -> {
                errorData.code = ErrorStatus.SOCKET_EXCEPTION
                errorData.message = getString(R.string.error_socket_exception)
            }

            // JSON 解析异常
            is JsonParseException, is JSONException, is ParseException -> {
                errorData.code = ErrorStatus.JSON_ERROR
                errorData.message = getString(R.string.error_json_parse)
            }

            // API 自定义异常
            is ApiException -> {
                errorData.code = this.code
                errorData.message = this.message
            }

            // IO 异常
            is IOException -> {
                errorData.code = ErrorStatus.OTHER_ERROR
                errorData.message = this.message ?: getString(R.string.error_io_exception)
            }

            // 其他常见异常
            is NullPointerException -> {
                errorData.code = ErrorStatus.NULL_POINTER
                errorData.message = getString(R.string.error_null_pointer)
            }

            is IndexOutOfBoundsException -> {
                errorData.code = ErrorStatus.INDEX_OUT_OF_BOUNDS
                errorData.message = getString(R.string.error_index_out_of_bounds)
            }

            is ClassCastException -> {
                errorData.code = ErrorStatus.CLASS_CAST
                errorData.message = getString(R.string.error_class_cast)
            }

            is ArrayStoreException -> {
                errorData.code = ErrorStatus.ARRAY_STORE
                errorData.message = getString(R.string.error_array_store)
            }

            is IllegalArgumentException -> {
                errorData.code = ErrorStatus.SERVER_ERROR
                errorData.message = this.message ?: getString(R.string.error_illegal_argument)
            }

            // 未知错误
            else -> {
                errorData.code = ErrorStatus.UNKNOWN_ERROR
                errorData.message = this.message ?: getString(R.string.error_unknown)
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
