package com.imcys.bilibilias.ui.setting

import androidx.compose.runtime.Immutable

@Immutable
data class SettingUIState(
    val isLogin: Boolean = false,
    val currentMid: Long = 0L
)
