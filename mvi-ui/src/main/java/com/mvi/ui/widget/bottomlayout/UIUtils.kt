package com.mvi.ui.widget.bottomlayout

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat

/**
 * @author chaychan
 * @date 2017/3/7  17:19
 */
object UIUtils {
    /**
     * dip-->px
     */
    @JvmStatic
    fun dip2Px(context: Context, dip: Int): Int {
        // px/dip = density;
        // density = dpi/160
        // 320*480 density = 1 1px = 1dp
        // 1280*720 density = 2 2px = 1dp
        val density = context.resources.displayMetrics.density
        return (dip * density + 0.5f).toInt()
    }

    @JvmStatic
    fun dp2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * 将sp值转换为px值，保证文字大小不变
     *
     * @param spValue
     * @return
     */
    @JvmStatic
    fun sp2px(context: Context, spValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (spValue * fontScale + 0.5f).toInt()
    }

    @JvmStatic
    fun px2dp(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    @JvmStatic
    fun getColor(context: Context, colorId: Int): Int {
        return ContextCompat.getColor(context, colorId)
    }

    @JvmStatic
    fun getDrawable(context: Context, resId: Int): Drawable? {
        return ContextCompat.getDrawable(context, resId)
    }
}
