package com.imcys.bilibilias.common.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AsRegexUtilTest {

    @Test
    fun parsesBvIdFromVideoUrl() {
        assertEquals(
            TextType.BILI.BV("BV1xx411c7mD"),
            AsRegexUtil.parse("https://www.bilibili.com/video/BV1xx411c7mD")
        )
    }

    @Test
    fun parsesBareBvId() {
        assertEquals(TextType.BILI.BV("BV1xx411c7mD"), AsRegexUtil.parse("BV1xx411c7mD"))
    }

    @Test
    fun parsesAvId() {
        assertEquals(TextType.BILI.AV(12345L), AsRegexUtil.parse("av12345"))
    }

    @Test
    fun parsesEpId() {
        assertEquals(
            TextType.BILI.EP(12345L),
            AsRegexUtil.parse("https://www.bilibili.com/bangumi/play/ep12345")
        )
    }

    @Test
    fun parsesSsId() {
        assertEquals(
            TextType.BILI.SS(12345L),
            AsRegexUtil.parse("https://www.bilibili.com/bangumi/play/ss12345")
        )
    }

    @Test
    fun parsesUserSpaceId() {
        assertEquals(
            TextType.BILI.UserSpace("123456"),
            AsRegexUtil.parse("https://space.bilibili.com/123456")
        )
    }

    @Test
    fun returnsNullForUnrecognizedText() {
        assertNull(AsRegexUtil.parse("这不是一个 B 站链接"))
        assertNull(AsRegexUtil.parse(""))
        assertNull(AsRegexUtil.parse("https://example.com/video/123"))
    }

    @Test
    fun bvTakesPrecedenceOverAvInSameText() {
        // isBV 在 parse 的分支中先于 isAV 判定
        assertEquals(
            TextType.BILI.BV("BV1xx411c7mD"),
            AsRegexUtil.parse("BV1xx411c7mD av99999")
        )
    }
}
