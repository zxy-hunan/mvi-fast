package com.mvi.core.ext

import androidx.lifecycle.viewModelScope
import com.mvi.core.base.MviIntent
import com.mvi.core.base.MviViewModel
import com.mvi.core.base.PagedList
import com.mvi.core.base.PagingState
import com.mvi.core.network.ApiResponse
import com.mvi.core.network.ExceptionHandle.handleException
import com.mvi.core.network.ExceptionHandle.isNetworkError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 分页请求扩展函数
 *
 * 功能特性:
 * 1. 自动处理首次加载和加载更多
 * 2. 保留当前数据，避免闪烁
 * 3. 统一的错误处理
 * 4. 支持 Refresh（重置到第一页）
 * 5. 自动合并数据
 *
 * 使用示例:
 * ```kotlin
 * class UserViewModel : MviViewModel<UserIntent>() {
 *     private var currentPage = 1
 *     private val pageSize = 20
 *     private val allUsers = mutableListOf<User>()
 *
 *     private val _pagingState = MutableStateFlow<PagingState<User>>(PagingState.Idle)
 *     val pagingState = _pagingState.asStateFlow()
 *
 *     private fun loadUsers(refresh: Boolean = false) {
 *         if (refresh) {
 *             currentPage = 1
 *             allUsers.clear()
 *         }
 *
 *         launchPagingRequest(
 *             stateFlow = _pagingState,
 *             currentPage = currentPage,
 *             pageSize = pageSize,
 *             currentData = allUsers.toList(),
 *             request = { page, size -> apiService.getUsers(page, size) }
 *         ) { pagedData ->
 *             // 成功回调：更新数据
 *             if (page == 1) {
 *                 allUsers.clear()
 *             }
 *             allUsers.addAll(pagedData.data)
 *             currentPage++
 *         }
 *     }
 * }
 * ```
 */

/**
 * 执行分页请求（自动更新状态流）
 *
 * @param stateFlow 分页状态流
 * @param currentPage 当前页码
 * @param pageSize 每页数量
 * @param currentData 当前已加载的数据
 * @param request 网络请求
 * @param onSuccess 成功回调
 */
fun <T, I : MviIntent> MviViewModel<I>.launchPagingRequest(
    stateFlow: MutableStateFlow<PagingState<T>>,
    currentPage: Int,
    pageSize: Int = 20,
    currentData: List<T> = emptyList(),
    request: suspend (page: Int, size: Int) -> ApiResponse<PagedList<T>>,
    onSuccess: ((PagedList<T>) -> Unit)? = null
) {
    viewModelScope.launch {
        try {
            // 首次加载显示 Loading
            if (currentPage == 1) {
                stateFlow.value = PagingState.Loading
            } else {
                // 加载更多显示 LoadingMore
                stateFlow.value = PagingState.LoadingMore(currentData)
            }

            // 执行请求
            val response = withContext(Dispatchers.IO) {
                request(currentPage, pageSize)
            }

            // 处理响应
            if (response.isSuccess()) {
                val pagedData = response.data

                if (pagedData != null && pagedData.data.isNotEmpty()) {
                    if (pagedData.hasMore) {
                        // 有更多数据
                        stateFlow.value = PagingState.Success(pagedData)
                        onSuccess?.invoke(pagedData)
                    } else {
                        // 没有更多数据
                        val allData = if (currentPage == 1) {
                            pagedData.data
                        } else {
                            currentData + pagedData.data
                        }
                        stateFlow.value = PagingState.NoMoreData(allData)
                        onSuccess?.invoke(pagedData)
                    }
                } else {
                    // 空数据
                    if (currentPage == 1) {
                        stateFlow.value = PagingState.Empty
                    } else {
                        stateFlow.value = PagingState.NoMoreData(currentData)
                    }
                }
            } else {
                // 业务错误
                stateFlow.value = PagingState.Error(response.message, currentData as List<Any>?)
            }

        } catch (e: Exception) {
            val errorData = e.handleException()
            stateFlow.value = PagingState.Error(errorData.message, currentData as List<Any>?)
        }
    }
}

/**
 * 执行分页请求（带回调）
 *
 * @param currentPage 当前页码
 * @param pageSize 每页数量
 * @param currentData 当前已加载的数据
 * @param showLoading 是否显示加载提示
 * @param onSuccess 成功回调
 * @param onError 错误回调
 * @param request 网络请求
 */
