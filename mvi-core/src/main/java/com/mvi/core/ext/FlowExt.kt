package com.mvi.core.ext

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Flow扩展函数
 */

/**
 * 在指定生命周期状态下收集Flow
 */
fun <T> Flow<T>.collectOn(
    lifecycleOwner: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (T) -> Unit
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(state) {
            collect { value ->
                action(value)
            }
        }
    }
}

/**
 * StateFlow扩展 - 仅在值变化时触发
 */
fun <T> StateFlow<T>.collectDistinct(
    lifecycleOwner: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (T) -> Unit
) {
    var lastValue: T? = null
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(state) {
            collect { value ->
                if (value != lastValue) {
                    lastValue = value
                    action(value)
                }
            }
        }
    }
}
