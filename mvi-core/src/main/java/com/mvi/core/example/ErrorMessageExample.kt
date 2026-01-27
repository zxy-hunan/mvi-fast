package com.mvi.core.example

import com.mvi.core.network.ExceptionHandle
import com.mvi.core.network.ExceptionHandle.handleException
import java.net.SocketTimeoutException

/**
 * 自定义错误消息使用示例
 *
 * 展示如何使用 ExceptionHandle 的自定义错误消息功能
 */
object ErrorMessageExample {

    /**
     * 示例1：根据错误码自定义消息
     */
    fun example1() {
        // 在 Application 中设置自定义错误消息
        ExceptionHandle.setCustomMessages(
            mapOf(
                401 to "请先登录",
                403 to "无权限访问此页面",
                404 to "页面不存在",
                500 to "服务器开小差了，请稍后再试",
                503 to "系统维护中，请稍后再试",
                1004 to "网络连接失败，请检查网络设置",
                1006 to "请求超时，请稍后重试"
            )
        )

        // 使用示例
        try {
            // 网络请求...
        } catch (e: Exception) {
            val errorData = e.handleException()
            // errorData.message 会使用自定义消息
            // 例如：401 -> "请先登录"
        }
    }

    /**
     * 示例2：根据异常类型自定义消息
     */
    fun example2() {
        // 根据异常类型设置自定义消息
        ExceptionHandle.setCustomClassMessages(
            mapOf(
                SocketTimeoutException::class.java to "网络连接超时，请检查网络",
                IllegalArgumentException::class.java to "参数错误，请检查输入"
            )
        )

        // 使用示例
        try {
            // 某些操作...
        } catch (e: SocketTimeoutException) {
            val errorData = e.handleException()
            // errorData.message 会是 "网络连接超时，请检查网络"
        }
    }

    /**
     * 示例3：同时使用两种自定义方式
     */
    fun example3() {
        // 可以同时设置两种自定义消息
        ExceptionHandle.setCustomMessages(
            mapOf(
                401 to "请先登录",
                500 to "服务器错误"
            )
        )

        ExceptionHandle.setCustomClassMessages(
            mapOf(
                SocketTimeoutException::class.java to "网络超时"
            )
        )

        // 优先级：
        // 1. 异常类型消息（最高）
        // 2. 错误码消息
        // 3. 国际化消息
        // 4. 默认英文消息（最低）

        try {
            // 网络请求...
        } catch (e: Exception) {
            val errorData = e.handleException()
            // 会按照优先级选择消息
        }
    }

    /**
     * 示例4：清除自定义消息
     */
    fun example4() {
        // 设置自定义消息
        ExceptionHandle.setCustomMessages(mapOf(401 to "请先登录"))

        // 使用自定义消息
        // ...

        // 清除自定义消息，恢复默认
        ExceptionHandle.clearCustomMessages()
    }

    /**
     * 示例5：动态更新自定义消息
     */
    fun example5() {
        // 初始设置
        ExceptionHandle.setCustomMessages(
            mapOf(
                401 to "请先登录",
                403 to "无权限"
            )
        )

        // 后续更新（会覆盖之前的设置）
        ExceptionHandle.setCustomMessages(
            mapOf(
                401 to "登录已过期，请重新登录",  // 更新401的消息
                503 to "系统维护中"  // 新增503的消息
            )
        )
    }

    /**
     * 示例6：在特定场景使用不同消息
     */
    fun example6() {
        // 场景1：登录页面
        fun setupLoginErrors() {
            ExceptionHandle.setCustomMessages(
                mapOf(
                    401 to "用户名或密码错误",
                    429 to "登录尝试次数过多，请稍后再试"
                )
            )
        }

        // 场景2：支付页面
        fun setupPaymentErrors() {
            ExceptionHandle.setCustomMessages(
                mapOf(
                    402 to "支付失败，余额不足",
                    403 to "支付权限被限制",
                    503 to "支付服务维护中"
                )
            )
        }

        // 使用场景
        when (currentPage) {
            "login" -> setupLoginErrors()
            "payment" -> setupPaymentErrors()
            else -> ExceptionHandle.clearCustomMessages()  // 其他页面使用默认消息
        }
    }

    /**
     * 示例7：多语言自定义消息
     */
    fun example7() {
        // 根据系统语言设置不同的自定义消息
        val language = java.util.Locale.getDefault().language

        val messages = when (language) {
            "zh" -> mapOf(
                401 to "请先登录",
                403 to "无权限访问"
            )
            "ja" -> mapOf(
                401 to "ログインしてください",
                403 to "アクセス権限がありません"
            )
            "ko" -> mapOf(
                401 to "로그인이 필요합니다",
                403 to "접근 권한이 없습니다"
            )
            else -> mapOf(  // 默认英文
                401 to "Please login first",
                403 to "Access denied"
            )
        }

        ExceptionHandle.setCustomMessages(messages)
    }

    // 辅助属性（伪代码）
    private var currentPage: String = ""
}

/**
 * 使用建议
 *
 * 1. 在 Application.onCreate() 中初始化 ExceptionHandle.init(context)
 * 2. 在特定页面（如登录页）设置自定义错误消息
 * 3. 在页面 onDestroy() 时清除自定义消息
 * 4. 对于通用的错误消息，建议直接修改 strings.xml 而非使用自定义消息
 * 5. 对于业务特定的错误消息（如支付失败），使用自定义消息功能
 */
