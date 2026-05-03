package com.imcys.bilibilias.download

import com.arthenica.ffmpegkit.FFmpegKitConfig

object FfmpegRuntimeConfig {

    fun apply(maxConcurrentDownloads: Int, enabledConcurrentMerge: Boolean) {
        val ffmpegConcurrency = if (enabledConcurrentMerge && maxConcurrentDownloads > 1) {
            maxConcurrentDownloads
        } else {
            1
        }

        FFmpegKitConfig.setAsyncConcurrencyLimit(ffmpegConcurrency)
        FFmpegKitConfig.setSessionHistorySize(ffmpegConcurrency)
    }
}
