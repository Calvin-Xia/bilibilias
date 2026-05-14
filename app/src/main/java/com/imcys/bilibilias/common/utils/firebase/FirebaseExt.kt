package com.imcys.bilibilias.common.utils.firebase

import android.util.Log
import androidx.navigation3.runtime.NavKey
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ParametersBuilder
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.imcys.bilibilias.common.utils.analyticsSafe
import com.imcys.bilibilias.datastore.AppSettings

object FirebaseExt {
    fun logLogin(method: String) {
        firebaseLog(FirebaseAnalytics.Event.LOGIN) {
            param(FirebaseAnalytics.Param.METHOD, method)
        }
    }

    fun logVideoParse(
        bvId: String?,
    ) {
        firebaseLog("parse_video") {
            bvId?.let { param("bvid", it) }
        }
    }

    fun logShareParse() {
        firebaseLog("parse_share")
    }

    fun logRestoreBackStack(navKey: NavKey? = null) {
        firebaseLog("restore_back_stack") {
            navKey?.let { param("top_screen", it.analyticsScreenName()) }
        }
    }

    fun logOpenSubjectDetail(subjectId: Long) {
        firebaseLog("open_subject_detail") {
            param("subject_id", subjectId)
        }
    }

    fun logOpenAppPage(navKey: NavKey) {
        firebaseLog(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(
                FirebaseAnalytics.Param.SCREEN_NAME,
                navKey.analyticsScreenName()
            )
            param(
                FirebaseAnalytics.Param.SCREEN_CLASS,
                "MainActivity"
            )
        }
    }

    fun logSwitchVideoParsePlatform(platform: AppSettings.VideoParsePlatform) {
        firebaseLog("switch_video_as_platform") {
            param(
                "platform",
                platform.name
            )
        }
    }

    fun logBangumiParse(
        epId: Long? = null,
        ssId: Long? = null,
    ) {
        firebaseLog("parse_bangumi") {
            epId?.let { param("epId", it) }
            ssId?.let { param("ssId", it) }
        }
    }
}

private fun NavKey.analyticsScreenName(): String {
    return this::class.simpleName ?: this::class.qualifiedName ?: "UnknownPage"
}

fun firebaseLog(name: String, block: ParametersBuilder.() -> Unit = {}) {
    analyticsSafe {
        Firebase.analytics.logEvent(name) {
            block()
        }
    }
}
