package com.imcys.bilibilias.shared.navigation

import androidx.navigation3.runtime.NavKey
import com.imcys.bilibilias.shared.feature.login.CookieLoginRoute
import com.imcys.bilibilias.shared.feature.setting.about.AboutRoute
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 导航栈以多态序列化写入 DataStore，升级用户的存档里可能是改名前的类名。
 *
 * 用真实注册表（navKeySerializersModule）验证，锁住 Route 的序列化名：
 * 一旦有人去掉 @SerialName，恢复旧存档会失败，用户表现为导航栈恢复异常。
 */
class NavRouteSerializationNameTest {

    private val json = Json {
        serializersModule = navKeySerializersModule
    }

    // NavKey 是接口，需要多态序列化器（与 BILIBILAISNavDisplay 中的用法一致）
    private val navKeySerializer = PolymorphicSerializer(NavKey::class)

    @Test
    fun cookieLoginRouteKeepsLegacySerialName() {
        // 旧版本写入的存档格式：polymorphic 类型名 + 对象体
        val legacy = """{"type":"com.imcys.bilibilias.shared.feature.login.CookeLoginRoute"}"""

        assertEquals(CookieLoginRoute, json.decodeFromString(navKeySerializer, legacy))
    }

    @Test
    fun aboutRouteKeepsLegacySerialName() {
        val legacy = """{"type":"com.imcys.bilibilias.shared.feature.setting.about.AboutRouter"}"""

        assertEquals(AboutRoute, json.decodeFromString(navKeySerializer, legacy))
    }

    @Test
    fun routesAreWrittenUnderLegacyName() {
        // 写入时也用旧名字，读旧档与写新档用的是同一个标识
        assertEquals(
            "com.imcys.bilibilias.shared.feature.login.CookeLoginRoute",
            encodedTypeName(CookieLoginRoute),
        )
        assertEquals(
            "com.imcys.bilibilias.shared.feature.setting.about.AboutRouter",
            encodedTypeName(AboutRoute),
        )
    }

    private fun encodedTypeName(route: NavKey): String? =
        Regex("\"type\":\"([^\"]+)\"")
            .find(json.encodeToString(navKeySerializer, route))
            ?.groupValues
            ?.get(1)
}
