package com.example.calendar.screens.taskcreate

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.components.DraggableTagList
import com.example.calendar.data.entity.ChecklistItem
import com.example.calendar.data.entity.Tag
import com.example.calendar.notification.ReminderOption
import com.example.calendar.notification.ReminderSetting
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import com.example.calendar.components.TagIconId
import com.example.calendar.data.entity.Template


// ============================================================
// 共通の小さいUIパーツ
// ============================================================

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1C1B1F),
        modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
    )
}

@Composable
fun WireframeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1,
    isError: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color(0xFF9AA0A6), fontSize = 14.sp) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        isError = isError,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF1A73E8),
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            errorBorderColor = MaterialTheme.colorScheme.error
        ),
        minLines = minLines,
        textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF1C1B1F)),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
    )
}

@Composable
fun WireframeDetailTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color(0xFF9AA0A6), fontSize = 13.sp) },
        modifier = Modifier.width(220.dp),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF1A73E8),
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        maxLines = 1,
        textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF1C1B1F)),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
    )
}

@Composable
fun TimeDisplayBox(
    dateTimeMillis: Long,
    isAllDay: Boolean,
    onClick: () -> Unit
) {
    val localDateTime = remember(dateTimeMillis) {
        LocalDateTime.ofInstant(Instant.ofEpochSecond(dateTimeMillis), ZoneId.systemDefault())
    }

    val formatter = remember(isAllDay) {
        if (isAllDay) {
            DateTimeFormatter.ofPattern("yyyy/MM/dd (E)", Locale.JAPANESE)
        } else {
            DateTimeFormatter.ofPattern("yyyy/MM/dd (E) HH:mm", Locale.JAPANESE)
        }
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(localDateTime.format(formatter), fontSize = 13.sp, color = Color(0xFF1C1B1F), maxLines = 1)
        }
    }
}

@Composable
fun DetailRowItem(
    icon: ImageVector,
    label: String,
    content: @Composable () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFF1C1B1F))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = Color(0xFF1C1B1F), modifier = Modifier.weight(1f))
        content()
    }
}

// ============================================================
// 【1】テンプレート選択行
// ============================================================

@Composable
fun TemplateSelectorRow(
    selectedTemplateName: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, Color(0xFFE8E5FF)), RoundedCornerShape(8.dp))
            .background(Color(0xFFF8E5FF))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF6200EE), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = selectedTemplateName, fontSize = 14.sp, color = Color(0xFF1C1B1F), modifier = Modifier.weight(1f))
        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF5F6368))
    }
}

// ============================================================
// テンプレート選択欄の直下に出す「最近使用したテンプレート」
// 常に3スロット分の幅を確保し、埋まっている分だけ左詰めで表示する。
// ============================================================

private const val RECENT_TEMPLATE_SLOT_COUNT = 3

@Composable
fun RecentTemplatesRow(
    recentTemplates: List<Template>,
    onTemplateClick: (Template) -> Unit
) {
    if (recentTemplates.isEmpty()) return

    SectionLabel("最近使用したテンプレート")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(RECENT_TEMPLATE_SLOT_COUNT) { index ->
            val template = recentTemplates.getOrNull(index)

            if (template != null) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1ECFB))
                        .clickable { onTemplateClick(template) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFF6200EE),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = template.title,
                        fontSize = 12.sp,
                        color = Color(0xFF1C1B1F),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            } else {
                // 空きスロット：レイアウト幅だけ確保して何も描画しない
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

// ============================================================
// 【2】タイトル入力
// ============================================================

@Composable
fun TitleSection(
    title: String,
    isTitleError: Boolean,
    onTitleChange: (String) -> Unit,
    label: String = "タイトル (必須)",
    placeholder: String = "タイトルを入力",
    errorMessage: String = "タイトルは必須入力です"
) {
    SectionLabel(label)
    WireframeTextField(
        value = title,
        onValueChange = onTitleChange,
        placeholder = placeholder,
        isError = isTitleError
    )
    if (isTitleError) {
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )
    }
}

// ============================================================
// 終日設定トグル
// ============================================================

@Composable
fun AllDayToggleRow(
    isAllDay: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFF5F6368), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("終日予定にする", fontSize = 14.sp, color = Color(0xFF1C1B1F))
        }
        Switch(
            checked = isAllDay,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF1A73E8)),
            modifier = Modifier.scale(0.85f)
        )
    }
}

// ============================================================
// 【3】時間設定
// ============================================================

