package com.example.calendar.domain

import com.example.calendar.data.entity.Task
import com.example.calendar.data.entity.Template
import java.time.LocalDate
import java.time.LocalTime

class TaskManager {
    /**
     * シーケンス図の convertToTask(template, date) に相当
     * テンプレートの内容をコピーして新しいTaskオブジェクトを作る
     */
    fun convertToTask(template: Template, targetDate: LocalDate): Task {
        val now = LocalTime.now()
        return Task(
            templateId = template.templateId,
            title = template.title,
            date = targetDate,
            startTime = now,
            endTime = now.plusMinutes(template.timeLength.toLong()),
            color = template.color,
            memo = template.description
            // 必要に応じて他の項目もテンプレートから引き継ぐ
        )
    }
}