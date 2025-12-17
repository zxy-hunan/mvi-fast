package com.mvi.core.example

import com.mvi.core.base.MviIntent
import com.mvi.core.base.MviViewModel
import com.mvi.core.ext.downloadFile
import com.mvi.core.ext.uploadFile
import com.mvi.core.ext.uploadFiles
import com.mvi.core.network.download.DownloadState
import com.mvi.core.network.upload.UploadInfo
import com.mvi.core.network.upload.UploadState
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/**
 * 文件上传下载使用示例
 *
 * 此文件展示如何在 ViewModel 中使用文件上传和下载功能
 */

// ============ Intent 定义 ============

sealed class FileIntent : MviIntent {
    data class UploadFile(val file: File) : FileIntent()
    data class UploadMultipleFiles(val files: List<File>) : FileIntent()
    data class DownloadFile(val url: String, val savePath: String) : FileIntent()
    data object CancelDownload : FileIntent()
}

// ============ ViewModel 示例 ============

class FileUploadDownloadViewModel : MviViewModel<FileIntent>() {

    // 上传状态
    val uploadState = MutableStateFlow<UploadState>(UploadState.Idle)

    // 下载状态
    val downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)

    override fun handleIntent(intent: FileIntent) {
        when (intent) {
            is FileIntent.UploadFile -> handleUploadFile(intent.file)
            is FileIntent.UploadMultipleFiles -> handleUploadMultipleFiles(intent.files)
            is FileIntent.DownloadFile -> handleDownloadFile(intent.url, intent.savePath)
            is FileIntent.CancelDownload -> handleCancelDownload()
        }
    }

    // ============ 单文件上传示例 ============

    private fun handleUploadFile(file: File) {
        uploadFile(
            uploadInfo = UploadInfo(
                file = file,
                contentType = "image/jpeg", // 根据文件类型设置
                fileName = file.name,
                formFieldName = "file" // 后端接口要求的字段名
            ),
            uploadStateFlow = uploadState
        ) { multipartBody ->
            // 调用你的 API Service
            // apiService.uploadFile(multipartBody)

            // 模拟返回（实际使用时替换为真实 API 调用）
            com.mvi.core.network.ApiResponse(
                code = 200,
                message = "Success",
                data = "Upload successful"
            )
        }
    }

    // ============ 多文件上传示例 ============

    private fun handleUploadMultipleFiles(files: List<File>) {
        val uploadInfoList = files.map { file ->
            UploadInfo(
                file = file,
                contentType = "image/jpeg",
                fileName = file.name,
                formFieldName = "files" // 多文件上传的字段名
            )
        }

        uploadFiles(
            uploadInfoList = uploadInfoList,
            uploadStateFlow = uploadState
        ) { parts ->
            // 调用你的 API Service
            // apiService.uploadMultipleFiles(parts)

            // 模拟返回（实际使用时替换为真实 API 调用）
            com.mvi.core.network.ApiResponse(
                code = 200,
                message = "Success",
                data = "All files uploaded"
            )
        }
    }

    // ============ 文件下载示例 ============

    private fun handleDownloadFile(url: String, savePath: String) {
        downloadFile(
            url = url,
            savePath = savePath,
            supportResume = true, // 支持断点续传
            downloadStateFlow = downloadState
        )
    }

    // ============ 取消下载示例 ============

    private fun handleCancelDownload() {
        // cancelDownload(savePath)
    }
}

// ============ Activity/Fragment 中使用示例 ============

/**
 * Activity 使用示例:
 *
 * ```kotlin
 * class FileActivity : MviActivity<FileIntent>() {
 *
 *     private val viewModel: FileUploadDownloadViewModel by viewModels()
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         // 观察上传状态
 *         lifecycleScope.launch {
 *             viewModel.uploadState.collect { state ->
 *                 when (state) {
 *                     is UploadState.Idle -> {
 *                         // 空闲状态
 *                     }
 *                     is UploadState.Uploading -> {
 *                         // 更新进度
 *                         updateProgress(state.progress)
 *                         binding.tvProgress.text = "Uploading ${state.progress}%"
 *                     }
 *                     is UploadState.Success -> {
 *                         // 上传成功
 *                         showToast("Upload successful")
 *                     }
 *                     is UploadState.Error -> {
 *                         // 上传失败
 *                         showToast(state.message)
 *                     }
 *                 }
 *             }
 *         }
 *
 *         // 观察下载状态
 *         lifecycleScope.launch {
 *             viewModel.downloadState.collect { state ->
 *                 when (state) {
 *                     is DownloadState.Idle -> {
 *                         // 空闲状态
 *                     }
 *                     is DownloadState.Downloading -> {
 *                         // 更新进度
 *                         updateProgress(state.progress)
 *                         binding.tvProgress.text = "Downloading ${state.progress}%"
 *                     }
 *                     is DownloadState.Success -> {
 *                         // 下载成功
 *                         showToast("Download completed")
 *                         // 可以安装APK或打开文件
 *                         installApk(state.file)
 *                     }
 *                     is DownloadState.Error -> {
 *                         // 下载失败
 *                         showToast(state.message)
 *                     }
 *                 }
 *             }
 *         }
 *
 *         // 选择文件后上传
 *         binding.btnUpload.setOnClickListener {
 *             val file = File("/path/to/file")
 *             viewModel.sendIntent(FileIntent.UploadFile(file))
 *         }
 *
 *         // 下载文件
 *         binding.btnDownload.setOnClickListener {
 *             viewModel.sendIntent(
 *                 FileIntent.DownloadFile(
 *                     url = "https://example.com/file.apk",
 *                     savePath = "/sdcard/Download/file.apk"
 *                 )
 *             )
 *         }
 *     }
 * }
 * ```
 */

// ============ API Service 定义示例 ============

/**
 * Retrofit API Service 示例:
 *
 * ```kotlin
 * interface FileApiService {
 *
 *     @Multipart
 *     @POST("upload/single")
 *     suspend fun uploadFile(
 *         @Part file: MultipartBody.Part
 *     ): ApiResponse<String>
 *
 *     @Multipart
 *     @POST("upload/multiple")
 *     suspend fun uploadMultipleFiles(
 *         @Part files: List<MultipartBody.Part>
 *     ): ApiResponse<String>
 *
 *     @Streaming
 *     @GET
 *     suspend fun downloadFile(
 *         @Url url: String
 *     ): Response<ResponseBody>
 * }
 * ```
 */
