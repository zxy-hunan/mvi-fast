package com.mvi.demo.ui

import com.mvi.core.base.MviIntent

/**
 * 用户列表 Intent
 */
sealed class UserListIntent : MviIntent {
    /**
     * 加载用户列表
     */
    data object LoadUsers : UserListIntent()

    /**
     * 刷新用户列表
     */
    data object RefreshUsers : UserListIntent()

    /**
     * 删除用户
     */
    data class DeleteUser(val userId: String) : UserListIntent()
}
