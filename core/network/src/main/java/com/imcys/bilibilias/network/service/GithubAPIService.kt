package com.imcys.bilibilias.network.service

import com.imcys.bilibilias.network.FlowNetWorkResult
import com.imcys.bilibilias.network.adapter.githubHttpRequest
import com.imcys.bilibilias.network.config.API
import com.imcys.bilibilias.network.model.github.GithubCommit
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class GithubAPIService(
    val httpClient: HttpClient
) {
    suspend fun getCommits(
        org: String,
        repository: String,
        perPage: Int? = null
    ): FlowNetWorkResult<List<GithubCommit>> = httpClient.githubHttpRequest {
        get(API.Github.COMMITS.format(org, repository)) {
            parameter("per_page", perPage)
        }
    }
}