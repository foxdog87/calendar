package com.foxdog.strucalendar.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxdog.strucalendar.data.dao.TagCustomFieldDao
import com.foxdog.strucalendar.data.dao.TemplateCustomFieldValueDao
import com.foxdog.strucalendar.data.entity.ChecklistItem
import com.foxdog.strucalendar.data.entity.Tag
import com.foxdog.strucalendar.data.entity.Template
import com.foxdog.strucalendar.data.entity.TemplateCustomFieldValue
import com.foxdog.strucalendar.data.recurrence.RecurrenceType
import com.foxdog.strucalendar.data.repository.TagRepository
import com.foxdog.strucalendar.data.repository.TemplateRepository
import com.foxdog.strucalendar.data.repository.getReminderSetting
import com.foxdog.strucalendar.data.settings.AppSettings
import com.foxdog.strucalendar.data.settings.SettingsRepository
import com.foxdog.strucalendar.notification.ReminderSetting
import com.foxdog.strucalendar.state.TemplateInputState
import com.foxdog.strucalendar.data.telemetry.AnalyticsLogger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class TemplateCreateViewModel(
    private val templateRepository: TemplateRepository,
    private val tagRepository: TagRepository,
    private val tagCustomFieldDao: TagCustomFieldDao,
    private val templateCustomFieldValueDao: TemplateCustomFieldValueDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    var inputState by mutableStateOf(TemplateInputState())
        private set

    private var initialInputStateSnapshot: TemplateInputState? = TemplateInputState()

    val hasUnsavedChanges: Boolean
        get() = initialInputStateSnapshot != null && initialInputStateSnapshot != inputState

    var isTitleError by mutableStateOf(false)
        private set

    val allTags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var editTemplateId: Long? = null

    val isEditMode: Boolean
        get() = editTemplateId != null

    fun loadTemplateForEdit(templateId: Long) {
        viewModelScope.launch {
            val template = templateRepository.getTemplateById(templateId) ?: return@launch
            val tagIds = templateRepository.getTagIdsForTemplate(templateId).toSet()
            val checklistItems = templateRepository.getChecklistItems(templateId)

            val customFieldValues =
                templateCustomFieldValueDao
                    .getByTemplateId(templateId)
                    .associate { it.fieldId to it.value }

            val customFields =
                tagCustomFieldDao.getByTagIds(
                    tagIds.toList()
                )

            editTemplateId = templateId

            // 既存テンプレートの繰り返し設定をUIへ復元
            val loadedRecurrenceType = try {
                template.recurrenceType?.let { RecurrenceType.valueOf(it) } ?: RecurrenceType.NONE
            } catch (e: IllegalArgumentException) {
                RecurrenceType.NONE
            }

            inputState = TemplateInputState(
                title = template.title,
                durationMinutes = (template.timeLength / 60).toInt(),
                description = template.description ?: "",
                memo = template.memo ?: "",
                checkList = checklistItems.mapIndexed { index, item ->
                    ChecklistItem(
                        id = 0L,
                        taskId = 0L,
                        text = item.text,
                        isChecked = false,
                        position = index
                    )
                },
                color = template.color,
                locationName = template.locationName,
                locationAddress = template.locationAddress,
                latitude = template.latitude,
                longitude = template.longitude,
                isAutoCompleted = template.isAutoCompleted,
                reminderSetting = template.getReminderSetting(),
                selectedTags = allTags.value.filter { it.tagId in tagIds },

                customFields = customFields,
                customFieldValues = customFieldValues,

                recurrenceType = loadedRecurrenceType,
                recurrenceIntervalDays = template.recurrenceIntervalDays ?: 1,
                recurrenceNth = template.recurrenceNth ?: 1,
                recurrenceWeekday = template.recurrenceWeekday ?: 1,
                recurrenceWeekdays = template.recurrenceWeekdays
                    ?.split(",")
                    ?.mapNotNull { it.trim().toIntOrNull() }
                    ?.toSet()
                    ?: emptySet(),
                recurrenceEndTime = template.recurrenceEndDate
            )
            initialInputStateSnapshot = inputState
        }
    }

    var isSaving by mutableStateOf(false)
        private set

    private var isSavingInternal = false

    enum class SaveState {
        IDLE,
        SAVING,
        SUCCESS,
        ERROR
    }

// ==========================================

    var saveState by mutableStateOf(SaveState.IDLE)
        private set

    var saveErrorMessage by mutableStateOf<String?>(null)
        private set

    fun saveTemplate(onSuccess: () -> Unit) {
        if (saveState != SaveState.IDLE) return

        if (inputState.title.trim().isBlank()) {
            isTitleError = true
            return
        }
        isTitleError = false

        saveState = SaveState.SAVING

        viewModelScope.launch {
            try {
                val (rType, rOffset, rDayOff, rHour, rMin) = when (val setting = inputState.reminderSetting) {
                    is ReminderSetting.None -> listOf(null, null, null, null, null)
                    is ReminderSetting.AtStartTime -> listOf("AT_START_TIME", null, null, null, null)
                    is ReminderSetting.Before -> listOf("BEFORE", setting.minutes, null, null, null)
                    is ReminderSetting.DayBefore -> listOf(
                        "DAY_BEFORE", null, setting.daysBack, setting.hour, setting.minute
                    )
                }

                val template = Template(
                    templateId = editTemplateId ?: 0L,
                    title = inputState.title,
                    icon = null,
                    timeLength = inputState.durationMinutes * 60L,
                    description = inputState.description.ifBlank { null },
                    color = inputState.color ?: Color(0xFF4285F4).toArgb(),
                    memo = inputState.memo,
                    locationName = inputState.locationName,
                    locationAddress = inputState.locationAddress,
                    latitude = inputState.latitude,
                    longitude = inputState.longitude,
                    dayCountTarget = null,
                    url = "",
                    attachmentPath = "",
                    isAutoCompleted = inputState.isAutoCompleted,
                    isAllDay = inputState.isAllDay,
                    reminderType = rType as String?,
                    reminderOffsetMinutes = rOffset as Int?,
                    reminderDayOffset = rDayOff as Int?,
                    reminderHour = rHour as Int?,
                    reminderMinute = rMin as Int?,

                    recurrenceType =
                        if (inputState.recurrenceType == RecurrenceType.NONE)
                            null
                        else
                            inputState.recurrenceType.name,

                    recurrenceIntervalDays =
                        if (inputState.recurrenceType == RecurrenceType.INTERVAL_DAYS)
                            inputState.recurrenceIntervalDays
                        else
                            null,

                    recurrenceNth =
                        if (inputState.recurrenceType == RecurrenceType.MONTHLY_NTH_WEEKDAY)
                            inputState.recurrenceNth
                        else
                            null,

                    recurrenceWeekday =
                        if (inputState.recurrenceType == RecurrenceType.MONTHLY_NTH_WEEKDAY)
                            inputState.recurrenceWeekday
                        else
                            null,

                    recurrenceWeekdays =
                        if (inputState.recurrenceType == RecurrenceType.WEEKLY_ON_DAYS &&
                            inputState.recurrenceWeekdays.isNotEmpty()
                        )
                            inputState.recurrenceWeekdays.sorted().joinToString(",")
                        else
                            null,

                    recurrenceEndDate = inputState.recurrenceEndTime
                )

                if (editTemplateId == null) {
                    val templateId = templateRepository.createTemplate(
                        template = template,
                        tags = inputState.selectedTags,
                        checklistItems = inputState.checkList
                    )

                    templateCustomFieldValueDao.upsertAll(
                        inputState.customFieldValues
                            .filterValues { it.isNotBlank() }
                            .map { (fieldId, value) ->
                                TemplateCustomFieldValue(
                                    templateId = templateId,
                                    fieldId = fieldId,
                                    value = value
                                )
                            }
                    )

                    AnalyticsLogger.logTemplateCreated()

                } else {
                    templateRepository.updateTemplate(
                        template = template,
                        tags = inputState.selectedTags,
                        checklistItems = inputState.checkList
                    )

                    templateCustomFieldValueDao.deleteByTemplateId(
                        template.templateId
                    )

                    templateCustomFieldValueDao.upsertAll(
                        inputState.customFieldValues
                            .filterValues { it.isNotBlank() }
                            .map { (fieldId, value) ->
                                TemplateCustomFieldValue(
                                    templateId = template.templateId,
                                    fieldId = fieldId,
                                    value = value
                                )
                            }
                    )

                    AnalyticsLogger.logTemplateUpdated()
                }

                saveState = SaveState.SUCCESS
                initialInputStateSnapshot = inputState
                onSuccess()

            } catch (e: Exception) {
                saveErrorMessage = "保存に失敗しました。もう一度お試しください。"
                saveState = SaveState.ERROR
            }
        }
    }

    fun updateInput(transform: (TemplateInputState) -> TemplateInputState) {
        inputState = transform(inputState)
    }

    fun toggleTagSelection(tag: Tag) {
        val currentSelected = inputState.selectedTags.toMutableList()
        val existingTag = currentSelected.find { it.tagId == tag.tagId }

        if (existingTag != null) {
            currentSelected.remove(existingTag)
        } else {
            currentSelected.add(tag)
        }

        viewModelScope.launch {
            val fields = tagCustomFieldDao.getByTagIds(
                currentSelected.map { it.tagId }
            )

            val fieldIds = fields.map { it.fieldId }.toSet()

            inputState = inputState.copy(
                selectedTags = currentSelected,
                customFields = fields,
                customFieldValues = inputState.customFieldValues
                    .filterKeys { it in fieldIds }
            )
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
            val newId = tagRepository.createTag(
                tag = tag,
                customFieldNames = customFieldNames
            )

            val savedTag = tag.copy(tagId = newId)

            val currentSelected = inputState.selectedTags.toMutableList()
            currentSelected.add(savedTag)

            inputState = inputState.copy(
                selectedTags = currentSelected
            )

            AnalyticsLogger.logTagCreated()
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
            AnalyticsLogger.logTagDeleted()
        }
    }

    fun updateTagOrder(tags: List<Tag>) {
        viewModelScope.launch {
            tagRepository.updateTagOrder(tags)
            AnalyticsLogger.logTagOrderChanged()
        }
    }

    fun updateCustomFieldValue(
        fieldId: Long,
        value: String
    ) {
        inputState = inputState.copy(
            customFieldValues =
                inputState.customFieldValues + (fieldId to value)
        )
    }

}