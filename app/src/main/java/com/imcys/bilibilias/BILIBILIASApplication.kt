package com.imcys.bilibilias

import android.app.Application
import androidx.appfunctions.service.AppFunctionConfiguration
import com.baidu.mobstat.StatService
import com.imcys.bilibilias.agent.functions.BILIAnalysisAppFunctions
import com.imcys.bilibilias.common.data.CommonBuildConfig
import com.imcys.bilibilias.common.memory.FairMemoryReceiver
import com.imcys.bilibilias.common.shizuku.ShizukuStateManager
import com.imcys.bilibilias.common.utils.StorageUtil.getKoin
import com.imcys.bilibilias.common.utils.baiduAnalyticsSafe
import com.imcys.bilibilias.data.repository.AppSettingsRepository
import com.imcys.bilibilias.data.di.repositoryModule
import com.imcys.bilibilias.database.di.databaseModule
import com.imcys.bilibilias.datastore.di.dataStoreModule
import com.imcys.bilibilias.di.appModule
import com.imcys.bilibilias.download.FfmpegRuntimeConfig
import com.imcys.bilibilias.download.NewDownloadManager
import com.imcys.bilibilias.network.di.netWorkModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class BILIBILIASApplication : Application(), AppFunctionConfiguration.Provider {

    private val shizukuStateManager: ShizukuStateManager by inject<ShizukuStateManager>()
    private var fairMemoryReceiver: FairMemoryReceiver? = null

    override fun onCreate() {
        super.onCreate()
        // 全局异常捕获
        // AppCrashHandler.instance.init(this)
        initBuildConfig()
        // 初始化百度统计
        baiduAnalyticsSafe {
            StatService.init(this, BuildConfig.BAIDU_STAT_ID, getString(R.string.app_channel))
        }
        // Koin依赖注入
        startKoin {
            if (BuildConfig.DEBUG) {
                androidLogger()
            }
            androidContext(this@BILIBILIASApplication)
            modules(
                dataStoreModule,
                netWorkModule,
                repositoryModule,
                databaseModule,
                appModule,
            )
        }
        fairMemoryReceiver = FairMemoryReceiver(this) {
            getKoin().get<NewDownloadManager>()
        }.also { receiver ->
            receiver.initialize()
        }
        initFFmpeg()
        // shizuku监听
        // shizukuStateManager.start()
    }

    private fun initFFmpeg() {
        val settingsRepository = getKoin().get<AppSettingsRepository>()
        val settings = runBlocking { settingsRepository.appSettingsFlow.first() }
        FfmpegRuntimeConfig.apply(
            maxConcurrentDownloads = settings.maxConcurrentDownloads,
            enabledConcurrentMerge = settings.enabledConcurrentMerge
        )
    }

    /**
     * 主动内存释放
     */
    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when {
            level == TRIM_MEMORY_UI_HIDDEN -> {
                fairMemoryReceiver?.trimMemory(clearTemporaryFiles = false)
            }

            level >= TRIM_MEMORY_RUNNING_LOW -> {
                fairMemoryReceiver?.trimMemory(clearTemporaryFiles = true)
            }
        }
    }

    override val appFunctionConfiguration: AppFunctionConfiguration
        get() = AppFunctionConfiguration
            .Builder()
            .addEnclosingClassFactory(BILIAnalysisAppFunctions::class.java) {
                getKoin().get<BILIAnalysisAppFunctions>()
            }
            .build()

    private fun initBuildConfig() {
        CommonBuildConfig.enabledAnalytics = BuildConfig.ENABLED_ANALYTICS
    }
}
