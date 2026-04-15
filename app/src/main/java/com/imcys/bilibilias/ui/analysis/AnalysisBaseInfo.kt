package com.imcys.bilibilias.ui.analysis

import androidx.compose.runtime.Immutable

@Immutable
data class AnalysisBaseInfo(
    val title: String = "",
    val cover: String = "",
    val enabledSelectInfo: Boolean = false,
)
