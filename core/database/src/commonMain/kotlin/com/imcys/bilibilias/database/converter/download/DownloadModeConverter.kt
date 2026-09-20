package com.imcys.bilibilias.database.converter.download

import androidx.room3.TypeConverter
import com.imcys.bilibilias.database.entity.download.DownloadMode

class DownloadModeConverter {
    /** 非法值回退到 [DownloadMode.AUDIO_VIDEO]。非空字段不能回退为 null，详见 [CookieEncodingConverter] 的说明。 */
    @TypeConverter
    fun fromString(value: String?): DownloadMode {
        return value?.let { runCatching { DownloadMode.valueOf(it) }.getOrNull() }
            ?: DownloadMode.AUDIO_VIDEO
    }

    @TypeConverter
    fun stringToDownloadMode(mode: DownloadMode?): String? {
        return mode?.name
    }
}