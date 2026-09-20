package com.imcys.bilibilias.database.converter.download

import androidx.room3.TypeConverter
import com.imcys.bilibilias.database.entity.download.MediaContainer

class MediaContainerConverter {
    @TypeConverter
    fun fromContainer(container: MediaContainer): String =
        container.extension

    /**
     * 未知扩展名回退到 [MediaContainer.MP4]。
     *
     * 转换器只能看到扩展名字符串，无法得知父任务的下载模式（音频/视频），因此统一按视频容器兜底；
     * 仅音频但扩展名非法的数据会得到 mp4，属已知取舍。
     */
    @TypeConverter
    fun toContainer(extension: String): MediaContainer =
        MediaContainer.entries.firstOrNull { it.extension == extension } ?: MediaContainer.MP4
}