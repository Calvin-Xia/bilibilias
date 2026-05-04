package com.imcys.bilibilias.common.data


object CommonBuildConfig {
    var enabledAnalytics = false
    var agreedPrivacyPolicy = false
    var gitCommitHash = ""
}


inline fun commonAnalyticsSafe(action: () -> Unit) {
    if (CommonBuildConfig.enabledAnalytics && CommonBuildConfig.agreedPrivacyPolicy) {
        action()
    }
}

fun isCommonAnalyticsSafe(): Boolean {
    return CommonBuildConfig.enabledAnalytics && CommonBuildConfig.agreedPrivacyPolicy
}