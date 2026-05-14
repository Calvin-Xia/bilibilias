package com.imcys.bilibilias.common.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.imcys.bilibilias.BuildConfig
import com.imcys.bilibilias.common.data.CommonBuildConfig
import com.imcys.bilibilias.common.event.sendToastEventOnBlocking
import com.imcys.bilibilias.network.ApiStatus
import com.imcys.bilibilias.network.FlowNetWorkResult
import com.imcys.bilibilias.network.NetWorkResult
import kotlinx.coroutines.flow.last
import kotlin.system.exitProcess

fun Context.openLink(url: String) {
    if (url.isEmpty()) return
    try {
        val intent = Intent().apply {
            action = "android.intent.action.VIEW"
            data = url.toUri()
        }
        startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(this, "无法打开链接", Toast.LENGTH_SHORT).show()
    }
}

fun String.copyText(context: Context, title: String) {
    val clipboard =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(title, this)
    clipboard.setPrimaryClip(clip)
    sendToastEventOnBlocking("已复制到剪贴板")
}

suspend fun <T> autoRequestRetry(
    onErrorTip: (NetWorkResult<T?>?) -> String,
    block: suspend () -> FlowNetWorkResult<T>
): T? {
    var count = 1
    var lastResult: NetWorkResult<T?>? = null
    while (count < 3) {
        val result = block().last()
        lastResult = result
        if (result.status == ApiStatus.SUCCESS) {
            return result.data
        }
        count++
    }
    error(onErrorTip(lastResult))
}


suspend fun <T> autoRequestRetry(
    block: suspend () -> FlowNetWorkResult<T>
): NetWorkResult<T?> {
    var count = 1
    var lastResult: NetWorkResult<T?>? = null
    while (count < 3) {
        val result = block().last()
        lastResult = result
        if (result.status == ApiStatus.SUCCESS) {
            return result
        }
        count++
    }
    return lastResult ?: error("请求异常")
}

inline fun analyticsSafe(action: () -> Unit) {
    if (BuildConfig.ENABLED_ANALYTICS && CommonBuildConfig.agreedPrivacyPolicy) {
        action()
    }
}

inline fun baiduAnalyticsSafe(action: () -> Unit) {
    if (BuildConfig.ENABLED_ANALYTICS && !BuildConfig.ENABLED_PLAY_APP_MODE && CommonBuildConfig.agreedPrivacyPolicy) {
        action()
    }
}

fun isEnabledAnalytics(): Boolean {
    return BuildConfig.ENABLED_ANALYTICS
}


fun restartApp(context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    val mainIntent = Intent.makeRestartActivityTask(intent!!.component)
    // 清除任务栈
    mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(mainIntent)
    exitProcess(0)
}

fun String.stripNewlines(replacement: String = " "): String =
    replace(Regex("[\\r\\n]+"), replacement)
