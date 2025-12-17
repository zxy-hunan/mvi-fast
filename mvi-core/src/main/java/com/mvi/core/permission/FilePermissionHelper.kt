package com.mvi.core.permission

import android.Manifest
import android.os.Build
import androidx.fragment.app.FragmentActivity
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.mvi.core.R

/**
 * 权限帮助类
 *
 * 支持 Android 1-16 所有版本的权限适配
 *
 * 使用示例:
 * ```kotlin
 * PermissionHelper.requestStoragePermission(activity) { granted ->
 *     if (granted) {
 *         // 执行文件操作
 *     } else {
 *         showToast("需要存储权限")
 *     }
 * }
 * ```
 */
object FilePermissionHelper {

    // ============ 存储权限 ============

    /**
     * 请求存储权限（读写）
     *
     * 自动适配不同 Android 版本:
     * - Android 13+ (API 33+): READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_AUDIO
     * - Android 11-12 (API 30-32): READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE
     * - Android 10- (API 29-): READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE
     */
    fun requestStoragePermission(
        activity: FragmentActivity,
        onResult: (Boolean) -> Unit
    ) {
        val permissions = getStoragePermissions()
        requestPermissions(activity, permissions, onResult)
    }

    /**
     * 检查存储权限是否已授予
     */
    fun hasStoragePermission(activity: FragmentActivity): Boolean {
        val permissions = getStoragePermissions()
        return XXPermissions.isGranted(activity, permissions)
    }

    // ============ 相机权限 ============

    /**
     * 请求相机权限
     */
    fun requestCameraPermission(
        activity: FragmentActivity,
        onResult: (Boolean) -> Unit
    ) {
        requestPermissions(activity, listOf(Manifest.permission.CAMERA), onResult)
    }

    /**
     * 检查相机权限是否已授予
     */
    fun hasCameraPermission(activity: FragmentActivity): Boolean {
        return XXPermissions.isGranted(activity, Manifest.permission.CAMERA)
    }

    // ============ 相机和存储权限 ============

    /**
     * 请求相机和存储权限（拍照上传场景）
     */
    fun requestCameraAndStoragePermission(
        activity: FragmentActivity,
        onResult: (Boolean) -> Unit
    ) {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        permissions.addAll(getStoragePermissions())
        requestPermissions(activity, permissions, onResult)
    }

    // ============ 位置权限 ============

