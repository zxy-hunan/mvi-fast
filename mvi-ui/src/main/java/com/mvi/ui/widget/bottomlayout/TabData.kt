package com.mvi.ui.widget.bottomlayout

import android.graphics.drawable.Drawable

/**
 * @author chay
 * @description:
 * @date 2024/7/14 16:58
 */
class TabData(
    var title: String = "",
    var normalIconResId: Int = 0,
    var normalIcon: Drawable? = null,
    var selectedIconResId: Int = 0,
    var selectedIcon: Drawable? = null,
    var lottieJson: String? = null,
    var iconWidth: Int = 0,
    var iconHeight: Int = 0
) {
    constructor(title: String, normalIcon: Drawable?, selectedIcon: Drawable?) : this() {
        this.title = title
        this.normalIcon = normalIcon
        this.selectedIcon = selectedIcon
    }

    constructor(title: String, normalIconResId: Int, selectedIconResId: Int) : this() {
        this.title = title
        this.normalIconResId = normalIconResId
        this.selectedIconResId = selectedIconResId
    }

    constructor(title: String, lottieJson: String?) : this() {
        this.title = title
        this.lottieJson = lottieJson
    }
}
