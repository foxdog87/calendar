package com.example.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun DateCell(date: LocalDate?, isToday: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(0.8f) // 少し縦長にしてタスク表示スペースを確保
            .border(0.2.dp, Color.LightGray)
            .clickable(enabled = date != null) { onClick() }
            .background(if (isToday) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
        contentAlignment = Alignment.TopCenter
    ) {
        if (date != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = 14.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = when(date.dayOfWeek.value) {
                        7 -> Color.Red // 日曜日
                        6 -> Color.Blue // 土曜日
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

fun buildMonthDates(yearMonth: YearMonth): List<LocalDate?> {
    val dates = mutableListOf<LocalDate?>()
    val firstDay = yearMonth.atDay(1)
    val firstDayOfWeek = firstDay.dayOfWeek.value % 7
    repeat(firstDayOfWeek) { dates.add(null) }
    for (day in 1..yearMonth.lengthOfMonth()) {
        dates.add(yearMonth.atDay(day))
    }
    while (dates.size % 7 != 0) { dates.add(null) }
    return dates
}