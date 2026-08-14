package com.example.calendar.data.repository

import com.example.calendar.data.dao.TemplateChecklistItemDao
import com.example.calendar.data.dao.TemplateDao
import com.example.calendar.data.dao.TemplateTagDao
import com.example.calendar.data.entity.ChecklistItem
import com.example.calendar.data.entity.Tag
import com.example.calendar.data.entity.Template
import com.example.calendar.data.entity.TemplateChecklistItem
import com.example.calendar.data.entity.TemplateTag
import com.example.calendar.notification.ReminderSetting
import kotlinx.coroutines.flow.Flow

class TemplateRepository(
    private val templateDao: TemplateDao,
    private val templateTagDao: TemplateTagDao,
    private val templateChecklistItemDao: TemplateChecklistItemDao
) {

    fun getAllTemplates(): Flow<List<Template>> = templateDao.getAllTemplates()

    fun getRecentTemplates(limit: Int = 3): Flow<List<Template>> =
        templateDao.getRecentTemplates(limit)

    // ▼ 追加: テンプレート本体の取得
    suspend fun getTemplateById(templateId: Long): Template? {
        return templateDao.getTemplateById(templateId)
    }

    // ▼ 追加: テンプレートに紐づくチェックリストの取得
    suspend fun getChecklistItems(templateId: Long): List<TemplateChecklistItem> {
        return templateChecklistItemDao.getByTemplateId(templateId)
    }

    // ▼ 追加: テンプレートに紐づくタグIDのリスト取得
    suspend fun getTagIdsForTemplate(templateId: Long): List<Long> {
        return templateTagDao.getTagIdsForTemplate(templateId)
    }

    suspend fun markAsUsed(templateId: Long) {
        templateDao.updateLastUsedAt(
            templateId = templateId,
            lastUsedAt = System.currentTimeMillis()
        )
    }

    suspend fun createTemplate(
        template: Template,
        tags: List<Tag>,
        checklistItems: List<ChecklistItem>
    ): Long {
        val maxPosition = templateDao.getMaxPosition() ?: -1

        val templateId = templateDao.insertTemplate(
            template.copy(position = maxPosition + 1)
        )

        if (tags.isNotEmpty()) {
            templateTagDao.insertAll(
                tags.map { tag -> TemplateTag(templateId = templateId, tagId = tag.tagId) }
            )
        }

        if (checklistItems.isNotEmpty()) {
            templateChecklistItemDao.insertAll(
                checklistItems.mapIndexed { index, item ->
                    TemplateChecklistItem(
                        id = 0L,
                        templateId = templateId,
                        text = item.text,
                        isChecked = item.isChecked,
                        position = index
                    )
                }
            )
        }

        return templateId
    }

    suspend fun deleteTemplate(template: Template) {
        templateTagDao.deleteForTemplate(template.templateId)
        templateChecklistItemDao.deleteForTemplate(template.templateId)
        templateDao.deleteTemplate(template)
    }

    suspend fun updateTemplateOrder(templates: List<Template>) {
        val reordered = templates.mapIndexed { index, template ->
            template.copy(position = index)
        }
        templateDao.updateAll(reordered)
    }

    suspend fun updateTemplate(
        template: Template,
        tags: List<Tag>,
        checklistItems: List<ChecklistItem>
    ) {
        // position / lastUsedAt は編集で変えたくないので、既存値を引き継ぐ
        val existing = templateDao.getTemplateById(template.templateId)
        val merged = template.copy(
            position = existing?.position ?: template.position,
            lastUsedAt = existing?.lastUsedAt
        )

        templateDao.updateTemplate(merged)

        templateTagDao.deleteForTemplate(template.templateId)
        if (tags.isNotEmpty()) {
            templateTagDao.insertAll(
                tags.map { tag -> TemplateTag(templateId = template.templateId, tagId = tag.tagId) }
            )
        }

        templateChecklistItemDao.deleteForTemplate(template.templateId)
        if (checklistItems.isNotEmpty()) {
            templateChecklistItemDao.insertAll(
                checklistItems.mapIndexed { index, item ->
                    TemplateChecklistItem(
                        id = 0L,
                        templateId = template.templateId,
                        text = item.text,
                        isChecked = item.isChecked, // ★ 修正：チェック状態の保存を追加
                        position = index
                    )
                }
            )
        }
    }
}

// ★ 修正：updateTemplateの中に迷い込んでいた拡張関数を外（クラスの外）に出しました
fun Template.getReminderSetting(): ReminderSetting {
    return when (this.reminderType) {
        "NONE", null -> ReminderSetting.None
        "AT_START_TIME" -> ReminderSetting.AtStartTime
        "BEFORE" -> ReminderSetting.Before(minutes = this.reminderOffsetMinutes ?: 0)
        "DAY_BEFORE" -> ReminderSetting.DayBefore(
            daysBack = this.reminderDayOffset ?: 1,
            hour = this.reminderHour ?: 9,
            minute = this.reminderMinute ?: 0
        )
        // 互換性維持（既存データへの対応）
        "THREE_DAYS_BEFORE" -> ReminderSetting.DayBefore(daysBack = 3, hour = 9, minute = 0)
        "MORNING" -> ReminderSetting.DayBefore(daysBack = 0, hour = 9, minute = 0)
        else -> ReminderSetting.None
    }
}