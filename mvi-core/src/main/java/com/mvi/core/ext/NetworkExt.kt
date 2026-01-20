package com.mvi.core.ext

import androidx.lifecycle.viewModelScope
import com.mvi.core.base.MviViewModel
import com.mvi.core.base.UiState
import com.mvi.core.network.ApiException
import com.mvi.core.network.ApiResponse
import com.mvi.core.network.ExceptionHandle.handleException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

/**
 * 网络请求扩展函数
 *
 * 优化点:
 * 1. 自动处理Loading状态
 * 2. 统一错误处理
 * 3. StateFlow自动更新
 * 4. DSL风格回调
 */

/**
 * 执行网络请求
 *
 * @param stateFlow 状态流,自动更新UI状态
 * @param autoShowLoading 是否自动显示Loading（默认true，避免与showLoading方法名冲突）
 * @param request 网络请求Block
 */
fun <T, I : com.mvi.core.base.MviIntent> MviViewModel<I>.launchRequest(
    stateFlow: MutableStateFlow<UiState<T>>,
    autoShowLoading: Boolean = true,
    request: suspend () -> ApiResponse<T>
) {
    viewModelScope.launch {
        try {
            // 显示Loading
            if (autoShowLoading) {
                stateFlow.value = UiState.Loading()
                showLoading(true)
            }

            // 执行请求
            val response = withContext(Dispatchers.IO) {
                request()
            }

            // 处理响应
            if (response.isSuccess()) {
                val data = response.data
                if (data != null) {
                    stateFlow.value = UiState.Success(data)
                } else {
                    stateFlow.value = UiState.Empty()
                }
            } else {
                stateFlow.value = UiState.Error(response.message)
                showToast(response.message)
            }

        } catch (e: Exception) {
            val errorData = e.handleException()
            stateFlow.value = UiState.Error(errorData.message, e)
            showToast(errorData.message)
        } finally {
            if (autoShowLoading) {
                showLoading(false)
            }
        }
    }
}

/**
 * 执行网络请求 (带回调)
 *
 * @param showLoading 是否显示Loading
 * @param onSuccess 成功回调
 * @param onError 错误回调
 * @param request 网络请求Block
 */
fun <T, I : com.mvi.core.base.MviIntent> MviViewModel<I>.launchRequest(
    showLoading: Boolean = true,
    onSuccess: (T) -> Unit = {},
    onError: (String, Throwable?) -> Unit = { _, _ -> },
    request: suspend () -> ApiResponse<T>
) {
    viewModelScope.launch {
        try {
            // 显示Loading
            if (showLoading) {
                showLoading(true)
            }

            // 执行请求
            val response = withContext(Dispatchers.IO) {
                request()
            }

            // 处理响应
            if (response.isSuccess() && response.data != null) {
                onSuccess(response.data)
            } else {
                val message = response.message
                onError(message, ApiException(response.code, message))
                showToast(message)
            }

        } catch (e: Exception) {
            val errorData = e.handleException()
            onError(errorData.message, e)
            showToast(errorData.message)
        } finally {
            if (showLoading) {
                showLoading(false)
            }
        }
    }
}

/**
 * 执行网络请求（带重试机制）
 *
 * @param stateFlow 状态流,自动更新UI状态
 * @param maxRetries 最大重试次数（默认3次）
 * @param retryDelay 重试延迟（毫秒，默认1000ms）
 * @param retryCondition 重试条件（默认网络错误才重试）
 * @param showLoading 是否显示Loading
 * @param request 网络请求Block
 */
fun <T, I : com.mvi.core.base.MviIntent> MviViewModel<I>.launchRequestWithRetry(
    stateFlow: MutableStateFlow<UiState<T>>,
    maxRetries: Int = 3,
    retryDelay: Long = 1000L,
    retryCondition: (Throwable) -> Boolean = { throwable ->
        throwable.handleException().isNetworkError()
    },
    showLoading: Boolean = true,
    request: suspend () -> ApiResponse<T>
) {
    viewModelScope.launch {
        var retryCount = 0
        
        while (retryCount <= maxRetries) {
            try {
                // 显示Loading
                if (showLoading) {
                    if (retryCount > 0) {
                        stateFlow.value = UiState.Loading("重试中... (${retryCount}/${maxRetries})")
                    } else {
                        stateFlow.value = UiState.Loading()
                    }
                    showLoading(true)
                }

                // 执行请求
                val response = withContext(Dispatchers.IO) {
                    request()
                }

                // 处理响应
                if (response.isSuccess()) {
                    val data = response.data
                    if (data != null) {
                        stateFlow.value = UiState.Success(data)
                        break // 成功，退出循环
                    } else {
                        stateFlow.value = UiState.Empty()
                        break
                    }
                } else {
                    // 业务错误，不重试
                    stateFlow.value = UiState.Error(response.message)
                    showToast(response.message)
                    break
                }

            } catch (e: Exception) {
                val errorData = e.handleException()
                
                // 判断是否需要重试
                if (retryCount < maxRetries && retryCondition(e)) {
                    retryCount++
                    // 指数退避：延迟时间递增
                    delay(retryDelay * retryCount)
                    continue // 继续重试
                } else {
                    // 达到最大重试次数或不符合重试条件
                    stateFlow.value = UiState.Error(errorData.message, e)
                    showToast(errorData.message)
                    break
                }
            } finally {
                if (showLoading) {
                    showLoading(false)
                }
            }
        }
    }
}

/**
 * 执行网络请求（带重试机制，回调版本）
 *
 * @param maxRetries 最大重试次数（默认3次）
 * @param retryDelay 重试延迟（毫秒，默认1000ms）
 * @param retryCondition 重试条件（默认网络错误才重试）
 * @param showLoading 是否显示Loading
 * @param onSuccess 成功回调
 * @param onError 错误回调
 * @param request 网络请求Block
 */
fun <T, I : com.mvi.core.base.MviIntent> MviViewModel<I>.launchRequestWithRetry(
    maxRetries: Int = 3,
    retryDelay: Long = 1000L,
    retryCondition: (Throwable) -> Boolean = { throwable ->
        throwable.handleException().isNetworkError()
    },
    showLoading: Boolean = true,
    onSuccess: (T) -> Unit = {},
    onError: (String, Throwable?) -> Unit = { _, _ -> },
    request: suspend () -> ApiResponse<T>
) {
    viewModelScope.launch {
        var retryCount = 0
        
        while (retryCount <= maxRetries) {
            try {
                // 显示Loading
                if (showLoading) {
                    showLoading(true)
                }

                // 执行请求
                val response = withContext(Dispatchers.IO) {
                    request()
                }

                // 处理响应
                if (response.isSuccess() && response.data != null) {
                    onSuccess(response.data)
                    break // 成功，退出循环
                } else {
                    // 业务错误，不重试
                    val message = response.message
                    onError(message, ApiException(response.code, message))
                    showToast(message)
                    break
                }

            } catch (e: Exception) {
                val errorData = e.handleException()
                
                // 判断是否需要重试
                if (retryCount < maxRetries && retryCondition(e)) {
                    retryCount++
                    // 指数退避：延迟时间递增
                    delay(retryDelay * retryCount)
                    continue // 继续重试
                } else {
                    // 达到最大重试次数或不符合重试条件
                    onError(errorData.message, e)
                    showToast(errorData.message)
                    break
                }
            } finally {
                if (showLoading) {
                    showLoading(false)
                }
            }
        }
    }
}