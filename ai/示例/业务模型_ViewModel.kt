package com.mvi.example.ui

import androidx.lifecycle.viewModelScope
import com.mvi.core.base.MviViewModel
import com.mvi.core.base.UiState
import com.mvi.core.base.UiEvent
import com.mvi.core.ext.launchRequest
import com.mvi.demo.api.UserApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 业务模型 - ViewModel 示例
 * 
 * 这是一个完整的 ViewModel 示例，展示了如何：
 * 1. 定义 Intent
 * 2. 管理 State
 * 3. 处理业务逻辑
 * 4. 发送 Event
 */
class UserViewModel(
    private val userApi: UserApi
) : MviViewModel<UserIntent>() {

    // ========== State 定义 ==========
    
    /**
     * 用户列表状态
     * 使用私有可变 StateFlow + 公开只读 StateFlow 模式
     */
    private val _userState = MutableStateFlow<UiState<List<User>>>(UiState.Idle)
    val userState: StateFlow<UiState<List<User>>> = _userState.asStateFlow()

    /**
     * 用户详情状态（示例：多个状态）
     */
    private val _userDetailState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val userDetailState: StateFlow<UiState<User>> = _userDetailState.asStateFlow()

    // ========== Intent 处理 ==========
    
    override fun handleIntent(intent: UserIntent) {
        when (intent) {
            is UserIntent.LoadUsers -> loadUsers()
            is UserIntent.RefreshUsers -> refreshUsers()
            is UserIntent.LoadUserDetail -> loadUserDetail(intent.userId)
            is UserIntent.DeleteUser -> deleteUser(intent.userId)
            is UserIntent.SearchUser -> searchUser(intent.keyword)
        }
    }

    // ========== 业务逻辑方法 ==========
    
    /**
     * 加载用户列表
     * 使用 launchRequest 自动处理 Loading、Success、Error 状态
     */
    private fun loadUsers() {
        launchRequest(_userState, showLoading = true) {
            userApi.getUserList()
        }
    }

    /**
     * 刷新用户列表
     * 不显示 Loading，使用下拉刷新的 Loading
     */
    private fun refreshUsers() {
        launchRequest(_userState, showLoading = false) {
            userApi.getUserList()
        }.invokeOnCompletion {
            // 刷新完成后显示提示
            sendEvent(UiEvent.ShowToast("刷新成功"))
        }
    }

    /**
     * 加载用户详情
     */
    private fun loadUserDetail(userId: String) {
        launchRequest(_userDetailState) {
            userApi.getUserDetail(userId)
        }
    }

    /**
     * 删除用户
     * 删除成功后刷新列表
     */
    private fun deleteUser(userId: String) {
        viewModelScope.launch {
            try {
                // 显示 Loading（可选）
                showLoading(true)
                
                // 执行删除请求
                val response = withContext(Dispatchers.IO) {
                    userApi.deleteUser(userId)
                }
                
                if (response.isSuccess()) {
                    // ✅ 发送成功提示
                    sendEvent(UiEvent.ShowToast("删除成功"))
                    
                    // ✅ 刷新列表
                    sendIntent(UserIntent.LoadUsers)
                } else {
                    // ✅ 发送错误提示
                    sendEvent(UiEvent.ShowToast(response.message))
                }
                
            } catch (e: Exception) {
                // ✅ 统一异常处理
                val errorData = e.handleException()
                sendEvent(UiEvent.ShowToast(errorData.message))
            } finally {
                showLoading(false)
            }
        }
    }

    /**
     * 搜索用户
     * 使用防抖处理搜索输入
     */
    private fun searchUser(keyword: String) {
        viewModelScope.launch {
            // 防抖：延迟 300ms 执行
            delay(300)
            
            if (keyword.isEmpty()) {
                // 关键词为空，加载全部
                loadUsers()
                return@launch
            }
            
            // 执行搜索
            launchRequest(_userState, showLoading = false) {
                userApi.searchUsers(keyword)
            }
        }
    }

    // ========== 辅助方法 ==========
    
    /**
     * 获取当前用户列表（用于其他操作）
     */
    fun getCurrentUsers(): List<User> {
        return (_userState.value as? UiState.Success)?.data ?: emptyList()
    }

    /**
     * 检查是否有数据
     */
    fun hasUsers(): Boolean {
        return _userState.value is UiState.Success && 
               (_userState.value as? UiState.Success)?.data?.isNotEmpty() == true
    }
}

/**
 * User Intent 定义
 * 
 * 命名规范：
 * - 使用动词开头：LoadUsers, DeleteUser, SearchUser
 * - 简单操作使用 object：data object LoadUsers
 * - 带参数使用 data class：data class DeleteUser(val userId: String)
 */
sealed class UserIntent : MviIntent {
    /** 加载用户列表 */
    data object LoadUsers : UserIntent()
    
    /** 刷新用户列表 */
    data object RefreshUsers : UserIntent()
    
    /** 加载用户详情 */
    data class LoadUserDetail(val userId: String) : UserIntent()
    
    /** 删除用户 */
    data class DeleteUser(val userId: String) : UserIntent()
    
    /** 搜索用户 */
    data class SearchUser(val keyword: String) : UserIntent()
}

/**
 * User 数据模型
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatar: String = ""
)
