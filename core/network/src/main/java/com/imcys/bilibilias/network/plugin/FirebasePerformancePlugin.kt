package com.imcys.bilibilias.network.plugin

import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders

class FirebasePerfPluginConfig {
    var traceNamePrefix: String = "ktor"
    var tracer: NetworkPerformanceTracer? = null
}

val FirebasePerfPlugin = createClientPlugin(
    name = "FirebasePerfPlugin",
    createConfiguration = ::FirebasePerfPluginConfig
) {
    val traceNamePrefix = pluginConfig.traceNamePrefix
    val tracer = pluginConfig.tracer

    on(Send) { request ->
        if (request.isSSE() || tracer == null) {
            return@on proceed(request)
        }

        tracer.onRequest(
            traceNamePrefix = traceNamePrefix,
            method = request.method.value,
            path = request.url.build().encodedPath,
            requestPayloadSize = null,
        )

        try {
            val call = proceed(request)
            tracer.recordSuccess(
                responseCode = call.response.status.value,
                responsePayloadSize = call.response.headers[HttpHeaders.ContentLength]?.toLongOrNull(),
                responseContentType = call.response.headers[HttpHeaders.ContentType]?.substringBefore(
                    ";"
                ),
            )
            call
        } catch (cause: Throwable) {
            tracer.recordFailure(cause)
            throw cause
        }
    }
}

interface NetworkPerformanceTracer {
    fun onRequest(
        traceNamePrefix: String,
        method: String,
        path: String,
        requestPayloadSize: Long? = null,
    )

    fun recordSuccess(
        responseCode: Int,
        responsePayloadSize: Long? = null,
        responseContentType: String? = null,
    )

    fun recordFailure(error: Throwable? = null)
}
