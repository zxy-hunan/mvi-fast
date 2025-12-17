package com.mvi.core.network

/**
 * 通用API响应封装
 *
 * @param T 数据类型
 */
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
) {
    /**
     * 是否成功
     */
    fun isSuccess(): Boolean = code == 200 || code == 0

    /**
     * 获取数据或抛出异常
     */
    fun getOrThrow(): T {
        if (isSuccess() && data != null) {
            return data
        }
        throw ApiException(code, message)
    }
}

/**
 * API异常
 */
class ApiException(val code: Int, override val message: String) : Exception(message)
