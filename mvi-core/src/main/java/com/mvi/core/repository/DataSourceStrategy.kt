package com.mvi.core.repository

/**
 * 数据源注解
 *
 * 用于标记 Repository 方法的数据源策略
 * 优先级低于全局配置
 *
 * 使用示例:
 * ```kotlin
 * @DataSource(DataSource.LOCAL_ONLY)
 * suspend fun getUser(id: String): User
 *
 * @DataSource(DataSource.NETWORK_ONLY)
 * suspend fun refreshUsers(): List<User>
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DataSourceAnnotation(val value: DataSource = DataSource.LOCAL_FIRST)

/**
 * 缓存策略注解
 *
 * 用于控制数据缓存行为
 *
 * @param cacheTime 缓存时间（毫秒），0表示不缓存，-1表示永久缓存
 * @param enableCache 是否启用缓存
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheStrategy(
    val cacheTime: Long = 5 * 60 * 1000, // 默认5分钟
    val enableCache: Boolean = true
)

/**
 * 强制刷新注解
 *
 * 标记的方法将强制从网络获取数据，忽略缓存和本地数据
 * 即使全局配置是 LOCAL_ONLY，此注解也会强制使用网络
 *
 * 使用示例:
 * ```kotlin
 * @ForceRefresh
 * suspend fun refreshUser(id: String): User
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ForceRefresh
