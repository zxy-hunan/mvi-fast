package com.mvi.core.base

import android.app.Application
import com.mvi.core.BuildConfig
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

        // 初始化 DoKit（仅在 Debug 版本）
        initDoKit()

        // 调用子类的初始化方法
        onInit()
    }

    /**
     * 初始化 DoraemonKit 调试工具
     * 仅在 Debug 版本生效
     */
    private fun initDoKit() {
        if (BuildConfig.DEBUG) {
            try {
                // 使用反射初始化，避免 Release 版本找不到类
                val doKitClass = Class.forName("com.didichuxing.doraemonkit.DoKit")
                val builderClass = Class.forName("com.didichuxing.doraemonkit.DoKit\$Builder")
                val builder = builderClass.getConstructor(Application::class.java).newInstance(this)
                
                // 配置 DoKit
                builderClass.getMethod("productId", String::class.java).invoke(builder, "MVI_Framework")
                
                // 开启网络监控
                try {
                    builderClass.getMethod("networkMonitor", Boolean::class.javaPrimitiveType).invoke(builder, true)
                } catch (e: Exception) {
                    // 忽略方法不存在的错误
                }
                
                // 开启性能监控
                try {
                    builderClass.getMethod("performanceMonitor", Boolean::class.javaPrimitiveType).invoke(builder, true)
                } catch (e: Exception) {
                    // 忽略方法不存在的错误
                }
                
                // 总是显示主图标（悬浮球）
                try {
                    builderClass.getMethod("alwaysShowMainIcon", Boolean::class.javaPrimitiveType).invoke(builder, true)
                } catch (e: Exception) {
                    // 忽略方法不存在的错误
                }
                
                builderClass.getMethod("build").invoke(builder)
                
                android.util.Log.d("MviApplication", "DoKit 初始化成功")
            } catch (e: Exception) {
                // DoKit 未添加依赖时忽略错误
                android.util.Log.w("MviApplication", "DoKit 初始化失败: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * 子类可重写此方法进行自定义初始化
     */
    protected open fun onInit() {
        // 子类实现
    }
}
