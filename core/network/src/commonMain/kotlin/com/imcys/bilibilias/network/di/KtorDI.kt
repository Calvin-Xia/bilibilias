package com.imcys.bilibilias.network.di

import com.imcys.bilibilias.common.data.CommonBuildConfig
import com.imcys.bilibilias.datastore.*
import com.imcys.bilibilias.network.AsCookiesStorage
import com.imcys.bilibilias.network.config.BILIBILI_URL
import com.imcys.bilibilias.network.logging.formatKtorLogMessage
import com.imcys.bilibilias.network.logging.KtorLogBridge
import com.imcys.bilibilias.network.plugin.AutoBILIInfoPlugin
import com.imcys.bilibilias.network.plugin.FirebasePerfPlugin
import com.imcys.bilibilias.network.plugin.RiskControlPlugin
import com.imcys.bilibilias.network.service.BILIBILITVAPIService
import com.imcys.bilibilias.network.service.BILIBILIWebAPIService
import com.imcys.bilibilias.network.service.BgmAPIService
import com.imcys.bilibilias.network.service.GithubAPIService
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.protobuf.protobuf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalSerializationApi::class)
val netWorkModule = module {
    single<Json> {
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            allowSpecialFloatingPointValues = true
            explicitNulls = false
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    single {
        AsCookiesStorage(get(), get())
    }

    single {
        platformHttpClient {
            BrowserUserAgent()
            install(HttpTimeout) {
                requestTimeoutMillis = 10000
            }
            install(SSE) {
                maxReconnectionAttempts = 4
                reconnectionTime = 2.seconds
            }
            install(AutoBILIInfoPlugin){
                appSetting = get(named("app_settings_datastore"))
            }
            install(RiskControlPlugin)
            install(ContentNegotiation) {
                json(get())
                protobuf(contentType = ContentType.Application.OctetStream)
            }
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 3)
                exponentialDelay()
            }
            install(HttpCookies) {
                storage = get<AsCookiesStorage>()
            }
            if (CommonBuildConfig.enabledNetworkLogging) {
                install(Logging) {
                    logger = object : Logger {
                        private val json: Json = get()

                        override fun log(message: String) {
                            KtorLogBridge.log(formatKtorLogMessage(message, json))
                        }
                    }
                    level = LogLevel.ALL
                }
            }
        }
    }

    // 专用于下载的纯净HttpClient，不装任何业务插件
    single(qualifier = named("DownloadHttpClient")) {
        platformHttpClient {
            BrowserUserAgent()
            install(HttpTimeout) {
                requestTimeoutMillis = 60000 // 下载可适当延长
            }
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 3)
                exponentialDelay()
            }
            install(HttpCookies) {
                storage = get<AsCookiesStorage>()
            }
            install(AutoBILIInfoPlugin){
                appSetting = get(named("app_settings_datastore"))
            }
            if (CommonBuildConfig.enabledNetworkLogging) {
                install(Logging) {
                    logger = object : Logger {
                        private val json: Json = get()

                        override fun log(message: String) {
                            KtorLogBridge.log(formatKtorLogMessage(message, json))
                        }
                    }
                    level = LogLevel.HEADERS
                }
            }
        }
    }

    //统用的网络请求不装任何业务插件
    single(qualifier = named("CommonApiHttpClient")) {
        platformHttpClient {
            BrowserUserAgent()
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000
                connectTimeoutMillis = 120_000
                socketTimeoutMillis = 120_000
            }
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 3)
                exponentialDelay()
            }
            install(ContentNegotiation) {
                json(get())
            }
            if (CommonBuildConfig.enabledNetworkLogging) {
                install(Logging) {
                    logger = object : Logger {
                        private val json: Json = get()

                        override fun log(message: String) {
                            KtorLogBridge.log(formatKtorLogMessage(message, json))
                        }
                    }
                    level = LogLevel.HEADERS
                }
            }
        }
    }


    // WebAPI
    single {
        BILIBILIWebAPIService(get())
    }
    // TvAPI
    single {
        BILIBILITVAPIService(get())
    }
    // BgmAPI
    single {
        BgmAPIService(get(qualifier = named("CommonApiHttpClient")))
    }
    // GithubAPI
    single {
        GithubAPIService(get(qualifier = named("CommonApiHttpClient")))
    }
}
