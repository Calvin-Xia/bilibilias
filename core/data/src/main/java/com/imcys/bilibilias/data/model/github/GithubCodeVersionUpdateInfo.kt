package com.imcys.bilibilias.data.model.github

import kotlinx.serialization.Serializable

@Serializable
data class GithubCodeVersionUpdateInfo(
    val hasUpdate: Boolean,
    val newHashList: List<String>,
    val tipMsg: String,
)
