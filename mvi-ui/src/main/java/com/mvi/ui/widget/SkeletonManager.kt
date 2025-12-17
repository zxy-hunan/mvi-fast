package com.mvi.ui.widget

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children

/**
 * 骨架屏管理器
 * 支持为任意View添加骨架屏效果
 *
 * 使用示例:
 * ```kotlin
 * // 创建骨架屏管理器
 * val skeletonManager = SkeletonManager(binding.recyclerView)
 *
 * // 显示骨架屏
 * skeletonManager.show()
 *
 * // 隐藏骨架屏
 * skeletonManager.hide()
 *
 * // 自定义配置
 * val config = SkeletonConfig(
 *     shimmerColor = Color.parseColor("#E0E0E0"),
 *     shimmerHighlightColor = Color.parseColor("#F5F5F5"),
 *     shimmerDuration = 1500L
 * )
 * val skeletonManager = SkeletonManager(binding.root, config)
 * ```
 */
class SkeletonManager(
    private val targetView: View,
    private val config: SkeletonConfig = SkeletonConfig()
) {

    private var skeletonView: View? = null
    private var shimmerAnimator: ValueAnimator? = null
    private var originalVisibility: Int = View.VISIBLE

    /**
     * 显示骨架屏
     */
    fun show() {
        if (skeletonView != null) return

        // 保存原始可见性
        originalVisibility = targetView.visibility

        // 创建骨架屏视图
        skeletonView = createSkeletonView()

        // 替换目标视图
        val parent = targetView.parent as? ViewGroup
        parent?.let {
            val index = it.indexOfChild(targetView)
            val layoutParams = targetView.layoutParams

            // 隐藏原始视图
            targetView.visibility = View.INVISIBLE

            // 添加骨架屏视图
            skeletonView?.layoutParams = layoutParams
            it.addView(skeletonView, index + 1)

            // 启动闪烁动画
            startShimmerAnimation()
        }
    }

    /**
     * 隐藏骨架屏
     */
    fun hide() {
        // 停止动画
        stopShimmerAnimation()

        // 恢复原始视图
        targetView.visibility = originalVisibility

        // 移除骨架屏视图
        skeletonView?.let {
            val parent = it.parent as? ViewGroup
            parent?.removeView(it)
        }
        skeletonView = null
    }

    /**
     * 是否正在显示
     */
    fun isShowing(): Boolean = skeletonView != null

    /**
     * 创建骨架屏视图
     */
    private fun createSkeletonView(): View {
        return config.skeletonViewCreator?.invoke(targetView)
            ?: createDefaultSkeletonView()
    }

    /**
     * 创建默认骨架屏视图
     */
    private fun createDefaultSkeletonView(): View {
        return SkeletonView(targetView.context, targetView, config)
    }

    /**
     * 启动闪烁动画
     */
    private fun startShimmerAnimation() {
        if (!config.enableShimmer) return

        shimmerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = config.shimmerDuration
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART

            addUpdateListener {
                skeletonView?.invalidate()
            }

            start()
        }
    }

    /**
     * 停止闪烁动画
     */
    private fun stopShimmerAnimation() {
        shimmerAnimator?.cancel()
        shimmerAnimator = null
    }
}

/**
 * 骨架屏视图
 */
private class SkeletonView(
    context: android.content.Context,
    private val targetView: View,
    private val config: SkeletonConfig
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var shimmerTranslate = 0f

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 绘制骨架背景
        paint.color = config.skeletonColor
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // 绘制闪烁效果
        if (config.enableShimmer) {
            drawShimmer(canvas)
        }
    }

    /**
     * 绘制闪烁效果
     */
    private fun drawShimmer(canvas: Canvas) {
        val shimmerWidth = width * config.shimmerWidth
        shimmerTranslate = (shimmerTranslate + config.shimmerSpeed) % (width + shimmerWidth)

        val shader = LinearGradient(
            shimmerTranslate - shimmerWidth,
            0f,
            shimmerTranslate,
            0f,
            intArrayOf(
                config.shimmerColor,
                config.shimmerHighlightColor,
                config.shimmerColor
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        paint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // 继续动画
        postInvalidateOnAnimation()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 使用目标视图的尺寸
        setMeasuredDimension(
            targetView.measuredWidth,
            targetView.measuredHeight
        )
    }
}

/**
 * 骨架屏配置
 */
data class SkeletonConfig(
    // 骨架屏背景颜色
    val skeletonColor: Int = 0xFFE0E0E0.toInt(),

    // 是否启用闪烁效果
    val enableShimmer: Boolean = true,

    // 闪烁颜色
    val shimmerColor: Int = 0xFFE0E0E0.toInt(),

    // 闪烁高亮颜色
    val shimmerHighlightColor: Int = 0xFFF5F5F5.toInt(),

    // 闪烁动画时长(毫秒)
    val shimmerDuration: Long = 1500L,

    // 闪烁宽度比例
    val shimmerWidth: Float = 0.3f,

    // 闪烁速度
    val shimmerSpeed: Float = 10f,

    // 自定义骨架屏视图创建器
    val skeletonViewCreator: ((View) -> View)? = null
)

/**
 * RecyclerView 骨架屏管理器
 * 用于显示列表的骨架屏效果
 */
class RecyclerViewSkeletonManager(
    private val recyclerView: androidx.recyclerview.widget.RecyclerView,
    private val skeletonLayoutResId: Int,
    private val itemCount: Int = 10,
    private val config: SkeletonConfig = SkeletonConfig()
) {

    private var originalAdapter: androidx.recyclerview.widget.RecyclerView.Adapter<*>? = null
    private var skeletonAdapter: SkeletonAdapter? = null

    /**
     * 显示骨架屏
     */
    fun show() {
        if (skeletonAdapter != null) return

        // 保存原始适配器
        originalAdapter = recyclerView.adapter

        // 设置骨架屏适配器
        skeletonAdapter = SkeletonAdapter(skeletonLayoutResId, itemCount, config)
        recyclerView.adapter = skeletonAdapter
    }

    /**
     * 隐藏骨架屏
     */
    fun hide() {
        // 恢复原始适配器
        recyclerView.adapter = originalAdapter
        skeletonAdapter = null
    }

    /**
     * 是否正在显示
     */
    fun isShowing(): Boolean = skeletonAdapter != null

    /**
     * 骨架屏适配器
     */
    private class SkeletonAdapter(
        private val layoutResId: Int,
        private val itemCount: Int,
        private val config: SkeletonConfig
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<SkeletonViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkeletonViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(layoutResId, parent, false)
            return SkeletonViewHolder(view, config)
        }

        override fun onBindViewHolder(holder: SkeletonViewHolder, position: Int) {
            holder.bind()
        }

        override fun getItemCount(): Int = itemCount
    }

    /**
     * 骨架屏ViewHolder
     */
    private class SkeletonViewHolder(
        itemView: View,
        private val config: SkeletonConfig
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {

        fun bind() {
            // 为所有子视图添加骨架屏效果
            applySkeletonToView(itemView)
        }

        private fun applySkeletonToView(view: View) {
            when (view) {
                is ViewGroup -> {
                    view.children.forEach { child ->
                        applySkeletonToView(child)
                    }
                }
                else -> {
                    // 为单个视图添加骨架屏背景
                    view.setBackgroundColor(config.skeletonColor)
                }
            }
        }
    }
}
