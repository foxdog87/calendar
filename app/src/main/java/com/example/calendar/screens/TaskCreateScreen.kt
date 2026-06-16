package com.example.calendar.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.viewmodel.CalendarViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

// ==========================================
// ドメインエンティティ定義（レポートのER図に完全準拠）
// ==========================================
data class TaskTemplate(val name: String, val defaultTitle: String, val defaultMemo: String)

data class TagEntity(
    val tagId: Long,
    val name: String,
    val color: Color,
    val icon: ImageVector? = null,
    val customFields: List<TagCustomFieldEntity> = emptyList()
)

data class TagCustomFieldEntity(
    val fieldId: Long,
    val tagId: Long,
    val fieldName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCreateScreen(
    viewModel: CalendarViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val taskState = viewModel.inputState
    val scrollState = rememberScrollState()

    val fieldBackgroundColor = Color(0xFFF7F7F7)
    val subLabelColor = Color.Gray

    // 局所UI状態管理
    var activeTarget by remember { mutableStateOf<String?>(null) }
    var isAllDay by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var selectedTemplateName by remember { mutableStateOf("選択してください") }
    var isDetailExpanded by remember { mutableStateOf(false) } // 詳細設定の開閉フラグ

    // レポート仕様に完全準拠した状態管理（表3: 属性群）
    var selectedTagIds by remember { mutableStateOf(setOf<Long>()) }
    var customFieldValues by remember { mutableStateOf(mapOf<Long, String>()) }
    var urlValue by remember { mutableStateOf("") }
    var locationValue by remember { mutableStateOf("") }
    var countdownValue by remember { mutableStateOf("") }
    var attachmentPathValue by remember { mutableStateOf("") }
    var isAutoCompletedState by remember { mutableStateOf(true) }

    // マスタデータ
    val availableTags = remember {
        listOf(
            TagEntity(
                tagId = 1L, name = "提出物", color = Color(0xFF1976D2), icon = Icons.Default.Edit,
                customFields = listOf(TagCustomFieldEntity(101L, 1L, "提出先・プラットフォーム"))
            ),
            TagEntity(
                tagId = 2L, name = "重要", color = Color(0xFFD32F2F), icon = Icons.Default.ErrorOutline,
                customFields = listOf(TagCustomFieldEntity(102L, 2L, "リカバリープラン"))
            ),
            TagEntity(tagId = 3L, name = "数学", color = Color.Black),
            TagEntity(tagId = 4L, name = "レポート", color = Color.Black)
        )
    }

    val templates = remember {
        listOf(
            TaskTemplate("学校の課題提出", "【宿題】期日：", "提出先：\n持ち物："),
            TaskTemplate("定期ミーティング", "定例進捗確認会", "アジェンダ：\n1. 進捗共有\n2. 課題の相談")
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("予定を作成", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.Close, contentDescription = "閉じる") }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.saveTask()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "保存", tint = Color.Black)
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

            // --- 【1】テンプレート選択 ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(fieldBackgroundColor)
                    .clickable { showTemplateDialog = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = selectedTemplateName, modifier = Modifier.weight(1f))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
            }

            // --- 【2】タイトル入力 ---
            SectionLabel("タイトル")
            MockupTextField(
                value = taskState.title,
                onValueChange = { title -> viewModel.updateInput { it.copy(title = title) } },
                placeholder = "タイトルを入力",
                backgroundColor = fieldBackgroundColor
            )

            // --- 【3】時間設定 ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionLabel("開始")
                            TimeDisplayBox(dateTime = taskState.startTime, backgroundColor = fieldBackgroundColor, onClick = { activeTarget = "START" })
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            SectionLabel("終了")
                            TimeDisplayBox(dateTime = taskState.endTime, backgroundColor = fieldBackgroundColor, onClick = { activeTarget = "END" })
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 28.dp)) {
                    Text("終日", fontSize = 11.sp, color = Color.Gray)
                    Switch(
                        checked = isAllDay,
                        onCheckedChange = { allDayChecked ->
                            isAllDay = allDayChecked
                            if (allDayChecked) {
                                viewModel.updateInput {
                                    it.copy(startTime = it.startTime.with(LocalTime.MIN), endTime = it.endTime.with(LocalTime.MAX))
                                }
                            }
                        }
                    )
                }
            }

            // --- 【4】タグセクション ---
            SectionLabel("タグ")
            Text("選択したタグ", fontSize = 12.sp, color = subLabelColor, modifier = Modifier.padding(bottom = 8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                availableTags.forEach { tag ->
                    val isSelected = selectedTagIds.contains(tag.tagId)
                    TagChip(
                        label = tag.name,
                        bgColor = if (isSelected) tag.color.copy(alpha = 0.15f) else fieldBackgroundColor,
                        textColor = if (isSelected) tag.color else Color.Black,
                        icon = tag.icon,
                        isSelected = isSelected,
                        onClick = {
                            selectedTagIds = if (isSelected) selectedTagIds - tag.tagId else selectedTagIds + tag.tagId
                        }
                    )
                }
            }

            // タグ連動カスタムフィールドの展開
            val activeCustomFields = remember(selectedTagIds) {
                availableTags.filter { selectedTagIds.contains(it.tagId) }.flatMap { it.customFields }
            }

            AnimatedVisibility(
                visible = activeCustomFields.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    activeCustomFields.forEach { field ->
                        SectionLabel(text = "【タグ連動入力】${field.fieldName}")
                        MockupTextField(
                            value = customFieldValues[field.fieldId] ?: "",
                            onValueChange = { newValue ->
                                customFieldValues = customFieldValues + (field.fieldId to newValue)
                            },
                            placeholder = "${field.fieldName}の情報を入力",
                            backgroundColor = Color(0xFFEBF3FC)
                        )
                    }
                }
            }

            // --- 【5】メモ ---
            SectionLabel("メモ")
            MockupTextField(
                value = taskState.memo,
                onValueChange = { memo -> viewModel.updateInput { it.copy(memo = memo) } },
                placeholder = "メモを入力",
                backgroundColor = fieldBackgroundColor,
                minLines = 3
            )

            // --- 【6】チェックリスト ---
            SectionLabel("チェックリスト")
            Button(
                onClick = { /* TODO */ },
                colors = ButtonDefaults.buttonColors(containerColor = fieldBackgroundColor, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("項目を追加")
                }
            }

            // --- 【7】詳細設定 (アコーディオンの開閉挙動) ---
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isDetailExpanded = !isDetailExpanded }
                    .padding(vertical = 4.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("詳細設定", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (isDetailExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // 単一責任の原則に基づき、開閉状態に応じてアニメーション表示
            AnimatedVisibility(
                visible = isDetailExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 添付ファイル (attachmentPath)
                    DetailItem(Icons.Default.AttachFile, "添付ファイル") {
                        Button(
                            onClick = { attachmentPathValue = "content://media/external/file/sample" },
                            colors = ButtonDefaults.buttonColors(containerColor = fieldBackgroundColor, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if(attachmentPathValue.isEmpty()) "ファイルを追加" else "選択済み", fontSize = 12.sp)
                        }
                    }

                    // URL入力
                    DetailInputItem(
                        icon = Icons.Default.Link,
                        label = "URL",
                        value = urlValue,
                        onValueChange = { urlValue = it },
                        placeholder = "URLを入力",
                        bgColor = fieldBackgroundColor
                    )

                    // 位置情報 (latitude, longitude)
                    DetailInputItem(
                        icon = Icons.Default.LocationOn,
                        label = "位置情報",
                        value = locationValue,
                        onValueChange = { locationValue = it },
                        placeholder = "場所を入力",
                        bgColor = fieldBackgroundColor
                    )

                    // カウントダウン (dayCountTarget)
                    DetailInputItem(
                        icon = Icons.Default.HourglassEmpty,
                        label = "カウントダウン",
                        value = countdownValue,
                        onValueChange = { countdownValue = it },
                        placeholder = "目標日時を入力",
                        bgColor = fieldBackgroundColor
                    )
                }
            }

            // --- 【8】完了時設定 ＆ カラーパレット ---
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("完了時の自動ステータス変更", modifier = Modifier.weight(1f), fontSize = 14.sp)
                Icon(Icons.Default.Info, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp).padding(end = 8.dp))
                // レポート属性 isAutoCompleted と連動
                Switch(checked = isAutoCompletedState, onCheckedChange = { isAutoCompletedState = it })
            }

            SectionLabel("色")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 32.dp)) {
                val colors = listOf(Color(0xFFFFCDD2), Color(0xFFF8BBD0), Color(0xFFE1BEE7), Color(0xFFD1C4E9), Color(0xFFC5CAE9), Color(0xFFBBDEFB), Color(0xFFB2EBF2))
                colors.forEachIndexed { index, color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(if(index == 0) 2.dp else 0.dp, Color.Gray, CircleShape)
                    )
                }
            }
        }
    }

    // 各種ダイアログ処理
    if (showTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text("テンプレートを選択", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    templates.forEach { template ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(fieldBackgroundColor)
                                .clickable {
                                    selectedTemplateName = template.name
                                    viewModel.updateInput { it.copy(title = template.defaultTitle, memo = template.defaultMemo) }
                                    showTemplateDialog = false
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = template.name, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTemplateDialog = false }) { Text("閉じる") } }
        )
    }

    if (activeTarget != null) {
        DateTimePickerWizard(
            target = activeTarget!!,
            onDismiss = { activeTarget = null },
            onDateTimeSelected = { finalDateTime ->
                viewModel.updateInput {
                    if (activeTarget == "START") it.copy(startTime = finalDateTime) else it.copy(endTime = finalDateTime)
                }
                activeTarget = null
            }
        )
    }
}