@Composable
fun TimeSection(
    startTime: Long,
    endTime: Long,
    isAllDay: Boolean,
    isDateTimeError: Boolean,
    onTimeBoxClick: (target: String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            SectionLabel("開始")
            TimeDisplayBox(dateTimeMillis = startTime, isAllDay = isAllDay, onClick = { onTimeBoxClick("START") })
        }
        if (!isAllDay) {
            Column(modifier = Modifier.weight(1f)) {
                SectionLabel("終了")
                TimeDisplayBox(dateTimeMillis = endTime, isAllDay = isAllDay, onClick = { onTimeBoxClick("END") })
            }
        }
    }
    if (isDateTimeError) {
        Text(
            text = "終了日時は開始日時より後に設定してください",
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp
        )
    }
}

// ============================================================
// 【4】タグセクション
// ============================================================

@Composable
fun TagSection(
    availableTags: List<Tag>,
    selectedTags: List<Tag>,
    isTagFolderExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onToggleTagSelection: (Tag) -> Unit,
    onUpdateTagOrder: (List<Tag>) -> Unit,
    onDeleteTagRequest: (Tag) -> Unit,
    onAddTagClick: () -> Unit
) {
    SectionLabel("タグ")
    Text(
        "選択したタグ（長押しで並び替え・削除）",
        fontSize = 12.sp,
        color = Color(0xFF70757A),
        modifier = Modifier.padding(bottom = 8.dp)
    )

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // チップの左右に16dpずつ余白を確保する。
        val usableWidthPx = with(density) { (maxWidth - 32.dp).toPx() }

        val displayTags = remember(
            availableTags,
            selectedTags,
            isTagFolderExpanded,
            usableWidthPx
        ) {
            if (isTagFolderExpanded) {
                availableTags
            } else {
                val defaultVisibleIds = LinkedHashSet<Long>()
                var usedWidthPx = 0f
                val spacingPx = with(density) { 8.dp.toPx() }

                availableTags.forEach { tag ->
                    // TagLabelと同じ、横8dpずつの余白 + 20dpアイコン +
                    // （アイコンがあれば）6dpの間隔 + 太字13spのタグ名で幅を計算する。
                    val textWidthPx = textMeasurer.measure(
                        text = tag.name,
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    ).size.width.toFloat()
                    val fixedWidthPx = with(density) {
                        (16.dp + 20.dp +
                                if (TagIconId.fromId(tag.icon) != null) 6.dp else 0.dp).toPx()
                    }
                    val chipWidthPx = fixedWidthPx + textWidthPx
                    val requiredWidthPx = chipWidthPx +
                            if (defaultVisibleIds.isEmpty()) 0f else spacingPx

                    if (
                        defaultVisibleIds.isEmpty() ||
                        usedWidthPx + requiredWidthPx <= usableWidthPx
                    ) {
                        defaultVisibleIds.add(tag.tagId)
                        usedWidthPx += requiredWidthPx
                    }
                }

                // 選択中のタグは、1行に入り切らない場合でも必ず追加表示する。
                selectedTags.forEach { defaultVisibleIds.add(it.tagId) }
                availableTags.filter { it.tagId in defaultVisibleIds }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DraggableTagList(
                tags = displayTags,
                allTags = availableTags,
                selectedTags = selectedTags,
                onTagClick = onToggleTagSelection,
                onOrderChanged = onUpdateTagOrder,
                onDeleteTagRequest = onDeleteTagRequest
            )

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .border(BorderStroke(1.dp, Color(0xFFE0E0E0)), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAddTagClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "タグを追加",
                    tint = Color(0xFF3C4043),
                    modifier = Modifier.size(18.dp)
                )
            }

            if (displayTags.size < availableTags.size || isTagFolderExpanded) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(
                        onClick = onExpandToggle,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = if (isTagFolderExpanded) "閉じる ▲" else "さらに表示 ▼",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
// ============================================================
// 【5】メモ
// ============================================================

@Composable
fun MemoSection(
    memo: String,
    onMemoChange: (String) -> Unit
) {
    SectionLabel("メモ")
    WireframeTextField(
        value = memo,
        onValueChange = onMemoChange,
        placeholder = "メモを入力",
        minLines = 3
    )
}

// ============================================================
// 【6】チェックリスト
// ============================================================

@Composable
fun ChecklistSection(
    checkList: List<ChecklistItem>,
    onCheckListChange: (List<ChecklistItem>) -> Unit
) {
    SectionLabel("チェックリスト")

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        checkList.forEachIndexed { index, item ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { checked ->
                        val updatedList = checkList.toMutableList()
                        updatedList[index] = item.copy(isChecked = checked)
                        onCheckListChange(updatedList)
                    }
                )

                OutlinedTextField(
                    value = item.text,
                    onValueChange = { newText ->
                        val updatedList = checkList.toMutableList()
                        updatedList[index] = item.copy(text = newText)
                        onCheckListChange(updatedList)
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("項目を入力") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )

                IconButton(
                    onClick = {
                        val updatedList = checkList
                            .filterIndexed { itemIndex, _ -> itemIndex != index }
                            .mapIndexed { newIndex, checklistItem -> checklistItem.copy(position = newIndex) }
                        onCheckListChange(updatedList)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "削除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        OutlinedButton(
            onClick = {
                onCheckListChange(
                    checkList + ChecklistItem(
                        id = 0L,
                        taskId = 0L,
                        text = "",
                        isChecked = false,
                        position = checkList.size
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color.Gray)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null,tint = Color(0xFF1C1B1F))
            Spacer(modifier = Modifier.width(4.dp))
            Text("項目を追加",color = Color(0xFF1C1B1F))
        }
    }
}

// ============================================================
// 【7】リマインダー通知
// ============================================================

@Composable
fun ReminderSection(
    reminderSetting: ReminderSetting,
    onReminderSettingChange: (ReminderSetting) -> Unit
) {
    var showNotificationMenu by remember { mutableStateOf(false) }
    val isNotifyEnabled = reminderSetting !is ReminderSetting.None

    DetailRowItem(Icons.Default.Notifications, "リマインダー通知") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.width(180.dp)
        ) {
            if (isNotifyEnabled) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    val currentOption = ReminderOption.fromDomain(reminderSetting)

                    OutlinedButton(
                        onClick = { showNotificationMenu = true },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1C1B1F))
                    ) {
                        Text(currentOption.label, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                    }

                    DropdownMenu(
                        expanded = showNotificationMenu,
                        onDismissRequest = { showNotificationMenu = false },
                        modifier = Modifier.background(Color.White),
                        containerColor = Color.White
                    ) {
                        ReminderOption.defaultOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(text = option.label, fontSize = 14.sp, color = Color.Black) },
                                onClick = {
                                    onReminderSettingChange(option.toDomain())
                                    showNotificationMenu = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Switch(
                checked = isNotifyEnabled,
                onCheckedChange = { isChecked ->
                    onReminderSettingChange(
                        if (isChecked) ReminderSetting.Before(10) else ReminderSetting.None
                    )
                },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF1A73E8)),
                modifier = Modifier.scale(0.85f)
            )
        }
    }
}

// ============================================================
// 場所セクション
// ============================================================

@Composable
fun LocationSection(
    locationName: String?,
    locationAddress: String?,
    latitude: Double?,
    longitude: Double?,
    onSelectClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current

    SectionLabel("場所")

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
    ) {
        if (locationName.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectClick() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF5F6368))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "場所を選択",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (latitude != null && longitude != null) {
                                val uri = Uri.parse(
                                    "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
                                )
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                        }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = "地図で見る",
                        tint = Color(0xFFD93025),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = locationName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1C1B1F)
                        )
                        if (!locationAddress.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = locationAddress,
                                fontSize = 12.sp,
                                color = Color(0xFF70757A),
                                maxLines = 2
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = Color(0xFF1A73E8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "地図で見る",
                                fontSize = 11.sp,
                                color = Color(0xFF1A73E8),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "場所を変更",
                        tint = Color(0xFF5F6368)
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "場所を削除",
                        tint = Color(0xFFD93025)
                    )
                }
            }
        }
    }
}

