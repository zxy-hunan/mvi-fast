package com.mvi.demo.ui

import com.mvi.core.base.MviViewModel
import com.mvi.core.base.UiState
import com.mvi.core.ext.launchRequest
import com.mvi.core.network.ApiResponse
import com.mvi.demo.api.DemoApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Demo ViewModel
 *
 * 展示如何使用MVI架构:
 * 1. 继承MviViewModel
 * 2. 使用StateFlow管理UI状态
 * 3. handleIntent处理用户操作
 * 4. launchRequest简化网络请求
 */
class DemoViewModel : MviViewModel<DemoIntent>() {

    // API服务
    private val api: DemoApi by lazy {
        // RetrofitClient.create(DemoApi::class.java)
        DemoApi.mockInstance() // 使用Mock数据演示
    }

    // 用户列表状态
    private val _userListState = MutableStateFlow<UiState<UserListData>>(UiState.Idle)
    val userListState: StateFlow<UiState<UserListData>> = _userListState.asStateFlow()

    /**
     * 处理Intent
     */
    override fun handleIntent(intent: DemoIntent) {
        when (intent) {
            is DemoIntent.LoadUsers -> loadUsers()
            is DemoIntent.Refresh -> refresh()
            is DemoIntent.DeleteUser -> deleteUser(intent.userId)
        }
    }

    /**
     * 加载用户列表 - 使用StateFlow自动更新
     */
    private fun loadUsers() {
        launchRequest(
            stateFlow = _userListState,
            showLoading = true
        ) {
            api.getUserList()
        }
    }

    /**
     * 刷新数据 - 使用回调方式
     */
    private fun refresh() {
        launchRequest(
            showLoading = false,
            onSuccess = { data: UserListData ->
                _userListState.value = UiState.Success(data)
                showToast("刷新成功")
            },
            onError = { message, _ ->
                showToast("刷新失败: $message")
            }
        ) {
            api.getUserList()
        }
    }

    /**
     * 删除用户
     */
    private fun deleteUser(userId: String) {
        launchRequest(
            onSuccess = { success: Boolean ->
                if (success) {
                    showToast("删除成功")
                    // 重新加载列表
                    loadUsers()
                }
            }
        ) {
            api.deleteUser(userId)
        }
    }
}
