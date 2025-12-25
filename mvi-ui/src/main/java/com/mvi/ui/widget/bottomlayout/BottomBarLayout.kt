package com.mvi.ui.widget.bottomlayout

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.mvi.ui.R

/**
 * @author ChayChan
 * @description: 底部页签根节点
 * @date 2017/6/23  11:02
 */
class BottomBarLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), ViewPager.OnPageChangeListener {

    private var titleTextBold = false
    private var titleTextSize = 12
    private var titleNormalColor = 0
    private var titleSelectedColor = 0
    private var marginTop = 0
    private var iconWidth = 0
    private var iconHeight = 0
    private var itemPadding = 0
    private var unreadTextSize = 10
    private var unreadNumThreshold = 99
    private var unreadTextColor = 0
    private var unreadTextBg: Drawable? = null
    private var msgTextSize = 6
    private var msgTextColor = 0
    private var msgTextBg: Drawable? = null
    private var notifyPointBg: Drawable? = null

    private var barBackground: Drawable? = null
    private var barHeight = 45

    private var floatIcon: Drawable? = null
    private var floatEnable = false
    private var floatMarginBottom = 0
    private var floatIconWidth = 0
    private var floatIconHeight = 0

    private var mViewPager: ViewPager? = null
    private val mItemViews = ArrayList<BottomBarItem>()
    private var mCurrentItem = 0
    private var mSmoothScroll = false

    private var mSameTabClickCallBack = false

    private var mViewPager2: ViewPager2? = null

