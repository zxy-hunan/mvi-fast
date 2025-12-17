package com.mvi.core.network.download

import java.io.File

/**
 * 下载状态
 */
sealed class DownloadState {
    /** 空闲状态 */
    data object Idle : DownloadState()

    /** 下载中 */
    data class Downloading(
        val progress: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : DownloadState()

    /** 下载成功 */
    data class Success(val file: File) : DownloadState()

    /** 下载失败 */
    data class Error(val message: String, val throwable: Throwable? = null) : DownloadState()
}
