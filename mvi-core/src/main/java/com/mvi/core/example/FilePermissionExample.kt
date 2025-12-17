package com.mvi.core.example

import androidx.fragment.app.FragmentActivity
import com.mvi.core.base.MviIntent
import com.mvi.core.base.MviViewModel
import com.mvi.core.ext.downloadFileWithPermission
import com.mvi.core.ext.uploadFileWithPermission
import com.mvi.core.ext.uploadFilesWithPermission
import com.mvi.core.ext.takePictureAndUploadWithPermission
import com.mvi.core.network.download.DownloadState
import com.mvi.core.network.upload.UploadInfo
import com.mvi.core.network.upload.UploadState
import com.mvi.core.permission.FilePermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/**
 * 文件上传下载权限处理使用示例
 *
 * 此文件展示如何在文件上传下载时处理权限请求
 */

// ============ Intent 定义 ============

sealed class FilePermissionIntent : MviIntent {
    data class UploadFileWithPermission(val activity: FragmentActivity, val file: File) : FilePermissionIntent()
    data class DownloadFileWithPermission(val activity: FragmentActivity, val url: String, val savePath: String) : FilePermissionIntent()
    data class TakePictureAndUpload(val activity: FragmentActivity, val file: File) : FilePermissionIntent()
}

// ============ ViewModel 示例 ============

class FilePermissionViewModel : MviViewModel<FilePermissionIntent>() {

    // 上传状态
    val uploadState = MutableStateFlow<UploadState>(UploadState.Idle)

    // 下载状态
    val downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)

    override fun handleIntent(intent: FilePermissionIntent) {
        when (intent) {
            is FilePermissionIntent.UploadFileWithPermission ->
                handleUploadFileWithPermission(intent.activity, intent.file)
            is FilePermissionIntent.DownloadFileWithPermission ->
                handleDownloadFileWithPermission(intent.activity, intent.url, intent.savePath)
            is FilePermissionIntent.TakePictureAndUpload ->
                handleTakePictureAndUpload(intent.activity, intent.file)
        }
    }

    // ============ 方式一: 使用带权限检查的扩展函数（推荐） ============

    private fun handleUploadFileWithPermission(activity: FragmentActivity, file: File) {
        uploadFileWithPermission(
            activity = activity,
            uploadInfo = UploadInfo(
                file = file,
                contentType = "image/jpeg",
                fileName = file.name,
                formFieldName = "file"
            ),
            uploadStateFlow = uploadState,
            onPermissionDenied = {
                // 权限被拒绝时的处理
                showToast("需要存储权限才能上传文件")
            }
        ) { multipartBody ->
            // 调用你的 API Service
            // apiService.uploadFile(multipartBody)

            // 模拟返回
            com.mvi.core.network.ApiResponse(
                code = 200,
                message = "Success",
                data = "Upload successful"
            )
        }
    }

    private fun handleDownloadFileWithPermission(activity: FragmentActivity, url: String, savePath: String) {
        downloadFileWithPermission(
            activity = activity,
            url = url,
            savePath = savePath,
            supportResume = true,
            downloadStateFlow = downloadState,
            onPermissionDenied = {
                // 权限被拒绝时的处理
                showToast("需要存储权限才能下载文件")
            }
        )
    }

    private fun handleTakePictureAndUpload(activity: FragmentActivity, file: File) {
        takePictureAndUploadWithPermission(
            activity = activity,
            uploadInfo = UploadInfo(
                file = file,
                contentType = "image/jpeg",
                fileName = file.name
            ),
            uploadStateFlow = uploadState,
            onPermissionDenied = {
                // 权限被拒绝时的处理
                showToast("需要相机和存储权限")
            }
        ) { multipartBody ->
            // 调用你的 API Service
            com.mvi.core.network.ApiResponse(
                code = 200,
                message = "Success",
                data = "Upload successful"
            )
        }
    }

    // ============ 方式二: 手动检查权限（更灵活） ============

    private fun handleUploadWithManualPermissionCheck(activity: FragmentActivity, file: File) {
        // 先检查权限是否已授予
        if (FilePermissionHelper.hasStoragePermission(activity)) {
            // 权限已授予，直接上传
            uploadFileDirectly(file)
        } else {
            // 请求权限
            FilePermissionHelper.requestStoragePermission(activity) { granted ->
                if (granted) {
                    // 权限授予成功
                    uploadFileDirectly(file)
                } else {
                    // 权限被拒绝
                    showToast("需要存储权限才能上传文件")
                    uploadState.value = UploadState.Error("Permission denied")
                }
            }
        }
    }

    private fun uploadFileDirectly(file: File) {
        // 使用不带权限检查的上传方法
        // uploadFile(...)
    }
}

