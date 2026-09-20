package com.imcys.bilibilias.database.converter

import androidx.room3.TypeConverter
import com.imcys.bilibilias.database.entity.LoginPlatform

class LoginPlatformConverter {
    /**
     * 非法值回退到 [LoginPlatform.WEB]。
     *
     * 该字段在实体中非空，Room 生成的读取代码在转换结果为 null 时会直接抛错，
     * 因此这里必须给出默认值，而不是让 valueOf 抛异常或返回 null。
     */
    @TypeConverter
    fun fromString(value: String?): LoginPlatform {
        return value?.let { runCatching { LoginPlatform.valueOf(it) }.getOrNull() }
            ?: LoginPlatform.WEB
    }

    @TypeConverter
    fun stringToLoginPlatform(loginPlatform: LoginPlatform?): String? {
        return loginPlatform?.name
    }
}