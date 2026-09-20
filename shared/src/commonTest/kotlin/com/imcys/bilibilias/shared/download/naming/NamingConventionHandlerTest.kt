package com.imcys.bilibilias.shared.download.naming

import androidx.datastore.core.DataStore
import com.imcys.bilibilias.data.repository.AppSettingsRepository
import com.imcys.bilibilias.database.entity.download.NamingConventionInfo
import com.imcys.bilibilias.datastore.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 命名规则处理的回归测试。
 *
 * 使用只读的内存 DataStore 构造 repository，覆盖占位符替换、文件名清洗与兜底分支。
 */
class NamingConventionHandlerTest {

    private fun handlerWith(
        videoRule: String = "{p_title}",
        bangumiRule: String = "{episode_title}"
    ): NamingConventionHandler {
        val settings = AppSettings(
            video_naming_rule = videoRule,
            bangumi_naming_rule = bangumiRule,
        )
        return NamingConventionHandler(AppSettingsRepository(FixedSettingsStore(settings)))
    }

    @Test
    fun videoRuleReplacesAllPlaceholders() {
        val result = handlerWith().buildFileNameWithConvention(
            namingRule = "{author} - {title}",
            conventionInfo = NamingConventionInfo.Video(
                title = "测试视频",
                author = "测试UP主"
            ),
            fileSuffix = "mp4"
        )

        assertEquals("测试UP主 - 测试视频.mp4", result)
    }

    @Test
    fun videoRuleFillsMissingPlaceholderWithEmptyString() {
        val result = handlerWith().buildFileNameWithConvention(
            namingRule = "{title}{p_title}",
            conventionInfo = NamingConventionInfo.Video(title = "标题"),
            fileSuffix = "mp4"
        )

        assertEquals("标题.mp4", result)
    }

    @Test
    fun slashInPlaceholderValueIsReplacedWithUnderscore() {
        val result = handlerWith().buildFileNameWithConvention(
            namingRule = "{title}",
            conventionInfo = NamingConventionInfo.Video(title = "第一集/第二集"),
            fileSuffix = "mp4"
        )

        assertEquals("第一集_第二集.mp4", result)
    }

    @Test
    fun consecutiveAndTrailingUnderscoresAreCollapsed() {
        val result = handlerWith().buildFileNameWithConvention(
            namingRule = "{author}_{title}_",
            conventionInfo = NamingConventionInfo.Video(title = "标题"),
            fileSuffix = "mp4"
        )

        // author 为空 → 连续下划线折叠为单个；末尾下划线被去掉
        assertEquals("_标题.mp4", result)
    }

    @Test
    fun donghuaRuleReplacesAllPlaceholders() {
        val result = handlerWith().buildFileNameWithConvention(
            namingRule = "{season_title} - {episode_number} - {episode_title}",
            conventionInfo = NamingConventionInfo.Donghua(
                seasonTitle = "第一季",
                episodeNumber = "01",
                episodeTitle = "开端"
            ),
            fileSuffix = "mkv"
        )

        assertEquals("第一季 - 01 - 开端.mkv", result)
    }

    @Test
    fun fileExtensionIsNotDuplicatedWhenRuleAlreadyContainsIt() {
        val result = handlerWith().buildFileNameWithConvention(
            namingRule = "{title}.mp4",
            conventionInfo = NamingConventionInfo.Video(title = "标题"),
            fileSuffix = "mp4"
        )

        assertEquals("标题.mp4", result)
    }

    @Test
    fun buildFileNameUsesVideoRuleFromSettings() = runTest {
        val result = handlerWith(videoRule = "{title}-{cid}")
            .buildFileName(
                conventionInfo = NamingConventionInfo.Video(title = "标题", cid = "12345"),
                fileExtension = "mp4"
            )

        assertEquals("标题-12345.mp4", result)
    }

    @Test
    fun buildFileNameUsesBangumiRuleFromSettings() = runTest {
        val result = handlerWith(bangumiRule = "{title}_{episode_number}")
            .buildFileName(
                conventionInfo = NamingConventionInfo.Donghua(title = "动画", episodeNumber = "03"),
                fileExtension = "mkv"
            )

        assertEquals("动画_03.mkv", result)
    }

    @Test
    fun buildFileNameFallsBackToUnknownWhenConventionIsNull() = runTest {
        val result = handlerWith().buildFileName(
            conventionInfo = null,
            fileExtension = "mp4"
        )

        assertEquals("unknown.mp4", result)
    }
}

/**
 * 只读的 DataStore 实现，仅用于向 repository 提供固定的 AppSettings。
 * 写入不被支持——命名规则测试不需要修改设置。
 */
private class FixedSettingsStore(
    private val settings: AppSettings
) : DataStore<AppSettings> {
    override val data: Flow<AppSettings> = flowOf(settings)

    override suspend fun updateData(transform: suspend (t: AppSettings) -> AppSettings): AppSettings =
        throw UnsupportedOperationException("测试用的只读 DataStore 不支持写入")
}
