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

/**
 * UiState 扩展函数 - 状态管理增强
 * 
 * 提供便捷的状态判断和数据获取方法
 */
val <T> UiState<T>.isIdle: Boolean
    get() = this is UiState.Idle

val <T> UiState<T>.isLoading: Boolean
    get() = this is UiState.Loading

val <T> UiState<T>.isSuccess: Boolean
    get() = this is UiState.Success

val <T> UiState<T>.isError: Boolean
    get() = this is UiState.Error || this is UiState.NetworkError

val <T> UiState<T>.isEmpty: Boolean
    get() = this is UiState.Empty

/**
 * 安全获取数据
 */
fun <T> UiState<T>.getDataOrNull(): T? = (this as? UiState.Success)?.data

fun <T> UiState<T>.getDataOrDefault(default: T): T = getDataOrNull() ?: default

/**
 * 获取错误信息
 */
fun <T> UiState<T>.getErrorMessageOrNull(): String? = when (this) {
    is UiState.Error -> message
    is UiState.NetworkError -> message
    else -> null
}

/**
 * 是否可以重试
 */
fun <T> UiState<T>.canRetry(): Boolean = when (this) {
    is UiState.Error -> canRetry
    is UiState.NetworkError -> true
    else -> false
}