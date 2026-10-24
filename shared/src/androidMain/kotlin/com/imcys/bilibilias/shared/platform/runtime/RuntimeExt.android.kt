package com.imcys.bilibilias.shared.platform.runtime

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.net.toUri
import org.koin.mp.KoinPlatform.getKoin
import java.net.URLDecoder
import java.net.URLEncoder

val koinApplication: Context = getKoin().get<Application>()

actual fun openLink(url: String): Boolean {
    return try {
        koinApplication.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                url.toUri()
            )
        )
        true
    } catch (e: Exception) {
        false
    }
}

actual fun format(format: String, vararg args: Any?): String {
    return String.format(format, *args)
}

actual fun getAppVersion(): Pair<Long, String> {
    val context = koinApplication
    val pm = context.packageManager
    val pkg = context.packageName
    val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION") pm.getPackageInfo(pkg, 0)
    }

    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        pi.longVersionCode
    } else {
        @Suppress("DEPRECATION") pi.versionCode.toLong()
    }
    val name = pi.versionName ?: "unknown"

    return code to name
}

actual fun urlEncode(url: String): String {
    return URLEncoder.encode(url, "UTF-8")
}

actual fun urlDecode(url: String): String {
    return URLDecoder.decode(url, "UTF-8")
}
