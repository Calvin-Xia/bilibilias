package com.imcys.bilibilias.database.converter.download

import androidx.room3.TypeConverter
import com.imcys.bilibilias.database.entity.download.DownloadPlatform

class DownloadPlatformConverter {
    /** 非法值回退到 [DownloadPlatform.BILIBILI]。非空字段不能回退为 null，详见 [CookieEncodingConverter] 的说明。 */
    @TypeConverter
    fun fromString(value: String?): DownloadPlatform {
        return value?.let { runCatching { DownloadPlatform.valueOf(it) }.getOrNull() }
            ?: DownloadPlatform.BILIBILI
    }

    @TypeConverter
    fun stringToDownloadPlatform(downloadPlatform: DownloadPlatform?): String? {
        return downloadPlatform?.name
    }
}