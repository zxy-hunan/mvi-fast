package com.mvi.core.storage

import android.os.Parcelable
import com.tencent.mmkv.MMKV

/**
 * MMKV存储封装
 *
 * 优化点:
 * 1. 单例模式
 * 2. 扩展函数简化调用
 * 3. 支持多种数据类型
 * 4. 线程安全
 */
object MmkvStorage {

    private lateinit var mmkv: MMKV

    /**
     * 初始化MMKV
     */
    fun init(rootDir: String) {
        MMKV.initialize(rootDir)
        mmkv = MMKV.defaultMMKV()
    }

    /**
     * 获取自定义实例
     */
    fun getInstance(mmapID: String = "MMKV.DEFAULT_MMAP_ID"): MMKV {
        return MMKV.mmkvWithID(mmapID)
    }

    // ==================== 基础操作 ====================

    /**
     * 保存String
     */
    fun putString(key: String, value: String) {
        mmkv.encode(key, value)
    }

    /**
     * 获取String
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return mmkv.decodeString(key, defaultValue) ?: defaultValue
    }

    /**
     * 保存Int
     */
    fun putInt(key: String, value: Int) {
        mmkv.encode(key, value)
    }

    /**
     * 获取Int
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return mmkv.decodeInt(key, defaultValue)
    }

    /**
     * 保存Long
     */
    fun putLong(key: String, value: Long) {
        mmkv.encode(key, value)
    }

    /**
     * 获取Long
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return mmkv.decodeLong(key, defaultValue)
    }

    /**
     * 保存Float
     */
    fun putFloat(key: String, value: Float) {
        mmkv.encode(key, value)
    }

    /**
     * 获取Float
     */
    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return mmkv.decodeFloat(key, defaultValue)
    }

    /**
     * 保存Double
     */
    fun putDouble(key: String, value: Double) {
        mmkv.encode(key, value)
    }

    /**
     * 获取Double
     */
    fun getDouble(key: String, defaultValue: Double = 0.0): Double {
        return mmkv.decodeDouble(key, defaultValue)
    }

    /**
     * 保存Boolean
     */
    fun putBoolean(key: String, value: Boolean) {
        mmkv.encode(key, value)
    }

    /**
     * 获取Boolean
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return mmkv.decodeBool(key, defaultValue)
    }

    /**
     * 保存ByteArray
     */
    fun putByteArray(key: String, value: ByteArray) {
        mmkv.encode(key, value)
    }

    /**
     * 获取ByteArray
     */
    fun getByteArray(key: String): ByteArray? {
        return mmkv.decodeBytes(key)
    }

    /**
     * 保存Parcelable对象
     */
    fun <T : Parcelable> putParcelable(key: String, value: T) {
        mmkv.encode(key, value)
    }

    /**
     * 获取Parcelable对象
     */
    fun <T : Parcelable> getParcelable(key: String, clazz: Class<T>): T? {
        return mmkv.decodeParcelable(key, clazz)
    }

    // ==================== 高级操作 ====================

    /**
     * 是否包含某个key
     */
    fun contains(key: String): Boolean {
        return mmkv.containsKey(key)
    }

    /**
     * 删除指定key
     */
    fun remove(key: String) {
        mmkv.removeValueForKey(key)
    }

    /**
     * 删除多个key
     */
    fun remove(keys: Array<String>) {
        mmkv.removeValuesForKeys(keys)
    }

    /**
     * 清空所有数据
     */
    fun clear() {
        mmkv.clearAll()
    }

    /**
     * 获取所有key
     */
    fun allKeys(): Array<String>? {
        return mmkv.allKeys()
    }

    /**
     * 同步数据到文件
     */
    fun sync() {
        mmkv.sync()
    }
}

// ==================== 扩展函数 ====================

/**
 * MMKV扩展 - 委托属性
 */
inline fun <reified T> mmkvDelegate(
    key: String,
    defaultValue: T
): MmkvDelegate<T> {
    return MmkvDelegate(key, defaultValue)
}

/**
 * MMKV委托类
 */
class MmkvDelegate<T>(
    private val key: String,
    private val defaultValue: T
) {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T {
        return when (defaultValue) {
            is String -> MmkvStorage.getString(key, defaultValue) as T
            is Int -> MmkvStorage.getInt(key, defaultValue) as T
            is Long -> MmkvStorage.getLong(key, defaultValue) as T
            is Float -> MmkvStorage.getFloat(key, defaultValue) as T
            is Double -> MmkvStorage.getDouble(key, defaultValue) as T
            is Boolean -> MmkvStorage.getBoolean(key, defaultValue) as T
            else -> throw IllegalArgumentException("不支持的类型")
        }
    }

    operator fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: T) {
        when (value) {
            is String -> MmkvStorage.putString(key, value)
            is Int -> MmkvStorage.putInt(key, value)
            is Long -> MmkvStorage.putLong(key, value)
            is Float -> MmkvStorage.putFloat(key, value)
            is Double -> MmkvStorage.putDouble(key, value)
            is Boolean -> MmkvStorage.putBoolean(key, value)
            else -> throw IllegalArgumentException("不支持的类型")
        }
    }
}
