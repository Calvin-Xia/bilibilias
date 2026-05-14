package com.imcys.bilibilias.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * 弹窗排序。
 */
@Immutable
data class DialogRequest(
    val key: String,
    val order: Int,
    val visible: Boolean,
    val content: @Composable () -> Unit
)

class DialogSortBuilder {
    private val dialogs = mutableListOf<DialogRequest>()
    private var nextOrder = 0
    private var nextKey = 0

    fun dialog(
        visible: Boolean,
        key: String = "dialog_${nextKey++}",
        order: Int = nextOrder++,
        content: @Composable () -> Unit
    ) {
        dialogs += DialogRequest(
            key = key,
            order = order,
            visible = visible,
            content = content
        )
    }

    fun build(): List<DialogRequest> = dialogs.toList()
}

@Composable
fun DialogSortHost(block: DialogSortBuilder.() -> Unit) {
    val dialogs = DialogSortBuilder().apply(block).build()
    dialogs
        .asSequence()
        .filter { it.visible }
        .minByOrNull { it.order }
        ?.content
        ?.invoke()
}
