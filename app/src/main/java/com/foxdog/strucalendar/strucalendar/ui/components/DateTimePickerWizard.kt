package com.foxdog.strucalendar.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerWizard(
    target: String,
    isAllDay: Boolean,
    onDismiss: () -> Unit,
    onDateTimeSelected: (Long) -> Unit
) {
    var isTimeStep by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = Instant.now().toEpochMilli())
    val now = LocalTime.now()
    val timeState = rememberTimePickerState(initialHour = now.hour, initialMinute = now.minute, is24Hour = true)

    if (isAllDay && isTimeStep) {
        val millis = dateState.selectedDateMillis ?: Instant.now().toEpochMilli()
        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

        onDateTimeSelected(
            LocalDateTime.of(date, LocalTime.MIN)
                .atZone(ZoneId.systemDefault())
                .toEpochSecond()
        )
        isTimeStep = false
        return
    }

    if (!isTimeStep) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { isTimeStep = true }) {
                    Text(if (isAllDay) "決定" else "次へ (時間選択)")
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
        ) {
            DatePicker(state = dateState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val millis = dateState.selectedDateMillis ?: Instant.now().toEpochMilli()
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    val time = LocalTime.of(timeState.hour, timeState.minute)

                    onDateTimeSelected(
                        LocalDateTime.of(date, time)
                            .atZone(ZoneId.systemDefault())
                            .toEpochSecond()
                    )
                }) {
                    Text("決定")
                }
            },
            dismissButton = { TextButton(onClick = { isTimeStep = false }) { Text("戻る") } },
            title = { Text(if (target == "START") "開始時間を設定" else "終了時間を設定") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timeState)
                }
            }
        )
    }
}