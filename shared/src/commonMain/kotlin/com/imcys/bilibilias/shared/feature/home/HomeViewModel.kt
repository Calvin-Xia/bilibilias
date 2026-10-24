package com.imcys.bilibilias.shared.feature.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imcys.bilibilias.data.model.BILILoginUserModel
import com.imcys.bilibilias.data.repository.AppSettingsRepository
import com.imcys.bilibilias.data.repository.QRCodeLoginRepository
import com.imcys.bilibilias.data.repository.RiskManagementRepository
import com.imcys.bilibilias.database.entity.BILIUsersEntity
import com.imcys.bilibilias.datastore.*
import com.imcys.bilibilias.datastore.AppSettingsSerializer
import com.imcys.bilibilias.datastore.source.UsersDataSource
import com.imcys.bilibilias.shared.download.runtime.SharedDownloadManager
import com.imcys.bilibilias.network.ApiStatus
import com.imcys.bilibilias.network.NetWorkResult
import com.imcys.bilibilias.network.config.API.App.SSE_HOST
import com.imcys.bilibilias.network.config.API.App.SSE_PATH
import com.imcys.bilibilias.network.config.API.App.SSE_PORT
import com.imcys.bilibilias.network.emptyNetWorkResult
import com.imcys.bilibilias.network.model.app.AppUpdateConfigInfo
import com.imcys.bilibilias.network.model.app.BannerConfigInfo
import com.imcys.bilibilias.network.model.app.BulletinConfigInfo
import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.ClientSSESessionWithDeserialization
import io.ktor.client.plugins.sse.deserialize
import io.ktor.client.plugins.sse.sse
import io.ktor.sse.TypedServerSentEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class HomeViewModel(
    private val httpClient: HttpClient,
    private val qrCodeLoginRepository: QRCodeLoginRepository,
    private val usersDataSource: UsersDataSource,
    private val riskManagementRepository: RiskManagementRepository,
    private val downloadManager: SharedDownloadManager,
    private val appSettingsRepository: AppSettingsRepository

) : ViewModel() {

    @Immutable
    data class UIState(
        val fromLoginEventConsumed: Boolean = false,
        val shownAppUpdate: Boolean = false
    )

    val appSettings = appSettingsRepository.appSettingsFlow

    val appSettingsState = appSettingsRepository.appSettingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppSettings.getDefaultInstance()
    )

    private var _uiState = MutableStateFlow(UIState())
    val uiState = _uiState.asStateFlow()

    private val _loginUserInfoState =
        MutableStateFlow<NetWorkResult<BILILoginUserModel?>>(emptyNetWorkResult())
    val loginUserInfoState = _loginUserInfoState.asStateFlow()

    private val _userLoginPlatformList =
        MutableStateFlow<List<BILIUsersEntity>>(emptyList())
    val userLoginPlatformList = _userLoginPlatformList.asStateFlow()

    val downloadListState = downloadManager.getAllDownloadTasks()


    private val _homeLayoutTypesetList = MutableStateFlow(emptyList<AppSettings.HomeLayoutItem>())

    var homeLayoutTypesetList = _homeLayoutTypesetList.asStateFlow()

    private val _bannerList = MutableStateFlow<List<BannerConfigInfo>>(emptyList())
    val bannerList = _bannerList.asStateFlow()


    private val _bulletinInfo = MutableStateFlow<BulletinConfigInfo?>(null)
    val bulletinInfo = _bulletinInfo.asStateFlow()

    private val _appUpdateInfo = MutableStateFlow<AppUpdateConfigInfo?>(null)
    val appUpdateInfo = _appUpdateInfo.asStateFlow()

    private val _useToolHistoryList =
        MutableStateFlow(toToolInfoList(AppSettingsSerializer.appSettingsDefault.useToolHistoryList))
    val useToolHistoryList = _useToolHistoryList.asStateFlow()

    // 使用工具历史列表
    init {
        initLayoutTypeset()
        initDownloadList()
        // requestSSEEvent()
        monitorUserState()
        updateWebSpi()
    }

    /**
     * 监听用户登录状态变化
     */
    private fun monitorUserState() {
        viewModelScope.launch(Dispatchers.IO) {
            usersDataSource.users.collect {
                if (it.currentUserId == 0L) {
                    _loginUserInfoState.value = emptyNetWorkResult()
                    _userLoginPlatformList.value = emptyList()
                } else {
                    showBILIUserInfo()
                }
            }
        }
    }

    private fun requestSSEEvent() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                httpClient.sse(
                    host = SSE_HOST,
                    port = SSE_PORT,
                    path = SSE_PATH,
                    deserialize = { typeInfo, jsonString ->
                        val serializer = Json.serializersModule.serializer(typeInfo.kotlinType!!)
                        Json.decodeFromString(serializer, jsonString)
                    }) {

                    while (true) {
                        incoming.collect { event ->
                            handleSSEEvent(event)
                        }
                    }
                }
            }
        }
    }

    /**
     * 处理服务器相关事件
     */
    private fun ClientSSESessionWithDeserialization.handleSSEEvent(event: TypedServerSentEvent<String>) {
        when (event.event) {
            "banner" -> {
                val bannerConfigInfo = deserialize<List<BannerConfigInfo>>(event.data)
                _bannerList.value = bannerConfigInfo ?: emptyList()
            }

            "bulletin" -> {
                val bulletinConfigInfo = deserialize<BulletinConfigInfo>(event.data)
                _bulletinInfo.value = bulletinConfigInfo
            }

            "appUpdate" -> {
                val appUpdateInfo = deserialize<AppUpdateConfigInfo>(event.data)
                _appUpdateInfo.value = appUpdateInfo
            }

            else -> {
                println("Received other event: ${event.event} with data: ${event.data}")
            }
        }
    }

    fun updateLastBulletinContent() {
        viewModelScope.launch {
            appSettingsRepository.updateLastBulletinContent(_bulletinInfo.value?.content ?: "")
        }
    }

    private fun toToolInfoList(toolNameList: List<String>): List<ToolInfo> {
        return toolNameList.mapNotNull { name ->
            runCatching {
                ToolInfo.valueOf(name)
            }.getOrNull()
        }.filter { info -> info.isScreen }
    }

    private fun initLayoutTypeset() {
        viewModelScope.launch {
            appSettingsRepository.asyncHomeLayoutTypesetList()
            appSettings.collect {
                _homeLayoutTypesetList.value = it.homeLayoutTypesetList
                _useToolHistoryList.value = toToolInfoList(it.useToolHistoryList)
            }

        }
    }

    private fun initDownloadList() {
        viewModelScope.launch(Dispatchers.IO) {
            downloadManager.initDownloadList()
        }
    }

    fun showBILIUserInfo() {
        viewModelScope.launch {
            initCurrentUserInfo()
        }
    }


    fun onNavigatedFromLogin() {
        viewModelScope.launch {
            if (loginUserInfoState.value.status != ApiStatus.SUCCESS) {
                showBILIUserInfo()
                _uiState.emit(uiState.value.copy(fromLoginEventConsumed = true))
            }
        }
    }


    fun updateWebSpi() {
        // 更新校验
        viewModelScope.launch(Dispatchers.IO) {
            riskManagementRepository.updateWebSpiCookie()
        }
    }

    /**
     * 暂停下载任务
     * [segmentId] 下载任务的ID
     */
    fun pauseDownloadTask(segmentId: Long) {
        viewModelScope.launch { downloadManager.pauseTask(segmentId) }
    }

    fun resumeDownloadTask(segmentId: Long) {
        viewModelScope.launch { downloadManager.resumeTask(segmentId) }
    }

    fun cancelDownloadTask(segmentId: Long) {
        viewModelScope.launch { downloadManager.cancelTask(segmentId) }
    }

    suspend fun initCurrentUserInfo() {
        if (!usersDataSource.isLogin()) return
        val biliUserList =
            qrCodeLoginRepository.getBILIUserListByUid(usersDataSource.getUserId())
        if (biliUserList.isEmpty()) return

        val currentUser = biliUserList.firstOrNull { it.id == usersDataSource.getUserId() }
            ?: biliUserList.firstOrNull()
            ?: return

        qrCodeLoginRepository.getLoginUserInfo(
            currentUser.loginPlatform,
            currentUser.accessToken
        )
            .collect {
                _loginUserInfoState.emit(it)

                // 更新本地记录
                when (it) {
                    is NetWorkResult.Success<*> -> {
                        biliUserList.forEach { user ->
                            qrCodeLoginRepository.updateBILIUser(
                                user.copy(
                                    mid = it.data?.mid ?: 0L,
                                    name = it.data?.name ?: "",
                                    face = it.data?.face ?: "",
                                    level = it.data?.level ?: 0,
                                    vipState = it.data?.vipState ?: 0,
                                )
                            )
                            _userLoginPlatformList.emit(biliUserList)
                        }
                    }

                    else -> {}
                }
            }


    }

    // 更新已经显示过
    fun onAppUpdateDialogShown() {
        viewModelScope.launch {
            _uiState.emit(uiState.value.copy(shownAppUpdate = true))
        }
    }

    // 更新使用工具记录
    fun updateUseToolRecord(tool: ToolInfo) {
        viewModelScope.launch {
            appSettingsRepository.updateUseToolRecord(tool.name)
        }
    }


}
