package com.imcys.bilibilias.database.converter

import com.imcys.bilibilias.database.entity.ASSharedCookieEncoding
import com.imcys.bilibilias.database.entity.LoginPlatform
import kotlin.test.Test
import kotlin.test.assertEquals

class LoginPlatformConverterTest {

    private val converter = LoginPlatformConverter()

    @Test
    fun knownValueIsParsed() {
        assertEquals(LoginPlatform.TV, converter.fromString("TV"))
    }

    @Test
    fun invalidValueFallsBackInsteadOfThrowing() {
        // 非空字段：Room 生成的读取代码在转换结果为 null 时会直接抛错，因此必须回退到默认值
        assertEquals(LoginPlatform.WEB, converter.fromString("NOT_A_PLATFORM"))
    }

    @Test
    fun nullFallsBackInsteadOfThrowing() {
        assertEquals(LoginPlatform.WEB, converter.fromString(null))
    }

    @Test
    fun enumIsWrittenAsName() {
        assertEquals("MOBILE", converter.stringToLoginPlatform(LoginPlatform.MOBILE))
    }
}

class CookieEncodingConverterTest {

    private val converter = CookieEncodingConverter()

    @Test
    fun knownValueIsParsed() {
        assertEquals(ASSharedCookieEncoding.BASE64_ENCODING, converter.fromString("BASE64_ENCODING"))
    }

    @Test
    fun invalidValueFallsBackInsteadOfThrowing() {
        assertEquals(
            ASSharedCookieEncoding.URI_ENCODING,
            converter.fromString("NOT_AN_ENCODING"),
        )
    }

    @Test
    fun nullFallsBackInsteadOfThrowing() {
        assertEquals(ASSharedCookieEncoding.URI_ENCODING, converter.fromString(null))
    }
}
