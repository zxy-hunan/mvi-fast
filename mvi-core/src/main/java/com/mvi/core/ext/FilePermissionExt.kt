package com.mvi.core.ext

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import com.mvi.core.base.MviViewModel
import com.mvi.core.network.ExceptionHandle.handleException
import com.mvi.core.network.download.DownloadManager
import com.mvi.core.network.download.DownloadState
import com.mvi.core.network.upload.UploadInfo
import com.mvi.core.network.upload.UploadRequestBody
import com.mvi.core.network.upload.UploadState
import com.mvi.core.permission.FilePermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody

/**
 * 带权限检查的文件上传和下载扩展函数
 */

// ============ 文件上传扩展（带权限检查） ============

/**
 * 上传单个文件（带权限检查）
 *
 * @param activity FragmentActivity 实例（用于请求权限）
 * @param uploadInfo 上传文件信息
 * @param uploadStateFlow 上传状态流（可选）
 * @param upload 上传函数（返回API响应）
 * @param onPermissionDenied 权限被拒绝时的回调（可选）
 *
 * 使用示例:
 * ```kotlin
 * uploadFileWithPermission(
 *     activity = this,
 *     uploadInfo = UploadInfo(file, "image/jpeg"),
 *     uploadStateFlow = uploadState,
 *     onPermissionDenied = {
 *         showToast("需要存储权限")
 *     }
 * ) { multipartBody ->
 *     apiService.uploadFile(multipartBody)
 * }
 * ```
 */
fun <T, I : com.mvi.core.base.MviIntent> MviViewModel<I>.uploadFileWithPermission(
    activity: FragmentActivity,
    uploadInfo: UploadInfo,
    uploadStateFlow: MutableStateFlow<UploadState>? = null,
    onPermissionDenied: (() -> Unit)? = null,
    upload: suspend (MultipartBody.Part) -> com.mvi.core.network.ApiResponse<T>
) {
    // 先检查权限
    FilePermissionHelper.requestStoragePermission(activity) { granted ->
        if (granted) {
            // 权限已授予，执行上传
            uploadFile(uploadInfo, uploadStateFlow, upload)
        } else {
            // 权限被拒绝
            uploadStateFlow?.value = UploadState.Error("需要存储权限")
            onPermissionDenied?.invoke()
        }
    }
}

/**
 * 上传多个文件（带权限检查）
 *
 * @param activity FragmentActivity 实例（用于请求权限）
 * @param uploadInfoList 上传文件列表
 * @param uploadStateFlow 上传状态流（可选）
 * @param upload 上传函数（返回API响应）
 * @param onPermissionDenied 权限被拒绝时的回调（可选）
 */
fun <T, I : com.mvi.core.base.MviIntent> MviViewModel<I>.uploadFilesWithPermission(
    activity: FragmentActivity,
    uploadInfoList: List<UploadInfo>,
    uploadStateFlow: MutableStateFlow<UploadState>? = null,
    onPermissionDenied: (() -> Unit)? = null,
    upload: suspend (List<MultipartBody.Part>) -> com.mvi.core.network.ApiResponse<T>
) {
    // 先检查权限
    FilePermissionHelper.requestStoragePermission(activity) { granted ->
        if (granted) {
            // 权限已授予，执行上传
            uploadFiles(uploadInfoList, uploadStateFlow, upload)
        } else {
            // 权限被拒绝
            uploadStateFlow?.value = UploadState.Error("需要存储权限")
            onPermissionDenied?.invoke()
        }
    }
}

// ============ 文件下载扩展（带权限检查） ============

/**
 * 下载文件（带权限检查）
 *
 * @param activity FragmentActivity 实例（用于请求权限）
 * @param url 下载链接
 * @param savePath 保存路径
 * @param supportResume 是否支持断点续传
 * @param downloadStateFlow 下载状态流（可选）
 * @param onPermissionDenied 权限被拒绝时的回调（可选）
 *
 * 使用示例:
 * ```kotlin
 * downloadFileWithPermission(
 *     activity = this,
 *     url = "https://example.com/file.apk",
 *     savePath = "/sdcard/Download/file.apk",
 *     downloadStateFlow = downloadState,
 *     onPermissionDenied = {
 *         showToast("需要存储权限")
 *     }
 * )
 * ```
 */
fun <I : com.mvi.core.base.MviIntent> MviViewModel<I>.downloadFileWithPermission(
    activity: FragmentActivity,
    url: String,
    savePath: String,
    supportResume: Boolean = true,
    downloadStateFlow: MutableStateFlow<DownloadState>? = null,
    downloadManager: DownloadManager = DownloadManager(),
    onPermissionDenied: (() -> Unit)? = null
) {
    // 先检查权限
    FilePermissionHelper.requestStoragePermission(activity) { granted ->
        if (granted) {
            // 权限已授予，执行下载
            downloadFile(url, savePath, supportResume, downloadStateFlow, downloadManager)
        } else {
            // 权限被拒绝
            downloadStateFlow?.value = DownloadState.Error("需要存储权限")
            onPermissionDenied?.invoke()
        }
    }
}

/**
 * 拍照并上传（带相机和存储权限检查）
 *
 * @param activity FragmentActivity 实例（用于请求权限）
 * @param uploadInfo 上传文件信息
 * @param uploadStateFlow 上传状态流（可选）
 * @param upload 上传函数（返回API响应）
 * @param onPermissionDenied 权限被拒绝时的回调（可选）
 */
fun <T, I : com.mvi.core.base.MviIntent> MviViewModel<I>.takePictureAndUploadWithPermission(
    activity: FragmentActivity,
    uploadInfo: UploadInfo,
    uploadStateFlow: MutableStateFlow<UploadState>? = null,
    onPermissionDenied: (() -> Unit)? = null,
    upload: suspend (MultipartBody.Part) -> com.mvi.core.network.ApiResponse<T>
) {
    // 请求相机和存储权限
    FilePermissionHelper.requestCameraAndStoragePermission(activity) { granted ->
        if (granted) {
            // 权限已授予，执行上传
            uploadFile(uploadInfo, uploadStateFlow, upload)
        } else {
            // 权限被拒绝
            uploadStateFlow?.value = UploadState.Error("需要相机和存储权限")
            onPermissionDenied?.invoke()
        }
    }
}
