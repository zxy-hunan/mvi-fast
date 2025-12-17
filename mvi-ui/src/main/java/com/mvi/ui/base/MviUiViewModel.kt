package com.mvi.ui.base

import com.mvi.core.base.MviIntent
import com.mvi.core.base.MviViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * MviUiViewModel - 支持 UI 扩展事件的 ViewModel
 *
 * 在 MviViewModel 基础上添加了 MviUiEvent 支持
 *
 * @param I Intent类型
 */
abstract class MviUiViewModel<I : MviIntent> : MviViewModel<I>() {

    // UI扩展事件通道
    private val _uiUiEvent = Channel<MviUiEvent>(Channel.BUFFERED)
    val uiUiEvent: Flow<MviUiEvent> = _uiUiEvent.receiveAsFlow()

    /**
     * 发送 UI 扩展事件
     */
    protected fun sendUiEvent(event: MviUiEvent) {
        _uiUiEvent.trySend(event)
    }

    /**
     * 显示空状态
     */
    protected fun showEmptyState(message: String = "", iconResId: Int = 0, onRetry: (() -> Unit)? = null) {
        sendUiEvent(MviUiEvent.ShowEmptyState(message, iconResId, onRetry))
    }

    /**
     * 显示错误状态
     */
    protected fun showErrorState(message: String = "", iconResId: Int = 0, onRetry: (() -> Unit)? = null) {
        sendUiEvent(MviUiEvent.ShowErrorState(message, iconResId, onRetry))
    }

    /**
     * 隐藏空状态
     */
    protected fun hideEmptyState() {
        sendUiEvent(MviUiEvent.HideEmptyState)
    }
}
