package com.imcys.bilibilias.ui.user.like

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imcys.bilibilias.data.repository.UserInfoRepository
import com.imcys.bilibilias.network.ApiStatus
import com.imcys.bilibilias.network.NetWorkResult
import com.imcys.bilibilias.network.emptyNetWorkResult
import com.imcys.bilibilias.network.model.user.BILIUserVideoLikeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


enum class LikePageType {
    LIKE,
    COIN
}


class LikeVideoViewModel(
    private val userInfoRepository: UserInfoRepository
) : ViewModel() {

    @Immutable
    data class UIState(
        val mid: Long = 0L,
        val currentMediaId: Long = 0L,
        val isDataUpdating: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UIState())
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()
    private val _likeVideoList =
        MutableStateFlow<NetWorkResult<BILIUserVideoLikeInfo?>>(emptyNetWorkResult())
    val likeVideoList = _likeVideoList.asStateFlow()

    fun initMid(mid: Long, type: LikePageType, isDataUpdating: Boolean = false) {
        _uiState.value =
            _uiState.value.copy(mid = mid, currentMediaId = 0L, isDataUpdating = isDataUpdating)
        viewModelScope.launch {
            when (type) {
                LikePageType.LIKE -> userInfoRepository.getLikeVideoList(mid)
                LikePageType.COIN -> userInfoRepository.getCoinVideoList(mid)
            }.collect { result ->
                val currentEmpty = _likeVideoList.value.data?.list.isNullOrEmpty()
                if (currentEmpty || result.status == ApiStatus.SUCCESS) {
                    _likeVideoList.value = result
                }
            }
        }
    }


}