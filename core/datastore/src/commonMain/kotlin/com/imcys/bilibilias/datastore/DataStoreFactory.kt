package com.imcys.bilibilias.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import okio.Path
import okio.SYSTEM

private const val APP_SETTINGS_FILE_NAME = "app_setting.pb"
private const val USER_FILE_NAME = "user.pb"
private const val GOOGLE_PLAY_FILE_NAME = "google_play.pb"

internal fun createAppSettingsStore(): DataStore<AppSettings> =
    createDataStore(
        fileName = APP_SETTINGS_FILE_NAME,
        serializer = AppSettingsSerializer,
        defaultValue = AppSettings.getDefaultInstance(),
    )

internal fun createUserStore(): DataStore<User> =
    createDataStore(
        fileName = USER_FILE_NAME,
        serializer = UserSerializer,
        defaultValue = User.getDefaultInstance(),
    )

internal fun createGooglePlayStore(): DataStore<GooglePlaySettings> =
    createDataStore(
        fileName = GOOGLE_PLAY_FILE_NAME,
        serializer = GooglePlayerSerializer,
        defaultValue = GooglePlaySettings.getDefaultInstance(),
    )

private fun <T> createDataStore(
    fileName: String,
    serializer: OkioSerializer<T>,
    defaultValue: T,
): DataStore<T> {
    return DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = serializer,
            producePath = { createDataStorePath(fileName) },
        ),
        // `.pb` 损坏时重置为默认值：设置与用户标记丢失可接受，比设置页持续抛异常更可取
        corruptionHandler = ReplaceFileCorruptionHandler { defaultValue },
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    )
}

internal expect fun createDataStorePath(fileName: String): Path
