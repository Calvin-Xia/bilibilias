package com.imcys.bilibilias.database.converter.download

import androidx.room3.TypeConverter
import com.imcys.bilibilias.database.entity.download.DownloadState

class DownloadStateConverter {
    /** 非法值回退到 [DownloadState.WAITING]（与实体默认值一致）。非空字段不能回退为 null，详见 [CookieEncodingConverter] 的说明。 */
    @TypeConverter
    fun fromString(value: String?): DownloadState {
        return value?.let { runCatching { DownloadState.valueOf(it) }.getOrNull() }
            ?: DownloadState.WAITING
    }

    @TypeConverter
    fun stringToDownloadState(type: DownloadState?): String? {
        return type?.name
    }
}