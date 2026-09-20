package com.imcys.bilibilias.database.converter.download

import androidx.room3.TypeConverter
import com.imcys.bilibilias.database.entity.download.DownloadStage

class DownloadStageConverter {
    /** 非法值回退到 [DownloadStage.DOWNLOAD]。非空字段不能回退为 null，详见 [CookieEncodingConverter] 的说明。 */
    @TypeConverter
    fun fromString(value: String?): DownloadStage {
        return value?.let { runCatching { DownloadStage.valueOf(it) }.getOrNull() }
            ?: DownloadStage.DOWNLOAD
    }

    @TypeConverter
    fun stringToDownloadStage(downloadStage: DownloadStage?): String? {
        return downloadStage?.name
    }
}