package com.imcys.bilibilias.ui.user.navigation

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import com.imcys.bilibilias.ui.user.UserScreen
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class UserRoute(
    val mid: Long = 0,
    val isAnalysisUser: Boolean = false
) : NavKey
