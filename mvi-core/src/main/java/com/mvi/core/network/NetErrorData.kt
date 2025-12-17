package com.mvi.core.network

/**
 * 网络错误数据类
 *
 * @property code 错误码
 * @property message 错误信息
 * @property data 额外数据（可选）
 */
data class NetErrorData(
    var code: Int = ErrorStatus.UNKNOWN_ERROR,
    var message: String = "error",
    var data: Any? = null
)
