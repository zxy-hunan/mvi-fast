package com.mvi.core.repository

/**
 * 数据源类型
 *
 * 用于控制Repository从哪里获取数据
 */
enum class DataSource {
    /**
     * 仅从网络获取数据
     */
    NETWORK_ONLY,

    /**
     * 仅从本地缓存获取数据
     */
    LOCAL_ONLY,

    /**
     * 先从本地获取，如果没有则从网络获取（推荐）
     */
    LOCAL_FIRST,

    /**
     * 先从网络获取，如果失败则从本地获取
     */
    NETWORK_FIRST,

    /**
     * 同时从本地和网络获取，优先使用网络数据
     */
    CACHE_THEN_NETWORK
}

/**
 * 全局数据源配置
 *
 * 用于控制整个应用的数据源策略
 * 优先级：全局配置 > 方法级别注解
 *
 * 使用示例:
 * ```kotlin
 * // 在 Application 中初始化
 * DataSourceConfig.setGlobalDataSource(DataSource.LOCAL_FIRST)
 *
 * // 在需要时切换
 * DataSourceConfig.setGlobalDataSource(DataSource.NETWORK_ONLY)
 *
 * // 临时禁用全局配置（使用方法级别配置）
 * DataSourceConfig.disableGlobal()
 * ```
 */
object DataSourceConfig {

    /**
     * 全局数据源策略
     * 默认：LOCAL_FIRST（先本地，后网络）
     */
    var globalDataSource: DataSource = DataSource.LOCAL_FIRST
        private set

    /**
     * 是否启用全局配置
     * 默认：true
     */
    var isGlobalEnabled: Boolean = true
        private set

    /**
     * 设置全局数据源
     *
     * @param dataSource 数据源类型
     */
    fun setGlobalDataSource(dataSource: DataSource) {
        globalDataSource = dataSource
        isGlobalEnabled = true
    }

    /**
     * 禁用全局配置
     *
     * 禁用后，方法级别的配置将生效
     */
    fun disableGlobal() {
        isGlobalEnabled = false
    }

    /**
     * 启用全局配置
     */
    fun enableGlobal() {
        isGlobalEnabled = true
    }

    /**
     * 获取有效的数据源
     *
     * 优先级：
     * 1. 方法级别数据源（如果不为 null）
     * 2. 全局数据源（如果启用）
     * 3. 默认值（LOCAL_FIRST）
     *
     * @param methodDataSource 方法级别数据源
     * @return 有效的数据源
     */
    fun getEffectiveDataSource(methodDataSource: DataSource? = null): DataSource {
        return methodDataSource ?: if (isGlobalEnabled) globalDataSource else DataSource.LOCAL_FIRST
    }

    /**
     * 重置为默认配置
     */
    fun reset() {
        globalDataSource = DataSource.LOCAL_FIRST
        isGlobalEnabled = true
    }
}
