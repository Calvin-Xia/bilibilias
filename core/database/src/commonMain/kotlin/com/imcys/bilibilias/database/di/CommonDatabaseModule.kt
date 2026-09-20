package com.imcys.bilibilias.database.di

import com.imcys.bilibilias.database.BILIBILIASDatabase
import com.imcys.bilibilias.database.crypto.CredentialCipher
import com.imcys.bilibilias.database.crypto.platformCredentialCipher
import com.imcys.bilibilias.database.dao.BILIUserCookiesDao
import com.imcys.bilibilias.database.dao.BILIUsersDao
import com.imcys.bilibilias.database.dao.EncryptedBILIUserCookiesDao
import com.imcys.bilibilias.database.dao.EncryptedBILIUsersDao
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun provideDatabase(): BILIBILIASDatabase

val databaseModule: Module = module {
    single<BILIBILIASDatabase> {
        provideDatabase()
    }
    single<CredentialCipher> {
        platformCredentialCipher()
    }
    // 必须显式声明接口类型：消费方按 BILIUsersDao / BILIUserCookiesDao 解析，
    // 若由 Koin 从 lambda 推断，注册类型会是装饰器的具体类型，解析时抛 NoDefinitionFoundException
    factory<BILIUsersDao> {
        EncryptedBILIUsersDao(get<BILIBILIASDatabase>().biliUsersDao(), get())
    }
    factory<BILIUserCookiesDao> {
        EncryptedBILIUserCookiesDao(get<BILIBILIASDatabase>().biliUserCookiesDao(), get())
    }
    factory {
        get<BILIBILIASDatabase>().downloadTaskDao()
    }
}
