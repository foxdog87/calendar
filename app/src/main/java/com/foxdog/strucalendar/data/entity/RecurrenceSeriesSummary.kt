package com.foxdog.strucalendar.data.entity

/**
 * 繰り返しタスクの1シリーズをまとめて表示するための射影データ。
 * tasksテーブルを recurrenceGroupId でグループ化した結果を受け取る。
 */
data class RecurrenceSeriesSummary(
    val recurrenceGroupId: String,
    val title: String,
    val recurrenceType: String?,
    val recurrenceIntervalDays: Int?,
    val recurrenceNth: Int?,
    val recurrenceWeekday: Int?,
    val recurrenceWeekdays: String?,
    val recurrenceEndDate: Long?,
    val occurrenceCount: Int,
    val firstStartTime: Long
)