package com.example.calendar.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.calendar.viewmodel.CalendarViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TaskCreateScreen(
    viewModel: CalendarViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    val taskState = viewModel.inputState

// 1. ★ダイアログの表示・非表示を管理するフラグ
    var showDatePicker by remember { mutableStateOf(false) }

    // 2. ★DatePickerの状態を管理するState（初期値として現在のミリ秒を設定）
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Instant.now().toEpochMilli()
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // (戻るボタン付きのヘッダー Row はそのまま維持)

        // --- 3. ★日付を選択するためのボタン（またはTextField風のパーツ） ---
        OutlinedButton(
            onClick = { showDatePicker = true }, // タップされたらダイアログをONにする
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            // 現在選択されている日付（下書き状態の date）を綺麗にフォーマットして表示
            val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
            Text(text = "日付: ${taskState.date.format(formatter)}")
        }

        // -----------------------------------------------------
        // 4. ★ダイアログ本体（フラグが true の時だけ画面最前面に登場する）
        // -----------------------------------------------------
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false }, // 外側をタップして閉じた時
                confirmButton = {
                    TextButton(
                        onClick = {
                            // 「決定」が押されたら、選択されたミリ秒を LocalDate に変換してViewModelに保存
                            datePickerState.selectedDateMillis?.let { millis ->
                                val selectedDate = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()

                                // 下書き状態（TaskInputState）の日付を上書き更新
                                viewModel.updateInput { it.copy(date = selectedDate) }
                            }
                            showDatePicker = false // ダイアログを閉じる
                        }
                    ) {
                        Text("決定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("キャンセル")
                    }
                }
            ) {
                // ダイアログの中身として、Android標準のDatePickerをセット
                DatePicker(state = datePickerState)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 1. タイトル入力欄
        TextField(
            value = taskState.title,
            onValueChange = { newText ->
                viewModel.updateInput { it.copy(title = newText) }
            },
            label = { Text("タイトル") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- ★設計変更に合わせた動的入力欄追加ロジック（新テーブル連動版） ---
        // 1. 選択されたすべてのタグのIDをリスト化
        val selectedTagIds = taskState.selectedTags.map { it.tagId }

        // 2. 新テーブル（マスタ）から、選択されたタグに紐付くカスタム項目定義をすべて抽出
        val activeFields = viewModel.sampleCustomFields.filter { it.tagId in selectedTagIds }

        // 3. 抽出されたカスタム項目の数だけTextFieldを自動生成して並べる
        activeFields.forEach { field ->
            // Mapから、このカスタム項目定義に対応する入力値を取得
            val currentInputValue = taskState.customFieldValues[field] ?: ""

            TextField(
                value = currentInputValue,
                onValueChange = { newText ->
                    // 項目定義（field）をそのまま渡してViewModel側の状態を更新
                    viewModel.updateCustomFieldValue(field, newText)
                },
                label = { Text(field.fieldName) }, // 「提出先」や「点数」が動的にラベルになる
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2. 場所入力欄 (★新設：ER図の location に対応)
        TextField(
            value = taskState.location,
            onValueChange = { newLocation ->
                viewModel.updateInput { it.copy(location = newLocation) }
            },
            label = { Text("場所") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. URL入力欄 (★新設：ER図の url に対応)
        TextField(
            value = taskState.url,
            onValueChange = { newUrl ->
                viewModel.updateInput { it.copy(url = newUrl) }
            },
            label = { Text("URL") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4. チェックリスト入力欄 (★新設：ER図の checkList に対応)
        TextField(
            value = taskState.checkList,
            onValueChange = { newCheckList ->
                viewModel.updateInput { it.copy(checkList = newCheckList) }
            },
            label = { Text("チェックリスト（改行で複数入力）") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3 // 少し広めの入力欄にする
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 5. メモ入力欄
        TextField(
            value = taskState.memo,
            onValueChange = { newMemo ->
                viewModel.updateInput { it.copy(memo = newMemo) }
            },
            label = { Text("メモ（任意）") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 6. 保存ボタン
        // 6. 保存ボタン
        Button(
            onClick = {
                viewModel.saveTask() // データを保存
                onNavigateBack()     // ★理由B: 保存が完了したら、自動でカレンダー画面に戻る
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存する")
        }
    }
}