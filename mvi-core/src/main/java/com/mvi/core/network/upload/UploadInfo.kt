package com.mvi.core.network.upload

import java.io.File

/**
 * 文件上传信息
 */
data class UploadInfo(
    val file: File,
    val contentType: String = "application/octet-stream",
    val fileName: String = file.name,
    val formFieldName: String = "file"
)

/**
 * 上传进度状态
 */
sealed class UploadState {
    /** 等待上传 */
    data object Idle : UploadState()

    /** 上传中 */
    data class Uploading(
        val progress: Int,
        val bytesWritten: Long,
        val totalBytes: Long
    ) : UploadState()

    /** 上传成功 */
    data class Success<T>(val data: T) : UploadState()

    /** 上传失败 */
    data class Error(val message: String, val throwable: Throwable? = null) : UploadState()
}
