package com.example.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.data.dao.TaskDao
import com.example.calendar.data.relation.TaskWithTags
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

// ★ クラス名を DateDetailViewModel に修正し、役割を完全に一致させる
class DateDetailViewModel(private val taskDao: TaskDao) : ViewModel() {

    private val _selectedDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    val filteredTasks: StateFlow<List<TaskWithTags>> = taskDao.getAllTasksWithTags()
        .combine(_selectedDate) { totalList, date ->
            totalList.filter { item ->
                val taskDate = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(item.task.startTime),
                    ZoneOffset.UTC
                ).toLocalDate()
                taskDate == date
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun toggleTaskCompletion(taskWithTags: TaskWithTags) {
        viewModelScope.launch {
            val currentTask = taskWithTags.task
            val newStatus = if (currentTask.completeState == "COMPLETED") "UNCOMPLETED" else "COMPLETED"
            val updatedTask = currentTask.copy(completeState = newStatus)
            taskDao.updateTask(updatedTask)
        }
    }
}