    /**
     * 请求位置权限（精确位置和大致位置）
     */
    fun requestLocationPermission(
        activity: FragmentActivity,
        onResult: (Boolean) -> Unit
    ) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ 需要同时请求精确和大致位置
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        requestPermissions(activity, permissions, onResult)
    }

    /**
     * 请求精确位置权限
     */
    fun requestFineLocationPermission(
        activity: FragmentActivity,
        onResult: (Boolean) -> Unit
    ) {
        requestPermissions(activity, listOf(Manifest.permission.ACCESS_FINE_LOCATION), onResult)
    }

    /**
     * 请求大致位置权限
     */
    fun requestCoarseLocationPermission(
        activity: FragmentActivity,
        onResult: (Boolean) -> Unit
    ) {
        requestPermissions(activity, listOf(Manifest.permission.ACCESS_COARSE_LOCATION), onResult)
    }

    /**
     * 检查位置权限是否已授予
     */
    fun hasLocationPermission(activity: FragmentActivity): Boolean {
        return XXPermissions.isGranted(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    // ============ 麦克风权限 ============

    /**
     * 请求麦克风权限
     */
    fun requestMicrophonePermission(
        activity: FragmentActivity,
        onResult: (Boolean) -> Unit
    ) {
        requestPermissions(activity, listOf(Manifest.permission.RECORD_AUDIO), onResult)
    }

    /**
     * 检查麦克风权限是否已授予
     */
    fun hasMicrophonePermission(activity: FragmentActivity): Boolean {
        return XXPermissions.isGranted(activity, Manifest.permission.RECORD_AUDIO)
    }

    // ============ 通讯录权限 ============

    /**
     * 请求通讯录权限
     */
    fun requestContactsPermission(
        activity: FragmentActivity,
        onResult: (Boolean) -> Unit
    ) {
        requestPermissions(activity, listOf(Manifest.permission.READ_CONTACTS), onResult)
    }

    /**
     * 检查通讯录权限是否已授予
     */
    fun hasContactsPermission(activity: FragmentActivity): Boolean {
        return XXPermissions.isGranted(activity, Manifest.permission.READ_CONTACTS)
    }

    // ============ 电话权限 ============

    /**
     * 请求电话权限
     */
    fun requestPhonePermission(
        activity: FragmentActivity,
        onResult: (Boolean) -> Unit
    ) {
        requestPermissions(activity, listOf(Manifest.permission.CALL_PHONE), onResult)
    }

    /**
     * 检查电话权限是否已授予
     */
    fun hasPhonePermission(activity: FragmentActivity): Boolean {
        return XXPermissions.isGranted(activity, Manifest.permission.CALL_PHONE)
    }

    // ============ 日历权限 ============

    /**
     * 请求日历权限
     */
    fun requestCalendarPermission(
        activity: FragmentActivity,
        onResult: (Boolean) -> Unit
    ) {
        requestPermissions(
            activity,
            listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
            onResult
        )
    }

    /**
     * 检查日历权限是否已授予
     */
    fun hasCalendarPermission(activity: FragmentActivity): Boolean {
        return XXPermissions.isGranted(
            activity,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
    }

    // ============ 短信权限 ============

    /**
     * 请求短信权限
     */
    fun requestSmsPermission(
        activity: FragmentActivity,
        onResult: (Boolean) -> Unit
    ) {
        requestPermissions(activity, listOf(Manifest.permission.SEND_SMS), onResult)
    }

    /**
     * 检查短信权限是否已授予
     */
    fun hasSmsPermission(activity: FragmentActivity): Boolean {
        return XXPermissions.isGranted(activity, Manifest.permission.SEND_SMS)
    }

    // ============ 身体传感器权限 ============

    /**
     * 请求身体传感器权限
     */
    fun requestSensorsPermission(
        activity: FragmentActivity,
        onResult: (Boolean) -> Unit
    ) {
        requestPermissions(activity, listOf(Manifest.permission.BODY_SENSORS), onResult)
    }

    /**
     * 检查身体传感器权限是否已授予
     */
    fun hasSensorsPermission(activity: FragmentActivity): Boolean {
        return XXPermissions.isGranted(activity, Manifest.permission.BODY_SENSORS)
    }

    // ============ 通知权限 (Android 13+) ============

    /**
     * 请求通知权限（Android 13+）
     */
    fun requestNotificationPermission(
        activity: FragmentActivity,
        onResult: (Boolean) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(activity, listOf(Manifest.permission.POST_NOTIFICATIONS), onResult)
        } else {
            // Android 13 以下不需要通知权限
            onResult(true)
        }
    }

    /**
     * 检查通知权限是否已授予（Android 13+）
     */
    fun hasNotificationPermission(activity: FragmentActivity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            XXPermissions.isGranted(activity, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    }

    // ============ 通用权限请求方法 ============

    /**
     * 请求自定义权限
     *
     * @param activity FragmentActivity 实例
     * @param permissions 权限列表
     * @param onResult 权限请求结果回调
     */
    fun requestPermissions(
        activity: FragmentActivity,
        permissions: List<String>,
        onResult: (Boolean) -> Unit
    ) {
        XXPermissions.with(activity)
            .permission(permissions)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    onResult(allGranted)
                }

                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    if (doNotAskAgain) {
                        // 权限被永久拒绝，引导用户到设置页面
                        showPermissionDeniedDialog(activity, permissions)
                    }
                    onResult(false)
                }
            })
    }

    /**
     * 检查权限是否已授予
     *
     * @param activity FragmentActivity 实例
     * @param permissions 权限列表
     * @return true 已授予，false 未授予
     */
    fun hasPermissions(activity: FragmentActivity, vararg permissions: String): Boolean {
        return XXPermissions.isGranted(activity, *permissions)
    }

    /**
     * 跳转到应用设置页面（用于手动授权）
     */
    fun startPermissionActivity(activity: FragmentActivity) {
        XXPermissions.startPermissionActivity(activity)
    }

    // ============ 私有方法 ============

    /**
     * 获取存储权限列表（根据 Android 版本自动适配）
     */
    private fun getStoragePermissions(): List<String> {
        return when {
            // Android 13+ (API 33+): 分离的媒体权限
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                listOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            }
            // Android 11-12 (API 30-32): 标准存储权限
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
            // Android 10- (API 29-): 标准存储权限
            else -> {
                listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }

    /**
     * 显示权限被拒绝的对话框（引导用户去设置页面）
     * 支持国际化
     */
    private fun showPermissionDeniedDialog(
        activity: FragmentActivity,
        deniedPermissions: List<String>
    ) {
        val permissionNames = deniedPermissions.joinToString("\n") { getPermissionName(activity, it) }

        android.app.AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.permission_denied_title))
            .setMessage(activity.getString(R.string.permission_denied_message, permissionNames))
            .setPositiveButton(activity.getString(R.string.permission_go_settings)) { _, _ ->
                startPermissionActivity(activity)
            }
            .setNegativeButton(activity.getString(R.string.permission_cancel), null)
            .show()
    }

    /**
     * 获取权限名称（用于显示，支持国际化）
     */
    private fun getPermissionName(activity: FragmentActivity, permission: String): String {
        return when (permission) {
            Manifest.permission.READ_EXTERNAL_STORAGE -> activity.getString(R.string.permission_storage_read)
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> activity.getString(R.string.permission_storage_write)
            Manifest.permission.READ_MEDIA_IMAGES -> activity.getString(R.string.permission_media_images)
            Manifest.permission.READ_MEDIA_VIDEO -> activity.getString(R.string.permission_media_video)
            Manifest.permission.READ_MEDIA_AUDIO -> activity.getString(R.string.permission_media_audio)
            Manifest.permission.CAMERA -> activity.getString(R.string.permission_camera)
            Manifest.permission.ACCESS_FINE_LOCATION -> activity.getString(R.string.permission_location_fine)
            Manifest.permission.ACCESS_COARSE_LOCATION -> activity.getString(R.string.permission_location_coarse)
            Manifest.permission.RECORD_AUDIO -> activity.getString(R.string.permission_microphone)
            Manifest.permission.READ_CONTACTS -> activity.getString(R.string.permission_contacts)
            Manifest.permission.CALL_PHONE -> activity.getString(R.string.permission_phone)
            Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR -> activity.getString(R.string.permission_calendar)
            Manifest.permission.SEND_SMS -> activity.getString(R.string.permission_sms)
            Manifest.permission.BODY_SENSORS -> activity.getString(R.string.permission_sensors)
            Manifest.permission.POST_NOTIFICATIONS -> activity.getString(R.string.permission_notification)
            else -> permission
        }
    }
}
