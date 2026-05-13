package com.example.calendar.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.calendar.viewmodel.CalendarViewModel

@Composable
fun TaskCreateScreen(viewModel: CalendarViewModel) {
    // ViewModelから現在の「下書き状態」を取得
    val taskState = viewModel.inputState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "予定の入力", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // 1. タイトル入力欄
        TextField(
            value = taskState.title,
            onValueChange = { newText ->
                // ここで updateInput と .copy() を使用！
                // 「今の状態(it)」をコピーして「タイトル」だけを書き換える指示
                viewModel.updateInput { it.copy(title = newText) }
            },
            label = { Text("タイトル") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. メモ入力欄
        TextField(
            value = taskState.memo,
            onValueChange = { newMemo ->
                // 「メモ」だけを書き換える指示
                viewModel.updateInput { it.copy(memo = newMemo) }
            },
            label = { Text("メモ（任意）") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        // 3. 保存ボタン
        Button(
            onClick = {
                viewModel.saveManualTask()
                // 本来はここで画面を閉じる処理が入ります
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存する")
        }
    }
}