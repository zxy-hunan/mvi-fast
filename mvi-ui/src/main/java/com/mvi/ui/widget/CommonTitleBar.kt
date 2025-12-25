package com.mvi.ui.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mvi.ui.R

/**
 * 通用标题导航栏
 *
 * 功能：
 * 1. 支持左侧返回按钮/自定义按钮
 * 2. 支持中间标题文字
 * 3. 支持右侧按钮（文字/图标）
 * 4. 支持自定义样式
 * 5. 提供点击事件回调
 *
 * 使用示例:
 * ```xml
 * <com.mvi.ui.widget.CommonTitleBar
 *     android:id="@+id/titleBar"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content" />
 * ```
 *
 * ```kotlin
 * titleBar.apply {
 *     setTitle("我的页面")
 *     setOnLeftClickListener { finish() }
 *     setRightText("保存")
 *     setOnRightClickListener { save() }
 * }
 * ```
 *
 * @author MVI Framework
 * @date 2024/12/24
 */
class CommonTitleBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val leftButton: ImageView
    private val titleText: TextView
    private val rightTextButton: TextView
    private val rightIconButton: ImageView

    init {
        // 加载布局
        LayoutInflater.from(context).inflate(R.layout.layout_common_title_bar, this, true)

        // 初始化View
        leftButton = findViewById(R.id.title_bar_left_button)
        titleText = findViewById(R.id.title_bar_title)
        rightTextButton = findViewById(R.id.title_bar_right_text_button)
        rightIconButton = findViewById(R.id.title_bar_right_icon_button)

        // 从XML属性中读取配置
        attrs?.let { initAttrs(it) }
    }

    private fun initAttrs(attrs: AttributeSet) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.CommonTitleBar)
        try {
            // 标题
            ta.getString(R.styleable.CommonTitleBar_titleText)?.let {
                setTitle(it)
            }

            // 标题颜色
            if (ta.hasValue(R.styleable.CommonTitleBar_titleTextColor)) {
                val titleColor = ta.getColor(R.styleable.CommonTitleBar_titleTextColor, 0)
                setTitleColor(titleColor)
            }

            // 背景色
            if (ta.hasValue(R.styleable.CommonTitleBar_titleBarBackground)) {
                val bgColor = ta.getColor(R.styleable.CommonTitleBar_titleBarBackground, 0)
                setBackgroundColor(bgColor)
            }

            // 左侧按钮显示
            val showLeftButton = ta.getBoolean(R.styleable.CommonTitleBar_showLeftButton, true)
            leftButton.visibility = if (showLeftButton) VISIBLE else GONE

            // 左侧按钮图标
            if (ta.hasValue(R.styleable.CommonTitleBar_leftIcon)) {
                val leftIcon = ta.getDrawable(R.styleable.CommonTitleBar_leftIcon)
                leftIcon?.let { setLeftIcon(it) }
            }

            // 右侧文字
            ta.getString(R.styleable.CommonTitleBar_rightText)?.let {
                setRightText(it)
            }

            // 右侧文字颜色
            if (ta.hasValue(R.styleable.CommonTitleBar_rightTextColor)) {
                val rightTextColor = ta.getColor(R.styleable.CommonTitleBar_rightTextColor, 0)
                setRightTextColor(rightTextColor)
            }

            // 右侧图标
            if (ta.hasValue(R.styleable.CommonTitleBar_rightIcon)) {
                val rightIcon = ta.getDrawable(R.styleable.CommonTitleBar_rightIcon)
                rightIcon?.let { setRightIcon(it) }
            }

        } finally {
            ta.recycle()
        }
    }

    // ========== 标题相关 ==========

    /**
     * 设置标题
     */
    fun setTitle(title: String) {
        titleText.text = title
    }

    /**
     * 设置标题
     */
    fun setTitle(@StringRes titleRes: Int) {
        titleText.setText(titleRes)
    }

    /**
     * 设置标题颜色
     */
    fun setTitleColor(@ColorInt color: Int) {
        titleText.setTextColor(color)
    }

    /**
     * 获取标题TextView
     */
    fun getTitleView(): TextView = titleText

    // ========== 左侧按钮相关 ==========

    /**
     * 设置左侧按钮图标
     */
    fun setLeftIcon(@DrawableRes iconRes: Int) {
        leftButton.setImageResource(iconRes)
        leftButton.visibility = VISIBLE
    }

    /**
     * 设置左侧按钮图标
     */
    fun setLeftIcon(drawable: Drawable?) {
        leftButton.setImageDrawable(drawable)
        leftButton.visibility = VISIBLE
    }

    /**
     * 隐藏左侧按钮
     */
    fun hideLeftButton() {
        leftButton.visibility = GONE
    }

    /**
     * 显示左侧按钮
     */
    fun showLeftButton() {
        leftButton.visibility = VISIBLE
    }

    /**
     * 设置左侧按钮点击事件
     */
    fun setOnLeftClickListener(listener: OnClickListener?) {
        leftButton.setOnClickListener(listener)
    }

    /**
     * 设置左侧按钮点击事件
     */
    fun setOnLeftClickListener(listener: (View) -> Unit) {
        leftButton.setOnClickListener(listener)
    }

    /**
     * 获取左侧按钮View
     */
    fun getLeftButton(): ImageView = leftButton

    // ========== 右侧按钮相关 ==========

    /**
     * 设置右侧文字按钮
     */
    fun setRightText(text: String) {
        rightTextButton.text = text
        rightTextButton.visibility = VISIBLE
        rightIconButton.visibility = GONE
    }

    /**
     * 设置右侧文字按钮
     */
    fun setRightText(@StringRes textRes: Int) {
        rightTextButton.setText(textRes)
        rightTextButton.visibility = VISIBLE
        rightIconButton.visibility = GONE
    }

    /**
     * 设置右侧文字颜色
     */
    fun setRightTextColor(@ColorInt color: Int) {
        rightTextButton.setTextColor(color)
    }

    /**
     * 设置右侧图标按钮
     */
    fun setRightIcon(@DrawableRes iconRes: Int) {
        rightIconButton.setImageResource(iconRes)
        rightIconButton.visibility = VISIBLE
        rightTextButton.visibility = GONE
    }

    /**
     * 设置右侧图标按钮
     */
    fun setRightIcon(drawable: Drawable?) {
        rightIconButton.setImageDrawable(drawable)
        rightIconButton.visibility = VISIBLE
        rightTextButton.visibility = GONE
    }

    /**
     * 隐藏右侧按钮
     */
    fun hideRightButton() {
        rightTextButton.visibility = GONE
        rightIconButton.visibility = GONE
    }

    /**
     * 设置右侧按钮点击事件（文字或图标按钮）
     */
    fun setOnRightClickListener(listener: OnClickListener?) {
        rightTextButton.setOnClickListener(listener)
        rightIconButton.setOnClickListener(listener)
    }

    /**
     * 设置右侧按钮点击事件（文字或图标按钮）
     */
    fun setOnRightClickListener(listener: (View) -> Unit) {
        rightTextButton.setOnClickListener(listener)
        rightIconButton.setOnClickListener(listener)
    }

    /**
     * 获取右侧文字按钮View
     */
    fun getRightTextButton(): TextView = rightTextButton

    /**
     * 获取右侧图标按钮View
     */
    fun getRightIconButton(): ImageView = rightIconButton

    /**
     * 清理所有监听器 - 防止内存泄漏
     * 在 Activity/Fragment onDestroy 时调用
     */
    fun clearListeners() {
        leftButton.setOnClickListener(null)
        rightTextButton.setOnClickListener(null)
        rightIconButton.setOnClickListener(null)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // View 从窗口分离时自动清理监听器
        clearListeners()
    }
}
