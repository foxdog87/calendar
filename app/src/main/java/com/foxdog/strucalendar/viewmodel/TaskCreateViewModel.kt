package com.foxdog.strucalendar.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxdog.strucalendar.data.dao.ChecklistItemDao
import com.foxdog.strucalendar.data.dao.TagCustomFieldDao
import com.foxdog.strucalendar.data.dao.TaskCustomFieldValueDao
import com.foxdog.strucalendar.data.dao.TemplateChecklistItemDao
import com.foxdog.strucalendar.data.dao.TemplateCustomFieldValueDao
import com.foxdog.strucalendar.data.entity.ChecklistItem
import com.foxdog.strucalendar.data.entity.Tag
import com.foxdog.strucalendar.data.entity.Task
import com.foxdog.strucalendar.data.entity.TaskCustomFieldValue
import com.foxdog.strucalendar.data.entity.Template
import com.foxdog.strucalendar.data.osm.OsmRepository
import com.foxdog.strucalendar.data.osm.model.OsmPoi
import com.foxdog.strucalendar.data.recurrence.RecurrenceCalculator
import com.foxdog.strucalendar.data.recurrence.RecurrenceRule
import com.foxdog.strucalendar.data.recurrence.RecurrenceType
import com.foxdog.strucalendar.data.repository.TagRepository
import com.foxdog.strucalendar.data.repository.TaskRepository
import com.foxdog.strucalendar.data.repository.TemplateRepository
import com.foxdog.strucalendar.data.repository.TemplateTagRepository
import com.foxdog.strucalendar.data.repository.getReminderSetting
import com.foxdog.strucalendar.data.settings.AppSettings
import com.foxdog.strucalendar.data.settings.SettingsRepository
import com.foxdog.strucalendar.data.telemetry.AnalyticsLogger
import com.foxdog.strucalendar.notification.ReminderSetting
import com.foxdog.strucalendar.notification.TaskAlarmScheduler
import com.foxdog.strucalendar.notification.getReminderSetting
import com.foxdog.strucalendar.state.TaskInputState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class TaskCreateViewModel(
    private val taskRepository: TaskRepository,
    private val tagRepository: TagRepository,
    private val templateRepository: TemplateRepository,
    private val osmRepository: OsmRepository,
    private val checklistItemDao: ChecklistItemDao,
    private val templateChecklistItemDao: TemplateChecklistItemDao,
    private val templateTagRepository: TemplateTagRepository,
    private val settingsRepository: SettingsRepository,
    private val taskCustomFieldValueDao: TaskCustomFieldValueDao,
    private val tagCustomFieldDao: TagCustomFieldDao,
    private val templateCustomFieldValueDao: TemplateCustomFieldValueDao,
) : ViewModel() {

    var inputState by mutableStateOf(TaskInputState())
        private set

    var isTitleError by mutableStateOf(false)
        private set

    var isDateTimeError by mutableStateOf(false)
        private set

    // 保存処理中の連打を防止する。テンプレート保存と同じく状態でガードする。
    var isSaving by mutableStateOf(false)
        private set

    private var editTaskId: Long? = null
    private var currentCompleteState = "UNCOMPLETED"
    private var currentRecurrenceGroupId: String? = null
    private var initialInputStateSnapshot: TaskInputState? = null

    val isEditMode: Boolean
        get() = editTaskId != null

    val hasUnsavedChanges: Boolean
        get() = initialInputStateSnapshot != null && initialInputStateSnapshot != inputState

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

        val adjustedState =
            if (!newState.isAllDay && newState.startTime != inputState.startTime) {
                if (newState.endTime <= newState.startTime) {
                    newState.copy(
                        endTime = newState.startTime + 3600L
                    )
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
        currentCompleteState = "UNCOMPLETED"
        currentRecurrenceGroupId = null

        val now = LocalDateTime.now()

        val nextHour =
            if (now.minute > 0) {
                now.plusHours(1)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0)
            } else {
                now.withMinute(0)
                    .withSecond(0)
                    .withNano(0)
            }

        val adjustedStart =
            selectedDate.atTime(nextHour.hour, 0)

        val adjustedEnd =
            adjustedStart.plusHours(1)

        isTitleError = false
        isDateTimeError = false

        val defaultColor =
            Color(0xFF4285F4).toArgb()

        val currentSettings =
            settingsRepository.settingsFlow

        viewModelScope.launch {
            val settings =
                currentSettings.first()

            inputState = TaskInputState(
                startTime = adjustedStart
                    .atZone(ZoneId.systemDefault())
                    .toEpochSecond(),

                endTime = adjustedEnd
                    .atZone(ZoneId.systemDefault())
                    .toEpochSecond(),

                selectedTags = emptyList(),

                color = defaultColor,

                isAllDay = false,

                reminderSetting =
                    if (settings.isNotificationEnabled) {
                        ReminderSetting.Before(
                            settings.defaultReminderOffsetMinutes
                        )
                    } else {
                        ReminderSetting.None
                    }
            )
            initialInputStateSnapshot = inputState
        }
    }

    fun loadTaskForEdit(taskId: Long) {
        viewModelScope.launch {

            val taskWithTags =
                taskRepository.getTaskWithTagsById(taskId)
                    ?: return@launch

            val checklistItems =
                taskRepository.getChecklistItems(taskId)

            val customFieldValues =
                taskCustomFieldValueDao
                    .getByTaskId(taskId)
                    .associate { it.fieldId to it.value }

            val customFields =
                tagCustomFieldDao.getByTagIds(
                    taskWithTags.tags.map { it.tagId }
                )

            val task =
                taskWithTags.task

            editTaskId = taskId

            isTitleError = false
            isDateTimeError = false

            currentCompleteState =
                task.completeState

            currentRecurrenceGroupId =
                task.recurrenceGroupId

            val loadedRecurrenceType =
                try {
                    task.recurrenceType?.let {
                        RecurrenceType.valueOf(it)
                    } ?: RecurrenceType.NONE
                } catch (e: IllegalArgumentException) {
                    RecurrenceType.NONE
                }

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

                customFields = customFields,
                customFieldValues = customFieldValues,

                isAutoCompleted =
                    task.isAutoCompleted,

                reminderSetting =
                    task.getReminderSetting(),

                selectedTags =
                    taskWithTags.tags,

                isAllDay =
                    task.isAllDay,

                recurrenceType =
                    loadedRecurrenceType,

                recurrenceIntervalDays =
                    task.recurrenceIntervalDays ?: 1,

                recurrenceNth =
                    task.recurrenceNth ?: 1,

                recurrenceWeekday =
                    task.recurrenceWeekday ?: 1,

                recurrenceWeekdays =
                    task.recurrenceWeekdays
                        ?.split(",")
                        ?.mapNotNull { it.trim().toIntOrNull() }
                        ?.toSet()
                        ?: emptySet(),

                recurrenceEndTime =
                    task.recurrenceEndDate
            )
            initialInputStateSnapshot = inputState
        }
    }

    fun toggleTaskTagSelection(tag: Tag) {
        val currentSelected =
            inputState.selectedTags.toMutableList()

        val existingTag =
            currentSelected.find {
                it.tagId == tag.tagId
            }

        if (existingTag != null) {
            currentSelected.remove(existingTag)
        } else {
            currentSelected.add(tag)
        }

        viewModelScope.launch {

            val fields =
                tagCustomFieldDao.getByTagIds(
                    currentSelected.map { it.tagId }
                )

            val fieldIds =
                fields.map { it.fieldId }.toSet()

            inputState =
                inputState.copy(
                    selectedTags = currentSelected,

                    customFields = fields,

                    customFieldValues =
                        inputState.customFieldValues
                            .filterKeys {
                                it in fieldIds
                            }
                )
        }
    }

    fun updateCustomFieldValue(
        fieldId: Long,
        value: String
    ) {
        inputState =
            inputState.copy(
                customFieldValues =
                    inputState.customFieldValues +
                            (fieldId to value)
            )
    }

    fun saveTask(
        context: Context,
        onSuccess: () -> Unit
    ) {
        if (isSaving) return

        if (inputState.title.trim().isBlank()) {
            isTitleError = true
            return
        }

        isTitleError = false

        val isInvalidRange =
            if (inputState.isAllDay) {
                val startDate =
                    Instant.ofEpochSecond(inputState.startTime)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                val endDate =
                    Instant.ofEpochSecond(inputState.endTime)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                endDate.isBefore(startDate)
            } else {
                inputState.endTime <= inputState.startTime
            }

        if (isInvalidRange) {
            isDateTimeError = true
            return
        }

        isDateTimeError = false
        isSaving = true

        viewModelScope.launch {
            try {
                val notificationEnabled =
                settingsRepository.settingsFlow
                    .first()
                    .isNotificationEnabled

            val finalStartTime: Long
            val finalEndTime: Long

            if (inputState.isAllDay) {

                val startLocalDate =
                    Instant.ofEpochSecond(
                        inputState.startTime
                    )
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()

                val endLocalDate =
                    Instant.ofEpochSecond(
                        inputState.endTime
                    )
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()

                finalStartTime =
                    startLocalDate
                        .atTime(0, 0)
                        .atZone(ZoneId.systemDefault())
                        .toEpochSecond()

                finalEndTime =
                    endLocalDate
                        .atTime(23, 59, 59)
                        .atZone(ZoneId.systemDefault())
                        .toEpochSecond()

            } else {

                finalStartTime =
                    inputState.startTime

                finalEndTime =
                    inputState.endTime
            }

            val (rType, rOffset, rDayOff, rHour, rMin) =
                when (val setting = inputState.reminderSetting) {

                    is ReminderSetting.None ->
                        listOf(
                            "NONE",
                            null,
                            null,
                            null,
                            null
                        )

                    is ReminderSetting.AtStartTime ->
                        listOf(
                            "AT_START_TIME",
                            null,
                            null,
                            null,
                            null
                        )

                    is ReminderSetting.Before ->
                        listOf(
                            "BEFORE",
                            setting.minutes,
                            null,
                            null,
                            null
                        )

                    is ReminderSetting.DayBefore ->
                        listOf(
                            "DAY_BEFORE",
                            null,
                            setting.daysBack,
                            setting.hour,
                            setting.minute
                        )
                }

            val scheduler =
                TaskAlarmScheduler(context)

            val isRecurring =
                editTaskId == null &&
                        inputState.recurrenceType != RecurrenceType.NONE

            if (isRecurring) {

                val startDate =
                    Instant.ofEpochSecond(finalStartTime)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()

                val endDate =
                    inputState.recurrenceEndTime?.let {
                        Instant.ofEpochSecond(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                        ?: RecurrenceCalculator.defaultEndDate(
                            startDate
                        )

                val rule =
                    RecurrenceRule(
                        type = inputState.recurrenceType,
                        intervalDays =
                            inputState.recurrenceIntervalDays,
                        nth =
                            inputState.recurrenceNth,
                        weekday =
                            inputState.recurrenceWeekday,
                        weekdays =
                            inputState.recurrenceWeekdays,
                        endDate = endDate
                    )

                val occurrenceDates =
                    RecurrenceCalculator.generateOccurrences(
                        startDate,
                        rule
                    )

                val groupId =
                    UUID.randomUUID().toString()

                val durationSeconds =
                    finalEndTime - finalStartTime

                occurrenceDates.forEach { occurrenceDate ->

                    val occurrenceStart =
                        occurrenceDate
                            .atTime(
                                Instant.ofEpochSecond(
                                    finalStartTime
                                )
                                    .atZone(
                                        ZoneId.systemDefault()
                                    )
                                    .toLocalTime()
                            )
                            .atZone(
                                ZoneId.systemDefault()
                            )
                            .toEpochSecond()

                    val occurrenceEnd =
                        if (inputState.isAllDay) {

                            occurrenceDate
                                .atTime(23, 59, 59)
                                .atZone(
                                    ZoneId.systemDefault()
                                )
                                .toEpochSecond()

                        } else {
                            occurrenceStart +
                                    durationSeconds
                        }

                    val occurrenceTask =
                        Task(
                            taskId = 0L,

                            title =
                                inputState.title,

                            startTime =
                                occurrenceStart,

                            endTime =
                                occurrenceEnd,

                            memo =
                                inputState.memo,

                            color =
                                inputState.color
                                    ?: Color(0xFF4285F4)
                                        .toArgb(),

                            attachmentPath =
                                inputState.attachmentPath,

                            url =
                                inputState.url,

                            locationName =
                                inputState.locationName,

                            locationAddress =
                                inputState.locationAddress,

                            latitude =
                                inputState.latitude,

                            longitude =
                                inputState.longitude,

                            isAutoCompleted =
                                inputState.isAutoCompleted,

                            completeState =
                                "UNCOMPLETED",

                            isAllDay =
                                inputState.isAllDay,

                            dayCountTarget =
                                null,

                            templateId =
                                null,

                            reminderType =
                                rType as String?,

                            reminderOffsetMinutes =
                                rOffset as Int?,

                            reminderDayOffset =
                                rDayOff as Int?,

                            reminderHour =
                                rHour as Int?,

                            reminderMinute =
                                rMin as Int?,

                            recurrenceGroupId =
                                groupId,

                            recurrenceType =
                                inputState.recurrenceType.name,

                            recurrenceIntervalDays =
                                if (
                                    inputState.recurrenceType ==
                                    RecurrenceType.INTERVAL_DAYS
                                ) {
                                    inputState.recurrenceIntervalDays
                                } else {
                                    null
                                },

                            recurrenceNth =
                                if (
                                    inputState.recurrenceType ==
                                    RecurrenceType.MONTHLY_NTH_WEEKDAY
                                ) {
                                    inputState.recurrenceNth
                                } else {
                                    null
                                },

                            recurrenceWeekday =
                                if (
                                    inputState.recurrenceType ==
                                    RecurrenceType.MONTHLY_NTH_WEEKDAY
                                ) {
                                    inputState.recurrenceWeekday
                                } else {
                                    null
                                },

                            recurrenceWeekdays =
                                if (
                                    inputState.recurrenceType ==
                                    RecurrenceType.WEEKLY_ON_DAYS &&
                                    inputState.recurrenceWeekdays.isNotEmpty()
                                ) {
                                    inputState.recurrenceWeekdays
                                        .sorted()
                                        .joinToString(",")
                                } else {
                                    null
                                },

                            recurrenceEndDate =
                                endDate
                                    .atStartOfDay(
                                        ZoneId.systemDefault()
                                    )
                                    .toEpochSecond()
                        )

                    val generatedId =
                        taskRepository
                            .insertTaskWithTagsAndChecklist(
                                task = occurrenceTask,
                                tags = inputState.selectedTags,
                                checklistItems = inputState.checkList
                            )

                    taskCustomFieldValueDao.upsertAll(
                        inputState.customFieldValues
                            .filterValues {
                                it.isNotBlank()
                            }
                            .map { (fieldId, value) ->

                                TaskCustomFieldValue(
                                    taskId = generatedId,
                                    fieldId = fieldId,
                                    value = value
                                )
                            }
                    )

                    if (notificationEnabled) {
                        scheduler.schedule(
                            occurrenceTask.copy(
                                taskId = generatedId
                            )
                        )
                    }
                }

                AnalyticsLogger.logTaskCreated()

            } else {

                val targetTask =
                    Task(
                        taskId =
                            editTaskId ?: 0L,

                        title =
                            inputState.title,

                        startTime =
                            finalStartTime,

                        endTime =
                            finalEndTime,

                        memo =
                            inputState.memo,

                        color =
                            inputState.color
                                ?: Color(0xFF4285F4)
                                    .toArgb(),

                        attachmentPath =
                            inputState.attachmentPath,

                        url =
                            inputState.url,

                        locationName =
                            inputState.locationName,

                        locationAddress =
                            inputState.locationAddress,

                        latitude =
                            inputState.latitude,

                        longitude =
                            inputState.longitude,

                        isAutoCompleted =
                            inputState.isAutoCompleted,

                        completeState =
                            currentCompleteState,

                        isAllDay =
                            inputState.isAllDay,

                        dayCountTarget =
                            null,

                        templateId =
                            null,

                        reminderType =
                            rType as String?,

                        reminderOffsetMinutes =
                            rOffset as Int?,

                        reminderDayOffset =
                            rDayOff as Int?,

                        reminderHour =
                            rHour as Int?,

                        reminderMinute =
                            rMin as Int?,

                        recurrenceGroupId =
                            currentRecurrenceGroupId,

                        recurrenceType =
                            if (inputState.recurrenceType == RecurrenceType.NONE) {
                                null
                            } else {
                                inputState.recurrenceType.name
                            },

                        recurrenceIntervalDays =
                            if (inputState.recurrenceType == RecurrenceType.INTERVAL_DAYS) {
                                inputState.recurrenceIntervalDays
                            } else {
                                null
                            },

                        recurrenceNth =
                            if (inputState.recurrenceType == RecurrenceType.MONTHLY_NTH_WEEKDAY) {
                                inputState.recurrenceNth
                            } else {
                                null
                            },

                        recurrenceWeekday =
                            if (inputState.recurrenceType == RecurrenceType.MONTHLY_NTH_WEEKDAY) {
                                inputState.recurrenceWeekday
                            } else {
                                null
                            },

                        recurrenceWeekdays =
                            if (inputState.recurrenceType == RecurrenceType.WEEKLY_ON_DAYS &&
                                inputState.recurrenceWeekdays.isNotEmpty()
                            ) {
                                inputState.recurrenceWeekdays.sorted().joinToString(",")
                            } else {
                                null
                            },

                        recurrenceEndDate =
                            inputState.recurrenceEndTime
                    )

                if (editTaskId == null) {

                    val generatedId =
                        taskRepository
                            .insertTaskWithTagsAndChecklist(
                                task = targetTask,
                                tags = inputState.selectedTags,
                                checklistItems = inputState.checkList
                            )

                    // 新規タスクのカスタム項目を保存
                    taskCustomFieldValueDao.upsertAll(
                        inputState.customFieldValues
                            .filterValues {
                                it.isNotBlank()
                            }
                            .map { (fieldId, value) ->

                                TaskCustomFieldValue(
                                    taskId = generatedId,
                                    fieldId = fieldId,
                                    value = value
                                )
                            }
                    )

                    val savedTask =
                        targetTask.copy(
                            taskId = generatedId
                        )

                    if (notificationEnabled) {
                        scheduler.schedule(
                            savedTask
                        )
                    }

                    AnalyticsLogger.logTaskCreated()

                } else {

                    scheduler.cancel(
                        targetTask
                    )

                    taskRepository.updateTaskWithTagsAndChecklist(
                        task = targetTask,
                        tags = inputState.selectedTags,
                        checklistItems = inputState.checkList
                    )

                    taskCustomFieldValueDao.deleteByTaskId(
                        targetTask.taskId
                    )

                    taskCustomFieldValueDao.upsertAll(
                        inputState.customFieldValues
                            .filterValues {
                                it.isNotBlank()
                            }
                            .map { (fieldId, value) ->

                                TaskCustomFieldValue(
                                    taskId =
                                        targetTask.taskId,
                                    fieldId =
                                        fieldId,
                                    value =
                                        value
                                )
                            }
                    )

                    if (
                        notificationEnabled &&
                        inputState.reminderSetting !is
                                ReminderSetting.None
                    ) {
                        scheduler.schedule(
                            targetTask
                        )
                    }

                    AnalyticsLogger.logTaskUpdated()
                }
            }

            initialInputStateSnapshot = inputState

            // 成功時は、画面遷移を開始するまで isSaving = true を維持する。
            // 先に false にすると、popBackStack() が完了するまでの一瞬に
            // 保存ボタンが再び有効になり、2回目の保存が走る可能性がある。
            // 成功後はこの画面を離れるため、ここでは false に戻さない。
            withContext(Dispatchers.Main) {
                onSuccess()
            }
            } catch (e: Exception) {
                // 保存に失敗した場合だけ再試行できるようにする。
                isSaving = false
                throw e
            }
        }
    }

    fun selectLocation(poi: OsmPoi) {
        inputState =
            inputState.copy(
                locationName = poi.name,
                locationAddress = poi.address,
                latitude = poi.latitude,
                longitude = poi.longitude
            )
    }

    fun clearLocation() {
        inputState =
            inputState.copy(
                locationName = null,
                locationAddress = null,
                latitude = null,
                longitude = null
            )
    }

    fun selectOsmPoi(poi: OsmPoi) {
        inputState =
            inputState.copy(
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
                osmSearchResults =
                    osmRepository.searchPoi(
                        keyword = keyword,
                        limit = 20
                    )
            } finally {
                isOsmSearching = false
            }
        }
    }

    suspend fun getCustomFieldNamesForTag(tagId: Long): List<String> {
        return tagCustomFieldDao.getByTagId(tagId).map { it.fieldName }
    }

    fun updateTag(
        tag: Tag,
        customFieldNames: List<String>
    ) {
        viewModelScope.launch {

            tagRepository.updateTagWithCustomFields(
                tag = tag,
                customFieldNames = customFieldNames
            )

            // selectedTags 内の同一タグ情報も更新し、customFields/customFieldValuesを再取得する
            val currentSelected =
                inputState.selectedTags.map {
                    if (it.tagId == tag.tagId) tag else it
                }

            val refreshedFields =
                tagCustomFieldDao.getByTagIds(
                    currentSelected.map { it.tagId }
                )

            val fieldIds =
                refreshedFields.map { it.fieldId }.toSet()

            inputState =
                inputState.copy(
                    selectedTags = currentSelected,
                    customFields = refreshedFields,
                    customFieldValues =
                        inputState.customFieldValues.filterKeys { it in fieldIds }
                )

            AnalyticsLogger.logTagUpdated()
        }
    }

    fun createTag(
        tag: Tag,
        customFieldNames: List<String>
    ) {
        viewModelScope.launch {

            val newId =
                tagRepository.createTag(
                    tag = tag,
                    customFieldNames = customFieldNames
                )

            val savedTag =
                tag.copy(
                    tagId = newId
                )

            val currentSelected =
                inputState.selectedTags.toMutableList()

            currentSelected.add(
                savedTag
            )

            val newFields =
                tagCustomFieldDao.getByTagIds(
                    currentSelected.map {
                        it.tagId
                    }
                )

            val fieldIds =
                newFields.map {
                    it.fieldId
                }.toSet()

            inputState =
                inputState.copy(
                    selectedTags =
                        currentSelected,

                    customFields =
                        newFields,

                    customFieldValues =
                        inputState.customFieldValues
                            .filterKeys {
                                it in fieldIds
                            }
                )

            AnalyticsLogger.logTagCreated()
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {

            tagRepository.deleteTag(
                tag
            )

            val currentSelected =
                inputState.selectedTags.toMutableList()

            val existingTag =
                currentSelected.find {
                    it.tagId == tag.tagId
                }

            if (existingTag != null) {

                currentSelected.remove(
                    existingTag
                )

                inputState =
                    inputState.copy(
                        selectedTags =
                            currentSelected
                    )
            }

            AnalyticsLogger.logTagDeleted()
        }
    }

    fun updateTagOrder(tags: List<Tag>) {
        viewModelScope.launch {
            tagRepository.updateTagOrder(
                tags
            )

            AnalyticsLogger.logTagOrderChanged()
        }
    }

    // 「最近使用したテンプレート」は、テンプレート適用直後には表示順を変えず、
    // タスク作成画面を開いたタイミングで最新の使用順を反映する。
    private val _recentTemplates = MutableStateFlow<List<Template>>(emptyList())
    val recentTemplates: StateFlow<List<Template>> = _recentTemplates

    fun refreshRecentTemplates() {
        viewModelScope.launch {
            _recentTemplates.value =
                templateRepository.getRecentTemplates(limit = 3).first()
        }
    }

    // 予定作成画面のオンボーディング表示可否を判定するために使う
    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    // オンボーディングを最後まで見た／スキップした際に完了フラグを保存する
    var showAllTutorialsCompletedDialog by mutableStateOf(false)
        private set

    fun dismissAllTutorialsCompletedDialog() {
        showAllTutorialsCompletedDialog = false
    }

    fun completeTaskCreateOnboarding() {
        viewModelScope.launch {
            settingsRepository.setTaskCreateOnboardingCompleted(true)
            if (settingsRepository.areAllOnboardingsCompleted()) {
                showAllTutorialsCompletedDialog = true
            }
        }
    }

    fun applyTemplate(template: Template) {
        viewModelScope.launch {

            val templateItems =
                templateChecklistItemDao.getByTemplateId(
                    template.templateId
                )

            val templateTagIds =
                templateTagRepository
                    .getTagIds(template.templateId)

            val templateCustomFieldValues =
                templateCustomFieldValueDao
                    .getByTemplateId(template.templateId)
                    .associate {
                        it.fieldId to it.value
                    }

            val customFields =
                tagCustomFieldDao.getByTagIds(
                    templateTagIds
                )

            val (startEpoch, _) =
                createDateTimePairKeepingDate(inputState.startTime)

            val endEpoch =
                startEpoch + template.timeLength

            val selectedTemplateTags =
                allTags.value.filter {
                    it.tagId in templateTagIds.toSet()
                }

            val templateRecurrenceType =
                try {
                    template.recurrenceType?.let {
                        RecurrenceType.valueOf(it)
                    } ?: RecurrenceType.NONE
                } catch (e: IllegalArgumentException) {
                    RecurrenceType.NONE
                }

            inputState =
                inputState.copy(
                    title = template.title,
                    startTime = startEpoch,
                    endTime = endEpoch,
                    memo = template.memo ?: "",

                    checkList =
                        templateItems.mapIndexed { index, item ->
                            ChecklistItem(
                                id = 0L,
                                taskId = 0L,
                                text = item.text,
                                isChecked = item.isChecked,
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
                    reminderSetting = template.getReminderSetting(),

                    customFields = customFields,
                    customFieldValues = templateCustomFieldValues,

                    recurrenceType = templateRecurrenceType,
                    recurrenceIntervalDays =
                        template.recurrenceIntervalDays ?: 1,
                    recurrenceNth =
                        template.recurrenceNth ?: 1,
                    recurrenceWeekday =
                        template.recurrenceWeekday ?: 1,
                    recurrenceWeekdays =
                        template.recurrenceWeekdays
                            ?.split(",")
                            ?.mapNotNull { it.trim().toIntOrNull() }
                            ?.toSet()
                            ?: emptySet(),
                    recurrenceEndTime =
                        template.recurrenceEndDate
                )

            templateRepository.markAsUsed(
                template.templateId
            )

            AnalyticsLogger.logTemplateApplied()
        }
    }

    private fun createDefaultDateTimePair(): Pair<Long, Long> {

        val now =
            LocalDateTime.now()

        val startDateTime =
            if (now.minute > 0) {

                now.plusHours(1)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0)

            } else {

                now.withMinute(0)
                    .withSecond(0)
                    .withNano(0)
            }

        val endDateTime =
            startDateTime.plusHours(1)

        return Pair(
            startDateTime
                .atZone(
                    ZoneId.systemDefault()
                )
                .toEpochSecond(),

            endDateTime
                .atZone(
                    ZoneId.systemDefault()
                )
                .toEpochSecond()
        )
    }

    /**
     * createDefaultDateTimePair()と異なり、日付部分は baseDateEpoch
     * （通常は inputState.startTime、カレンダー画面で選択中の日付）から取得し、
     * 時刻部分のみ「次の正時」で算出する。テンプレート適用時に選択中の日付が
     * 現在時刻の日付にリセットされてしまう不具合の修正用。
     */
    private fun createDateTimePairKeepingDate(baseDateEpoch: Long): Pair<Long, Long> {

        val baseDate =
            Instant.ofEpochSecond(baseDateEpoch)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

        val now =
            LocalDateTime.now()

        val nextHour =
            if (now.minute > 0) {
                now.plusHours(1).hour
            } else {
                now.hour
            }

        val startDateTime =
            baseDate.atTime(nextHour, 0)

        val endDateTime =
            startDateTime.plusHours(1)

        return Pair(
            startDateTime
                .atZone(ZoneId.systemDefault())
                .toEpochSecond(),

            endDateTime
                .atZone(ZoneId.systemDefault())
                .toEpochSecond()
        )
    }
}