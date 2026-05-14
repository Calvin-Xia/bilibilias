package com.imcys.bilibilias.ui.weight

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.imcys.bilibilias.ui.utils.DialogSortBuilder

@Composable
fun ASAlertDialog(
    showState: Boolean = false,
    clickBlankDismiss: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,

    ) {
    if (showState) {
        AlertDialog(
            title = title,
            text = text,
            icon = icon,
            modifier = modifier,
            onDismissRequest = {
                if (clickBlankDismiss) {
                    onDismiss?.invoke()
                }
            },
            confirmButton = confirmButton,
            dismissButton = dismissButton
        )
    }
}


context(_: DialogSortBuilder)
@Composable
fun ASAlertDialog(
    clickBlankDismiss: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    ) {
    ASAlertDialog(
        showState = true,
        clickBlankDismiss = clickBlankDismiss,
        icon = icon,
        title = title,
        text = text,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        onDismiss = onDismiss
    )
}


