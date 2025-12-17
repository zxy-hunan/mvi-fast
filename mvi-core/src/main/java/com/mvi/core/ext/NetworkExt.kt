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
 * @param showLoading 是否显示Loading
 * @param request 网络请求Block
 */
fun <T, I : com.mvi.core.base.MviIntent> MviViewModel<I>.launchRequest(
    stateFlow: MutableStateFlow<UiState<T>>,
    showLoading: Boolean = true,
    request: suspend () -> ApiResponse<T>
) {
    viewModelScope.launch {
        try {
            // 显示Loading
            if (showLoading) {
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
            if (showLoading) {
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
