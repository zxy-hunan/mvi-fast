package com.mvi.demo.ui

import com.mvi.core.base.MviIntent

/**
 * Demo Intent
 */
sealed class DemoIntent : MviIntent {
    /** 加载用户列表 */
    data object LoadUsers : DemoIntent()

    /** 刷新数据 */
    data object Refresh : DemoIntent()

    /** 删除用户 */
    data class DeleteUser(val userId: String) : DemoIntent()
}