fun <T, I : MviIntent> MviViewModel<I>.launchPagingRequest(
    currentPage: Int,
    pageSize: Int = 20,
    currentData: List<T> = emptyList(),
    showLoading: Boolean = true,
    onSuccess: (PagedList<T>) -> Unit,
    onError: (String, Throwable?) -> Unit = { _, _ -> },
    request: suspend (page: Int, size: Int) -> ApiResponse<PagedList<T>>
) {
    viewModelScope.launch {
        try {
            // 显示加载提示
            if (showLoading && currentPage == 1) {
                showLoading(true)
            }

            // 执行请求
            val response = withContext(Dispatchers.IO) {
                request(currentPage, pageSize)
            }

            // 处理响应
            if (response.isSuccess() && response.data != null) {
                onSuccess(response.data)
            } else {
                onError(response.message, null)
            }

        } catch (e: Exception) {
            val errorData = e.handleException()
            onError(errorData.message, e)
        } finally {
            if (showLoading && currentPage == 1) {
                showLoading(false)
            }
        }
    }
}

/**
 * 执行分页请求（带重试机制）
 *
 * @param stateFlow 分页状态流
 * @param currentPage 当前页码
 * @param pageSize 每页数量
 * @param currentData 当前已加载的数据
 * @param maxRetries 最大重试次数（默认3次）
 * @param retryDelay 重试延迟（毫秒，默认1000ms）
 * @param retryCondition 重试条件（默认网络错误才重试）
 * @param request 网络请求
 * @param onSuccess 成功回调
 */
fun <T, I : MviIntent> MviViewModel<I>.launchPagingRequestWithRetry(
    stateFlow: MutableStateFlow<PagingState<T>>,
    currentPage: Int,
    pageSize: Int = 20,
    currentData: List<T> = emptyList(),
    maxRetries: Int = 3,
    retryDelay: Long = 1000L,
    retryCondition: (Throwable) -> Boolean = { throwable ->
        throwable.handleException().isNetworkError()
    },
    request: suspend (page: Int, size: Int) -> ApiResponse<PagedList<T>>,
    onSuccess: ((PagedList<T>) -> Unit)? = null
) {
    viewModelScope.launch {
        var retryCount = 0

        while (retryCount <= maxRetries) {
            try {
                // 首次加载显示 Loading
                if (currentPage == 1) {
                    if (retryCount > 0) {
                        stateFlow.value = PagingState.Loading
                    } else {
                        stateFlow.value = PagingState.Loading
                    }
                } else {
                    // 加载更多
                    stateFlow.value = PagingState.LoadingMore(currentData)
                }

                // 执行请求
                val response = withContext(Dispatchers.IO) {
                    request(currentPage, pageSize)
                }

                // 处理响应
                if (response.isSuccess()) {
                    val pagedData = response.data

                    if (pagedData != null && pagedData.data.isNotEmpty()) {
                        if (pagedData.hasMore) {
                            stateFlow.value = PagingState.Success(pagedData)
                            onSuccess?.invoke(pagedData)
                        } else {
                            val allData = if (currentPage == 1) {
                                pagedData.data
                            } else {
                                currentData + pagedData.data
                            }
                            stateFlow.value = PagingState.NoMoreData(allData)
                            onSuccess?.invoke(pagedData)
                        }
                    } else {
                        if (currentPage == 1) {
                            stateFlow.value = PagingState.Empty
                        } else {
                            stateFlow.value = PagingState.NoMoreData(currentData)
                        }
                    }
                    break // 成功，退出循环
                } else {
                    // 业务错误，不重试
                    stateFlow.value = PagingState.Error(response.message, currentData as List<Any>?)
                    break
                }

            } catch (e: Exception) {
                val errorData = e.handleException()

                // 判断是否需要重试
                if (retryCount < maxRetries && retryCondition(e)) {
                    retryCount++
                    kotlinx.coroutines.delay(retryDelay * retryCount)
                    continue // 继续重试
                } else {
                    // 达到最大重试次数或不符合重试条件
                    stateFlow.value = PagingState.Error(errorData.message, currentData as List<Any>?)
                    break
                }
            }
        }
    }
}

/**
 * 检查是否可以加载更多
 */
fun <T> MutableStateFlow<PagingState<T>>.canLoadMore(): Boolean {
    return value.canLoadMore()
}

/**
 * 检查是否正在加载
 */
fun <T> MutableStateFlow<PagingState<T>>.isLoading(): Boolean {
    return value.isLoading()
}

/**
 * 获取当前数据
 */
@Suppress("UNCHECKED_CAST")
fun <T> MutableStateFlow<PagingState<T>>.getData(): List<T> {
    return when (val state = value) {
        is PagingState.Success -> state.data.data as List<T>
        is PagingState.LoadingMore -> state.currentData as List<T>
        is PagingState.NoMoreData -> state.currentData as List<T>
        is PagingState.Error -> (state.currentData ?: emptyList()) as List<T>
        else -> emptyList()
    }
}

/**
 * 重置分页状态
 */
fun <T> MutableStateFlow<PagingState<T>>.resetPaging() {
    value = PagingState.Idle
}
