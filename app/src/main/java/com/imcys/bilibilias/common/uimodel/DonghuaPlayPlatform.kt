package com.imcys.bilibilias.common.uimodel

import androidx.annotation.IdRes
import com.imcys.bilibilias.R

enum class DonghuaPlayPlatform(
    val searchUrl: String? = null,
    val officialUrl: String? = null,
    @field:IdRes val iconResId: Int? = null,
) {
    Netflix(officialUrl = "https://www.netflix.com", iconResId = R.drawable.ic_netflix),
    BiliBili(
        iconResId = R.drawable.ic_bilibili_color
    ),
    Youtube(
        iconResId = R.drawable.ic_youtube
    )
}