package com.mvi.demo.ui

import android.content.Intent
import android.view.LayoutInflater
import com.mvi.core.base.MviActivity
import com.mvi.core.base.UiState
import com.mvi.core.ext.collectOn
import com.mvi.demo.databinding.ActivityDemoBinding

/**
 * Demo Activity
 *
 * 展示如何使用MVI框架:
 * 1. 继承MviActivity
 * 2. 观察StateFlow状态变化
 * 3. 发送Intent触发业务逻辑
 */
class DemoActivity : MviActivity<ActivityDemoBinding, DemoViewModel, DemoIntent>() {

    override fun createBinding(): ActivityDemoBinding {
        return ActivityDemoBinding.inflate(LayoutInflater.from(this))
    }

    override fun getViewModelClass(): Class<DemoViewModel> {
        return DemoViewModel::class.java
    }

    override fun initView() {
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // 原有的功能按钮
        binding.btnLoadUsers.setOnClickListener {
            sendIntent(DemoIntent.LoadUsers)
        }

        binding.btnRefresh.setOnClickListener {
            sendIntent(DemoIntent.Refresh)
        }

        // 跳转到缺省页示例
        binding.btnEmptyStateDemo?.setOnClickListener {
            startActivity(Intent(this, EmptyStateDemoActivity::class.java))
        }

        // 跳转到骨架屏示例
        binding.btnSkeletonDemo?.setOnClickListener {
            startActivity(Intent(this, SkeletonDemoActivity::class.java))
        }

        // 跳转到综合示例
        binding.btnComprehensiveDemo?.setOnClickListener {
            startActivity(Intent(this, ComprehensiveDemoActivity::class.java))
        }

        // 跳转到 Dialog 示例
        binding.btnDialogDemo?.setOnClickListener {
            startActivity(Intent(this, DialogDemoActivity::class.java))
        }

        // 跳转到 MviUi 用户列表示例
        binding.btnUserListDemo?.setOnClickListener {
            startActivity(Intent(this, UserListActivity::class.java))
        }
    }

    override fun observeData() {
        // 观察用户列表状态
        viewModel.userListState.collectOn(this) { state ->
            when (state) {
                is UiState.Idle -> {
                    // 初始状态
                    binding.tvResult.text = "点击按钮加载数据"
                }

                is UiState.Loading -> {
                    // 加载中
                    binding.tvResult.text = "加载中..."
                }

                is UiState.Success -> {
                    // 成功
                    val users = state.data.users
                    val result = buildString {
                        append("用户列表 (共${state.data.total}人):\n\n")
                        users.forEach { user ->
                            append("${user.name}\n")
                            append("Email: ${user.email}\n\n")
                        }
                    }
                    binding.tvResult.text = result
                }

                is UiState.Error -> {
                    // 错误
                    binding.tvResult.text = "错误: ${state.message}"
                }

                is UiState.Empty -> {
                    // 空数据
                    binding.tvResult.text = "暂无数据"
                }

                else -> {

                }
            }
        }
    }

    override fun handleLoading(show: Boolean) {
        // 可以在这里显示/隐藏全局Loading对话框
        binding.progressBar.visibility = if (show) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }
}
