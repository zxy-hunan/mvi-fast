package com.mvi.core.base

import android.app.Application
import com.mvi.core.network.ExceptionHandle

/**
 * MVI Core Application 基类
 *
 * 使用说明:
 * 1. 继承此类创建你的 Application
 * 2. 在 AndroidManifest.xml 中注册
 *
 * 示例:
 * ```kotlin
 * class MyApplication : MviApplication() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         // 你的初始化代码
 *     }
 * }
 * ```
 */
open class MviApplication : Application() {

    companion object {
        /**
         * 全局 Application 实例
         */
        lateinit var instance: MviApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 初始化异常处理（支持国际化）
        ExceptionHandle.init(this)

        // 调用子类的初始化方法
        onInit()
    }

    /**
     * 子类可重写此方法进行自定义初始化
     */
    protected open fun onInit() {
        // 子类实现
    }
}
