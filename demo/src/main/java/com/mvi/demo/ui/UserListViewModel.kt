package com.mvi.demo.ui

import androidx.lifecycle.viewModelScope
import com.mvi.core.base.UiState
import com.mvi.ui.base.MviUiViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 用户列表 ViewModel
 *
 * 使用 MviUiViewModel 演示空状态和错误状态的自动处理
 */
class UserListViewModel : MviUiViewModel<UserListIntent>() {

    private val _userState = MutableStateFlow<UiState<List<User>>>(UiState.Idle)
    val userState: StateFlow<UiState<List<User>>> = _userState.asStateFlow()

    // 模拟用户数据
    private val mockUsers = listOf(
        User("1", "张三", "zhangsan@example.com", ""),
        User("2", "李四", "lisi@example.com", ""),
        User("3", "王五", "wangwu@example.com", ""),
        User("4", "赵六", "zhaoliu@example.com", ""),
        User("5", "孙七", "sunqi@example.com", "")
    )

    // 模拟加载状态：0=成功, 1=空数据, 2=错误
    private var loadStateIndex = 0

    override fun handleIntent(intent: UserListIntent) {
        when (intent) {
            is UserListIntent.LoadUsers -> loadUsers()
            is UserListIntent.RefreshUsers -> refreshUsers()
            is UserListIntent.DeleteUser -> deleteUser(intent.userId)
        }
    }

    /**
     * 加载用户列表
     */
    private fun loadUsers() {
        viewModelScope.launch {
            _userState.value = UiState.Loading("加载中...")
            showLoading(true)

            try {
                // 模拟网络延迟
                delay(1500)

                showLoading(false)

                // 循环演示不同的状态
                when (loadStateIndex % 3) {
                    0 -> {
                        // 成功加载数据
                        hideEmptyState()
                        _userState.value = UiState.Success(mockUsers)
                        showToast("加载成功")
                    }
                    1 -> {
                        // 空数据状态
                        showEmptyState(
                            message = "还没有用户数据哦~",
                            onRetry = { handleIntent(UserListIntent.LoadUsers) }
                        )
                        _userState.value = UiState.Empty("暂无数据")
                    }
                    2 -> {
                        // 错误状态
                        showErrorState(
                            message = "加载失败，请重试",
                            onRetry = { handleIntent(UserListIntent.LoadUsers) }
                        )
                        _userState.value = UiState.Error("网络错误")
                    }
                }

                // 切换到下一个状态
                loadStateIndex++

            } catch (e: Exception) {
                showLoading(false)
                showErrorState(
                    message = "加载失败: ${e.message}",
                    onRetry = { handleIntent(UserListIntent.LoadUsers) }
                )
                _userState.value = UiState.Error(e.message ?: "未知错误")
            }
        }
    }

    /**
     * 刷新用户列表
     */
    private fun refreshUsers() {
        viewModelScope.launch {
            showToast("刷新中...")
            delay(1000)
            loadUsers()
        }
    }

    /**
     * 删除用户
     */
    private fun deleteUser(userId: String) {
        viewModelScope.launch {
            try {
                delay(500) // 模拟删除操作
                showToast("删除成功")

                // 重新加载
                val currentUsers = (userState.value as? UiState.Success)?.data ?: return@launch
                val updatedUsers = currentUsers.filter { it.id != userId }

                if (updatedUsers.isEmpty()) {
                    showEmptyState("已删除所有用户")
                } else {
                    _userState.value = UiState.Success(updatedUsers)
                }
            } catch (e: Exception) {
                showToast("删除失败: ${e.message}")
            }
        }
    }
}
