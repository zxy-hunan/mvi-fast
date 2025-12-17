package com.mvi.demo.ui

import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Toast
import com.dylanc.viewbinding.getBinding
import com.mvi.core.base.MviActivity
import com.mvi.ui.base.MviDialog
import com.mvi.core.base.MviIntent
import com.mvi.core.base.MviViewModel
import com.mvi.demo.R
import com.mvi.demo.databinding.ActivityDialogDemoBinding
import com.mvi.demo.databinding.DialogConfirmBinding
import com.mvi.demo.ui.dialog.ConfirmDialog
import com.mvi.demo.ui.dialog.InputDialog
import com.mvi.demo.ui.dialog.ListDialog
import com.mvi.demo.ui.dialog.UserInfoDialog
import per.goweii.layer.core.anim.AnimStyle
import per.goweii.layer.core.ktx.onClick
import per.goweii.layer.core.ktx.onPostShow
import per.goweii.layer.dialog.ktx.contentView

/**
 * Dialog 示例页面
 * 展示 MviDialog 的各种用法
 */
class DialogDemoActivity : MviActivity<ActivityDialogDemoBinding, DialogDemoViewModel, DialogDemoIntent>() {

    override fun createBinding(): ActivityDialogDemoBinding {
        return ActivityDialogDemoBinding.inflate(LayoutInflater.from(this))
    }

    override fun getViewModelClass(): Class<DialogDemoViewModel> {
        return DialogDemoViewModel::class.java
    }

    override fun initView() {
        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar?.title = "Dialog 示例"
        binding.toolbar?.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        // 确认对话框
        binding.btnShowConfirmDialog.setOnClickListener {
            showConfirmDialog()
        }

        // 输入对话框
        binding.btnShowInputDialog.setOnClickListener {
            showInputDialog()
        }

        // 列表对话框
        binding.btnShowListDialog.setOnClickListener {
            showListDialog()
        }

        // 底部对话框（自定义）
        binding.btnShowBottomDialog.setOnClickListener {
            showBottomDialog()
        }

        // 不可关闭对话框
        binding.btnShowNonCancelableDialog.setOnClickListener {
            showNonCancelableDialog()
        }

        // 函数式方式 - 居中对话框
        binding.btnShowFunctionalCenter?.setOnClickListener {
            showFunctionalCenterDialog()
        }

        // 函数式方式 - 底部对话框
        binding.btnShowFunctionalBottom?.setOnClickListener {
            showFunctionalBottomDialog()
        }

        // ViewModel 对话框示例
        binding.btnShowViewModelDialog?.setOnClickListener {
            showViewModelDialog()
        }
    }

