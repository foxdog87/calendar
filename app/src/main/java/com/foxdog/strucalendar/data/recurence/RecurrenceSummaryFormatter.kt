package com.foxdog.strucalendar.data.recurrence

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.foxdog.strucalendar.data.entity.RecurrenceSeriesSummary

object RecurrenceSummaryFormatter {

    private val weekdayLabels = mapOf(1 to "月", 2 to "火", 3 to "水", 4 to "木", 5 to "金", 6 to "土", 7 to "日")
    private val nthLabels = mapOf(1 to "第1", 2 to "第2", 3 to "第3", 4 to "第4", 5 to "最終")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.JAPANESE)

    fun ruleLabel(summary: RecurrenceSeriesSummary): String {
        return when (summary.recurrenceType) {
            "INTERVAL_DAYS" -> "${summary.recurrenceIntervalDays ?: 1}日ごと"
            "MONTHLY_NTH_WEEKDAY" -> {
                val nth = nthLabels[summary.recurrenceNth] ?: "第1"
                val weekday = weekdayLabels[summary.recurrenceWeekday] ?: "月"
                "毎月 ${nth}${weekday}曜日"
            }
            "WEEKLY_ON_DAYS" -> {
                val days = summary.recurrenceWeekdays
                    ?.split(",")
                    ?.mapNotNull { it.trim().toIntOrNull() }
                    ?.sorted()
                    ?.mapNotNull { weekdayLabels[it] }
                    ?: emptyList()
                if (days.isEmpty()) "毎週" else "毎週 ${days.joinToString("・")}曜日"
            }
            else -> "繰り返し"
        }
    }

    fun endDateLabel(summary: RecurrenceSeriesSummary): String {
        val endDate = summary.recurrenceEndDate ?: return "終了日未設定"
        val localDate = Instant.ofEpochSecond(endDate).atZone(ZoneId.systemDefault()).toLocalDate()
        return localDate.format(dateFormatter) + "まで"
    }
}