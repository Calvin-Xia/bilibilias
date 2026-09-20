package com.imcys.bilibilias.database

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE download_segment ADD COLUMN platform_unique_id TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE download_segment ADD COLUMN naming_convention_info TEXT")
    }
}
val MIGRATION_3_4 = object : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE download_segment ADD COLUMN media_container TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE download_segment ADD COLUMN quality_description TEXT")
        connection.execSQL(
            """
                UPDATE download_segment
              SET media_container = CASE download_mode
                                      WHEN 'AUDIO_VIDEO' THEN 'mp4'
                                      WHEN 'AUDIO_ONLY'  THEN 'mp3'
                                      WHEN 'VIDEO_ONLY'  THEN 'mp4'
                                      ELSE 'mp4'
                                    END
            """.trimIndent()
        )
    }
}

/**
 * 清空存量明文凭据。
 *
 * 4 版本起 token 与 Cookie 改为加密存储，历史数据是明文，无法在不持有旧格式密钥的前提下
 * 原地重加密，因此直接清除：升级用户需要重新登录。清除后启动时的登录态对账会把
 * DataStore 中残留的登录标记重置，避免出现"显示已登录但请求无凭据"。
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("UPDATE bili_users SET access_token = NULL, refresh_token = NULL")
        connection.execSQL("DELETE FROM bili_user_cookies")
    }
}