    /**
     * 确认对话框示例
     */
    private fun showConfirmDialog() {
        ConfirmDialog(this)
            .setTitle("删除确认")
            .setContent("确定要删除这条记录吗？此操作不可恢复。")
            .setConfirmText("确定删除")
            .setCancelText("取消")
            .setOnConfirmClick {
                Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
            }
            .setOnCancelClick {
                Toast.makeText(this, "已取消", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    /**
     * 输入对话框示例
     */
    private fun showInputDialog() {
        InputDialog(this)
            .setTitle("请输入姓名")
            .setHint("请输入您的姓名")
            .setConfirmText("提交")
            .setCancelText("取消")
            .setOnConfirmClick { input ->
                if (input.isNotEmpty()) {
                    Toast.makeText(this, "您输入的是：$input", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "输入不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    /**
     * 列表对话框示例
     */
    private fun showListDialog() {
        val items = listOf(
            "拍照",
            "从相册选择",
            "从文件选择",
            "取消"
        )

        ListDialog(this)
            .setTitle("请选择图片来源")
            .setItems(items)
            .setOnItemClick { index, item ->
                Toast.makeText(this, "选择了：$item (索引:$index)", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    /**
     * 底部对话框示例（使用 MviBottomDialog）
     */
    private fun showBottomDialog() {
        val items = listOf(
            "分享到微信",
            "分享到朋友圈",
            "分享到QQ",
            "分享到微博",
            "复制链接"
        )

        ListDialog(this)
            .setTitle("分享到")
            .setItems(items)
            .setOnItemClick { index, item ->
                Toast.makeText(this, "分享：$item", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    /**
     * 不可关闭对话框示例
     */
    private fun showNonCancelableDialog() {
        ConfirmDialog(this)
            .setTitle("重要提示")
            .setContent("这是一个重要的提示信息，必须点击按钮才能关闭。")
            .setConfirmText("我知道了")
            .setCancelText("稍后再说")
            .setOnConfirmClick {
                Toast.makeText(this, "已确认", Toast.LENGTH_SHORT).show()
            }
            .apply {
                // 设置不可通过点击外部或返回键关闭
                setCancelableOnTouchOutside(false)
                setCancelableOnClickKeyBack(false)
            }
            .show()
    }

    /**
     * 函数式方式 - 居中对话框示例
     * 类似于你项目中的使用方式
     */
    private fun showFunctionalCenterDialog() {
        MviDialog.showCenter(this) {
            contentView(R.layout.dialog_confirm)
            onPostShow {
                val binding = viewHolder.content.getBinding<DialogConfirmBinding>()
                binding?.apply {
                    tvTitle.text = "函数式对话框"
                    tvContent.text = "这是使用函数式方式创建的对话框，类似于你项目中的 showFundDialog 方法。"
                    btnConfirm.text = "确定"
                    btnCancel.text = "取消"

                    btnConfirm.setOnClickListener {
                        Toast.makeText(this@DialogDemoActivity, "点击了确定", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }

                    btnCancel.setOnClickListener {
                        dismiss()
                    }

                    ivClose.setOnClickListener {
                        dismiss()
                    }
                }
            }
        }
    }

    /**
     * 函数式方式 - 底部对话框示例
     * 完全复刻你项目中的使用方式
     */
    private fun showFunctionalBottomDialog() {
        MviDialog.showBottom(this) {
            contentView(R.layout.dialog_confirm)
            onPostShow {
                val binding = viewHolder.content.getBinding<DialogConfirmBinding>()
                binding?.apply {
                    tvTitle.text = "底部弹窗"
                    tvContent.text = "这是从底部弹出的对话框，支持下滑关闭。\n\n这种方式完全兼容你项目中的用法。"
                    btnConfirm.text = "选项1"
                    btnCancel.text = "选项2"

                    btnConfirm.setOnClickListener {
                        Toast.makeText(this@DialogDemoActivity, "选择了选项1", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }

                    btnCancel.setOnClickListener {
                        Toast.makeText(this@DialogDemoActivity, "选择了选项2", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                }
            }
            // 也可以使用 onClick 简化关闭操作
            onClick(R.id.ivClose) { dismiss() }
        }
    }

    /**
     * ViewModel 对话框示例
     * 演示如何使用 MviViewModelDialog
     */
    private fun showViewModelDialog() {
        UserInfoDialog(this)
            .setOnSaveSuccess { userInfo ->
                Toast.makeText(
                    this,
                    "保存成功: ${userInfo.name}, ${userInfo.age}岁",
                    Toast.LENGTH_SHORT
                ).show()
                // 可以在这里处理保存成功后的逻辑
            }
            .show()
    }

    override fun observeData() {
        // 当前页面不需要观察数据
    }
}

/**
 * DialogDemo Intent
 */
sealed class DialogDemoIntent : MviIntent {
    data object Init : DialogDemoIntent()
}

/**
 * DialogDemo ViewModel
 */
class DialogDemoViewModel : MviViewModel<DialogDemoIntent>() {
    override fun handleIntent(intent: DialogDemoIntent) {
        when (intent) {
            is DialogDemoIntent.Init -> {
                // 初始化逻辑
            }
        }
    }
}
