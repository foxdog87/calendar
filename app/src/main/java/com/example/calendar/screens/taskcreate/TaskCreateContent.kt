package com.example.calendar.screens.taskcreate

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.calendar.components.TagCreateDialog
import com.example.calendar.components.TagIconSource
import com.example.calendar.components.LocationSearchDialog
import com.example.calendar.components.DateTimePickerWizard
import com.example.calendar.components.DraggableTemplateList
import com.example.calendar.data.entity.Tag
import com.example.calendar.data.entity.Template
import com.example.calendar.data.osm.model.OsmPoi
import com.example.calendar.state.TaskInputState


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TaskCreateContent(
    taskState: TaskInputState,
    availableTags: List<Tag>,
    templates: List<Template>,
    recentTemplates: List<Template>, // ★ 追加
    isTitleError: Boolean = false,
    isDateTimeError: Boolean = false,
    onNavigateBack: () -> Unit,
    onSaveTask: () -> Unit,
    onUpdateInput: ((TaskInputState) -> TaskInputState) -> Unit,
    onToggleTagSelection: (Tag) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    onCreateTag: (Tag) -> Unit,
    onApplyTemplate: (Template) -> Unit,
    onUpdateTagOrder: (List<Tag>) -> Unit,
    onUpdateTemplateOrder: (List<Template>) -> Unit,
    onNavigateToTemplateCreate: () -> Unit,
    osmSearchResults: List<OsmPoi> = emptyList(),
    isOsmSearching: Boolean = false,
    onSearchOsmPoi: (String) -> Unit = {},
    onSelectOsmPoi: (OsmPoi) -> Unit = {},
    onClearLocation: () -> Unit = {},
    onDeleteTemplate: (Template) -> Unit,             // ★ 追加
    onNavigateToTemplateEdit: (Long) -> Unit
) {
    // ...
    // ★ 削除: val templates = remember { listOf(Template(...)) }
    // ... {
    val scrollState = rememberScrollState()

    val fieldBackgroundColor = Color(0xFFFAFAFA)
    val templateIconColor = Color(0xFF6200EE)

    var activeTarget by remember { mutableStateOf<String?>(null) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var selectedTemplateName by remember { mutableStateOf("テンプレートを選択") }

    var showTagCreateDialog by remember { mutableStateOf(false) }
    var isTagFolderExpanded by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<Tag?>(null) }

    var showLocationSearchDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // State追加
    var templateToDelete by remember { mutableStateOf<Template?>(null) }

    val sampleTemplates = remember {
        listOf(
            Template(
                templateId = 1L,
                title = "【宿題】期日：",
                icon = "Assignment",
                timeLength = 3600L,
                description = "学校の課題提出用のテンプレートです",
                color = Color(0xFF4285F4).value.toInt(),
                memo = "提出先：\n持ち物：",
                locationName = "東京駅",
                locationAddress = "東京都千代田区丸の内1丁目",
                dayCountTarget = null,
                url = "",
                attachmentPath = "",
                isAutoCompleted = false
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
                        color = Color(0xFF1C1B1F)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "閉じる",
                            tint = Color(0xFF1C1B1F),
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
                            .background(Color(0xFF1A73E8), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "保存",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
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


            TemplateSelectorRow(
                    selectedTemplateName = selectedTemplateName,
            onClick = { showTemplateDialog = true }
            )

            // ★ 追加：テンプレート選択欄の直下に「最近使用した3件」を表示

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

            AllDayToggleRow(
                isAllDay = taskState.isAllDay,
                onToggle = { isChecked ->
                    onUpdateInput { current -> current.copy(isAllDay = isChecked) }
                }
            )

            TimeSection(
                startTime = taskState.startTime,
                endTime = taskState.endTime,
                isAllDay = taskState.isAllDay,
                isDateTimeError = isDateTimeError,
                onTimeBoxClick = { target -> activeTarget = target }
            )

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

            MemoSection(
                memo = taskState.memo,
                onMemoChange = { newMemo ->
                    onUpdateInput { current -> current.copy(memo = newMemo) }
                }
            )

            ChecklistSection(
                checkList = taskState.checkList,
                onCheckListChange = { newList ->
                    onUpdateInput { current -> current.copy(checkList = newList) }
                }
            )

            ReminderSection(
                reminderSetting = taskState.reminderSetting,
                onReminderSettingChange = { newSetting ->
                    onUpdateInput { current -> current.copy(reminderSetting = newSetting) }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            LocationSection(
                locationName = taskState.locationName,
                locationAddress = taskState.locationAddress,
                latitude = taskState.latitude,
                longitude = taskState.longitude,
                onSelectClick = { showLocationSearchDialog = true },
                onEditClick = { showLocationSearchDialog = true },
                onDeleteClick = onClearLocation
            )

            AutoCompleteAndColorSection(
                isAutoCompleted = taskState.isAutoCompleted,
                onAutoCompletedChange = { isAuto ->
                    onUpdateInput { current -> current.copy(isAutoCompleted = isAuto) }
                },
                onInfoClick = { showInfoDialog = true },
                selectedColor = taskState.color ?: Color(0xFF4285F4).toArgb(),
                onColorSelected = { color ->
                    onUpdateInput { current -> current.copy(color = color) }
                }
            )
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("削除する") }
            }
        )
    }

    if (showTagCreateDialog) {
        TagCreateDialog(
            onDismissRequest = { showTagCreateDialog = false },
            onTagSave = { name, iconSource, color, _ ->
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

                onCreateTag(newTag)
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



                    // --- 新規テンプレートを作成する ---
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
                        Text(text = "新規テンプレートを作成する", fontSize = 14.sp, color = Color(0xFF1C1B1F))
                    }

                    // --- 適用しない ---
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
                        Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFF5F6368))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "適用しない", fontSize = 14.sp, color = Color(0xFF5F6368))
                    }

                    // --- すべてのテンプレート(重複除去なし・ドラッグ並び替え可) ---
                    if (templates.isNotEmpty()) {
                        Text(
                            text = "すべてのテンプレート",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF70757A),
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
                                onNavigateToTemplateEdit(template.templateId) // ★ 追加
                            },
                            onDeleteClick = { template ->
                                templateToDelete = template // ダイアログ内はそのまま、確認ダイアログを別途出す
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
            isAllDay = taskState.isAllDay,
            onDismiss = { activeTarget = null },
            onDateTimeSelected = { finalDateTimeLong ->
                onUpdateInput { current ->
                    if (activeTarget == "START") current.copy(startTime = finalDateTimeLong)
                    else current.copy(endTime = finalDateTimeLong)
                }
                activeTarget = null
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("削除する") }
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
}
