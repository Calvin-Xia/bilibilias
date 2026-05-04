package com.imcys.bilibilias.data.repository

import com.imcys.bilibilias.common.data.CommonBuildConfig
import com.imcys.bilibilias.data.model.github.GithubCodeVersionUpdateInfo
import com.imcys.bilibilias.network.NetWorkResult
import com.imcys.bilibilias.network.mapData
import com.imcys.bilibilias.network.service.GithubAPIService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GithubInfoRepository(
    private val githubAPIService: GithubAPIService
) {
    suspend fun getLastCommitInfo() =
        githubAPIService.getCommits("1250422131", "bilibilias").map { result ->
            result.mapData { commits, response ->
                // 防御：空列表处理
                if (commits.isNullOrEmpty()) {
                    return@mapData GithubCodeVersionUpdateInfo(
                        hasUpdate = false,
                        tipMsg = "无法获取版本信息",
                        newHashList = emptyList(),
                    )
                }

                // 提取当前版本的短 hash，避免重复 substring
                val currentShortHash = CommonBuildConfig.gitCommitHash.take(8)

                // 单次遍历：同时查找索引和确认存在性
                val currentCommitIndex = commits.indexOfFirst {
                    it.sha.take(8) == currentShortHash
                }

                // 当前版本不在提交历史中
                if (currentCommitIndex == -1) {
                    return@mapData GithubCodeVersionUpdateInfo(
                        hasUpdate = false,
                        tipMsg = "$currentShortHash 无法追踪该版本信息（可能使用了本地修改或未推送的提交）",
                        newHashList = emptyList(),
                    )
                }

                return@mapData if (currentCommitIndex == 0) {
                    // 当前版本就是最新提交
                    GithubCodeVersionUpdateInfo(
                        hasUpdate = false,
                        tipMsg = "当前已是最新版本",
                        newHashList = emptyList(),
                    )
                } else {
                    val newCommits = commits.take(currentCommitIndex)
                    GithubCodeVersionUpdateInfo(
                        hasUpdate = true,
                        tipMsg = "发现 ${newCommits.size} 个可用提交：${
                            newCommits.map {
                                it.sha.take(
                                    8
                                )
                            }
                        }",
                        newHashList = newCommits.map { it.sha.take(8) },
                    )
                }
            }
        }
}

