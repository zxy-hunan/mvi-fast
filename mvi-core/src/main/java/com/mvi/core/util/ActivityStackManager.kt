package com.mvi.core.util

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * Activity 生命周期管理工具
 *
 * 功能：
 * 1. 管理所有 Activity 的生命周期
 * 2. 提供便捷的 Activity 栈操作方法
 * 3. 防止 Activity 内存泄漏
 * 4. 支持批量操作（如退出所有Activity）
 *
 * 使用示例:
 * ```kotlin
 * class MainActivity : AppCompatActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         ActivityStackManager.addActivity(this)
 *     }
 *
 *     override fun onDestroy() {
 *         super.onDestroy()
 *         ActivityStackManager.removeActivity(this)
 *     }
 * }
 * ```
 *
 * @author MVI Framework
 * @date 2024/12/24
 */
object ActivityStackManager {

    // 使用 WeakReference 防止内存泄漏
    private val activityStack = mutableListOf<WeakReference<Activity>>()

    /**
     * 添加 Activity 到栈中
     * @param activity 要添加的 Activity
     */
    @JvmStatic
    fun addActivity(activity: Activity) {
        // 清理已经被回收的Activity引用
        cleanUpDeadReferences()

        // 添加新的Activity引用
        activityStack.add(WeakReference(activity))
    }

    /**
     * 从栈中移除 Activity
     * @param activity 要移除的 Activity
     */
    @JvmStatic
    fun removeActivity(activity: Activity) {
        activityStack.removeAll { it.get() == activity || it.get() == null }
    }

    /**
     * 结束指定的 Activity
     * @param activity 要结束的 Activity
     */
    @JvmStatic
    fun finishActivity(activity: Activity?) {
        activity?.let {
            if (!it.isFinishing) {
                it.finish()
            }
            removeActivity(it)
        }
    }

    /**
     * 结束指定类名的 Activity
     * @param clazz Activity 的 Class 对象
     */
    @JvmStatic
    fun finishActivity(clazz: Class<*>) {
        activityStack.toList().forEach { weakRef ->
            weakRef.get()?.let { activity ->
                if (activity.javaClass == clazz) {
                    finishActivity(activity)
                }
            }
        }
    }

    /**
     * 获取当前栈顶的 Activity
     * @return 栈顶的 Activity，如果栈为空则返回 null
     */
    @JvmStatic
    fun currentActivity(): Activity? {
        cleanUpDeadReferences()
        return activityStack.lastOrNull()?.get()
    }

    /**
     * 结束所有 Activity
     */
    @JvmStatic
    fun finishAllActivity() {
        activityStack.toList().forEach { weakRef ->
            weakRef.get()?.let { activity ->
                if (!activity.isFinishing) {
                    activity.finish()
                }
            }
        }
        activityStack.clear()
    }

    /**
     * 结束除指定 Activity 外的所有 Activity
     * @param clazz 要保留的 Activity 的 Class 对象
     */
    @JvmStatic
    fun finishAllActivityExcept(clazz: Class<*>) {
        activityStack.toList().forEach { weakRef ->
            weakRef.get()?.let { activity ->
                if (activity.javaClass != clazz && !activity.isFinishing) {
                    activity.finish()
                }
            }
        }
        activityStack.removeAll { it.get()?.javaClass != clazz }
    }

    /**
     * 退出应用程序
     */
    @JvmStatic
    fun exitApp() {
        try {
            finishAllActivity()
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 获取当前 Activity 栈的大小
     * @return Activity 栈的大小
     */
    @JvmStatic
    fun getActivityStackSize(): Int {
        cleanUpDeadReferences()
        return activityStack.size
    }

    /**
     * 判断指定的 Activity 是否在栈中
     * @param clazz Activity 的 Class 对象
     * @return true 表示在栈中，false 表示不在栈中
     */
    @JvmStatic
    fun isActivityInStack(clazz: Class<*>): Boolean {
        activityStack.forEach { weakRef ->
            weakRef.get()?.let { activity ->
                if (activity.javaClass == clazz) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * 清理已经被回收的 Activity 引用
     */
    private fun cleanUpDeadReferences() {
        activityStack.removeAll { it.get() == null }
    }
}
