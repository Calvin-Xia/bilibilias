package com.imcys.bilibilias.ui.login.navigation

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import com.imcys.bilibilias.database.entity.LoginPlatform
import com.imcys.bilibilias.ui.analysis.navigation.AnalysisRoute
import kotlinx.serialization.Serializable

@Serializable
object LoginRoute: NavKey

@Serializable
@Immutable
data class QRCodeLoginRoute(
    val defaultLoginPlatform: LoginPlatform = LoginPlatform.WEB,
    val isFromRoam: Boolean = false,
    val isFromAnalysis: Boolean = false,
): NavKey
