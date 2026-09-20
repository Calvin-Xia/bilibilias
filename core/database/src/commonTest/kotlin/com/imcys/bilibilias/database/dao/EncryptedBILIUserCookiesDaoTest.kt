package com.imcys.bilibilias.database.dao

import com.imcys.bilibilias.database.crypto.FakeCredentialCipher
import com.imcys.bilibilias.database.entity.BILIUserCookiesEntity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EncryptedBILIUserCookiesDaoTest {

    private val delegate = InMemoryBILIUserCookiesDao()
    private val dao = EncryptedBILIUserCookiesDao(delegate, FakeCredentialCipher())

    private fun cookie(
        id: Long = 1,
        name: String = "SESSDATA",
        value: String = "sess-value",
    ) = BILIUserCookiesEntity(
        id = id,
        userId = 42,
        name = name,
        value = value,
        domain = ".bilibili.com",
        path = "/",
    )

    @Test
    fun insertEncryptsValueOnly() = runTest {
        dao.insertBILIUserCookie(cookie())

        val stored = assertNotNull(delegate.rows.firstOrNull())
        assertEquals("enc:sess-value", stored.value)
        // 这些列同样是 String，加密不得波及它们
        assertEquals("SESSDATA", stored.name)
        assertEquals(".bilibili.com", stored.domain)
        assertEquals("/", stored.path)
    }

    @Test
    fun readReturnsDecryptedValue() = runTest {
        dao.insertBILIUserCookie(cookie())

        val loaded = assertNotNull(dao.getBILIUserCookiesByUid(42).firstOrNull())
        assertEquals("sess-value", loaded.value)
        assertEquals("SESSDATA", loaded.name)
    }

    @Test
    fun multipleCookiesRoundTrip() = runTest {
        dao.insertBILIUserCookie(cookie(name = "SESSDATA", value = "a"))
        dao.insertBILIUserCookie(cookie(id = 2, name = "bili_jct", value = "b"))

        val values = dao.getBILIUserCookiesByUid(42).associate { it.name to it.value }
        assertEquals(mapOf("SESSDATA" to "a", "bili_jct" to "b"), values)
    }

    @Test
    fun undecryptableCookieIsDropped() = runTest {
        // 模拟密钥失效：库里是上一个密钥写下的密文
        delegate.rows.add(cookie(name = "SESSDATA", value = "legacy-cipher"))
        delegate.rows.add(cookie(id = 2, name = "bili_jct", value = "legacy-cipher"))

        // Cookie 值是非空字段，无法用 null 表达不可用，因此整体剔除；
        // 调用方等同于该 Cookie 不存在，不会拿到密文
        assertEquals(0, dao.getBILIUserCookiesByUid(42).size)
    }

    private class InMemoryBILIUserCookiesDao : BILIUserCookiesDao {
        val rows = mutableListOf<BILIUserCookiesEntity>()

        override suspend fun insertBILIUserCookie(biliUserCookiesEntity: BILIUserCookiesEntity): Long {
            rows.add(biliUserCookiesEntity)
            return biliUserCookiesEntity.id
        }

        override suspend fun getBILIUserCookiesByUid(userId: Long): List<BILIUserCookiesEntity> =
            rows.filter { it.userId == userId }

        override suspend fun deleteBILICookiesByUid(userId: Long): Int =
            rows.filter { it.userId == userId }.also { rows.removeAll(it) }.size
    }
}
