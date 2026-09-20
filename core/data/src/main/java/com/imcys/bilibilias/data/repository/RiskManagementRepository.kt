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
    private var cachedSpiB3: String? = null
    private var cachedSpiB4: String? = null

    /**
     * 更新签名
     *
     * BUVID 是设备级 Cookie，进程内获取一次即可，因此缓存返回值；Mutex 用于避免首页并发加载时重复请求。
     */
    suspend fun updateWebSpiCookie() = spiMutex.withLock {
        if (cachedSpiB3 != null && cachedSpiB4 != null) return

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

                    cachedSpiB3 = spi.b3
                    cachedSpiB4 = spi.b4
                }

                else -> {}
            }
        }
    }

}
