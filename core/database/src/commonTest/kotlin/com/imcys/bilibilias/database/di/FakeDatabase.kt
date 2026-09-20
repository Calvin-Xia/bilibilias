package com.imcys.bilibilias.database.di

import com.imcys.bilibilias.database.BILIBILIASDatabase
import com.imcys.bilibilias.database.dao.BILIUserCookiesDao
import com.imcys.bilibilias.database.dao.BILIUsersDao
import com.imcys.bilibilias.database.dao.DownloadTaskDao
import com.imcys.bilibilias.database.entity.BILIUserCookiesEntity
import com.imcys.bilibilias.database.entity.BILIUsersEntity
import com.imcys.bilibilias.database.entity.LoginPlatform

/**
 * 只用于验证 Koin 绑定的空壳数据库，不连接真实存储。
 *
 * 装饰器在构造时会调用 biliUsersDao() / biliUserCookiesDao() 取被委托对象，
 * 因此这里必须返回可用的空实现，而不是抛异常。
 */
internal class FakeDatabase : BILIBILIASDatabase() {
    override fun biliUsersDao(): BILIUsersDao = FakeUsersDao
    override fun biliUserCookiesDao(): BILIUserCookiesDao = FakeCookiesDao
    override fun downloadTaskDao(): DownloadTaskDao = throw UnsupportedOperationException()
    override suspend fun clearAllTables() = Unit
}

private object FakeUsersDao : BILIUsersDao {
    override suspend fun insertBILIUser(biliUsersEntity: BILIUsersEntity): Long = 0
    override suspend fun updateBILIUser(biliUsersEntity: BILIUsersEntity) = Unit
    override suspend fun getBILIUserByMidAndPlatform(mid: Long, loginPlatform: LoginPlatform): BILIUsersEntity? = null
    override suspend fun getBILIUserByPlatform(loginPlatform: LoginPlatform): BILIUsersEntity? = null
    override suspend fun getBILIUserListByUid(uid: Long): List<BILIUsersEntity> = emptyList()
    override suspend fun getBILIUserListByPlatform(loginPlatform: LoginPlatform): List<BILIUsersEntity> = emptyList()
    override suspend fun getBILIUserByUid(uid: Long): BILIUsersEntity? = null
    override suspend fun deleteBILIUserByUid(userId: Long): Int = 0
}

private object FakeCookiesDao : BILIUserCookiesDao {
    override suspend fun insertBILIUserCookie(biliUserCookiesEntity: BILIUserCookiesEntity): Long = 0
    override suspend fun getBILIUserCookiesByUid(userId: Long): List<BILIUserCookiesEntity> = emptyList()
    override suspend fun deleteBILICookiesByUid(userId: Long): Int = 0
}
