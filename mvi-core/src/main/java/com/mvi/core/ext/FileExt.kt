package com.mvi.core.ext

import androidx.lifecycle.viewModelScope
import com.mvi.core.base.MviViewModel
import com.mvi.core.base.UiState
import com.mvi.core.network.ExceptionHandle.handleException
import com.mvi.core.network.download.DownloadManager
import com.mvi.core.network.download.DownloadState
import com.mvi.core.network.upload.UploadInfo
import com.mvi.core.network.upload.UploadRequestBody
import com.mvi.core.network.upload.UploadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody

/**
 * 文件上传和下载扩展函数
 */

// ============ 文件上传扩展 ============

/**
 * 上传单个文件
 *
 * @param uploadInfo 上传文件信息
 * @param uploadStateFlow 上传状态流（可选）
 * @param upload 上传函数（返回API响应）
 *
 * 使用示例:
 * ```kotlin
 * val uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
 *
 * uploadFile(
 *     uploadInfo = UploadInfo(file, "image/jpeg"),
 *     uploadStateFlow = uploadState
 * ) { multipartBody ->
 *     apiService.uploadFile(multipartBody)
 * }
 * ```
 */
fun <T, I : com.mvi.core.base.MviIntent> MviViewModel<I>.uploadFile(
    uploadInfo: UploadInfo,
    uploadStateFlow: MutableStateFlow<UploadState>? = null,
    upload: suspend (MultipartBody.Part) -> com.mvi.core.network.ApiResponse<T>
) {
    this.viewModelScope.launch {
        try {
            uploadStateFlow?.value = UploadState.Idle

            // 创建支持进度的RequestBody
            val requestBody = UploadRequestBody(
                file = uploadInfo.file,
                contentType = uploadInfo.contentType
            ) { bytesWritten, totalBytes ->
                val progress = ((bytesWritten * 100) / totalBytes).toInt()
                uploadStateFlow?.value = UploadState.Uploading(progress, bytesWritten, totalBytes)
            }

            // 创建MultipartBody.Part
            val multipartBody = MultipartBody.Part.createFormData(
                uploadInfo.formFieldName,
                uploadInfo.fileName,
                requestBody
            )

            // 执行上传
            val response = withContext(Dispatchers.IO) {
                upload(multipartBody)
            }

            // 处理响应
            if (response.isSuccess() && response.data != null) {
                uploadStateFlow?.value = UploadState.Success(response.data)
            } else {
                uploadStateFlow?.value = UploadState.Error(response.message)
            }
        } catch (e: Exception) {
            val errorData = e.handleException()
            uploadStateFlow?.value = UploadState.Error(errorData.message, e)
        }
    }
}

/**
 * 上传多个文件
 *
 * @param uploadInfoList 上传文件列表
 * @param uploadStateFlow 上传状态流（可选）
 * @param upload 上传函数（返回API响应）
 */
fun <T, I : com.mvi.core.base.MviIntent> MviViewModel<I>.uploadFiles(
    uploadInfoList: List<UploadInfo>,
    uploadStateFlow: MutableStateFlow<UploadState>? = null,
    upload: suspend (List<MultipartBody.Part>) -> com.mvi.core.network.ApiResponse<T>
) {
    this.viewModelScope.launch {
        try {
            uploadStateFlow?.value = UploadState.Idle

            val totalBytes = uploadInfoList.sumOf { it.file.length() }
            var uploadedBytes = 0L

            // 创建MultipartBody.Part列表
            val parts = uploadInfoList.map { uploadInfo ->
                val requestBody = UploadRequestBody(
                    file = uploadInfo.file,
                    contentType = uploadInfo.contentType
                ) { bytesWritten, _ ->
                    uploadedBytes += bytesWritten
                    val progress = ((uploadedBytes * 100) / totalBytes).toInt()
                    uploadStateFlow?.value = UploadState.Uploading(progress, uploadedBytes, totalBytes)
                }

                MultipartBody.Part.createFormData(
                    uploadInfo.formFieldName,
                    uploadInfo.fileName,
                    requestBody
                )
            }

            // 执行上传
            val response = withContext(Dispatchers.IO) {
                upload(parts)
            }

            // 处理响应
            if (response.isSuccess() && response.data != null) {
                uploadStateFlow?.value = UploadState.Success(response.data)
            } else {
                uploadStateFlow?.value = UploadState.Error(response.message)
            }
        } catch (e: Exception) {
            val errorData = e.handleException()
            uploadStateFlow?.value = UploadState.Error(errorData.message, e)
        }
    }
}

// ============ 文件下载扩展 ============

/**
 * 下载文件
 *
 * @param url 下载链接
 * @param savePath 保存路径
 * @param supportResume 是否支持断点续传
 * @param downloadStateFlow 下载状态流（可选）
 *
 * 使用示例:
 * ```kotlin
 * val downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
 *
 * downloadFile(
 *     url = "https://example.com/file.apk",
 *     savePath = "/sdcard/Download/file.apk",
 *     downloadStateFlow = downloadState
 * )
 * ```
 */
fun <I : com.mvi.core.base.MviIntent> MviViewModel<I>.downloadFile(
    url: String,
    savePath: String,
    supportResume: Boolean = true,
    downloadStateFlow: MutableStateFlow<DownloadState>? = null,
    downloadManager: DownloadManager = DownloadManager()
) {
    this.viewModelScope.launch {
        downloadManager.download(url, savePath, supportResume).collect { state ->
            downloadStateFlow?.value = state
        }
    }
}

/**
 * 取消下载
 */
fun <I : com.mvi.core.base.MviIntent> MviViewModel<I>.cancelDownload(
    savePath: String,
    downloadManager: DownloadManager = DownloadManager()
) {
    downloadManager.cancelDownload(savePath)
}
