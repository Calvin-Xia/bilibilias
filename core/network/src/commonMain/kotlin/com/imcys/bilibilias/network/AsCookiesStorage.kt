package com.imcys.bilibilias.network


import com.imcys.bilibilias.database.dao.BILIUserCookiesDao
import com.imcys.bilibilias.database.entity.ASSharedCookieEncoding
import com.imcys.bilibilias.database.entity.BILIUserCookiesEntity
import com.imcys.bilibilias.datastore.*
import com.imcys.bilibilias.datastore.source.UsersDataSource
import com.imcys.bilibilias.network.utils.platformEpochMillis
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi


@OptIn(ExperimentalSerializationApi::class)
class AsCookiesStorage(
    private val usersDataSource: UsersDataSource,
    private val biliUserCookiesDao: BILIUserCookiesDao
) : CookiesStorage {
    private val cookies = mutableListOf<Cookie>()
    private val daoCoroutineScope = CoroutineScope(Dispatchers.Default + Job())
    private suspend fun isLogin() = usersDataSource.isLogin()
    private var isInit = false
    // 添加协程的 Mutex 锁
    private val cookieMutex = Mutex()
    // Cookie 回写数据库的防抖任务与串行锁
    private var cookieWriteBackJob: Job? = null
    private val writeBackMutex = Mutex()

    init {
        // 初始化当前Cookie
        runCatching {
            daoCoroutineScope.launch {
                syncDataBaseCookies()
                isInit = true
            }
        }
    }

    /**
     * 同步数据库的Cookie
     */
    suspend fun syncDataBaseCookies() {
        if (!isLogin()) {
            // 未来登录走其他的操作
        } else {
            // 登录
            val dataBaseCookies =
                biliUserCookiesDao.getBILIUserCookiesByUid(usersDataSource.getUserId())
            cookieMutex.withLock {
                dataBaseCookies.forEach {
                    cookies.removeAll { cookie -> cookie.name == it.name }
                    val cookie = Cookie(
                        name = it.name,
                        value = it.value,
                        encoding = CookieEncoding.valueOf(it.encoding.name),
                        domain = it.domain,
                        path = it.path,
                        secure = it.secure,
                        httpOnly = it.httpOnly
                    )
                    cookies.add(cookie)
                }
            }
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> {
        if (!isInit) {
            syncDataBaseCookies()
        }
        return cookieMutex.withLock {
            cookies.run {
                if (usersDataSource.users.first().notUseBuvid3) {
                    filter { it.name != "buvid3" }
                } else {
                    this.toList()
                }
            }
        }
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        // 无 Expires 的会话 cookie（expires == null）保留在内存；仅明确已过期的才丢弃
        val expiresAt = cookie.expires?.timestamp
        if (expiresAt != null && expiresAt < platformEpochMillis()) return

        val mCookie = cookie.copy(domain = requestUrl.host)
        cookieMutex.withLock {
            cookies.removeAll { it -> it.name == mCookie.name }
            cookies.add(mCookie)
        }
        if (requestUrl.host.isBilibiliHost()) {
            scheduleCookieWriteBack()
        }
    }


    suspend fun getCookieValue(key: String): String? {
        return cookieMutex.withLock {
            cookies.find { it.name == key }?.value
        }
    }

    suspend fun getAllCookies(): MutableList<Cookie> {
        return cookieMutex.withLock {
            cookies.toMutableList()
        }
    }

    suspend fun updateAllCookies(cookie: MutableList<Cookie>) {
        cookieMutex.withLock {
            cookies.clear()
            cookies.addAll(cookie)
        }
    }

    suspend fun clearCookies() {
        cookieMutex.withLock {
            cookies.clear()
        }
    }

    private fun String.isBilibiliHost(): Boolean =
        this == "bilibili.com" || endsWith(".bilibili.com")

    /**
     * B 站会在响应里轮换登录 Cookie（如 SESSDATA）。新值只留在内存的话，进程重启后
     * 会退回数据库里登录时刻的旧值，服务端随即判定未登录。收到 bilibili 域的
     * Set-Cookie 后防抖回写数据库（经 EncryptedBILIUserCookiesDao 落盘）。
     */
    private fun scheduleCookieWriteBack() {
        cookieWriteBackJob?.cancel()
        cookieWriteBackJob = daoCoroutineScope.launch {
            delay(COOKIE_WRITE_BACK_DEBOUNCE_MS)
            runCatching { writeCookiesToDatabase() }
        }
    }

    private suspend fun writeCookiesToDatabase() {
        writeBackMutex.withLock {
            if (!isLogin()) return
            val userId = usersDataSource.getUserId()
            val snapshot = cookieMutex.withLock {
                cookies.filter { it.domain?.isBilibiliHost() == true }.map { it.copy() }
            }
            if (snapshot.isEmpty()) return
            biliUserCookiesDao.deleteBILICookiesByUid(userId)
            snapshot.forEach {
                biliUserCookiesDao.insertBILIUserCookie(
                    BILIUserCookiesEntity(
                        userId = userId,
                        name = it.name,
                        value = it.value,
                        path = it.path,
                        secure = it.secure,
                        domain = it.domain,
                        encoding = ASSharedCookieEncoding.valueOf(it.encoding.name),
                        httpOnly = it.httpOnly
                    )
                )
            }
        }
    }

    override fun close() {
    }

    companion object {
        private const val COOKIE_WRITE_BACK_DEBOUNCE_MS = 2_000L
    }

}
