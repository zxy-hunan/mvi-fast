package com.mvi.ui.widget.bottomlayout

//import com.airbnb.lottie.LottieAnimationView

/**
 * @author ChayChan
 * @description: 去除LottieAnimationView的缓存
 * @date 2020/11/23  16:02
 */
/*class MyLottieAnimationView : LottieAnimationView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    *//**
     * 重写此方法将LottieAnimationView的缓存去除
     * 解决因异常情况或旋转方向后页面重新加载
     * 导致lottie文件读取成最后一个tab文件的bug
     * @return
     *//*
    override fun onSaveInstanceState(): Parcelable? {
        return null
    }
}*/
