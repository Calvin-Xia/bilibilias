package com.imcys.bilibilias.database.dao

import com.imcys.bilibilias.database.crypto.FakeCredentialCipher
import com.imcys.bilibilias.database.entity.BILIUsersEntity
import com.imcys.bilibilias.database.entity.LoginPlatform
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EncryptedBILIUsersDaoTest {

    private val delegate = InMemoryBILIUsersDao()
    private val dao = EncryptedBILIUsersDao(delegate, FakeCredentialCipher())

    private fun user(
        accessToken: String? = "access",
        refreshToken: String? = "refresh",
    ) = BILIUsersEntity(
        id = 1,
        loginPlatform = LoginPlatform.WEB,
        mid = 42,
        name = "测试用户",
        face = "https://example.com/face.jpg",
        level = 6,
        vipState = 0,
        refreshToken = refreshToken,
        accessToken = accessToken,
    )

    @Test
    fun insertStoresEncryptedTokens() = runTest {
        dao.insertBILIUser(user())

        val stored = assertNotNull(delegate.rows.firstOrNull())
        assertEquals("enc:access", stored.accessToken)
        assertEquals("enc:refresh", stored.refreshToken)
    }

    @Test
    fun insertKeepsOtherFieldsReadable() = runTest {
        dao.insertBILIUser(user())

        val stored = assertNotNull(delegate.rows.firstOrNull())
        assertEquals("测试用户", stored.name)
        assertEquals("https://example.com/face.jpg", stored.face)
        assertEquals(42, stored.mid)
    }

    @Test
    fun readReturnsDecryptedTokens() = runTest {
        dao.insertBILIUser(user())

        val loaded = assertNotNull(dao.getBILIUserByUid(1))
        assertEquals("access", loaded.accessToken)
        assertEquals("refresh", loaded.refreshToken)
    }

    @Test
    fun updateEncryptsTokens() = runTest {
        dao.insertBILIUser(user())
        dao.updateBILIUser(user(accessToken = "access-2", refreshToken = "refresh-2"))

        assertEquals("enc:access-2", assertNotNull(delegate.rows.firstOrNull()).accessToken)
        assertEquals("access-2", assertNotNull(dao.getBILIUserByUid(1)).accessToken)
    }

    @Test
    fun nullTokensStayNull() = runTest {
        dao.insertBILIUser(user(accessToken = null, refreshToken = null))

        val stored = assertNotNull(delegate.rows.firstOrNull())
        assertNull(stored.accessToken)
        assertNull(stored.refreshToken)

        val loaded = assertNotNull(dao.getBILIUserByUid(1))
        assertNull(loaded.accessToken)
        assertNull(loaded.refreshToken)
    }

    @Test
    fun undecryptableTokenIsTreatedAsMissing() = runTest {
        // 模拟密钥失效：库里是上一个密钥写下的密文
        delegate.rows.add(user().copy(accessToken = "legacy-cipher", refreshToken = "legacy-cipher"))

        val loaded = assertNotNull(dao.getBILIUserByUid(1))
        assertNull(loaded.accessToken)
        assertNull(loaded.refreshToken)
    }

    @Test
    fun listReadsAreDecrypted() = runTest {
        dao.insertBILIUser(user())

        val list = dao.getBILIUserListByPlatform(LoginPlatform.WEB)
        assertEquals(1, list.size)
        assertEquals("access", list.first().accessToken)
    }

    private class InMemoryBILIUsersDao : BILIUsersDao {
        val rows = mutableListOf<BILIUsersEntity>()

        override suspend fun insertBILIUser(biliUsersEntity: BILIUsersEntity): Long {
            rows.add(biliUsersEntity)
            return biliUsersEntity.id
        }

        override suspend fun updateBILIUser(biliUsersEntity: BILIUsersEntity) {
            rows.replaceAll { if (it.id == biliUsersEntity.id) biliUsersEntity else it }
        }

        override suspend fun getBILIUserByMidAndPlatform(
            mid: Long,
            loginPlatform: LoginPlatform,
        ): BILIUsersEntity? = rows.firstOrNull { it.mid == mid && it.loginPlatform == loginPlatform }

        override suspend fun getBILIUserByPlatform(loginPlatform: LoginPlatform): BILIUsersEntity? =
            rows.firstOrNull { it.loginPlatform == loginPlatform }

        override suspend fun getBILIUserListByUid(uid: Long): List<BILIUsersEntity> =
            rows.filter { it.id == uid }

        override suspend fun getBILIUserListByPlatform(
            loginPlatform: LoginPlatform,
        ): List<BILIUsersEntity> = rows.filter { it.loginPlatform == loginPlatform }

        override suspend fun getBILIUserByUid(uid: Long): BILIUsersEntity? =
            rows.firstOrNull { it.id == uid }

        override suspend fun deleteBILIUserByUid(userId: Long): Int =
            rows.filter { it.id == userId }.also { rows.removeAll(it) }.size
    }
}
