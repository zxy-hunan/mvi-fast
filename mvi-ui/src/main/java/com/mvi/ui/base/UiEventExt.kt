package com.mvi.ui.base

/**
 * mvi-ui 模块专属的 UI 事件扩展
 * 用于处理空状态、错误状态等 UI 相关事件
 *
 * 注意: 由于 Kotlin 的 sealed class 跨模块限制,
 * 这些事件类不继承自 UiEvent,而是作为独立的事件类型
 * 在 ViewModel 中使用时,可以通过手动发送或使用辅助方法
 *
 * ⚠️ 内存泄漏警告:
 * onRetry 回调函数会被保存在 Flow/Channel 中,请注意:
 * 1. 避免在 lambda 中直接引用 Activity/Fragment
 * 2. 推荐使用 ViewModel 的方法引用: `::retryLoad`
 * 3. 或使用轻量级的 Intent 方式: `{ sendIntent(RetryIntent) }`
 *
 * 正确示例:
 * ```kotlin
 * // ✅ 推荐 - 使用方法引用
 * showErrorState(onRetry = ::retryLoadData)
 *
 * // ✅ 推荐 - 使用 Intent
 * showErrorState(onRetry = { sendIntent(LoadDataIntent) })
 *
 * // ❌ 错误 - 直接引用 Activity
 * showErrorState(onRetry = { activity.recreate() })
 * ```
 */
sealed interface MviUiEvent {
    /**
     * 显示空状态
     * @param message 空状态提示信息
     * @param iconResId 图标资源ID
     * @param onRetry 重试回调
     */
    data class ShowEmptyState(
        val message: String = "",
        val iconResId: Int = 0,
        val onRetry: (() -> Unit)? = null
    ) : MviUiEvent

    /**
     * 显示错误状态
     * @param message 错误提示信息
     * @param iconResId 图标资源ID
     * @param onRetry 重试回调
     */
    data class ShowErrorState(
        val message: String = "",
        val iconResId: Int = 0,
        val onRetry: (() -> Unit)? = null
    ) : MviUiEvent

    /**
     * 隐藏空状态/错误状态
     */
    data object HideEmptyState : MviUiEvent
}