// ==========================================
// 共通UIコンポーネント（重複排除・完全型安全化）
// ==========================================

@Composable
fun SectionLabel(text: String) {
    Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
}

@Composable
fun MockupTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    backgroundColor: Color,
    minLines: Int = 1
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.Gray) },
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = backgroundColor,
            unfocusedContainerColor = backgroundColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        minLines = minLines
    )
}

@Composable
fun TimeDisplayBox(dateTime: LocalDateTime, backgroundColor: Color, onClick: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy/MM/dd (E) HH:mm", Locale.JAPANESE) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(dateTime.format(formatter), fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
fun TagChip(
    label: String,
    bgColor: Color,
    textColor: Color,
    icon: ImageVector? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) BorderStroke(1.dp, textColor) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(label, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun DetailItem(icon: ImageVector, label: String, content: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
        content()
    }
}

@Composable
fun DetailInputItem(
    icon: ImageVector,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    bgColor: Color
) {
    DetailItem(icon, label) {
        Box(
            modifier = Modifier
                .width(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder, color = Color.Gray, fontSize = 12.sp) },
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                maxLines = 1,
                singleLine = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerWizard(
    target: String,
    onDismiss: () -> Unit,
    onDateTimeSelected: (LocalDateTime) -> Unit
) {
    var isTimeStep by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = Instant.now().toEpochMilli())
    val now = LocalTime.now()
    val timeState = rememberTimePickerState(initialHour = now.hour, initialMinute = now.minute, is24Hour = true)

    if (!isTimeStep) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = { isTimeStep = true }) { Text("次へ (時間選択)") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
        ) { DatePicker(state = dateState) }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = dateState.selectedDateMillis ?: Instant.now().toEpochMilli()
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        val time = LocalTime.of(timeState.hour, timeState.minute)
                        onDateTimeSelected(LocalDateTime.of(date, time))
                    }
                ) { Text("決定") }
            },
            dismissButton = { TextButton(onClick = { isTimeStep = false }) { Text("戻る") } },
            title = { Text(if (target == "START") "開始時間を設定" else "終了時間を設定") },
            text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = timeState) } }
        )
    }
}