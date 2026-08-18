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
import java.time.ZoneId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.mutableStateMapOf
import com.foxdog.strucalendar.components.SpotlightOnboardingOverlay
import com.foxdog.strucalendar.components.SpotlightShape
import com.foxdog.strucalendar.components.SpotlightStep
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TaskCreateContent(
    taskState: TaskInputState,
    availableTags: List<Tag>,
    templates: List<Template>,
    recentTemplates: List<Template>,
    isTitleError: Boolean = false,
    isDateTimeError: Boolean = false,
    onNavigateBack: () -> Unit,
    onSaveTask: () -> Unit,
    onUpdateInput: ((TaskInputState) -> TaskInputState) -> Unit,
    onToggleTagSelection: (Tag) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    onCreateTag: (Tag, List<String>) -> Unit,
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
    onOnboardingFinished: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val calColors = MaterialTheme.calendarColors

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope() // ★ 追加

    val fieldBackgroundColor = colorScheme.surfaceVariant
    val templateIconColor = calColors.templateAccent


    var isDetailsExpanded by remember(alwaysShowDetailedSettings) {
        mutableStateOf(alwaysShowDetailedSettings)
    }

    var activeTarget by remember { mutableStateOf<String?>(null) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var selectedTemplateName by remember { mutableStateOf("テンプレートを選択") }

    var showTagCreateDialog by remember { mutableStateOf(false) }
    var isTagFolderExpanded by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<Tag?>(null) }

    var showLocationSearchDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    var templateToDelete by remember { mutableStateOf<Template?>(null) }

    val targetRects = remember { mutableStateMapOf<String, Rect>() }
    var onboardingDismissed by remember { mutableStateOf(false) }

    // ★ 追加：スクロール用の Requester をステップごとに用意
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
                        "予定を作成",
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
                        onClick = onSaveTask,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .background(colorScheme.primary, CircleShape)
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionLabel("テンプレートを選択")

            // ★ 修正：bringIntoViewRequester を追加
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
                onTimeBoxClick = { target -> activeTarget = target }
            )

            AllDayToggleRow(
                isAllDay = taskState.isAllDay,
                onToggle = { isChecked ->
                    onUpdateInput { current -> current.copy(isAllDay = isChecked) }
                }
            )

            // ★ 修正：bringIntoViewRequester を追加
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
                    onAddTagClick = { showTagCreateDialog = true }
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

            // （※以前の質問で追加されたカスタム項目セクションをここに配置）
            /* CustomFieldsSection(
                fields = taskState.customFields,
                values = taskState.customFieldValues,
                onValueChange = onCustomFieldValueChange
            ) */

            // ★ 修正：bringIntoViewRequester を追加
            Box(
                modifier = Modifier
                    .bringIntoViewRequester(detailsRequester)
                    .onGloballyPositioned { coordinates ->
                        targetRects["details_section"] = coordinates.boundsInRoot()
                    }
            ) {
                ExpandableDetailsSection(
                    isExpanded = isDetailsExpanded && !showOnboarding,
                    onToggle = { if (!showOnboarding) isDetailsExpanded = !isDetailsExpanded }
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
                        }
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

                    RecurrenceSection(
                        recurrenceType = taskState.recurrenceType,
                        intervalDays = taskState.recurrenceIntervalDays,
                        nth = taskState.recurrenceNth,
                        weekday = taskState.recurrenceWeekday,
                        endDateMillis = taskState.recurrenceEndTime,
                        baseDateMillis = taskState.startTime,
                        onTypeChange = { type ->
                            onUpdateInput { current -> current.copy(recurrenceType = type) }
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
                        onEndDateClick = { activeTarget = "RECURRENCE_END" }
                    )
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

    if (showTagCreateDialog) {
        TagCreateDialog(
            onDismissRequest = { showTagCreateDialog = false },
            onTagSave = { name, iconSource, color, customFields ->
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
                    customFields
                )
                showTagCreateDialog = false
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

    if (activeTarget != null) {
        DateTimePickerWizard(
            target = activeTarget!!,
            isAllDay = if (activeTarget == "RECURRENCE_END") true else taskState.isAllDay,
            onDismiss = { activeTarget = null },
            onDateTimeSelected = { finalDateTimeLong ->
                onUpdateInput { current ->
                    when (activeTarget) {
                        "START" -> {
                            val newStartDate = Instant.ofEpochSecond(finalDateTimeLong)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()

                            val endTimeOfDay = Instant.ofEpochSecond(current.endTime)
                                .atZone(ZoneId.systemDefault())
                                .toLocalTime()

                            val newEnd = LocalDateTime.of(newStartDate, endTimeOfDay)
                                .atZone(ZoneId.systemDefault())
                                .toEpochSecond()

                            current.copy(startTime = finalDateTimeLong, endTime = newEnd)
                        }
                        "RECURRENCE_END" -> current.copy(recurrenceEndTime = finalDateTimeLong)
                        else -> current.copy(endTime = finalDateTimeLong)
                    }
                }
                activeTarget = null
            }
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
            // ★ 追加：ステップが切り替わったときのスクロール処理
            onStepShown = { step ->
                coroutineScope.launch {
                    delay(100) // 描画完了を待つためのごく短い待機
                    requesters[step.targetKey]?.bringIntoView()
                }
            }
        )
    }
}