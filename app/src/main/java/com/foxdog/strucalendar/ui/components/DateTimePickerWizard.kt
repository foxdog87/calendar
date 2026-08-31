package com.foxdog.strucalendar.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDatePickerDialog(
    initialDateMillis: Long,
    title: String,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val dateState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = dateState.selectedDateMillis ?: initialDateMillis
                val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                onDateSelected(date)
            }) { Text("決定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    ) {
        androidx.compose.foundation.layout.Column {
            Text(title, modifier = Modifier.padding(start = 24.dp, top = 16.dp))
            DatePicker(state = dateState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    title: String,
    onDismiss: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    val timeState = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = timeState)
            }
        },
        confirmButton = {
            TextButton(onClick = { onTimeSelected(timeState.hour, timeState.minute) }) { Text("決定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    )
}
