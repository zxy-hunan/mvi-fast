package com.mvi.core.base

import android.app.Application
import com.mvi.core.BuildConfig
import com.mvi.core.network.ExceptionHandle
import com.mvi.core.util.MviLog

/**
 * MVI Core Application 基类
 *
 * 优化点:
 * 1. 可选继承：如果不继承MviApplication，也能正常使用框架
 * 2. 空安全设计：instance 可能为null
 * 3. 自动初始化异常处理和调试工具
 *
 * 使用说明:
 * 方式一（推荐）：继承此类
 * ```kotlin
 * class MyApplication : MviApplication() {
 *     override fun onInit() {
 *         // 你的初始化代码
 *         MmkvStorage.init(filesDir.absolutePath)
 *         RetrofitClient.init { ... }
 *     }
 * }
 * ```
 *
 * 方式二：不继承，手动初始化
 * ```kotlin
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         // 手动初始化框架组件
 *         ExceptionHandle.init(this)
 *         MmkvStorage.init(filesDir.absolutePath)
 *         RetrofitClient.init { ... }
 *     }
 * }
 * ```
 */
open class MviApplication : Application() {

    companion object {
        /**
         * 全局 Application 实例
         * 注意：如果用户不继承MviApplication，此值为null
         */
        var instance: MviApplication? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 初始化日志（优先级最高，用于记录其他初始化过程）
        MviLog.init(
            level = if (BuildConfig.DEBUG) MviLog.Level.DEBUG else MviLog.Level.ERROR,
            enabled = BuildConfig.DEBUG
        )

        // 初始化异常处理（支持国际化，可选）
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

                MviLog.i("MviApplication", "DoKit 初始化成功")
            } catch (e: Exception) {
                // DoKit 未添加依赖时忽略错误
                MviLog.w("MviApplication", "DoKit 初始化失败: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * 子类可重写此方法进行自定义初始化
     * 建议在此方法中初始化：
     * - MmkvStorage.init()
     * - RetrofitClient.init()
     */
    protected open fun onInit() {
        // 子类实现
    }

    override fun onTerminate() {
        super.onTerminate()
        instance = null
    }
}
