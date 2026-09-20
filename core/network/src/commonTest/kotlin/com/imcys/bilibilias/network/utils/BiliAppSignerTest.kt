package com.imcys.bilibilias.network.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * APP 签名算法回归测试。
 *
 * 签名结果由 B 站服务端校验，任何算法改动都会导致接口鉴权失败，
 * 因此这里锁定一个已知输入的确切输出。
 */
class BiliAppSignerTest {

    @Test
    fun appSignMatchesKnownDigest() {
        val params = mutableMapOf("foo" to "bar")

        val sign = BiliAppSigner.appSign(params)

        // appkey=4409e2ce8ffd12b8&foo=bar + APP_SEC 的 MD5
        assertEquals("2ebc3b75ff882316cb2f99128ac6ce42", sign)
    }

    @Test
    fun appSignInjectsAppKeyIntoParams() {
        val params = mutableMapOf("foo" to "bar")

        BiliAppSigner.appSign(params)

        assertEquals(BiliAppSigner.APP_KEY, params["appkey"])
    }

    @Test
    fun paramOrderDoesNotAffectSign() {
        val first = mutableMapOf("a" to "1", "b" to "2")
        val second = mutableMapOf("b" to "2", "a" to "1")

        assertEquals(BiliAppSigner.appSign(first), BiliAppSigner.appSign(second))
    }

    @Test
    fun signIsLowercaseHexOfMd5Length() {
        val sign = BiliAppSigner.appSign(mutableMapOf("foo" to "bar"))

        assertTrue(sign != null, "签名不应为 null")
        assertEquals(32, sign.length)
        assertTrue(sign.all { it in "0123456789abcdef" }, "签名应为小写十六进制：$sign")
    }

    @Test
    fun tvDeviceInfoContainsRequiredKeys() {
        val info = BiliAppSigner.biliTvDeviceInfo

        listOf(
            "bili_local_id", "build", "buvid", "channel", "device", "device_id",
            "device_name", "device_platform", "fingerprint", "guid",
            "local_fingerprint", "local_id", "mobi_app", "networkstate",
            "platform", "sys_ver"
        ).forEach { key ->
            assertTrue(info.containsKey(key), "缺少设备指纹字段：$key")
        }
    }
}
