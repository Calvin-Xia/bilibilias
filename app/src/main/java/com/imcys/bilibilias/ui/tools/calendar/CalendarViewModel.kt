package com.imcys.bilibilias.ui.tools.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imcys.bilibilias.data.model.bgm.BgmCalendarWeekData
import com.imcys.bilibilias.data.repository.BgmRepository
import com.imcys.bilibilias.network.NetWorkResult
import com.imcys.bilibilias.network.emptyNetWorkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class CalendarViewModel(private val bgmRepository: BgmRepository) : ViewModel() {

    val calendarData: StateFlow<NetWorkResult<List<BgmCalendarWeekData>>>
        field = MutableStateFlow<NetWorkResult<List<BgmCalendarWeekData>>>(emptyNetWorkResult())

    val selectedWeekIndex: StateFlow<Int>
        field = MutableStateFlow<Int>(currentWeekIndex)


    val currentWeekIndex: Int
        get() {
            return when (val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> 6
                else -> dayOfWeek - Calendar.MONDAY
            }
        }

    init {
        getCalendarInfo()
    }

    fun updateSelectedWeekIndex(index: Int) {
        selectedWeekIndex.value = index
    }

    fun getCalendarInfo() {
        viewModelScope.launch {
            bgmRepository.getNextCalendar().collect {
                calendarData.value = it
            }
        }
    }
}
