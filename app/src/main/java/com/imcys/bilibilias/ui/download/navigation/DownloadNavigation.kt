package com.imcys.bilibilias.ui.download.navigation

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.Serializable


@Serializable
@Immutable
data class DownloadRoute(
    val defaultListIndex: Int = 0,
): NavKey

