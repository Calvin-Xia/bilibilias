package com.imcys.bilibilias.database.converter.download

import com.imcys.bilibilias.database.entity.download.DownloadMode
import com.imcys.bilibilias.database.entity.download.DownloadPlatform
import com.imcys.bilibilias.database.entity.download.DownloadStage
import com.imcys.bilibilias.database.entity.download.DownloadState
import com.imcys.bilibilias.database.entity.download.DownloadTaskNodeType
import com.imcys.bilibilias.database.entity.download.DownloadTaskType
import com.imcys.bilibilias.database.entity.download.MediaContainer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 覆盖 B2：这些字段在实体中都是非空，Room 生成的读取代码在转换结果为 null 时会直接抛错，
 * 因此遇到非法字符串必须回退到默认值而不是抛异常。
 */
class DownloadConvertersFallbackTest {

    @Test
    fun downloadModeFallsBackToAudioVideo() {
        assertEquals(DownloadMode.AUDIO_VIDEO, DownloadModeConverter().fromString("BROKEN"))
        assertEquals(DownloadMode.AUDIO_VIDEO, DownloadModeConverter().fromString(null))
        assertEquals(DownloadMode.VIDEO_ONLY, DownloadModeConverter().fromString("VIDEO_ONLY"))
    }

    @Test
    fun downloadPlatformFallsBackToBilibili() {
        assertEquals(DownloadPlatform.BILIBILI, DownloadPlatformConverter().fromString("BROKEN"))
        assertEquals(DownloadPlatform.BILIBILI, DownloadPlatformConverter().fromString(null))
        assertEquals(DownloadPlatform.ACFUN, DownloadPlatformConverter().fromString("ACFUN"))
    }

    @Test
    fun downloadStageFallsBackToDownload() {
        assertEquals(DownloadStage.DOWNLOAD, DownloadStageConverter().fromString("BROKEN"))
        assertEquals(DownloadStage.DOWNLOAD, DownloadStageConverter().fromString(null))
        assertEquals(DownloadStage.MERGE, DownloadStageConverter().fromString("MERGE"))
    }

    @Test
    fun downloadStateFallsBackToWaiting() {
        assertEquals(DownloadState.WAITING, DownloadStateConverter().fromString("BROKEN"))
        assertEquals(DownloadState.WAITING, DownloadStateConverter().fromString(null))
        assertEquals(DownloadState.MERGING, DownloadStateConverter().fromString("MERGING"))
    }

    @Test
    fun nodeTypeFallsBackToVideoPage() {
        assertEquals(
            DownloadTaskNodeType.BILI_VIDEO_PAGE,
            DownloadTaskNodeTypeConverter().fromString("BROKEN"),
        )
        assertEquals(
            DownloadTaskNodeType.BILI_VIDEO_PAGE,
            DownloadTaskNodeTypeConverter().fromString(null),
        )
        assertEquals(
            DownloadTaskNodeType.BILI_DONGHUA_EPISOD,
            DownloadTaskNodeTypeConverter().fromString("BILI_DONGHUA_EPISOD"),
        )
    }

    @Test
    fun taskTypeFallsBackToVideo() {
        assertEquals(DownloadTaskType.BILI_VIDEO, DownloadTaskTypeConverter().fromString("BROKEN"))
        assertEquals(DownloadTaskType.BILI_VIDEO, DownloadTaskTypeConverter().fromString(null))
        assertEquals(DownloadTaskType.BILI_DONGHUA, DownloadTaskTypeConverter().fromString("BILI_DONGHUA"))
    }

    @Test
    fun mediaContainerFallsBackToMp4ForUnknownExtension() {
        // 扩展名非法时统一按视频容器兜底
        assertEquals(MediaContainer.MP4, MediaContainerConverter().toContainer("broken"))
        assertEquals(MediaContainer.MP4, MediaContainerConverter().toContainer(""))
    }

    @Test
    fun mediaContainerParsesKnownExtensions() {
        assertEquals(MediaContainer.MKV, MediaContainerConverter().toContainer("mkv"))
        assertEquals(MediaContainer.M4A, MediaContainerConverter().toContainer("m4a"))
        assertEquals(MediaContainer.MP3, MediaContainerConverter().toContainer("mp3"))
    }
}
