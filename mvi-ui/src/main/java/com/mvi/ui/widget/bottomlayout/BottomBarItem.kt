package com.mvi.ui.widget.bottomlayout

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.mvi.ui.R
import java.util.Locale

/**
 * @author ChayChan
 * @description: 底部tab条目
 * @date 2017/6/23  9:14
 */
class BottomBarItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val mContext: Context = context
    private lateinit var mImageView: ImageView
    private lateinit var mTvUnread: TextView
    private lateinit var mTvNotify: TextView
    private lateinit var mTvMsg: TextView
    private lateinit var mTextView: TextView

    private lateinit var mBuilder: Builder

    /**
     * 检查传入的值是否完善
     */
    private fun checkValues() {
        if (!::mBuilder.isInitialized) {
            throw IllegalStateException("Builder is null")
        }

        if (mBuilder.unreadTextBg == null) {
            mBuilder.unreadTextBg = UIUtils.getDrawable(mContext, R.drawable.shape_unread)
        }

        if (mBuilder.msgTextBg == null) {
            mBuilder.msgTextBg = UIUtils.getDrawable(mContext, R.drawable.shape_msg)
        }

        if (mBuilder.notifyPointBg == null) {
            mBuilder.notifyPointBg = UIUtils.getDrawable(mContext, R.drawable.shape_notify_point)
        }
    }

    private fun init() {
        orientation = VERTICAL
        gravity = Gravity.CENTER

        val view = initView()

        val layoutParams = mImageView.layoutParams as FrameLayout.LayoutParams
        if (mBuilder.iconWidth != 0 && mBuilder.iconHeight != 0) {
            //如果有设置图标的宽度和高度，则设置ImageView的宽高
            layoutParams.width = mBuilder.iconWidth
            layoutParams.height = mBuilder.iconHeight
        }

        if (!TextUtils.isEmpty(mBuilder.lottieJson)) {
            // Lottie相关代码已注释
        } else {
            mImageView.setImageDrawable(mBuilder.normalIcon)
            mImageView.layoutParams = layoutParams
        }

        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mBuilder.titleTextSize.toFloat())
        mTextView.paint.isFakeBoldText = mBuilder.titleTextBold
        mTvUnread.setTextSize(TypedValue.COMPLEX_UNIT_PX, mBuilder.unreadTextSize.toFloat())
        mTvUnread.setTextColor(mBuilder.unreadTextColor)
        mTvUnread.background = mBuilder.unreadTextBg

        mTvMsg.setTextSize(TypedValue.COMPLEX_UNIT_PX, mBuilder.msgTextSize.toFloat())
        mTvMsg.setTextColor(mBuilder.msgTextColor)
        mTvMsg.background = mBuilder.msgTextBg

        mTvNotify.background = mBuilder.notifyPointBg

        mTextView.setTextColor(mBuilder.titleNormalColor)
        mTextView.text = mBuilder.title

        val textLayoutParams = mTextView.layoutParams as LayoutParams
        textLayoutParams.topMargin = mBuilder.marginTop
        mTextView.layoutParams = textLayoutParams

        addView(view)
    }

    private fun initView(): View {
        val view = View.inflate(mContext, R.layout.item_bottom_bar, null)
        if (mBuilder.itemPadding != 0) {
            view.setPadding(
                mBuilder.itemPadding,
                mBuilder.itemPadding,
                mBuilder.itemPadding,
                mBuilder.itemPadding
            )
        }
        mImageView = view.findViewById(R.id.iv_icon)
        mTvUnread = view.findViewById(R.id.tv_unred_num)
        mTvMsg = view.findViewById(R.id.tv_msg)
        mTvNotify = view.findViewById(R.id.tv_point)
        mTextView = view.findViewById(R.id.tv_text)

        mImageView.visibility = if (TextUtils.isEmpty(mBuilder.lottieJson)) VISIBLE else GONE

        return view
    }

    fun getTitle(): String = mBuilder.title

    fun getImageView(): ImageView = mImageView

    fun getTextView(): TextView = mTextView

    fun setNormalIcon(normalIcon: Drawable) {
        mBuilder.normalIcon = normalIcon
        refreshTab()
    }

    fun setNormalIcon(resId: Int) {
        setNormalIcon(UIUtils.getDrawable(mContext, resId)!!)
    }

    fun setSelectedIcon(selectedIcon: Drawable) {
        mBuilder.selectedIcon = selectedIcon
        refreshTab()
    }

    fun setSelectedIcon(resId: Int) {
        setSelectedIcon(UIUtils.getDrawable(mContext, resId)!!)
    }

    fun refreshTab(isSelected: Boolean) {
        setSelected(isSelected)
        refreshTab()
    }

    fun refreshTab() {
        if (!TextUtils.isEmpty(mBuilder.lottieJson)) {
            // Lottie相关代码已注释
        } else {
            mImageView.setImageDrawable(if (isSelected) mBuilder.selectedIcon else mBuilder.normalIcon)
        }

        mTextView.setTextColor(if (isSelected) mBuilder.titleSelectedColor else mBuilder.titleNormalColor)
    }

    private fun setTvVisible(tv: TextView) {
        //都设置为不可见
        mTvUnread.visibility = GONE
        mTvMsg.visibility = GONE
        mTvNotify.visibility = GONE

        tv.visibility = VISIBLE
    }

    fun getUnreadNumThreshold(): Int = mBuilder.unreadNumThreshold

    fun setUnreadNumThreshold(unreadNumThreshold: Int) {
        mBuilder.unreadNumThreshold = unreadNumThreshold
    }

    fun setUnreadNum(unreadNum: Int) {
        setTvVisible(mTvUnread)
        when {
            unreadNum <= 0 -> mTvUnread.visibility = GONE
            unreadNum <= mBuilder.unreadNumThreshold -> mTvUnread.text = unreadNum.toString()
            else -> mTvUnread.text = String.format(Locale.CHINA, "%d+", mBuilder.unreadNumThreshold)
        }
    }

    fun setMsg(msg: String) {
        setTvVisible(mTvMsg)
        mTvMsg.text = msg
    }

    fun hideMsg() {
        mTvMsg.visibility = GONE
    }

    fun showNotify() {
        setTvVisible(mTvNotify)
    }

    fun hideNotify() {
        mTvNotify.visibility = GONE
    }

    fun create(builder: Builder): BottomBarItem {
        mBuilder = builder
        checkValues()
        init()
        return this
    }

    class Builder(val context: Context) {
        var normalIcon: Drawable? = null
        var selectedIcon: Drawable? = null
        var title: String = ""
        var titleTextBold: Boolean = false
        var titleTextSize: Int = UIUtils.sp2px(context, 12f)
        var titleNormalColor: Int = getColor(R.color.bbl_999999)
        var titleSelectedColor: Int = getColor(R.color.bbl_ff0000)
        var marginTop: Int = 0
        var iconWidth: Int = 0
        var iconHeight: Int = 0
        var itemPadding: Int = 0
        var unreadTextSize: Int = UIUtils.sp2px(context, 10f)
        var unreadNumThreshold: Int = 99
        var unreadTextColor: Int = getColor(R.color.white)
        var unreadTextBg: Drawable? = null
        var msgTextSize: Int = UIUtils.sp2px(context, 6f)
        var msgTextColor: Int = getColor(R.color.white)
        var msgTextBg: Drawable? = null
        var notifyPointBg: Drawable? = null
        var lottieJson: String? = null

        fun normalIcon(normalIcon: Drawable) = apply { this.normalIcon = normalIcon }

        fun selectedIcon(selectedIcon: Drawable) = apply { this.selectedIcon = selectedIcon }

        fun title(titleId: Int) = apply { this.title = context.getString(titleId) }

        fun title(title: String) = apply { this.title = title }

        fun titleTextBold(titleTextBold: Boolean) = apply { this.titleTextBold = titleTextBold }

        fun titleTextSize(titleTextSize: Int) = apply { this.titleTextSize = titleTextSize }

        fun titleNormalColor(titleNormalColor: Int) = apply { this.titleNormalColor = titleNormalColor }

        fun titleSelectedColor(titleSelectedColor: Int) = apply { this.titleSelectedColor = titleSelectedColor }

        fun marginTop(marginTop: Int) = apply { this.marginTop = marginTop }

        fun iconWidth(iconWidth: Int) = apply { this.iconWidth = iconWidth }

        fun iconHeight(iconHeight: Int) = apply { this.iconHeight = iconHeight }

        fun itemPadding(itemPadding: Int) = apply { this.itemPadding = itemPadding }

        fun unreadTextSize(unreadTextSize: Int) = apply { this.unreadTextSize = unreadTextSize }

        fun unreadNumThreshold(unreadNumThreshold: Int) = apply { this.unreadNumThreshold = unreadNumThreshold }

        fun msgTextSize(msgTextSize: Int) = apply { this.msgTextSize = msgTextSize }

        fun unreadTextBg(unreadTextBg: Drawable?) = apply { this.unreadTextBg = unreadTextBg }

        fun unreadTextColor(unreadTextColor: Int) = apply { this.unreadTextColor = unreadTextColor }

        fun msgTextColor(msgTextColor: Int) = apply { this.msgTextColor = msgTextColor }

        fun msgTextBg(msgTextBg: Drawable?) = apply { this.msgTextBg = msgTextBg }

        fun notifyPointBg(notifyPointBg: Drawable?) = apply { this.notifyPointBg = notifyPointBg }

        fun lottieJson(lottieJson: String?) = apply { this.lottieJson = lottieJson }

        fun create(normalIcon: Drawable?, selectedIcon: Drawable?, text: String): BottomBarItem {
            this.normalIcon = normalIcon
            this.selectedIcon = selectedIcon
            this.title = text
            return BottomBarItem(context).create(this)
        }

        fun create(normalIconId: Int, selectedIconId: Int, text: String): BottomBarItem {
            return create(
                UIUtils.getDrawable(context, normalIconId),
                UIUtils.getDrawable(context, selectedIconId),
                text
            )
        }

        private fun getColor(colorId: Int): Int {
            return UIUtils.getColor(context, colorId)
        }
    }
}