    private val mLlTab: LinearLayout

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.BottomBarLayout)
        initAttrs(ta, context)
        mLlTab = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            background = barBackground ?: UIUtils.getDrawable(context, R.color.tab_gb)
        }
        addView(mLlTab)
        ta.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Log.i("bottomBarLayout", "width: $measuredWidth height: $barHeight")
        val params = LayoutParams(measuredWidth, barHeight).apply {
            gravity = Gravity.BOTTOM
        }
        mLlTab.layoutParams = params
    }

    private fun initAttrs(ta: TypedArray, context: Context) {
        mSmoothScroll = ta.getBoolean(R.styleable.BottomBarLayout_smoothScroll, mSmoothScroll)
        mSameTabClickCallBack = ta.getBoolean(R.styleable.BottomBarLayout_sameTabClickCallBack, mSameTabClickCallBack)
        barBackground = ta.getDrawable(R.styleable.BottomBarLayout_barBackground)
        barHeight = ta.getDimensionPixelSize(R.styleable.BottomBarLayout_barHeight, UIUtils.dip2Px(context, barHeight))
        floatEnable = ta.getBoolean(R.styleable.BottomBarLayout_floatEnable, floatEnable)
        floatIcon = ta.getDrawable(R.styleable.BottomBarLayout_floatIcon)
        floatMarginBottom = ta.getDimensionPixelSize(R.styleable.BottomBarLayout_floatMarginBottom, UIUtils.dip2Px(context, floatMarginBottom))
        floatIconWidth = ta.getDimensionPixelSize(R.styleable.BottomBarLayout_floatIconWidth, UIUtils.dip2Px(context, floatIconWidth))
        floatIconHeight = ta.getDimensionPixelSize(R.styleable.BottomBarLayout_floatIconHeight, UIUtils.dip2Px(context, floatIconHeight))

        titleTextBold = ta.getBoolean(R.styleable.BottomBarLayout_itemTextBold, titleTextBold)
        titleTextSize = ta.getDimensionPixelSize(R.styleable.BottomBarLayout_itemTextSize, UIUtils.sp2px(context, titleTextSize.toFloat()))

        titleNormalColor = ta.getColor(R.styleable.BottomBarLayout_textColorNormal, UIUtils.getColor(context, R.color.bbl_999999))
        titleSelectedColor = ta.getColor(R.styleable.BottomBarLayout_textColorSelected, UIUtils.getColor(context, R.color.bbl_ff0000))

        marginTop = ta.getDimensionPixelSize(R.styleable.BottomBarLayout_itemMarginTop, UIUtils.dip2Px(context, marginTop))

        iconWidth = ta.getDimensionPixelSize(R.styleable.BottomBarLayout_iconWidth, 0)
        iconHeight = ta.getDimensionPixelSize(R.styleable.BottomBarLayout_iconHeight, 0)
        itemPadding = ta.getDimensionPixelSize(R.styleable.BottomBarLayout_itemPadding, 0)

        unreadTextSize = ta.getDimensionPixelSize(R.styleable.BottomBarLayout_unreadTextSize, UIUtils.sp2px(context, unreadTextSize.toFloat()))
        unreadTextColor = ta.getColor(R.styleable.BottomBarLayout_unreadTextColor, UIUtils.getColor(context, R.color.white))
        unreadTextBg = ta.getDrawable(R.styleable.BottomBarLayout_unreadTextBg)

        msgTextSize = ta.getDimensionPixelSize(R.styleable.BottomBarLayout_msgTextSize, UIUtils.sp2px(context, msgTextSize.toFloat()))
        msgTextColor = ta.getColor(R.styleable.BottomBarLayout_msgTextColor, UIUtils.getColor(context, R.color.white))
        msgTextBg = ta.getDrawable(R.styleable.BottomBarLayout_msgTextBg)

        notifyPointBg = ta.getDrawable(R.styleable.BottomBarLayout_notifyPointBg)

        unreadNumThreshold = ta.getInteger(R.styleable.BottomBarLayout_unreadThreshold, unreadNumThreshold)
    }

    fun setViewPager(viewPager: ViewPager?) {
        mViewPager = viewPager
        mViewPager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                handlePageSelected(position)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    fun setViewPager2(viewPager2: ViewPager2?) {
        mViewPager2 = viewPager2
        mViewPager2?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                handlePageSelected(position)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    private fun createBottomBarItem(
        normalIcon: Drawable?,
        selectedIcon: Drawable?,
        title: String,
        iconWidth: Int,
        iconHeight: Int,
        lottieJson: String?
    ): BottomBarItem {
        return BottomBarItem.Builder(context)
            .titleTextBold(titleTextBold)
            .titleTextSize(titleTextSize)
            .titleNormalColor(titleNormalColor)
            .iconHeight(iconHeight)
            .iconWidth(iconWidth)
            .marginTop(marginTop)
            .itemPadding(itemPadding)
            .titleSelectedColor(titleSelectedColor)
            .lottieJson(lottieJson)
            .unreadNumThreshold(unreadNumThreshold)
            .unreadTextBg(unreadTextBg)
            .unreadTextSize(unreadTextSize)
            .unreadTextColor(unreadTextColor)
            .msgTextBg(msgTextBg)
            .msgTextColor(msgTextColor)
            .msgTextSize(msgTextSize)
            .notifyPointBg(notifyPointBg)
            .create(normalIcon, selectedIcon, title)
    }

    fun setData(tabData: List<TabData>?) {
        require(!tabData.isNullOrEmpty()) { "tabData is null" }

        mItemViews.clear()
        mLlTab.removeAllViews()

        // 添加tab
        for (i in tabData.indices) {
            val itemData = tabData[i]
            val normalIcon = if (!TextUtils.isEmpty(itemData.lottieJson)) {
                null
            } else {
                itemData.normalIcon ?: UIUtils.getDrawable(context, itemData.normalIconResId)
            }
            val selectedIcon = if (!TextUtils.isEmpty(itemData.lottieJson)) {
                null
            } else {
                itemData.selectedIcon ?: UIUtils.getDrawable(context, itemData.selectedIconResId)
            }
            val iconWidth = if (itemData.iconWidth == 0) this.iconWidth else itemData.iconWidth
            val iconHeight = if (itemData.iconHeight == 0) this.iconHeight else itemData.iconHeight
            val item = createBottomBarItem(normalIcon, selectedIcon, itemData.title, iconWidth, iconHeight, itemData.lottieJson)
            addItem(item)
        }

        // 如果开启凸起 且是 其他tab总数是偶数
        if (floatEnable && tabData.size % 2 == 0) {
            val item = createBottomBarItem(floatIcon, floatIcon, "", floatIconWidth, floatIconHeight, "")
            addItem(item, (tabData.size + 1) / 2, true)
        }

        mItemViews[0].refreshTab(true)
    }

    fun addItem(item: BottomBarItem) {
        addItem(item, -1, false)
    }

    fun addItem(item: BottomBarItem, index: Int, isFloatItem: Boolean) {
        if (index == -1) {
            mItemViews.add(item)
        } else {
            mItemViews.add(index, item)
        }

        val position = if (index != -1) index else mItemViews.size - 1
        Log.e("bottomBarLayout", "position: $position")

        var view: View = item
        val layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT).apply {
            weight = 1f
            gravity = Gravity.CENTER
        }
        view.layoutParams = layoutParams

        if (isFloatItem) {
            val params = LayoutParams(floatIconWidth, floatIconHeight).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = floatMarginBottom
            }
            addView(item, params)
            view = View(context)
        }

        mLlTab.addView(view, position, layoutParams)

        // tab添加点击事件
        for (i in mItemViews.indices) {
            mItemViews[i].setOnClickListener(MyOnClickListener(i))
        }
    }

    fun removeItem(position: Int) {
        if (position in mItemViews.indices) {
            val item = mItemViews[position]
            if (mItemViews.contains(item)) {
                resetState()
                mLlTab.removeViewAt(position)
            }
            mItemViews.remove(item)

            // tab添加点击事件
            for (i in mItemViews.indices) {
                mItemViews[i].setOnClickListener(MyOnClickListener(i))
            }
        }
    }

    private fun handlePageSelected(position: Int) {
        // 滑动时判断是否需要拦截跳转
        if (mOnPageChangeInterceptor?.onIntercepted(position) == true) {
            setCurrentItem(mCurrentItem)
            return
        }
        resetState()
        mItemViews[position].refreshTab(true)
        val prePos = mCurrentItem
        mCurrentItem = position
        onItemSelectedListener?.onItemSelected(getBottomItem(mCurrentItem), prePos, mCurrentItem)
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    override fun onPageSelected(position: Int) {
        handlePageSelected(position)
    }

    override fun onPageScrollStateChanged(state: Int) {}

    private inner class MyOnClickListener(private val currentIndex: Int) : OnClickListener {
        override fun onClick(v: View) {
            // 点击时判断是否需要拦截跳转
            if (mOnPageChangeInterceptor?.onIntercepted(currentIndex) == true) {
                return
            }

            if (currentIndex == mCurrentItem) {
                // 如果还是同个页签，判断是否要回调
                if (onItemSelectedListener != null && mSameTabClickCallBack) {
                    onItemSelectedListener?.onItemSelected(getBottomItem(currentIndex), mCurrentItem, currentIndex)
                }
            } else {
                if (mViewPager != null || mViewPager2 != null) {
                    mViewPager?.setCurrentItem(currentIndex, mSmoothScroll)
                        ?: mViewPager2?.setCurrentItem(currentIndex, mSmoothScroll)
                    return
                }
                onItemSelectedListener?.onItemSelected(getBottomItem(currentIndex), mCurrentItem, currentIndex)
                updateTabState(currentIndex)
            }
        }
    }

    private fun updateTabState(position: Int) {
        resetState()
        mCurrentItem = position
        mItemViews[mCurrentItem].refreshTab(true)
    }

    /**
     * 重置当前按钮的状态
     */
    private fun resetState() {
        if (mCurrentItem < mItemViews.size) {
            if (mItemViews[mCurrentItem].isSelected) {
                mItemViews[mCurrentItem].refreshTab(false)
            }
        }
    }

    fun setCurrentItem(currentItem: Int) {
        if (mViewPager != null || mViewPager2 != null) {
            mViewPager?.setCurrentItem(currentItem, mSmoothScroll)
                ?: mViewPager2?.setCurrentItem(currentItem, mSmoothScroll)
        } else {
            onItemSelectedListener?.onItemSelected(getBottomItem(currentItem), mCurrentItem, currentItem)
            updateTabState(currentItem)
        }
    }

    /**
     * 设置未读数
     *
     * @param position  底部标签的下标
     * @param unreadNum 未读数
     */
    fun setUnread(position: Int, unreadNum: Int) {
        mItemViews[position].setUnreadNum(unreadNum)
    }

    /**
     * 设置提示消息
     *
     * @param position 底部标签的下标
     * @param msg      未读数
     */
    fun setMsg(position: Int, msg: String) {
        mItemViews[position].setMsg(msg)
    }

    /**
     * 隐藏提示消息
     *
     * @param position 底部标签的下标
     */
    fun hideMsg(position: Int) {
        mItemViews[position].hideMsg()
    }

    /**
     * 显示提示的小红点
     *
     * @param position 底部标签的下标
     */
    fun showNotify(position: Int) {
        mItemViews[position].showNotify()
    }

    /**
     * 隐藏提示的小红点
     *
     * @param position 底部标签的下标
     */
    fun hideNotify(position: Int) {
        mItemViews[position].hideNotify()
    }

    fun getCurrentItem(): Int = mCurrentItem

    fun setSmoothScroll(smoothScroll: Boolean) {
        mSmoothScroll = smoothScroll
    }

    fun getBottomItem(position: Int): BottomBarItem = mItemViews[position]

    private var onItemSelectedListener: OnItemSelectedListener? = null

    interface OnItemSelectedListener {
        fun onItemSelected(bottomBarItem: BottomBarItem, previousPosition: Int, currentPosition: Int)
    }

    fun setOnItemSelectedListener(listener: OnItemSelectedListener?) {
        onItemSelectedListener = listener
    }

    private var mOnPageChangeInterceptor: OnPageChangeInterceptor? = null

    fun setOnPageChangeInterceptor(interceptor: OnPageChangeInterceptor?) {
        mOnPageChangeInterceptor = interceptor
    }

    interface OnPageChangeInterceptor {
        fun onIntercepted(position: Int): Boolean
    }
    
    /**
     * 清理所有监听器 - 防止内存泄漏
     * 在 Activity/Fragment onDestroy 时调用
     */
    fun clearListeners() {
        // 清理页面变化监听器
        mViewPager?.removeOnPageChangeListener(this)
        mViewPager2?.unregisterOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {})
        
        // 清理 item 点击监听器
        mItemViews.forEach { item ->
            item.setOnClickListener(null)
        }
        
        // 清理回调接口
        onItemSelectedListener = null
        mOnPageChangeInterceptor = null
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // View 从窗口分离时自动清理
        clearListeners()
    }
}
