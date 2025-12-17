package com.mvi.core.network

/**
 * 网络请求错误状态码
 */
object ErrorStatus {
    /**
     * 响应成功
     */
    const val SUCCESS = 0

    /**
     * 其它错误
     */
    const val OTHER_ERROR = 1001

    /**
     * 未知错误
     */
    const val UNKNOWN_ERROR = 1002

    /**
     * 服务器内部错误
     */
    const val SERVER_ERROR = 1003

    /**
     * 网络连接异常
     */
    const val NETWORK_ERROR = 1004

    /**
     * 数据解析异常
     */
    const val JSON_ERROR = 1005

    /**
     * 网络连接超时
     */
    const val NETWORK_TIMEOUT = 1006

    /**
     * 空指针异常
     */
    const val NULL_POINTER = 1007

    /**
     * 数组越界异常
     */
    const val INDEX_OUT_OF_BOUNDS = 1008

    /**
     * 类转换异常
     */
    const val CLASS_CAST = 1009

    /**
     * 数据存储异常
     */
    const val ARRAY_STORE = 1010

    /**
     * Socket连接异常
     */
    const val SOCKET_EXCEPTION = 1011

    /**
     * 服务提示信息
     */
    const val SERVICE_TIPS = 1012
}
