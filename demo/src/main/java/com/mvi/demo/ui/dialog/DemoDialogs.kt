package com.mvi.demo.ui.dialog

import android.app.Activity
import android.view.Gravity
import android.view.LayoutInflater
import com.mvi.ui.base.MviBottomDialog
import com.mvi.ui.base.MviCenterDialog
import com.mvi.ui.base.MviDialog
import com.mvi.demo.databinding.DialogConfirmBinding
import com.mvi.demo.databinding.DialogInputBinding
import com.mvi.demo.databinding.DialogListBinding
import per.goweii.layer.core.anim.AnimStyle
import per.goweii.layer.core.widget.SwipeLayout
import per.goweii.layer.dialog.DialogLayer

/**
 * 确认对话框
 * 居中显示，带标题、内容、确认和取消按钮
 */
class ConfirmDialog(activity: Activity) : MviCenterDialog<DialogConfirmBinding>(activity) {

    private var title: String = "提示"
    private var content: String = ""
    private var confirmText: String = "确定"
    private var cancelText: String = "取消"
    private var onConfirmClick: (() -> Unit)? = null
    private var onCancelClick: (() -> Unit)? = null

    override fun createBinding(inflater: LayoutInflater): DialogConfirmBinding {
        return DialogConfirmBinding.inflate(inflater)
    }

    override fun initView() {
        binding.apply {
            tvTitle.text = title
            tvContent.text = content
            btnConfirm.text = confirmText
            btnCancel.text = cancelText

            btnConfirm.setOnClickListener {
                onConfirmClick?.invoke()
                dismiss()
            }

            btnCancel.setOnClickListener {
                onCancelClick?.invoke()
                dismiss()
            }

            ivClose.setOnClickListener {
                dismiss()
            }
        }
    }

    fun setTitle(title: String): ConfirmDialog {
        this.title = title
        return this
    }

    fun setContent(content: String): ConfirmDialog {
        this.content = content
        return this
    }

    fun setConfirmText(text: String): ConfirmDialog {
        this.confirmText = text
        return this
    }

    fun setCancelText(text: String): ConfirmDialog {
        this.cancelText = text
        return this
    }

    fun setOnConfirmClick(callback: () -> Unit): ConfirmDialog {
        this.onConfirmClick = callback
        return this
    }

    fun setOnCancelClick(callback: () -> Unit): ConfirmDialog {
        this.onCancelClick = callback
        return this
    }
}

/**
 * 输入对话框
 * 居中显示，带输入框
 */
class InputDialog(activity: Activity) : MviCenterDialog<DialogInputBinding>(activity) {

    private var title: String = "请输入"
    private var hint: String = ""
    private var confirmText: String = "确定"
    private var cancelText: String = "取消"
    private var onConfirmClick: ((String) -> Unit)? = null

    override fun createBinding(inflater: LayoutInflater): DialogInputBinding {
        return DialogInputBinding.inflate(inflater)
    }

    override fun initView() {
        binding.apply {
            tvTitle.text = title
            etInput.hint = hint

            btnConfirm.text = confirmText
            btnCancel.text = cancelText

            btnConfirm.setOnClickListener {
                val input = etInput.text.toString()
                onConfirmClick?.invoke(input)
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

    fun setTitle(title: String): InputDialog {
        this.title = title
        return this
    }

    fun setHint(hint: String): InputDialog {
        this.hint = hint
        return this
    }

    fun setConfirmText(text: String): InputDialog {
        this.confirmText = text
        return this
    }

    fun setCancelText(text: String): InputDialog {
        this.cancelText = text
        return this
    }

    fun setOnConfirmClick(callback: (String) -> Unit): InputDialog {
        this.onConfirmClick = callback
        return this
    }
}

/**
 * 列表选择对话框
 * 底部弹出，支持下滑关闭
 */
class ListDialog(activity: Activity) : MviBottomDialog<DialogListBinding>(activity) {

    private var title: String = "请选择"
    private var items: List<String> = emptyList()
    private var onItemClick: ((Int, String) -> Unit)? = null

    override fun createBinding(inflater: LayoutInflater): DialogListBinding {
        return DialogListBinding.inflate(inflater)
    }

    override fun initView() {
        binding.apply {
            tvTitle.text = title

            // 动态添加列表项
            llItems.removeAllViews()
            items.forEachIndexed { index, item ->
                val itemView = LayoutInflater.from(activity)
                    .inflate(com.mvi.demo.R.layout.item_dialog_list, llItems, false)

                itemView.findViewById<android.widget.TextView>(com.mvi.demo.R.id.tvItem).text = item
                itemView.setOnClickListener {
                    onItemClick?.invoke(index, item)
                    dismiss()
                }

                llItems.addView(itemView)
            }

            ivClose.setOnClickListener {
                dismiss()
            }

            btnCancel.setOnClickListener {
                dismiss()
            }
        }
    }

    fun setTitle(title: String): ListDialog {
        this.title = title
        return this
    }

    fun setItems(items: List<String>): ListDialog {
        this.items = items
        return this
    }

    fun setOnItemClick(callback: (Int, String) -> Unit): ListDialog {
        this.onItemClick = callback
        return this
    }
}
