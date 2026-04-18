package com.imcys.bilibilias.common.event

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * 页面跳转的栈处理策略。
 */
sealed interface NavigatePageMode {
    /**
     * 直接压入栈顶，不做任何复用处理。
     */
    data object Push : NavigatePageMode

    /**
     * 在栈内按页面类型复用。
     * 如果命中同类型页面，则复用该位置并移除其上的页面。
     */
    data object ReuseInStack : NavigatePageMode

    /**
     * 在栈顶复用。
     * 如果栈内已存在完全相同的页面，则移除旧实例并放到栈顶。
     */
    data object MoveToTop : NavigatePageMode

    /**
     * 用目标页面替换当前栈顶。
     */
    data object ReplaceTop : NavigatePageMode

    /**
     * 清空整个栈后只保留目标页面。
     */
    data object ClearAndPush : NavigatePageMode
}

/**
 * 全局页面跳转事件。
 *
 * `navKey` 表示目标页面，`mode` 用于声明本次导航如何处理回退栈。
 */
data class NavigatePageEvent(
    val navKey: NavKey,
    val mode: NavigatePageMode = NavigatePageMode.MoveToTop
)

private val navigatePageEventChannel = Channel<NavigatePageEvent>(Channel.UNLIMITED)

/**
 * 页面跳转事件流，由导航层统一收集并执行。
 */
val navigatePageEventFlow = navigatePageEventChannel.receiveAsFlow()

/**
 * 发送页面跳转事件。
 *
 * 默认使用 `MoveToTop`，适合大多数“如果已有就提到最上面，否则新增”的场景。
 */
fun sendNavigatePageEvent(
    navKey: NavKey,
    mode: NavigatePageMode = NavigatePageMode.MoveToTop
) {
    navigatePageEventChannel.trySend(
        NavigatePageEvent(
            navKey = navKey,
            mode = mode
        )
    )
}
