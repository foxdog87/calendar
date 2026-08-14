package com.example.calendar.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.data.dao.ChecklistItemDao
import com.example.calendar.data.dao.TemplateChecklistItemDao
import com.example.calendar.data.entity.ChecklistItem
import com.example.calendar.data.entity.Tag
import com.example.calendar.data.entity.Task
import com.example.calendar.data.entity.Template
import com.example.calendar.data.osm.model.OsmPoi
import com.example.calendar.data.osm.OsmRepository
import com.example.calendar.data.repository.TagRepository
import com.example.calendar.data.repository.TaskRepository
import com.example.calendar.data.repository.TemplateRepository
import com.example.calendar.data.repository.TemplateTagRepository
import com.example.calendar.data.repository.getReminderSetting
import com.example.calendar.notification.ReminderSetting
import com.example.calendar.notification.TaskAlarmScheduler
import com.example.calendar.notification.getReminderSetting
import com.example.calendar.state.TaskInputState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class TaskCreateViewModel(
    private val taskRepository: TaskRepository,
    private val tagRepository: TagRepository,
    private val templateRepository: TemplateRepository, // ★ 追加
    private val osmRepository: OsmRepository,
    private val checklistItemDao: ChecklistItemDao,
    private val templateChecklistItemDao: TemplateChecklistItemDao,
    private val templateTagRepository : TemplateTagRepository
) : ViewModel() {

    var inputState by mutableStateOf(TaskInputState())
        private set

    var isTitleError by mutableStateOf(false)
        private set
    var isDateTimeError by mutableStateOf(false)
        private set

    private var editTaskId: Long? = null
    private var currentCompleteState = "UNCOMPLETED"

    var osmSearchResults by mutableStateOf<List<OsmPoi>>(emptyList())
        private set

    var isOsmSearching by mutableStateOf(false)
        private set

    val allTags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val templates: StateFlow<List<Template>> = templateRepository.getAllTemplates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ★ 追加
    fun updateTemplateOrder(templates: List<Template>) {
        viewModelScope.launch {
            templateRepository.updateTemplateOrder(templates)
        }
    }

    fun deleteTemplate(template: Template) {
        viewModelScope.launch {
            templateRepository.deleteTemplate(template)
        }
    }



    fun updateInput(transform: (TaskInputState) -> TaskInputState) {
        val newState = transform(inputState)
        val adjustedState = if (!newState.isAllDay && newState.startTime != inputState.startTime) {
            if (newState.endTime <= newState.startTime) {
                newState.copy(endTime = newState.startTime + 3600L)
            } else {
                newState
            }
        } else {
            newState
        }
        inputState = adjustedState
    }

    fun prepareCreateTask(selectedDate: LocalDate) {
        editTaskId = null
        val now = LocalDateTime.now()
        val nextHour = if (now.minute > 0) now.plusHours(1).withMinute(0).withSecond(0)
            .withNano(0) else now.withMinute(0).withSecond(0).withNano(0)
        val adjustedStart = selectedDate.atTime(nextHour.hour, 0)
        val adjustedEnd = adjustedStart.plusHours(1)

        isTitleError = false
        isDateTimeError = false
        val defaultColor = Color(0xFF4285F4).toArgb()

        inputState = TaskInputState(
            startTime = adjustedStart.atZone(ZoneId.systemDefault()).toEpochSecond(),
            endTime = adjustedEnd.atZone(ZoneId.systemDefault()).toEpochSecond(),
            selectedTags = emptyList(),
            color = defaultColor,
            isAllDay = false,
            // ★ 新規作成時は必ず None（安全）
            reminderSetting = ReminderSetting.None
        )
    }

    fun loadTaskForEdit(taskId: Long) {
        viewModelScope.launch {

            val taskWithTags =
                taskRepository.getTaskWithTagsById(taskId)
                    ?: return@launch

            val checklistItems =
                taskRepository.getChecklistItems(taskId)

            val task =
                taskWithTags.task

            editTaskId = taskId
            isTitleError = false
            isDateTimeError = false

            currentCompleteState =
                task.completeState

            inputState = TaskInputState(
                title = task.title,

                startTime = task.startTime,
                endTime = task.endTime,

                memo = task.memo ?: "",

                checkList = checklistItems,

                color = task.color,

                attachmentPath =
                    task.attachmentPath ?: "",

                url =
                    task.url ?: "",

                locationName =
                    task.locationName,

                locationAddress =
                    task.locationAddress,

                latitude =
                    task.latitude,

                longitude =
                    task.longitude,

                isAutoCompleted =
                    task.isAutoCompleted,

                reminderSetting =
                    task.getReminderSetting(),

                selectedTags =
                    taskWithTags.tags,

                isAllDay =
                    task.isAllDay
            )
        }
    }

    fun toggleTagSelection(tag: Tag) {
        val currentSelected = inputState.selectedTags.toMutableList()
        val existingTag = currentSelected.find { it.tagId == tag.tagId }

        if (existingTag != null) {
            currentSelected.remove(existingTag)
        } else {
            currentSelected.add(tag)
        }
        inputState = inputState.copy(selectedTags = currentSelected)
    }

    fun saveTask(context: Context, onSuccess: () -> Unit) {
        if (inputState.title.trim().isBlank()) {
            isTitleError = true
            return
        }
        isTitleError = false

        if (!inputState.isAllDay && inputState.endTime <= inputState.startTime) {
            isDateTimeError = true
            return
        }
        isDateTimeError = false

        viewModelScope.launch {
            val finalStartTime: Long
            val finalEndTime: Long

            if (inputState.isAllDay) {
                val localDate = Instant.ofEpochSecond(inputState.startTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                finalStartTime =
                    localDate.atTime(0, 0).atZone(ZoneId.systemDefault()).toEpochSecond()
                finalEndTime =
                    localDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond()
            } else {
                finalStartTime = inputState.startTime
                finalEndTime = inputState.endTime
            }

            val (rType, rOffset, rDayOff, rHour, rMin) = when (val setting = inputState.reminderSetting) {
                is ReminderSetting.None -> listOf("NONE", null, null, null, null) // ★ nullから統一
                is ReminderSetting.AtStartTime -> listOf("AT_START_TIME", null, null, null, null)
                is ReminderSetting.Before -> listOf("BEFORE", setting.minutes, null, null, null)
                is ReminderSetting.DayBefore -> listOf(
                    "DAY_BEFORE", null, setting.daysBack, setting.hour, setting.minute
                )
            }

            val targetTask = Task(
                taskId = editTaskId ?: 0L,
                title = inputState.title,
                startTime = finalStartTime,
                endTime = finalEndTime,

                memo = inputState.memo,

                color = inputState.color ?: Color(0xFF4285F4).toArgb(),

                attachmentPath = inputState.attachmentPath,
                url = inputState.url,

                locationName = inputState.locationName,
                locationAddress = inputState.locationAddress,
                latitude = inputState.latitude,
                longitude = inputState.longitude,

                isAutoCompleted = inputState.isAutoCompleted,
                completeState = currentCompleteState,
                isAllDay = inputState.isAllDay,

                dayCountTarget = null,
                templateId = null,

                reminderType = rType as String?,
                reminderOffsetMinutes = rOffset as Int?,
                reminderDayOffset = rDayOff as Int?,
                reminderHour = rHour as Int?,
                reminderMinute = rMin as Int?
            )

            val scheduler = TaskAlarmScheduler(context)

            if (editTaskId == null) {

                val generatedId =
                    taskRepository.insertTaskWithTagsAndChecklist(
                        task = targetTask,
                        tags = inputState.selectedTags,
                        checklistItems = inputState.checkList
                    )

                val savedTask = targetTask.copy(
                    taskId = generatedId
                )

                scheduler.schedule(savedTask)

            } else {

                scheduler.cancel(targetTask)

                taskRepository.updateTaskWithTagsAndChecklist(
                    task = targetTask,
                    tags = inputState.selectedTags,
                    checklistItems = inputState.checkList
                )

                if (inputState.reminderSetting !is ReminderSetting.None) {
                    scheduler.schedule(targetTask)
                }
            }

            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun selectLocation(poi: OsmPoi) {
        inputState = inputState.copy(
            locationName = poi.name,
            locationAddress = poi.address,
            latitude = poi.latitude,
            longitude = poi.longitude
        )
    }

    fun clearLocation() {
        inputState = inputState.copy(
            locationName = null,
            locationAddress = null,
            latitude = null,
            longitude = null
        )
    }

    fun selectOsmPoi(poi: OsmPoi) {
        inputState = inputState.copy(
            locationName = poi.name,
            locationAddress = poi.address,
            latitude = poi.latitude,
            longitude = poi.longitude
        )

        osmSearchResults = emptyList()
    }

    fun searchOsmPoi(keyword: String) {
        if (keyword.isBlank()) {
            osmSearchResults = emptyList()
            return
        }

        viewModelScope.launch {
            isOsmSearching = true

            try {
                osmSearchResults = osmRepository.searchPoi(
                    keyword = keyword,
                    limit = 20
                )
            } finally {
                isOsmSearching = false
            }
        }
    }

    fun createTag(tag: Tag) {
        viewModelScope.launch {
            val newId = tagRepository.createTag(tag)
            val savedTag = tag.copy(tagId = newId)
            val currentSelected = inputState.selectedTags.toMutableList()
            currentSelected.add(savedTag)
            inputState = inputState.copy(selectedTags = currentSelected)
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            tagRepository.deleteTag(tag)
            val currentSelected = inputState.selectedTags.toMutableList()
            val existingTag = currentSelected.find { it.tagId == tag.tagId }
            if (existingTag != null) {
                currentSelected.remove(existingTag)
                inputState = inputState.copy(selectedTags = currentSelected)
            }
        }
    }

    fun updateTagOrder(tags: List<Tag>) {
        viewModelScope.launch {
            tagRepository.updateTagOrder(tags)
        }
    }


    val recentTemplates: StateFlow<List<Template>> = templateRepository.getRecentTemplates(limit = 3)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun applyTemplate(template: Template) {
        viewModelScope.launch {

            val templateItems =
                templateChecklistItemDao.getByTemplateId(
                    template.templateId
                )

            val (startEpoch, _) = createDefaultDateTimePair()
            val endEpoch = startEpoch + template.timeLength

            val selectedTemplateTags = allTags.value.filter {
                it.tagId in templateTagRepository.getTagIds(template.templateId).toSet()
            }

            inputState = inputState.copy(
                title = template.title,
                startTime = startEpoch,
                endTime = endEpoch,
                memo = template.memo ?: "",

                // ★ テンプレートに保存されていた isChecked の状態をそのまま引き継ぐ
                checkList = templateItems.mapIndexed { index, item ->
                    ChecklistItem(
                        id = 0L,
                        taskId = 0L,
                        text = item.text,
                        isChecked = item.isChecked, // false固定ではなく、item.isCheckedを使う
                        position = index
                    )
                },

                color = template.color,
                locationName = template.locationName,
                locationAddress = template.locationAddress,
                latitude = template.latitude,
                longitude = template.longitude,
                url = template.url ?: "",
                attachmentPath = template.attachmentPath ?: "",
                isAutoCompleted = template.isAutoCompleted,
                selectedTags = selectedTemplateTags,
                reminderSetting = template.getReminderSetting()
            )
            templateRepository.markAsUsed(template.templateId)
        }
    }

    private fun createDefaultDateTimePair(): Pair<Long, Long> {
        val now = LocalDateTime.now()
        val startDateTime = if (now.minute > 0) now.plusHours(1).withMinute(0).withSecond(0)
            .withNano(0) else now.withMinute(0).withSecond(0).withNano(0)
        val endDateTime = startDateTime.plusHours(1)

        return Pair(
            startDateTime.atZone(ZoneId.systemDefault()).toEpochSecond(),
            endDateTime.atZone(ZoneId.systemDefault()).toEpochSecond()
        )
    }

}
