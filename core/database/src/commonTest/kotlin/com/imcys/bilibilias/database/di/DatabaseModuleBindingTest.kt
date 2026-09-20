package com.imcys.bilibilias.database.di

import com.imcys.bilibilias.database.BILIBILIASDatabase
import com.imcys.bilibilias.database.crypto.CredentialCipher
import com.imcys.bilibilias.database.crypto.PassthroughCredentialCipher
import com.imcys.bilibilias.database.dao.BILIUserCookiesDao
import com.imcys.bilibilias.database.dao.BILIUsersDao
import com.imcys.bilibilias.database.dao.EncryptedBILIUserCookiesDao
import com.imcys.bilibilias.database.dao.EncryptedBILIUsersDao
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * 守护 databaseModule 的注册类型。
 *
 * 消费方（UserInfoRepository、AsCookiesStorage、RoamPlugin、SettingViewModel 等）
 * 按接口类型请求 DAO；若定义被推断成装饰器的具体类型，解析时会抛
 * NoDefinitionFoundException。这里直接按接口类型解析，防止再次退化。
 */
class DatabaseModuleBindingTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun daosAreResolvableByInterfaceType() {
        startTestKoin()
        val koin = org.koin.core.context.GlobalContext.get()

        assertNotNull(koin.get<BILIUsersDao>())
        assertNotNull(koin.get<BILIUserCookiesDao>())
    }

    @Test
    fun resolvedDaoIsTheEncryptingDecorator() {
        startTestKoin()
        val koin = org.koin.core.context.GlobalContext.get()

        // 装饰器必须真的接上，否则凭据会以明文落库
        assertIs<EncryptedBILIUsersDao>(koin.get<BILIUsersDao>())
        assertIs<EncryptedBILIUserCookiesDao>(koin.get<BILIUserCookiesDao>())
    }

    /**
     * 后声明的模块会覆盖同类型定义，因此把假数据库放在 databaseModule 之后，
     * 避免真实的 provideDatabase() 在 host test 中去取 Android Application。
     */
    private fun startTestKoin() {
        startKoin {
            modules(
                databaseModule,
                module {
                    single<BILIBILIASDatabase> { FakeDatabase() }
                    single<CredentialCipher> { PassthroughCredentialCipher }
                },
            )
        }
    }
}