// ============ Activity 中使用示例 ============

/**
 * Activity 使用示例:
 *
 * ```kotlin
 * class FileActivity : MviActivity<FilePermissionIntent>() {
 *
 *     private val viewModel: FilePermissionViewModel by viewModels()
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         // 观察上传状态
 *         lifecycleScope.launch {
 *             viewModel.uploadState.collect { state ->
 *                 when (state) {
 *                     is UploadState.Uploading -> {
 *                         updateProgress(state.progress)
 *                     }
 *                     is UploadState.Success -> {
 *                         showToast("Upload successful")
 *                     }
 *                     is UploadState.Error -> {
 *                         showToast(state.message)
 *                     }
 *                     else -> {}
 *                 }
 *             }
 *         }
 *
 *         // 上传文件（自动处理权限）
 *         binding.btnUpload.setOnClickListener {
 *             val file = File("/path/to/file")
 *             viewModel.sendIntent(
 *                 FilePermissionIntent.UploadFileWithPermission(this, file)
 *             )
 *         }
 *
 *         // 下载文件（自动处理权限）
 *         binding.btnDownload.setOnClickListener {
 *             viewModel.sendIntent(
 *                 FilePermissionIntent.DownloadFileWithPermission(
 *                     activity = this,
 *                     url = "https://example.com/file.apk",
 *                     savePath = getExternalFilesDir(null)?.absolutePath + "/file.apk"
 *                 )
 *             )
 *         }
 *
 *         // 拍照并上传（自动处理相机和存储权限）
 *         binding.btnTakePicture.setOnClickListener {
 *             // 先拍照获取图片文件
 *             val photoFile = File("/path/to/photo.jpg")
 *             viewModel.sendIntent(
 *                 FilePermissionIntent.TakePictureAndUpload(this, photoFile)
 *             )
 *         }
 *     }
 * }
 * ```
 */

// ============ 在 AndroidManifest.xml 中声明权限 ============

/**
 * 在 app 模块的 AndroidManifest.xml 中添加以下权限声明:
 *
 * ```xml
 * <?xml version="1.0" encoding="utf-8"?>
 * <manifest xmlns:android="http://schemas.android.com/apk/res/android">
 *
 *     <!-- 相机权限 -->
 *     <uses-permission android:name="android.permission.CAMERA" />
 *
 *     <!-- 存储权限 (Android 12 及以下) -->
 *     <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
 *         android:maxSdkVersion="32" />
 *     <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
 *         android:maxSdkVersion="32" />
 *
 *     <!-- 媒体权限 (Android 13+) -->
 *     <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
 *     <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
 *     <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
 *
 *     <!-- 管理外部存储权限 (可选，需要特殊审核) -->
 *     <!-- <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" /> -->
 *
 *     <application>
 *         <!-- 如果项目已适配 Android 10 分区存储，添加此配置 -->
 *         <meta-data
 *             android:name="ScopedStorage"
 *             android:value="true" />
 *     </application>
 *
 * </manifest>
 * ```
 */

// ============ 权限处理最佳实践 ============

/**
 * 最佳实践建议:
 *
 * 1. **优先使用带权限检查的扩展函数**
 *    - uploadFileWithPermission()
 *    - downloadFileWithPermission()
 *    - takePictureAndUploadWithPermission()
 *
 * 2. **提供友好的权限说明**
 *    在请求权限前，向用户解释为什么需要该权限
 *
 * 3. **处理权限被拒绝的情况**
 *    - 提供明确的提示信息
 *    - 引导用户到设置页面手动授权
 *
 * 4. **使用分区存储（Android 10+）**
 *    - 优先使用 getExternalFilesDir() 等 App 专属目录
 *    - 避免使用 MANAGE_EXTERNAL_STORAGE（需要特殊审核）
 *
 * 5. **适配不同 Android 版本**
 *    - FilePermissionHelper 已自动处理版本适配
 *    - Android 13+ 使用新的媒体权限
 *    - Android 12- 使用标准存储权限
 *
 * 6. **测试不同权限状态**
 *    - 权限未授予
 *    - 权限已授予
 *    - 权限被永久拒绝
 */
