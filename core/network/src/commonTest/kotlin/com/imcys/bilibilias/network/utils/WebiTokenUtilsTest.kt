package com.imcys.bilibilias.network.utils

import com.imcys.bilibilias.network.model.BILILoginUserInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * WBI 签名密钥生成的回归测试。
 *
 * mixinKey 由 img_key + sub_key 按固定 64 位索引表置换后取前 32 位，
 * 该密钥参与 w_rid 计算，算法偏差会导致接口被风控。
 */
class WebiTokenUtilsTest {

    private fun wbiImg(imgKey: String, subKey: String) = BILILoginUserInfo.WbiImg(
        imgUrl = "https://i0.hdslb.com/bfs/wbi/$imgKey.png",
        subUrl = "https://i0.hdslb.com/bfs/wbi/$subKey.png"
    )

    @Test
    fun setKeyProducesKnownMixinKey() {
        WebiTokenUtils.setKey(
            wbiImg(
                imgKey = "7cd084941338484aae1ad9425b84077c",
                subKey = "4932caff0ff746eab6f01bf08b70ac45"
            )
        )

        // imgKey + subKey 按 64 位置换表重排后取前 32 位
        assertEquals("ea1db124af3c7062474693fa704f4ff8", WebiTokenUtils.key)
    }

    @Test
    fun setKeyTruncatesTo32Chars() {
        WebiTokenUtils.setKey(
            wbiImg(
                imgKey = "7cd084941338484aae1ad9425b84077c",
                subKey = "4932caff0ff746eab6f01bf08b70ac45"
            )
        )

        assertNotNull(WebiTokenUtils.key)
        assertEquals(32, WebiTokenUtils.key!!.length)
    }

    @Test
    fun setKeyReturnsNullWhenMixinKeyTooShort() {
        // 置换表最大索引为 63，组合后长度不足 64 时无法生成 key
        WebiTokenUtils.setKey(wbiImg(imgKey = "abc", subKey = "def"))

        assertNull(WebiTokenUtils.key)
    }
}
