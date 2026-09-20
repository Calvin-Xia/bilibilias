package com.imcys.bilibilias.database.converter.download

import androidx.room3.TypeConverter
import com.imcys.bilibilias.database.entity.download.DownloadTaskNodeType

class DownloadTaskNodeTypeConverter {
    /** 非法值回退到 [DownloadTaskNodeType.BILI_VIDEO_PAGE]。非空字段不能回退为 null，详见 [CookieEncodingConverter] 的说明。 */
    @TypeConverter
    fun fromString(value: String?): DownloadTaskNodeType {
        return value?.let { runCatching { DownloadTaskNodeType.valueOf(it) }.getOrNull() }
            ?: DownloadTaskNodeType.BILI_VIDEO_PAGE
    }

    @TypeConverter
    fun stringToDownloadTaskNodeType(type: DownloadTaskNodeType?): String? {
        return type?.name
    }
}