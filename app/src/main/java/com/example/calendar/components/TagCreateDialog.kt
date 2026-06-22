package com.example.calendar.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.example.calendar.components.TagIconId

// ★ 修正：型安全な TagIconId を保持できるように変更
sealed class TagIconSource {
    object InitialText : TagIconSource()
    data class Vector(val iconId: TagIconId) : TagIconSource()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagCreateDialog(
    onDismissRequest: () -> Unit,
    onTagSave: (name: String, iconSource: TagIconSource, color: Color, customFields: List<String>) -> Unit
) {
    val tagName = remember { mutableStateOf("") }
    val customFields = remember { mutableStateListOf<String>() }
    val selectedIconSource = remember { mutableStateOf<TagIconSource>(TagIconSource.InitialText) }



    // ★ 改善：独自の定義リストを廃止し、マスターEnumから自動展開
    val sampleIconEnums = remember { TagIconId.entries }

    val sampleColors = listOf(
        Color(0xFFFFD54F), Color(0xFFFF8A80), Color(0xFF80D8FF), Color(0xB369F0AE), Color(0xFFEA80FC), Color(0xFFFFB74D),
        Color(0xFFFF4081), Color(0xFF29B6F6), Color(0xFF00E676), Color(0xFFD1C4E9), Color(0xFFE1BEE7), Color(0xFFB0BEC5)
    )
    val selectedColor = remember { mutableStateOf(sampleColors[0]) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "新規タグの作成",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- 1. タグ名入力 ---
                item {
                    OutlinedTextField(
                        value = tagName.value,
                        onValueChange = {
                            tagName.value = it
                            if (selectedIconSource.value is TagIconSource.InitialText) {
                                selectedIconSource.value = TagIconSource.InitialText
                            }
                        },
                        label = { Text("タグ名を入力") },
                        placeholder = { Text("例: 提出物、自主学習、至急") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // --- 2. アイコンプレビュー---
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(selectedColor.value.copy(alpha = 0.2f))
                                .border(
                                    width = 2.dp,
                                    color = selectedColor.value,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when (val source = selectedIconSource.value) {

                                is TagIconSource.InitialText -> {
                                    val displayChar =
                                        if (tagName.value.isNotBlank())
                                            tagName.value.take(1)
                                        else
                                            "?"

                                    Text(
                                        text = displayChar,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = selectedColor.value
                                    )
                                }

                                is TagIconSource.Vector -> {
                                    Icon(
                                        imageVector = source.iconId.vector,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = selectedColor.value
                                    )
                                }
                            }
                        }
                    }
                }



                // --- 3. タグカラー選択 ---
                item {
                    Text(
                        text = "タグカラー",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        maxItemsInEachRow = 6
                    ) {
                        sampleColors.forEach { color ->
                            val isSelected = selectedColor.value == color
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(width = if (isSelected) 3.dp else 0.dp, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, shape = CircleShape)
                                    .clickable { selectedColor.value = color }
                            )
                        }
                    }
                }
                // --- 4. サンプルアイコン20選 ---
                item {
                    Text(
                        text = "アイコンを選択",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 5
                    ) {
                        sampleIconEnums.forEach { iconEnum ->
                            // ★ 改善：型安全なEnumによる選択状態の判定
                            val isSelected = (selectedIconSource.value as? TagIconSource.Vector)?.iconId == iconEnum
                            val currentVector = iconEnum.vector

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) selectedColor.value.copy(alpha = 0.3f) else Color.Transparent)
                                    .border(width = if (isSelected) 2.dp else 1.dp, color = if (isSelected) selectedColor.value else Color.LightGray, shape = CircleShape)
                                    .clickable { selectedIconSource.value = TagIconSource.Vector(iconEnum) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = currentVector,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = if (isSelected) selectedColor.value else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // --- 5. カスタム項目 ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "カスタム項目",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        OutlinedButton(
                            onClick = { customFields.add("") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("追加", fontSize = 12.sp)
                        }
                    }
                }

                itemsIndexed(customFields) { index, fieldText ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = fieldText,
                            onValueChange = { newValue -> customFields[index] = newValue },
                            label = { Text("項目名 ${index + 1}") },
                            placeholder = { Text("例: 提出先、範囲") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        IconButton(
                            onClick = { customFields.removeAt(index) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("キャンセル") } },
        confirmButton = {
            Button(
                onClick = {
                    if (tagName.value.isNotBlank()) {
                        onTagSave(tagName.value, selectedIconSource.value, selectedColor.value, customFields.filter { it.isNotBlank() })
                    }
                },
                enabled = tagName.value.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                // ★ 1. テキストを「タグを保存する」へ修正
                Text("タグを保存する")
            }
        }
    )
}