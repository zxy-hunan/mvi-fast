package com.mvi.core.base

/**
 * UI状态封装
 * 简化的状态管理,替代原框架的BaseIntent
 */
sealed class UiState<out T> {
    /** 初始状态 */
    data object Idle : UiState<Nothing>()

    /** 加载中 */
    data class Loading(val message: String = "Loading...") : UiState<Nothing>()

    /** 成功 */
    data class Success<T>(val data: T) : UiState<T>()

    /** 错误 */
    data class Error(
        val message: String,
        val throwable: Throwable? = null,
        val canRetry: Boolean = true
    ) : UiState<Nothing>()

    /** 空数据 */
    data class Empty(val message: String = "No data") : UiState<Nothing>()

    /** 网络错误 */
    data class NetworkError(val message: String = "Network error") : UiState<Nothing>()
}

/**
 * UI事件 (一次性事件,如Toast、导航等)
 */
sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class ShowLoading(val show: Boolean) : UiEvent()
    data class Navigate(val route: String) : UiEvent()
}
