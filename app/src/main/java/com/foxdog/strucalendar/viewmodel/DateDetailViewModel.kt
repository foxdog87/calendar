package com.foxdog.strucalendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxdog.strucalendar.data.dao.HolidayDao
import com.foxdog.strucalendar.data.dao.TaskDao
import com.foxdog.strucalendar.data.relation.TaskWithTags
import com.foxdog.strucalendar.data.settings.SettingsRepository
import com.foxdog.strucalendar.data.telemetry.AnalyticsLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DateDetailViewModel(
    private val taskDao: TaskDao,
    private val holidayDao: HolidayDao,
    private val settingsRepository: SettingsRepository,
    private val deviceCountryCode: String // 端末ロケールから判定した国コード。設定で上書きされていない場合のフォールバック
) : ViewModel() {

    private val _selectedDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    val filteredTasks: StateFlow<List<TaskWithTags>> = taskDao.getAllTasksWithTags()
        .combine(_selectedDate) { totalList, date ->
            totalList.filter { item ->
                val startDate = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(item.task.startTime),
                    ZoneId.systemDefault()
                ).toLocalDate()
                val endDate = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(item.task.endTime),
                    ZoneId.systemDefault()
                ).toLocalDate()
                val normalizedEndDate = if (endDate.isBefore(startDate)) startDate else endDate
                !date.isBefore(startDate) && !date.isAfter(normalizedEndDate)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 選択中の日付の祝日名（無ければnull）
    private val _holidayName = MutableStateFlow<String?>(null)
    val holidayName: StateFlow<String?> = _holidayName

    fun setDate(date: LocalDate) {
        _selectedDate.value = date
        AnalyticsLogger.logDateDetailOpened()

        // 日付が変わるたびにその日の祝日名をDBキャッシュから引く（API通信はしない）
        // 設定で祝日の国が明示的に選ばれていればそちらを優先し、なければ端末ロケール判定に従う
        viewModelScope.launch {
            val overrideCode = settingsRepository.settingsFlow.map { it.holidayCountryCode }.first()
            val effectiveCode = overrideCode ?: deviceCountryCode

            val entities = holidayDao.getHolidaysForDate(date.toString())
            _holidayName.value = entities.firstOrNull { it.countryCode == effectiveCode }?.localName
        }
    }

    fun toggleTaskCompletion(taskWithTags: TaskWithTags) {
        viewModelScope.launch {
            val currentTask = taskWithTags.task
            val newStatus = if (currentTask.completeState == "COMPLETED") "UNCOMPLETED" else "COMPLETED"
            val updatedTask = currentTask.copy(completeState = newStatus)
            taskDao.updateTask(updatedTask)
            AnalyticsLogger.logTaskCompletionToggled()
        }
    }

    fun togglePin(taskWithTags: TaskWithTags) {
        viewModelScope.launch {
            val currentTask = taskWithTags.task
            taskDao.setPinned(currentTask.taskId, !currentTask.isPinned)
        }
    }
}