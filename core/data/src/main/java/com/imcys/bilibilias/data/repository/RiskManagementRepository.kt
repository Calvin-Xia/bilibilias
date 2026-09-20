package com.imcys.bilibilias.data.repository

import com.imcys.bilibilias.database.currentTimeMillis
import com.imcys.bilibilias.network.AsCookiesStorage
import com.imcys.bilibilias.network.NetWorkResult
import com.imcys.bilibilias.network.config.BILIBILI_URL
import com.imcys.bilibilias.network.config.BUVID3
import com.imcys.bilibilias.network.config.BUVID4
import com.imcys.bilibilias.network.service.BILIBILIWebAPIService
import io.ktor.client.plugins.cookies.addCookie
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RiskManagementRepository(
    private val webApiService: BILIBILIWebAPIService,
    private val asCookiesStorage: AsCookiesStorage
) {
    private val spiMutex = Mutex()

    /**
     * 更新签名
     *
     * BUVID 是设备级 Cookie，已存在时无需重复请求 SPI；Mutex 用于避免首页并发加载时重复请求。
     *
     * 判定以 Cookie 存储的实际内容为准，不在实例内缓存返回值：登出会清空 Cookie
     * （SettingViewModel.logout），若用实例字段做缓存，登出后 BUVID 无法恢复，
     * 匿名请求会因缺少 buvid 而更易触发风控。
     */
    suspend fun updateWebSpiCookie() = spiMutex.withLock {
        if (!isSpiCookieMissing()) return

        webApiService.getWebSpiInfo().collect { result ->
            when (result) {
                is NetWorkResult.Success -> {
                    val spi = result.data ?: return@collect
                    asCookiesStorage.addCookie(
                        BILIBILI_URL, Cookie(
                            name = BUVID3,
                            value = spi.b3,
                            expires = GMTDate(currentTimeMillis() + 86400 * 1000L),
                            path = "/"
                        )
                    )
                    asCookiesStorage.addCookie(
                        BILIBILI_URL, Cookie(
                            name = BUVID4,
                            value = spi.b4,
                            expires = GMTDate(currentTimeMillis() + 86400 * 1000L),
                            path = "/"
                        )
                    )
                }

                else -> {}
            }
        }
    }

    private suspend fun isSpiCookieMissing(): Boolean =
        asCookiesStorage.getCookieValue(BUVID3) == null ||
            asCookiesStorage.getCookieValue(BUVID4) == null

}
