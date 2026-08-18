package com.foxdog.strucalendar.screens.templatecreate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxdog.strucalendar.components.TagCreateDialog
import com.foxdog.strucalendar.components.TagIconSource
import com.foxdog.strucalendar.data.entity.Tag
import com.foxdog.strucalendar.notification.ReminderSetting
import com.foxdog.strucalendar.screens.taskcreate.AutoCompleteSection
import com.foxdog.strucalendar.screens.taskcreate.ChecklistSection
import com.foxdog.strucalendar.screens.taskcreate.ColorSection
import com.foxdog.strucalendar.screens.taskcreate.CustomFieldsSection
import com.foxdog.strucalendar.screens.taskcreate.ExpandableDetailsSection
import com.foxdog.strucalendar.screens.taskcreate.MemoSection
import com.foxdog.strucalendar.screens.taskcreate.RecurrenceSection
import com.foxdog.strucalendar.screens.taskcreate.ReminderSection
import com.foxdog.strucalendar.screens.taskcreate.SectionLabelWithIcon
import com.foxdog.strucalendar.screens.taskcreate.TagSection
import com.foxdog.strucalendar.screens.taskcreate.TitleSection
import com.foxdog.strucalendar.screens.taskcreate.WireframeTextField
import com.foxdog.strucalendar.state.TemplateInputState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateCreateContent(
    templateState: TemplateInputState,
    availableTags: List<Tag>,
    isTitleError: Boolean = false,
    isSaving: Boolean,
    onSaveTemplate: () -> Unit,
    onNavigateBack: () -> Unit,
    onUpdateInput: ((TemplateInputState) -> TemplateInputState) -> Unit,
    onToggleTagSelection: (Tag) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    onCreateTag: (Tag, List<String>) -> Unit,
    onUpdateTagOrder: (List<Tag>) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    val scrollState = rememberScrollState()
    var showTagCreateDialog by remember { mutableStateOf(false) }
    var isTagFolderExpanded by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<Tag?>(null) }
    var isDetailsExpanded by remember { mutableStateOf(false) }
    var showRecurrenceEndPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "テンプレートを作成",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "閉じる",
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSaveTemplate,
                        enabled = !isSaving,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .background(
                                color = if (isSaving) {
                                    colorScheme.onSurfaceVariant
                                } else {
                                    colorScheme.primary
                                },
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "保存",
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colorScheme.surface
                )
            )
        },
        containerColor = colorScheme.surface
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {

            Spacer(modifier = Modifier.height(8.dp))

            TitleSection(
                title = templateState.title,
                isTitleError = isTitleError,
                label = "テンプレート名 (必須)",
                placeholder = "テンプレート名を入力",
                onTitleChange = { newTitle ->
                    onUpdateInput { it.copy(title = newTitle) }
                }
            )

            SectionLabelWithIcon(
                "説明",
                Icons.Default.Description
            )

            WireframeTextField(
                value = templateState.description,
                onValueChange = { newDescription ->
                    onUpdateInput { it.copy(description = newDescription) }
                },
                placeholder = "このテンプレートの用途などを入力（任意）",
                minLines = 2
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("終日予定", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                Switch(
                    checked = templateState.isAllDay,
                    onCheckedChange = { isAllDay ->
                        onUpdateInput { it.copy(isAllDay = isAllDay) }
                    }
                )
            }

            // ★ 修正：終日ではない時だけ「所要時間」を表示する
            if (!templateState.isAllDay) {
                DurationSection(
                    durationMinutes = templateState.durationMinutes,
                    onDurationChange = { minutes ->
                        onUpdateInput { it.copy(durationMinutes = minutes) }
                    }
                )
            }

            TagSection(
                availableTags = availableTags,
                selectedTags = templateState.selectedTags,
                isTagFolderExpanded = isTagFolderExpanded,
                onExpandToggle = {
                    isTagFolderExpanded = !isTagFolderExpanded
                },
                onToggleTagSelection = onToggleTagSelection,
                onUpdateTagOrder = onUpdateTagOrder,
                onDeleteTagRequest = { tag ->
                    tagToDelete = tag
                },
                onAddTagClick = {
                    showTagCreateDialog = true
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            MemoSection(
                memo = templateState.memo,
                onMemoChange = { newMemo ->
                    onUpdateInput { it.copy(memo = newMemo) }
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            ColorSection(
                selectedColor = templateState.color
                    ?: Color(0xFF4285F4).toArgb(),
                onColorSelected = { color ->
                    onUpdateInput { it.copy(color = color) }
                }
            )


            CustomFieldsSection(
                fields = templateState.customFields,
                values = templateState.customFieldValues,
                onValueChange = { fieldId, value ->
                    onUpdateInput {
                        it.copy(
                            customFieldValues =
                                it.customFieldValues + (fieldId to value)
                        )
                    }
                }
            )

            ExpandableDetailsSection(
                isExpanded = isDetailsExpanded,
                onToggle = {
                    isDetailsExpanded = !isDetailsExpanded
                }
            ) {

                ChecklistSection(
                    checkList = templateState.checkList,
                    onCheckListChange = { newList ->
                        onUpdateInput { it.copy(checkList = newList) }
                    }
                )

                ReminderSection(
                    reminderSetting = templateState.reminderSetting,
                    isAllDay = templateState.isAllDay, // ★ 修正：固定のfalseから変更
                    onReminderSettingChange = { setting ->
                        onUpdateInput { it.copy(reminderSetting = setting) }
                    }
                )

                AutoCompleteSection(
                    isAutoCompleted = templateState.isAutoCompleted,
                    onAutoCompletedChange = { isAuto ->
                        onUpdateInput {
                            it.copy(isAutoCompleted = isAuto)
                        }
                    },
                    onInfoClick = {}
                )

                RecurrenceSection(
                    recurrenceType = templateState.recurrenceType,
                    intervalDays = templateState.recurrenceIntervalDays,
                    nth = templateState.recurrenceNth,
                    weekday = templateState.recurrenceWeekday,
                    endDateMillis = templateState.recurrenceEndTime,
                    baseDateMillis = System.currentTimeMillis() / 1000,
                    onTypeChange = { type ->
                        onUpdateInput {
                            it.copy(recurrenceType = type)
                        }
                    },
                    onIntervalDaysChange = { days ->
                        onUpdateInput {
                            it.copy(recurrenceIntervalDays = days)
                        }
                    },
                    onNthChange = { nth ->
                        onUpdateInput {
                            it.copy(recurrenceNth = nth)
                        }
                    },
                    onWeekdayChange = { weekday ->
                        onUpdateInput {
                            it.copy(recurrenceWeekday = weekday)
                        }
                    },
                    onEndDateClick = {
                        showRecurrenceEndPicker = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (tagToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                tagToDelete = null
            },
            title = {
                Text(
                    "タグの削除",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "タグ「${tagToDelete?.name}」を削除しますか？\n(この操作は取り消せません)"
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        tagToDelete = null
                    }
                ) {
                    Text("キャンセル")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        tagToDelete?.let(onDeleteTag)
                        tagToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("削除する")
                }
            }
        )
    }

    if (showTagCreateDialog) {
        TagCreateDialog(
            onDismissRequest = {
                showTagCreateDialog = false
            },
            onTagSave = { name, iconSource, color, customFieldNames ->

                val iconString = when (iconSource) {
                    is TagIconSource.InitialText -> null
                    is TagIconSource.Vector -> iconSource.iconId.id
                }

                val newTag = Tag(
                    tagId = 0L,
                    name = name,
                    color = color.toArgb(),
                    icon = iconString
                )

                onCreateTag(
                    newTag,
                    customFieldNames
                )

                showTagCreateDialog = false
            }
        )
    }

    if (showRecurrenceEndPicker) {
        com.foxdog.strucalendar.components.DateTimePickerWizard(
            target = "RECURRENCE_END",
            isAllDay = true,
            onDismiss = {
                showRecurrenceEndPicker = false
            },
            onDateTimeSelected = { finalDateTimeLong ->
                onUpdateInput {
                    it.copy(
                        recurrenceEndTime = finalDateTimeLong
                    )
                }
                showRecurrenceEndPicker = false
            }
        )
    }
}