// ============================================================
// 【8】自動ステータス ＆ カラーパレット
// ============================================================

@Composable
fun AutoCompleteAndColorSection(
    isAutoCompleted: Boolean,
    onAutoCompletedChange: (Boolean) -> Unit,
    onInfoClick: () -> Unit,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit
) {
    Spacer(modifier = Modifier.height(20.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Text(
            text = "完了時の自動ステータス変更",
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = Color(0xFF1C1B1F)
        )

        IconButton(onClick = onInfoClick, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Default.Info,
                contentDescription = "説明を表示",
                tint = Color(0xFF1A73E8),
                modifier = Modifier.size(18.dp)
            )
        }

        Switch(
            checked = isAutoCompleted,
            onCheckedChange = onAutoCompletedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF1A73E8))
        )
    }

    Spacer(modifier = Modifier.height(12.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFF5F6368), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("色", fontSize = 14.sp, color = Color(0xFF1C1B1F))
        }

        val paletteColors = listOf(
            Color(0xFF4285F4), Color(0xFF81C784), Color(0xFFFFB74D),
            Color(0xFFD1C4E9), Color(0xFFF48FB1), Color(0xFFE0E0E0)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            paletteColors.forEach { color ->
                val isSelected = selectedColor == color.toArgb()

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            border = if (isSelected) BorderStroke(2.dp, Color(0xFF1A73E8)) else BorderStroke(0.dp, Color.Transparent),
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(color.toArgb()) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(modifier = Modifier.size(6.dp).background(Color.White, CircleShape))
                    }
                }
            }
        }
    }
}