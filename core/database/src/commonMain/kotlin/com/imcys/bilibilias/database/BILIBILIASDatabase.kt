package com.imcys.bilibilias.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.SQLiteDriver
import com.imcys.bilibilias.database.dao.BILIUserCookiesDao
import com.imcys.bilibilias.database.dao.BILIUsersDao
import com.imcys.bilibilias.database.dao.DownloadTaskDao
import com.imcys.bilibilias.database.entity.BILIUserCookiesEntity
import com.imcys.bilibilias.database.entity.BILIUsersEntity
import com.imcys.bilibilias.database.entity.download.DownloadSegment
import com.imcys.bilibilias.database.entity.download.DownloadTask
import com.imcys.bilibilias.database.entity.download.DownloadTaskNode
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

@ConstructedBy(BILIBILIASDatabaseConstructor::class)
@Database(
    entities = [
        BILIUsersEntity::class,
        BILIUserCookiesEntity::class,
        DownloadTask::class,
        DownloadTaskNode::class,
        DownloadSegment::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class BILIBILIASDatabase : RoomDatabase() {
    abstract fun biliUsersDao(): BILIUsersDao

    abstract fun biliUserCookiesDao(): BILIUserCookiesDao

    abstract fun downloadTaskDao(): DownloadTaskDao
}

@Suppress("KotlinNoActualForExpect")
expect object BILIBILIASDatabaseConstructor :
    RoomDatabaseConstructor<BILIBILIASDatabase> {
    override fun initialize(): BILIBILIASDatabase
}

internal fun buildDatabase(
    builder: RoomDatabase.Builder<BILIBILIASDatabase>
): BILIBILIASDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        .build()
}