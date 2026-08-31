package com.foxdog.strucalendar.screens.taskcreate

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxdog.strucalendar.components.TagCreateDialog
import com.foxdog.strucalendar.components.TagIconSource
import com.foxdog.strucalendar.components.LocationSearchDialog
import com.foxdog.strucalendar.components.DateTimePickerWizard
import com.foxdog.strucalendar.components.DraggableTemplateList
import com.foxdog.strucalendar.data.entity.Tag
import com.foxdog.strucalendar.data.entity.Template
import com.foxdog.strucalendar.data.osm.model.OsmPoi
import com.foxdog.strucalendar.state.TaskInputState
import com.foxdog.strucalendar.ui.theme.calendarColors
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.runtime.mutableStateMapOf
import com.foxdog.strucalendar.components.SpotlightOnboardingOverlay
import com.foxdog.strucalendar.components.SpotlightShape
import com.foxdog.strucalendar.components.SpotlightStep
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.ui.geometry.Rect
import com.foxdog.strucalendar.components.SimpleDatePickerDialog
import com.foxdog.strucalendar.components.SimpleTimePickerDialog
import com.foxdog.strucalendar.data.recurrence.RecurrenceType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TaskCreateContent(
    taskState: TaskInputState,
    availableTags: List<Tag>,
    templates: List<Template>,
    recentTemplates: List<Template>,
    isTitleError: Boolean = false,
    isEditMode: Boolean = false,
    isDateTimeError: Boolean = false,
    isSaving: Boolean = false,
    onNotificationPermissionNeeded: () -> Unit = {},
    onNavigateBack: () -> Unit,
    onSaveTask: () -> Unit,
    onUpdateInput: ((TaskInputState) -> TaskInputState) -> Unit,
    onToggleTagSelection: (Tag) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    onCreateTag: (Tag, List<String>) -> Unit,
    onUpdateTag: (Tag, List<String>) -> Unit,
    onLoadCustomFieldsForTag: suspend (Long) -> List<String>,
    onApplyTemplate: (Template) -> Unit,
    onUpdateTagOrder: (List<Tag>) -> Unit,
    onUpdateTemplateOrder: (List<Template>) -> Unit,
    onNavigateToTemplateCreate: () -> Unit,
    alwaysShowDetailedSettings: Boolean,
    osmSearchResults: List<OsmPoi> = emptyList(),
    onCustomFieldValueChange: (Long, String) -> Unit,
    isOsmSearching: Boolean = false,
    onSearchOsmPoi: (String) -> Unit = {},
    onSelectOsmPoi: (OsmPoi) -> Unit = {},
    onClearLocation: () -> Unit = {},
    onDeleteTemplate: (Template) -> Unit,
    onNavigateToTemplateEdit: (Long) -> Unit,
    showOnboarding: Boolean = false,
    onOnboardingFinished: () -> Unit = {},
    showAllTutorialsCompletedDialog: Boolean = false,
    onDismissAllTutorialsCompletedDialog: () -> Unit = {},
    hasUnsavedChanges: Boolean = false,
    confirmDiscardChanges: Boolean = true,
) {
    val colorScheme = MaterialTheme.colorScheme
    val calColors = MaterialTheme.calendarColors

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var showDiscardConfirmDialog by remember { mutableStateOf(false) }

    val attemptClose: () -> Unit = {
        if (confirmDiscardChanges && hasUnsavedChanges) {
            showDiscardConfirmDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler { attemptClose() }

    val fieldBackgroundColor = colorScheme.surfaceVariant
    val templateIconColor = calColors.templateAccent


    var isDetailsExpanded by remember(alwaysShowDetailedSettings) {
        mutableStateOf(alwaysShowDetailedSettings)
    }

    val recurrenceBringIntoViewRequester = remember { BringIntoViewRequester() }

    var activeTarget by remember { mutableStateOf<String?>(null) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var selectedTemplateName by remember { mutableStateOf("テンプレートを選択") }

    var showTagCreateDialog by remember { mutableStateOf(false) }
    var tagToEdit by remember { mutableStateOf<Tag?>(null) }
    var isTagFolderExpanded by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<Tag?>(null) }

    var showLocationSearchDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    var templateToDelete by remember { mutableStateOf<Template?>(null) }

    val targetRects = remember { mutableStateMapOf<String, Rect>() }
    var onboardingDismissed by remember { mutableStateOf(false) }

    // スクロール用の Requester をステップごとに用意
    val templateRequester = remember { BringIntoViewRequester() }
    val tagRequester = remember { BringIntoViewRequester() }
    val detailsRequester = remember { BringIntoViewRequester() }
    val requesters = mapOf(
        "template_selector" to templateRequester,
        "tag_section" to tagRequester,
        "details_section" to detailsRequester
    )

    val onboardingSteps = remember {
        listOf(
            SpotlightStep(
                targetKey = "template_selector",
                title = "テンプレートを使う",
                description = "よく使う予定はテンプレート化しておくと、タイトルや時間、タグなどを一括で読み込めます。"
            ),
            SpotlightStep(
                targetKey = "tag_section",
                title = "タグをつける",
                description = "予定にタグを付けると、カレンダー上での色分けや一覧画面での絞り込みに使えます。"
            ),
            SpotlightStep(
                targetKey = "details_section",
                title = "詳細設定を開く",
                description = "チェックリストや通知、場所、繰り返しなどの細かい設定はここから開けます。"
            )
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (isEditMode) "予定を編集" else "予定を作成",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = attemptClose) {
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
                        onClick = onSaveTask,
                        enabled = !isSaving,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .background(
                                if (isSaving) colorScheme.onSurfaceVariant else colorScheme.primary,
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "保存",
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colorScheme.surface)
            )
        },
        containerColor = colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionLabel("テンプレートを選択")

            // bringIntoViewRequester を追加
            Box(
                modifier = Modifier
                    .bringIntoViewRequester(templateRequester)
                    .onGloballyPositioned { coordinates ->
                        targetRects["template_selector"] = coordinates.boundsInRoot()
                    }
            ) {
                TemplateSelectorRow(
                    selectedTemplateName = selectedTemplateName,
                    onClick = { showTemplateDialog = true }
                )
            }

            RecentTemplatesRow(
                recentTemplates = recentTemplates,
                onTemplateClick = { template ->
                    selectedTemplateName = template.title
                    onApplyTemplate(template)
                }
            )

            TitleSection(
                title = taskState.title,
                isTitleError = isTitleError,
                onTitleChange = { newTitle ->
                    onUpdateInput { current -> current.copy(title = newTitle) }
                }
            )


            TimeSection(
                startTime = taskState.startTime,
                endTime = taskState.endTime,
                isAllDay = taskState.isAllDay,
                isDateTimeError = isDateTimeError,
                onTimeBoxClick = { target -> activeTarget = target },
                onTimeOnlyBoxClick = { target -> activeTarget = target }
            )

            AllDayToggleRow(
                isAllDay = taskState.isAllDay,
                onToggle = { isChecked ->
                    onUpdateInput { current -> current.copy(isAllDay = isChecked) }
                }
            )

            // bringIntoViewRequester を追加
            Column(
                modifier = Modifier
                    .bringIntoViewRequester(tagRequester)
                    .onGloballyPositioned { coordinates ->
                        targetRects["tag_section"] = coordinates.boundsInRoot()
                    }
            ) {
                TagSection(
                    availableTags = availableTags,
                    selectedTags = taskState.selectedTags,
                    isTagFolderExpanded = isTagFolderExpanded,
                    onExpandToggle = { isTagFolderExpanded = !isTagFolderExpanded },
                    onToggleTagSelection = onToggleTagSelection,
                    onUpdateTagOrder = onUpdateTagOrder,
                    onDeleteTagRequest = { tag -> tagToDelete = tag },
                    onEditTagRequest = { tag ->
                        tagToEdit = tag
                        showTagCreateDialog = true
                    },
                    onAddTagClick = {
                        tagToEdit = null
                        showTagCreateDialog = true
                    }
                )
            }

            MemoSection(
                memo = taskState.memo,
                onMemoChange = { newMemo ->
                    onUpdateInput { current -> current.copy(memo = newMemo) }
                }
            )

            ColorSection(
                selectedColor = taskState.color ?: Color(0xFF4285F4).toArgb(),
                onColorSelected = { color ->
                    onUpdateInput { current -> current.copy(color = color) }
                }
            )

            CustomFieldsSection(
                fields = taskState.customFields,
                values = taskState.customFieldValues,
                onValueChange = onCustomFieldValueChange
            )


            Box(
                modifier = Modifier
                    .bringIntoViewRequester(detailsRequester)
                    .onGloballyPositioned { coordinates ->
                        targetRects["details_section"] = coordinates.boundsInRoot()
                    }
            ) {
                ExpandableDetailsSection(
                    isExpanded = isDetailsExpanded && !showOnboarding,
                    onToggle = {
                        if (!showOnboarding) {
                            val expanding = !isDetailsExpanded
                            isDetailsExpanded = expanding
                            if (expanding) {
                                coroutineScope.launch {
                                    // 展開後に詳細設定全体が見える位置まで移動する。
                                    delay(300)
                                    detailsRequester.bringIntoView()
                                }
                            }
                        }
                    }
                ) {
                    ChecklistSection(
                        checkList = taskState.checkList,
                        onCheckListChange = { newList ->
                            onUpdateInput { current -> current.copy(checkList = newList) }
                        }
                    )

                    ReminderSection(
                        reminderSetting = taskState.reminderSetting,
                        isAllDay = taskState.isAllDay,
                        onReminderSettingChange = { newSetting ->
                            onUpdateInput { current -> current.copy(reminderSetting = newSetting) }
                        },
                        onNotificationPermissionNeeded = onNotificationPermissionNeeded
                    )

                    LocationSection(
                        locationName = taskState.locationName,
                        locationAddress = taskState.locationAddress,
                        latitude = taskState.latitude,
                        longitude = taskState.longitude,
                        onSelectClick = { showLocationSearchDialog = true },
                        onEditClick = { showLocationSearchDialog = true },
                        onDeleteClick = onClearLocation
                    )

                    AutoCompleteSection(
                        isAutoCompleted = taskState.isAutoCompleted,
                        onAutoCompletedChange = { isAuto ->
                            onUpdateInput { current -> current.copy(isAutoCompleted = isAuto) }
                        },
                        onInfoClick = { showInfoDialog = true }
                    )

                    Column(
                        modifier = Modifier
                            .bringIntoViewRequester(recurrenceBringIntoViewRequester)
                    ) {
                        RecurrenceSection(
                            recurrenceType = taskState.recurrenceType,
                            intervalDays = taskState.recurrenceIntervalDays,
                            nth = taskState.recurrenceNth,
                            weekday = taskState.recurrenceWeekday,
                            weekdays = taskState.recurrenceWeekdays,
                            endDateMillis = taskState.recurrenceEndTime,
                            baseDateMillis = taskState.startTime,
                            onTypeChange = { type ->
                                onUpdateInput { current -> current.copy(recurrenceType = type) }
                                if (type != RecurrenceType.NONE) {
                                    coroutineScope.launch {
                                        delay(300)
                                        recurrenceBringIntoViewRequester.bringIntoView()
                                    }
                                }
                            },
                            onIntervalDaysChange = { days ->
                                onUpdateInput { current -> current.copy(recurrenceIntervalDays = days) }
                            },
                            onNthChange = { nth ->
                                onUpdateInput { current -> current.copy(recurrenceNth = nth) }
                            },
                            onWeekdayChange = { weekday ->
                                onUpdateInput { current -> current.copy(recurrenceWeekday = weekday) }
                            },
                            onWeekdaysToggle = { day ->
                                onUpdateInput { current ->
                                    val newSet = if (day in current.recurrenceWeekdays) {
                                        current.recurrenceWeekdays - day
                                    } else {
                                        current.recurrenceWeekdays + day
                                    }
                                    current.copy(recurrenceWeekdays = newSet)
                                }
                            },
                            onEndDateClick = { activeTarget = "RECURRENCE_END" }
                        )
                    }
                }
            }
        }
    }

    // ================================================================
    // ダイアログ群
    // ================================================================

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("自動ステータス変更とは", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    "タスクの終了時刻を過ぎたとき、自動で「完了済み」に変更します。\n\n" +
                            "ミーティングや授業など、時間が終われば完了とみなせる予定に向いています。\n\n" +
                            "【ONがおすすめ】\n" +
                            "・ミーティング\n" +
                            "・授業\n" +
                            "・イベント\n\n" +
                            "【OFFがおすすめ】\n" +
                            "・書類提出\n" +
                            "・レポート作成\n" +
                            "・宿題"
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("閉じる", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (tagToDelete != null) {
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("タグの削除", fontWeight = FontWeight.Bold) },
            text = { Text("タグ「${tagToDelete?.name}」を削除しますか？\n(この操作は取り消せません)") },
            dismissButton = {
                TextButton(onClick = { tagToDelete = null }) { Text("キャンセル") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        tagToDelete?.let { target -> onDeleteTag(target) }
                        tagToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
                ) { Text("削除する") }
            }
        )
    }

    if (templateToDelete != null) {
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = { Text("テンプレートの削除", fontWeight = FontWeight.Bold) },
            text = { Text("テンプレート「${templateToDelete?.title}」を削除しますか？\n(この操作は取り消せません)") },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) { Text("キャンセル") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        templateToDelete?.let { target -> onDeleteTemplate(target) }
                        templateToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
                ) { Text("削除する") }
            }
        )
    }

    if (showTagCreateDialog) {
        var editCustomFields by remember(tagToEdit) { mutableStateOf<List<String>>(emptyList()) }
        LaunchedEffect(tagToEdit) {
            tagToEdit?.let { tag ->
                editCustomFields = onLoadCustomFieldsForTag(tag.tagId)
            }
        }

        TagCreateDialog(
            confirmDiscardChanges = confirmDiscardChanges,
            existingTag = tagToEdit,
            existingCustomFields = editCustomFields,
            onDismissRequest = {
                showTagCreateDialog = false
                tagToEdit = null
            },
            onTagSave = { name, iconSource, color, customFields ->
                val iconString = when (iconSource) {
                    is TagIconSource.InitialText -> null
                    is TagIconSource.Vector -> iconSource.iconId.id
                }

                val editing = tagToEdit
                if (editing != null) {
                    onUpdateTag(
                        editing.copy(
                            name = name,
                            color = color.toArgb(),
                            icon = iconString
                        ),
                        customFields
                    )
                } else {
                    val newTag = Tag(
                        tagId = 0L,
                        name = name,
                        color = color.toArgb(),
                        icon = iconString
                    )

                    onCreateTag(
                        newTag,
                        customFields
                    )
                }
                showTagCreateDialog = false
                tagToEdit = null
            }
        )
    }

    if (showTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text("テンプレートを選択", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(fieldBackgroundColor)
                            .clickable {
                                showTemplateDialog = false
                                onNavigateToTemplateCreate()
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = templateIconColor)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "新規テンプレートを作成する", fontSize = 14.sp, color = colorScheme.onSurface)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(fieldBackgroundColor)
                            .clickable {
                                selectedTemplateName = "テンプレートを選択"
                                onUpdateInput { current -> current.copy(title = "", memo = "", checkList = emptyList()) }
                                showTemplateDialog = false
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "適用しない", fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
                    }

                    if (templates.isNotEmpty()) {
                        Text(
                            text = "すべてのテンプレート",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )

                        DraggableTemplateList(
                            templates = templates,
                            onOrderChanged = onUpdateTemplateOrder,
                            onTemplateClick = { template ->
                                selectedTemplateName = template.title
                                onApplyTemplate(template)
                                showTemplateDialog = false
                            },
                            onEditClick = { template ->
                                showTemplateDialog = false
                                onNavigateToTemplateEdit(template.templateId)
                            },
                            onDeleteClick = { template ->
                                templateToDelete = template
                            },
                            fieldBackgroundColor = fieldBackgroundColor,
                            templateIconColor = templateIconColor
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplateDialog = false }) { Text("閉じる") }
            }
        )
    }

    if (activeTarget == "RECURRENCE_END") {
        DateTimePickerWizard(
            target = activeTarget!!,
            isAllDay = true,
            onDismiss = { activeTarget = null },
            onDateTimeSelected = { finalDateTimeLong ->
                onUpdateInput { current -> current.copy(recurrenceEndTime = finalDateTimeLong) }
                activeTarget = null
            }
        )
    } else if (activeTarget == "START_DATE" || activeTarget == "END_DATE") {
        val isStart = activeTarget == "START_DATE"
        val baseMillis = if (isStart) taskState.startTime else taskState.endTime
        val baseDate = Instant.ofEpochSecond(baseMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        SimpleDatePickerDialog(
            initialDateMillis = baseDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            title = if (isStart) "開始日を設定" else "終了日を設定",
            onDismiss = { activeTarget = null },
            onDateSelected = { newDate ->
                onUpdateInput { current ->
                    val baseTime = if (isStart) current.startTime else current.endTime
                    val timeOfDay = Instant.ofEpochSecond(baseTime).atZone(ZoneId.systemDefault()).toLocalTime()
                    val newEpoch = LocalDateTime.of(newDate, timeOfDay)
                        .atZone(ZoneId.systemDefault())
                        .toEpochSecond()
                    if (isStart) current.copy(startTime = newEpoch) else current.copy(endTime = newEpoch)
                }
                activeTarget = null
            }
        )
    } else if (activeTarget == "START_TIME" || activeTarget == "END_TIME") {
        val isStart = activeTarget == "START_TIME"
        val baseMillis = if (isStart) taskState.startTime else taskState.endTime
        val baseLocalTime = Instant.ofEpochSecond(baseMillis).atZone(ZoneId.systemDefault()).toLocalTime()
        SimpleTimePickerDialog(
            initialHour = baseLocalTime.hour,
            initialMinute = baseLocalTime.minute,
            title = if (isStart) "開始時刻を設定" else "終了時刻を設定",
            onDismiss = { activeTarget = null },
            onTimeSelected = { hour, minute ->
                onUpdateInput { current ->
                    val baseTime = if (isStart) current.startTime else current.endTime
                    val date = Instant.ofEpochSecond(baseTime).atZone(ZoneId.systemDefault()).toLocalDate()
                    val newEpoch = LocalDateTime.of(date, LocalTime.of(hour, minute))
                        .atZone(ZoneId.systemDefault())
                        .toEpochSecond()
                    if (isStart) current.copy(startTime = newEpoch) else current.copy(endTime = newEpoch)
                }
                activeTarget = null
            }
        )
    }


    if (showLocationSearchDialog) {
        LocationSearchDialog(
            searchResults = osmSearchResults,
            isSearching = isOsmSearching,
            onSearch = onSearchOsmPoi,
            onSelect = { poi ->
                onSelectOsmPoi(poi)
                showLocationSearchDialog = false
            },
            onDismiss = { showLocationSearchDialog = false },
            onSaveTextLocation = { name ->
                onUpdateInput { current ->
                    current.copy(locationName = name, locationAddress = null)
                }
            },
        )
    }

    if (showOnboarding && !onboardingDismissed) {
        SpotlightOnboardingOverlay(
            steps = onboardingSteps,
            targetRects = targetRects,
            introTitle = "予定作成のガイドを開始します",
            introDescription = "テンプレート・タグ・詳細設定について、簡単にご紹介します。",
            onSkip = {
                onboardingDismissed = true
                onOnboardingFinished()
            },
            onShowLater = {
                onboardingDismissed = true
            },
            onFinish = {
                onboardingDismissed = true
                onOnboardingFinished()
            },
            // ステップが切り替わったときのスクロール処理
            onStepShown = { step ->
                coroutineScope.launch {
                    delay(100) // 描画完了を待つためのごく短い待機
                    requesters[step.targetKey]?.bringIntoView()
                }
            }
        )
    }
    if (showAllTutorialsCompletedDialog) {
        AlertDialog(
            onDismissRequest = onDismissAllTutorialsCompletedDialog,
            title = { Text("すべてのチュートリアルが完了しました", fontWeight = FontWeight.Bold) },
            text = { Text("チュートリアルを再度確認したい場合、設定画面から再度有効にすることができます。") },
            confirmButton = {
                TextButton(onClick = onDismissAllTutorialsCompletedDialog) { Text("OK") }
            }
        )
    }

    if (showDiscardConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmDialog = false },
            title = { Text("編集内容を破棄しますか？", fontWeight = FontWeight.Bold) },
            text = { Text("このまま閉じると、入力した内容は保存されません。") },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmDialog = false }) { Text("キャンセル") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardConfirmDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
                ) { Text("閉じる") }
            }
        )
    }
}