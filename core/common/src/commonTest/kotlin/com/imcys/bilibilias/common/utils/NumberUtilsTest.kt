package com.imcys.bilibilias.common.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberUtilsTest {

    @Test
    fun nullFormatsAsZero() {
        assertEquals("0", NumberUtils.formatLargeNumber(null))
    }

    @Test
    fun smallNumbersAreUnchanged() {
        assertEquals("0", NumberUtils.formatLargeNumber(0))
        assertEquals("1", NumberUtils.formatLargeNumber(1))
        assertEquals("9999", NumberUtils.formatLargeNumber(9999))
    }

    @Test
    fun wanRangeUsesWanSuffix() {
        assertEquals("1万", NumberUtils.formatLargeNumber(10000))
        assertEquals("1.2万", NumberUtils.formatLargeNumber(12345))
        assertEquals("9999.9万", NumberUtils.formatLargeNumber(99_999_000))
    }

    @Test
    fun yiRangeUsesYiSuffix() {
        assertEquals("1亿", NumberUtils.formatLargeNumber(100_000_000))
        assertEquals("1.2亿", NumberUtils.formatLargeNumber(123_456_789))
    }

    @Test
    fun decimalsAreTruncatedNotRounded() {
        // 19999 / 10000 = 1.9999 → 截断为 1.9，而不是四舍五入成 2
        assertEquals("1.9万", NumberUtils.formatLargeNumber(19999))
    }
}
