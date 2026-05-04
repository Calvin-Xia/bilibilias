package com.imcys.bilibilias.network.adapter

import com.imcys.bilibilias.network.FlowNetWorkResult
import com.imcys.bilibilias.network.NetWorkResult
import com.imcys.bilibilias.network.httpRequest
import com.imcys.bilibilias.network.model.BiliApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.flow.FlowCollector

class GithubNetWorkAdapter<Data> : NetWorkAdapter<Data> {
    context(collector: FlowCollector<NetWorkResult<Data>>)
    override suspend fun handleSuccess(
        data: Data?,
        apiResponse: BiliApiResponse<Data>,
        response: HttpResponse
    ) {
        when (apiResponse.code) {
            200 -> collector.emit(NetWorkResult.Success(data, apiResponse))
        }
    }

    context(response: HttpResponse)
    override suspend fun <Data> conversion(data: Data?): BiliApiResponse<Data> {
        return BiliApiResponse<Data>(
            code = response.status.value,
            message = response.status.description,
            data = data,
            result = null
        )
    }


}
inline fun <reified Data> HttpClient.githubHttpRequest(
    crossinline request: suspend HttpClient.() -> HttpResponse,
): FlowNetWorkResult<Data> = httpRequest(adapter = GithubNetWorkAdapter(), request)
