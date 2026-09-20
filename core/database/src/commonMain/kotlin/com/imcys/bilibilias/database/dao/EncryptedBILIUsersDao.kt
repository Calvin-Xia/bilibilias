package com.imcys.bilibilias.database.dao

import com.imcys.bilibilias.database.crypto.CredentialCipher
import com.imcys.bilibilias.database.entity.BILIUsersEntity
import com.imcys.bilibilias.database.entity.LoginPlatform

/**
 * 在 DAO 边界对账号 token 做加解密。
 *
 * 业务代码拿到的始终是明文实体，[delegate] 只会看到密文；解密失败时 token 以 null 返回，
 * 使请求退化为未登录状态，而不是带着不可用凭据继续调用。
 */
class EncryptedBILIUsersDao(
    private val delegate: BILIUsersDao,
    private val cipher: CredentialCipher,
) : BILIUsersDao {

    override suspend fun insertBILIUser(biliUsersEntity: BILIUsersEntity): Long =
        delegate.insertBILIUser(biliUsersEntity.encrypted())

    override suspend fun updateBILIUser(biliUsersEntity: BILIUsersEntity) =
        delegate.updateBILIUser(biliUsersEntity.encrypted())

    override suspend fun getBILIUserByMidAndPlatform(
        mid: Long,
        loginPlatform: LoginPlatform,
    ): BILIUsersEntity? = delegate.getBILIUserByMidAndPlatform(mid, loginPlatform)?.decrypted()

    override suspend fun getBILIUserByPlatform(loginPlatform: LoginPlatform): BILIUsersEntity? =
        delegate.getBILIUserByPlatform(loginPlatform)?.decrypted()

    override suspend fun getBILIUserListByUid(uid: Long): List<BILIUsersEntity> =
        delegate.getBILIUserListByUid(uid).map { it.decrypted() }

    override suspend fun getBILIUserListByPlatform(
        loginPlatform: LoginPlatform,
    ): List<BILIUsersEntity> =
        delegate.getBILIUserListByPlatform(loginPlatform).map { it.decrypted() }

    override suspend fun getBILIUserByUid(uid: Long): BILIUsersEntity? =
        delegate.getBILIUserByUid(uid)?.decrypted()

    override suspend fun deleteBILIUserByUid(userId: Long): Int =
        delegate.deleteBILIUserByUid(userId)

    private fun BILIUsersEntity.encrypted(): BILIUsersEntity = copy(
        accessToken = accessToken?.let(cipher::encrypt),
        refreshToken = refreshToken?.let(cipher::encrypt),
    )

    private fun BILIUsersEntity.decrypted(): BILIUsersEntity = copy(
        accessToken = accessToken?.let(cipher::decrypt),
        refreshToken = refreshToken?.let(cipher::decrypt),
    )
}
