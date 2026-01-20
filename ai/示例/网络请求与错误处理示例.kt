package com.mvi.example.network

import com.mvi.core.base.MviViewModel
import com.mvi.core.base.UiState
import com.mvi.core.base.UiEvent
import com.mvi.core.ext.launchRequest
import com.mvi.core.network.ApiResponse
import com.mvi.core.network.ExceptionHandle
import com.mvi.core.network.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 网络请求与错误处理示例
 * 
 * 展示了各种网络请求场景和错误处理方式
 */
class NetworkExampleViewModel(
    private val userApi: UserApi
) : MviViewModel<UserIntent>() {

    private val _userState = MutableStateFlow<UiState<List<User>>>(UiState.Idle)
    val userState = _userState.asStateFlow()

    // ========== 示例 1: 基础网络请求 ==========
    
    /**
     * 使用 launchRequest 自动处理状态
     * 优点：代码简洁，自动处理 Loading、Success、Error
     */
    private fun loadUsersSimple() {
        launchRequest(_userState) {
            userApi.getUserList()
        }
    }

    // ========== 示例 2: 不显示 Loading 的请求 ==========
    
    /**
     * 下拉刷新时不显示全局 Loading
     */
    private fun refreshUsers() {
        launchRequest(_userState, showLoading = false) {
            userApi.getUserList()
        }
    }

    // ========== 示例 3: 带回调的网络请求 ==========
    
    /**
     * 使用回调方式处理结果
     */
    private fun loadUsersWithCallback() {
        launchRequest(
            showLoading = true,
            onSuccess = { users ->
                // 成功回调
                _userState.value = UiState.Success(users)
                sendEvent(UiEvent.ShowToast("加载成功"))
            },
            onError = { message, throwable ->
                // 错误回调
                _userState.value = UiState.Error(message, throwable)
                sendEvent(UiEvent.ShowToast(message))
            }
        ) {
            userApi.getUserList()
        }
    }

    // ========== 示例 4: 手动处理网络请求 ==========
    
    /**
     * 手动处理网络请求（需要更多控制时使用）
     */
    private fun loadUsersManual() {
        viewModelScope.launch {
            try {
                // 1. 显示 Loading
                _userState.value = UiState.Loading("加载中...")
                showLoading(true)

                // 2. 执行网络请求（IO 线程）
                val response = withContext(Dispatchers.IO) {
                    userApi.getUserList()
                }

                // 3. 处理响应
                if (response.isSuccess() && response.data != null) {
                    _userState.value = UiState.Success(response.data)
                } else {
                    _userState.value = UiState.Error(response.message)
                    sendEvent(UiEvent.ShowToast(response.message))
                }

            } catch (e: Exception) {
                // 4. 统一异常处理
                val errorData = e.handleException()
                _userState.value = UiState.Error(errorData.message, e)
                sendEvent(UiEvent.ShowToast(errorData.message))
                
            } finally {
                // 5. 隐藏 Loading
                showLoading(false)
            }
        }
    }

    // ========== 示例 5: 处理特定错误 ==========
    
    /**
     * 根据错误类型进行不同处理
     */
    private fun loadUsersWithErrorHandling() {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    userApi.getUserList()
                }

                if (response.isSuccess() && response.data != null) {
                    _userState.value = UiState.Success(response.data)
                } else {
                    // 处理业务错误
                    handleBusinessError(response.code, response.message)
                }

            } catch (e: Exception) {
                val errorData = e.handleException()
                
                // 根据错误类型处理
                when {
                    errorData.isNetworkError() -> {
                        // 网络错误：显示网络错误状态
                        _userState.value = UiState.NetworkError(errorData.message)
                        sendEvent(UiEvent.ShowToast("网络连接失败，请检查网络"))
                    }
                    
                    errorData.needReLogin() -> {
                        // 需要重新登录
                        _userState.value = UiState.Error("登录已过期，请重新登录")
                        sendEvent(UiEvent.Navigate("/login"))
                    }
                    
                    errorData.isServerError() -> {
                        // 服务器错误
                        _userState.value = UiState.Error("服务器错误，请稍后重试")
                        sendEvent(UiEvent.ShowToast("服务器错误"))
                    }
                    
                    else -> {
                        // 其他错误
                        _userState.value = UiState.Error(errorData.message, e)
                        sendEvent(UiEvent.ShowToast(errorData.message))
                    }
                }
            }
        }
    }

    // ========== 示例 6: 并发请求 ==========
    
    /**
     * 并发执行多个请求
     */
    private fun loadMultipleData() {
        viewModelScope.launch {
            try {
                showLoading(true)
                
                // 并发执行
                val usersDeferred = async(Dispatchers.IO) {
                    userApi.getUserList()
                }
                val postsDeferred = async(Dispatchers.IO) {
                    userApi.getUserPosts()
                }
                
                // 等待所有请求完成
                val users = usersDeferred.await()
                val posts = postsDeferred.await()
                
                // 处理结果
                if (users.isSuccess() && posts.isSuccess()) {
                    _userState.value = UiState.Success(users.data ?: emptyList())
                    // 处理 posts...
                }
                
            } catch (e: Exception) {
                val errorData = e.handleException()
                _userState.value = UiState.Error(errorData.message, e)
                sendEvent(UiEvent.ShowToast(errorData.message))
            } finally {
                showLoading(false)
            }
        }
    }

    // ========== 示例 7: 重试机制 ==========
    
    /**
     * 带重试机制的网络请求
     */
    private fun loadUsersWithRetry(maxRetries: Int = 3) {
        viewModelScope.launch {
            var retryCount = 0
            var success = false
            
            while (retryCount < maxRetries && !success) {
                try {
                    val response = withContext(Dispatchers.IO) {
                        userApi.getUserList()
                    }
                    
                    if (response.isSuccess() && response.data != null) {
                        _userState.value = UiState.Success(response.data)
                        success = true
                    } else {
                        retryCount++
                        if (retryCount < maxRetries) {
                            delay(1000 * retryCount)  // 指数退避
                        }
                    }
                    
                } catch (e: Exception) {
                    retryCount++
                    if (retryCount < maxRetries) {
                        delay(1000 * retryCount)
                    } else {
                        val errorData = e.handleException()
                        _userState.value = UiState.Error(errorData.message, e)
                        sendEvent(UiEvent.ShowToast("加载失败，已重试 $maxRetries 次"))
                    }
                }
            }
        }
    }

    // ========== 辅助方法 ==========
    
    /**
     * 处理业务错误
     */
    private fun handleBusinessError(code: Int, message: String) {
        when (code) {
            400 -> {
                _userState.value = UiState.Error("请求参数错误")
                sendEvent(UiEvent.ShowToast("请求参数错误"))
            }
            401 -> {
                _userState.value = UiState.Error("未授权，请重新登录")
                sendEvent(UiEvent.Navigate("/login"))
            }
            403 -> {
                _userState.value = UiState.Error("没有权限访问")
                sendEvent(UiEvent.ShowToast("没有权限访问"))
            }
            404 -> {
                _userState.value = UiState.Error("资源不存在")
                sendEvent(UiEvent.ShowToast("资源不存在"))
            }
            500 -> {
                _userState.value = UiState.Error("服务器错误")
                sendEvent(UiEvent.ShowToast("服务器错误，请稍后重试"))
            }
            else -> {
                _userState.value = UiState.Error(message)
                sendEvent(UiEvent.ShowToast(message))
            }
        }
    }
}

/**
 * API 接口示例
 */
interface UserApi {
    suspend fun getUserList(): ApiResponse<List<User>>
    suspend fun getUserPosts(): ApiResponse<List<Post>>
}

/**
 * 数据模型示例
 */
data class User(
    val id: String,
    val name: String,
    val email: String
)

data class Post(
    val id: String,
    val title: String,
    val content: String
)
