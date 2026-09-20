package com.imcys.bilibilias.database.converter.download

import androidx.room3.TypeConverter
import com.imcys.bilibilias.database.entity.download.DownloadTaskType

class DownloadTaskTypeConverter {
    /** 非法值回退到 [DownloadTaskType.BILI_VIDEO]。非空字段不能回退为 null，详见 [CookieEncodingConverter] 的说明。 */
    @TypeConverter
    fun fromString(value: String?): DownloadTaskType {
        return value?.let { runCatching { DownloadTaskType.valueOf(it) }.getOrNull() }
            ?: DownloadTaskType.BILI_VIDEO
    }

    @TypeConverter
    fun stringToDownloadTaskType(mode: DownloadTaskType?): String? {
        return mode?.name
    }
}