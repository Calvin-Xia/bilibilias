package com.imcys.bilibilias.weight

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ASCollapsingToolbar(
    maxHeight: Dp,
    minHeight: Dp,
    toolbar: @Composable BoxScope.() -> Unit,
    onChangeProgress: (progress: Float) -> Unit = {},
    content: @Composable ColumnScope.(NestedScrollConnection) -> Unit,
) {
    val density = LocalDensity.current
    val maxHeightPx = with(density) { maxHeight.toPx() }
    val minHeightPx = with(density) { minHeight.toPx() }
    var toolbarHeightPx by remember { mutableFloatStateOf(maxHeightPx) }

    val currentOnChangeProgress by rememberUpdatedState(onChangeProgress)

    LaunchedEffect(maxHeightPx, minHeightPx) {
        snapshotFlow {
            ((maxHeightPx - toolbarHeightPx) / (maxHeightPx - minHeightPx)).coerceIn(0f, 1f)
        }.collect { progress ->
            currentOnChangeProgress(progress)
        }
    }

    val nestedScrollConnection = remember(minHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0) {
                    val newHeight = toolbarHeightPx + delta
                    val constrainedHeight = newHeight.coerceIn(minHeightPx, maxHeightPx)
                    val consumed = constrainedHeight - toolbarHeightPx
                    if (consumed != 0f) {
                        toolbarHeightPx = constrainedHeight
                        return Offset(0f, consumed)
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                if (delta > 0) {
                    val newHeight = toolbarHeightPx + delta
                    val constrainedHeight = newHeight.coerceIn(minHeightPx, maxHeightPx)
                    val consumed = constrainedHeight - toolbarHeightPx
                    if (consumed != 0f) {
                        toolbarHeightPx = constrainedHeight
                        return Offset(0f, consumed)
                    }
                }
                return Offset.Zero
            }
        }
    }

    Column(Modifier.nestedScroll(nestedScrollConnection)) {
        Box(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .height(with(density) { toolbarHeightPx.toDp() })
            ,
            content = toolbar
        )
        content(nestedScrollConnection)
    }
}
