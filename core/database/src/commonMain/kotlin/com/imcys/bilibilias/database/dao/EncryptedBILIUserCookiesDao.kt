package com.imcys.bilibilias.database.dao

import com.imcys.bilibilias.database.crypto.CredentialCipher
import com.imcys.bilibilias.database.entity.BILIUserCookiesEntity

/**
 * 在 DAO 边界对 Cookie 值做加解密。
 *
 * Cookie 的 [BILIUserCookiesEntity.value] 是非空字段，无法像 token 一样用 null 表达不可用，
 * 因此解密失败的 Cookie 会从读取结果中剔除——调用方等同于该 Cookie 不存在，不会拿到密文。
 */
class EncryptedBILIUserCookiesDao(
    private val delegate: BILIUserCookiesDao,
    private val cipher: CredentialCipher,
) : BILIUserCookiesDao {

    override suspend fun insertBILIUserCookie(biliUserCookiesEntity: BILIUserCookiesEntity): Long =
        delegate.insertBILIUserCookie(
            biliUserCookiesEntity.copy(value = cipher.encrypt(biliUserCookiesEntity.value))
        )

    override suspend fun getBILIUserCookiesByUid(userId: Long): List<BILIUserCookiesEntity> =
        delegate.getBILIUserCookiesByUid(userId).mapNotNull { entity ->
            cipher.decrypt(entity.value)?.let { entity.copy(value = it) }
        }

    override suspend fun deleteBILICookiesByUid(userId: Long): Int =
        delegate.deleteBILICookiesByUid(userId)
}
