package com.imcys.bilibilias.firebase

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.imcys.bilibilias.common.data.commonAnalyticsSafe
import com.imcys.bilibilias.network.plugin.NetworkPerformanceTracer

class FirebaseNetworkPerformanceTracer(
    private var trace: Trace? = null
) : NetworkPerformanceTracer {

    override fun onRequest(
        traceNamePrefix: String,
        method: String,
        path: String,
        requestPayloadSize: Long?,
    ) {
        val traceName = buildTraceName(traceNamePrefix, method, path)
        commonAnalyticsSafe {
            trace = FirebasePerformance.getInstance()
                .newTrace(traceName)
                .apply {
                    putAttribute("method", method)
                    putAttribute("path", path)
                    requestPayloadSize?.let { putMetric("request_payload_size", it) }
                    start()
                }
        }
    }

    override fun recordSuccess(
        responseCode: Int,
        responsePayloadSize: Long?,
        responseContentType: String?,
    ) {
        val currentTrace = trace ?: return
        commonAnalyticsSafe {
            currentTrace.putAttribute("status_code", responseCode.toString())
            currentTrace.putAttribute("success", (responseCode in 200..299).toString())
            responsePayloadSize?.takeIf { it >= 0L }?.let {
                currentTrace.putMetric("response_payload_size", it)
            }
            responseContentType?.takeIf { it.isNotBlank() }?.let {
                currentTrace.putAttribute("content_type", it)
            }
            currentTrace.stop()
        }
    }

    override fun recordFailure(error: Throwable?) {
        val currentTrace = trace ?: return
        commonAnalyticsSafe {
            error?.javaClass?.simpleName?.takeIf { it.isNotBlank() }?.let {
                currentTrace.putAttribute("error", it)
            }
            currentTrace.putAttribute("success", "false")
            currentTrace.stop()
        }
    }
}

private fun buildTraceName(
    traceNamePrefix: String,
    method: String,
    path: String,
): String {
    val normalizedMethod = method.lowercase()
    val normalizedPath = path
        .replace('/', '_')
        .replace(TRACE_NAME_INVALID_CHAR_REGEX, "_")
        .trim('_')
        .ifEmpty { "root" }

    return "${traceNamePrefix}_${normalizedMethod}_$normalizedPath"
        .take(100)
}

private val TRACE_NAME_INVALID_CHAR_REGEX = Regex("[^A-Za-z0-9_]")
