package com.imcys.bilibilias.common.event

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking

/**
 * 全局 Toast/Snackbar 事件模型。
 *
 * 统一描述一条要展示给用户的消息，以及消息展示完成后的结果回调。
 */
sealed class ToastEvent(
    open val message: String,
    open val onResult: (SnackbarResult) -> Unit,
    open val duration: SnackbarDuration = SnackbarDuration.Short
) {
    /**
     * 带操作按钮的 Snackbar 事件。
     */
    data class ActionToastEvent(
        override val message: String,
        val actionLabel: String,
        override val duration: SnackbarDuration = SnackbarDuration.Indefinite,
        override val onResult: (SnackbarResult) -> Unit = {}
    ) : ToastEvent(message, onResult, duration)

    /**
     * 普通文本 Snackbar 事件。
     */
    data class NormalToastEvent(
        override val message: String,
        override val duration: SnackbarDuration = SnackbarDuration.Short,
        override val onResult: (SnackbarResult) -> Unit = {}
    ) : ToastEvent(message, onResult, duration)
}

/**
 * Toast 事件通道。
 */
private val toastEventChannel = Channel<ToastEvent>(Channel.UNLIMITED)

/**
 * Toast 事件流，由界面层统一收集并展示。
 */
val toastEventFlow = toastEventChannel.receiveAsFlow()

/**
 * 发送一条 Toast/Snackbar 事件。
 *
 * 当 `actionLabel` 不为空时发送带按钮事件，否则发送普通消息事件。
 */
suspend fun sendToastEvent(
    message: String,
    actionLabel: String? = null,
    duration: SnackbarDuration = SnackbarDuration.Short,
    onResult: (SnackbarResult) -> Unit = {}
) {
    if (actionLabel != null) {
        toastEventChannel.send(
            ToastEvent.ActionToastEvent(
                message = message,
                actionLabel = actionLabel,
                duration = duration,
                onResult = onResult
            )
        )
    } else {
        toastEventChannel.send(
            ToastEvent.NormalToastEvent(
                message = message,
                duration = duration,
                onResult = onResult
            )
        )
    }
}

/**
 * 在阻塞上下文中发送 Toast/Snackbar 事件。
 *
 * 适用于当前调用链无法直接使用挂起函数的场景。
 */
fun sendToastEventOnBlocking(
    message: String,
    actionLabel: String? = null,
    duration: SnackbarDuration = SnackbarDuration.Short,
    onResult: (SnackbarResult) -> Unit = {}
) {
    runBlocking {
        sendToastEvent(
            message = message,
            actionLabel = actionLabel,
            duration = duration,
            onResult = onResult
        )
    }
}
