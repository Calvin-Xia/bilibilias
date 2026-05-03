package com.imcys.bilibilias.ui.setting.download

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.imcys.bilibilias.data.repository.AppSettingsRepository
import com.imcys.bilibilias.download.FfmpegRuntimeConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownloadConfigViewModel(
    application: Application,
    private val appSettingsRepository: AppSettingsRepository
) : AndroidViewModel(application) {
    companion object {
        const val MAX_CONCURRENT_DOWNLOADS_WITH_SERIAL_MERGE = 10
    }

    val appSettings = appSettingsRepository.appSettingsFlow

    private val _maxSupportedConcurrentDownloads =
        MutableStateFlow(calculateMaxSupportedConcurrentDownloads(application))
    val maxSupportedConcurrentDownloads = _maxSupportedConcurrentDownloads.asStateFlow()

    fun updateMaxConcurrentDownloads(value: Int) {
        viewModelScope.launch {
            val settings = appSettingsRepository.appSettingsFlow.first()
            val limit = if (settings.enabledConcurrentMerge) {
                _maxSupportedConcurrentDownloads.value
            } else {
                MAX_CONCURRENT_DOWNLOADS_WITH_SERIAL_MERGE
            }
            val downloadCount = value.coerceIn(1, limit)
            appSettingsRepository.updateMaxConcurrentDownloads(downloadCount)
            syncFfmpegConfig()
        }
    }

    fun updateEnabledConcurrentMerge(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                val settings = appSettingsRepository.appSettingsFlow.first()
                val clampedDownloadCount = settings.maxConcurrentDownloads
                    .coerceIn(1, _maxSupportedConcurrentDownloads.value)
                if (clampedDownloadCount != settings.maxConcurrentDownloads) {
                    appSettingsRepository.updateMaxConcurrentDownloads(clampedDownloadCount)
                }
            }
            appSettingsRepository.updateEnabledConcurrentMerge(enabled)
            syncFfmpegConfig()
        }
    }

    private suspend fun syncFfmpegConfig() {
        val settings = appSettingsRepository.appSettingsFlow.first()
        FfmpegRuntimeConfig.apply(
            maxConcurrentDownloads = settings.maxConcurrentDownloads,
            enabledConcurrentMerge = settings.enabledConcurrentMerge
        )
    }

    private fun calculateMaxSupportedConcurrentDownloads(context: Context): Int {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val isLowRamDevice = activityManager?.isLowRamDevice ?: false
        val memoryClass = activityManager?.memoryClass ?: 128
        val processors = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

        return when {
            isLowRamDevice || memoryClass <= 128 || processors <= 4 -> 2
            memoryClass <= 192 || processors <= 6 -> 3
            memoryClass <= 256 || processors <= 8 -> 4
            else -> 5
        }
    }
}
