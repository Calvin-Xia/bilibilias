package com.imcys.bilibilias.database.di

import com.imcys.bilibilias.database.BILIBILIASDatabase
import com.imcys.bilibilias.database.crypto.CredentialCipher
import com.imcys.bilibilias.database.crypto.platformCredentialCipher
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
    factory {
        EncryptedBILIUsersDao(get<BILIBILIASDatabase>().biliUsersDao(), get())
    }
    factory {
        EncryptedBILIUserCookiesDao(get<BILIBILIASDatabase>().biliUserCookiesDao(), get())
    }
    factory {
        get<BILIBILIASDatabase>().downloadTaskDao()
    }
}
