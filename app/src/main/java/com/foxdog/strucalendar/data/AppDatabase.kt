package com.foxdog.strucalendar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import com.foxdog.strucalendar.data.dao.TaskDao
import com.foxdog.strucalendar.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import androidx.room.TypeConverters
import com.foxdog.strucalendar.data.converter.ReminderTypeConverter
import com.foxdog.strucalendar.data.dao.ChecklistItemDao
import com.foxdog.strucalendar.data.dao.HolidayDao
import com.foxdog.strucalendar.data.dao.TagCustomFieldDao
import com.foxdog.strucalendar.data.dao.TagDao
import com.foxdog.strucalendar.data.dao.TagDisplayOrderDao
import com.foxdog.strucalendar.data.dao.TaskCustomFieldValueDao
import com.foxdog.strucalendar.data.dao.TaskTagDao
import com.foxdog.strucalendar.data.dao.TemplateChecklistItemDao
import com.foxdog.strucalendar.data.dao.TemplateCustomFieldValueDao
import com.foxdog.strucalendar.data.dao.TemplateDao
import com.foxdog.strucalendar.data.dao.TemplateTagDao
import com.foxdog.strucalendar.data.dao.TemplateDisplayOrderDao

@Database(
    entities = [
        Task::class,
        Tag::class,
        TaskTag::class,
        ChecklistItem::class,
        Template::class,
        TemplateTag::class,
        TagCustomField::class,
        TemplateChecklistItem::class,
        TagDisplayOrder::class,
        TemplateDisplayOrder::class,
        HolidayEntity::class,
        TaskCustomFieldValue::class,
        TemplateCustomFieldValue::class
    ],
    version = 21,
    exportSchema = false
)
@TypeConverters(ReminderTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun checklistItemDao(): ChecklistItemDao
    abstract fun templateChecklistItemDao(): TemplateChecklistItemDao
    abstract fun templateDao(): TemplateDao
    abstract fun templateTagDao(): TemplateTagDao
    abstract fun templateDisplayOrderDao(): TemplateDisplayOrderDao

    abstract fun templateCustomFieldValueDao(): TemplateCustomFieldValueDao
    abstract fun tagDao(): TagDao
    abstract fun tagDisplayOrderDao(): TagDisplayOrderDao
    abstract fun taskTagDao(): TaskTagDao

    abstract fun holidayDao(): HolidayDao

    abstract fun taskCustomFieldValueDao(): TaskCustomFieldValueDao

    abstract fun tagCustomFieldDao(): TagCustomFieldDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS template_display_orders (templateId INTEGER NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(templateId))"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO template_display_orders (templateId, position) SELECT templateId, templateId - 1 FROM templates"
                )
            }
        }

        fun getDatabase(
            context: Context,
            scope: CoroutineScope
        ): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calendar_database"
                )
                    // ... (前略)

                    .addCallback(object : RoomDatabase.Callback() {

                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)

                            scope.launch(Dispatchers.IO) {
                                val nowEpoch = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond()
                                val oneDay = 86400L

                                // ==========================================
                                // 1. サンプルタグの作成
                                // ==========================================
                                db.execSQL("INSERT INTO tags (name, icon, color) VALUES ('仕事', 'work', ${0xFF1A73E8.toInt()})")
                                db.execSQL("INSERT INTO tags (name, icon, color) VALUES ('プライベート', 'person', ${0xFF34A853.toInt()})")
                                db.execSQL("INSERT INTO tags (name, icon, color) VALUES ('重要', 'flag', ${0xFFD93025.toInt()})")
                                db.execSQL("INSERT INTO tags (name, icon, color) VALUES ('健康', 'favorite', ${0xFFFF8A65.toInt()})")

                                // ==========================================
                                // 2. サンプルタスクの作成（DB追加カラム対応、2つだけに削減）
                                // ==========================================

                                // タスク①：今日のサンプルタスク
                                db.execSQL(
                                    """
                                    INSERT INTO tasks (
                                        templateId, title, startTime, endTime, color, memo,
                                        locationName, locationAddress, latitude, longitude,
                                        dayCountTarget, url, attachmentPath, isAutoCompleted,
                                        completeState, isAllDay, isPinned,
                                        reminderType, reminderOffsetMinutes, reminderDayOffset, reminderHour, reminderMinute,
                                        recurrenceGroupId, recurrenceType, recurrenceIntervalDays, recurrenceNth, recurrenceWeekday, recurrenceEndDate
                                    )
                                    VALUES (
                                        NULL, 'サンプルタスク（今日）', $nowEpoch, ${nowEpoch + 3600}, ${0xFF1A73E8.toInt()}, 'これは自動生成されたサンプルタスクです。',
                                        NULL, NULL, NULL, NULL,
                                        NULL, NULL, NULL, 0,
                                        'UNCOMPLETED', 0, 0,
                                        NULL, NULL, NULL, NULL, NULL,
                                        NULL, NULL, NULL, NULL, NULL, NULL
                                    )
                                    """.trimIndent()
                                )
                                db.execSQL("INSERT OR IGNORE INTO task_tag (taskId, tagId) VALUES (1, 1)")

                                // タスク②：明日のサンプルタスク（チェックリスト付き）
                                db.execSQL(
                                    """
                                    INSERT INTO tasks (
                                        templateId, title, startTime, endTime, color, memo,
                                        locationName, locationAddress, latitude, longitude,
                                        dayCountTarget, url, attachmentPath, isAutoCompleted,
                                        completeState, isAllDay, isPinned,
                                        reminderType, reminderOffsetMinutes, reminderDayOffset, reminderHour, reminderMinute,
                                        recurrenceGroupId, recurrenceType, recurrenceIntervalDays, recurrenceNth, recurrenceWeekday, recurrenceEndDate
                                    )
                                    VALUES (
                                        NULL, 'サンプルタスク（明日）', ${nowEpoch + oneDay}, ${nowEpoch + oneDay + 1800}, ${0xFFD93025.toInt()}, '自由に編集・削除して使ってみてください。',
                                        NULL, NULL, NULL, NULL,
                                        NULL, NULL, NULL, 0,
                                        'UNCOMPLETED', 0, 0,
                                        NULL, NULL, NULL, NULL, NULL,
                                        NULL, NULL, NULL, NULL, NULL, NULL
                                    )
                                    """.trimIndent()
                                )
                                db.execSQL("INSERT OR IGNORE INTO task_tag (taskId, tagId) VALUES (2, 2)")
                                db.execSQL("INSERT OR IGNORE INTO task_tag (taskId, tagId) VALUES (2, 3)")
                                db.execSQL("INSERT INTO checklist_items (taskId, text, isChecked, position) VALUES (2, '使い方を確認する', 0, 0)")
                                db.execSQL("INSERT INTO checklist_items (taskId, text, isChecked, position) VALUES (2, '自分の予定を追加してみる', 0, 1)")

                                // ==========================================
                                // 3. サンプルテンプレートの作成（DB追加カラム対応・isAllDayをfalseに設定）
                                // ==========================================

                                // テンプレート①：定例会議（仕事）
                                db.execSQL(
                                    """
                                    INSERT INTO templates (
                                        title, icon, timeLength, description, color, memo,
                                        locationName, locationAddress, latitude, longitude,
                                        dayCountTarget, url, attachmentPath, isAutoCompleted, isAllDay,
                                        reminderType, reminderOffsetMinutes, reminderDayOffset, reminderHour, reminderMinute,
                                        position, lastUsedAt,
                                        recurrenceType, recurrenceIntervalDays, recurrenceNth, recurrenceWeekday, recurrenceEndDate
                                    )
                                    VALUES (
                                        '会議', NULL, 3600, '仕事の打ち合わせ・定例会議用（60分）', ${0xFF1A73E8.toInt()}, '議題を事前に確認しておく。',
                                        NULL, NULL, NULL, NULL,
                                        NULL, NULL, NULL, 0, 0,
                                        'BEFORE', 10, NULL, NULL, NULL,
                                        0, NULL,
                                        NULL, NULL, NULL, NULL, NULL
                                    )
                                    """.trimIndent()
                                )
                                db.execSQL("INSERT OR IGNORE INTO template_tag (templateId, tagId) VALUES (1, 1)")

                                // テンプレート②：買い物（プライベート）
                                db.execSQL(
                                    """
                                    INSERT INTO templates (
                                        title, icon, timeLength, description, color, memo,
                                        locationName, locationAddress, latitude, longitude,
                                        dayCountTarget, url, attachmentPath, isAutoCompleted, isAllDay,
                                        reminderType, reminderOffsetMinutes, reminderDayOffset, reminderHour, reminderMinute,
                                        position, lastUsedAt,
                                        recurrenceType, recurrenceIntervalDays, recurrenceNth, recurrenceWeekday, recurrenceEndDate
                                    )
                                    VALUES (
                                        '買い物', NULL, 3600, '日用品・食材の買い出し用', ${0xFF34A853.toInt()}, 'ポイントカードを忘れずに。',
                                        NULL, NULL, NULL, NULL,
                                        NULL, NULL, NULL, 0, 0,
                                        NULL, NULL, NULL, NULL, NULL,
                                        1, NULL,
                                        NULL, NULL, NULL, NULL, NULL
                                    )
                                    """.trimIndent()
                                )
                                db.execSQL("INSERT OR IGNORE INTO template_tag (templateId, tagId) VALUES (2, 2)")
                                db.execSQL("INSERT INTO template_checklist_items (templateId, text, isChecked, position) VALUES (2, '買い物リストの確認', 0, 0)")
                                db.execSQL("INSERT INTO template_checklist_items (templateId, text, isChecked, position) VALUES (2, 'エコバッグ', 0, 1)")

                                // テンプレート③：運動（健康）
                                db.execSQL(
                                    """
                                    INSERT INTO templates (
                                        title, icon, timeLength, description, color, memo,
                                        locationName, locationAddress, latitude, longitude,
                                        dayCountTarget, url, attachmentPath, isAutoCompleted, isAllDay,
                                        reminderType, reminderOffsetMinutes, reminderDayOffset, reminderHour, reminderMinute,
                                        position, lastUsedAt,
                                        recurrenceType, recurrenceIntervalDays, recurrenceNth, recurrenceWeekday, recurrenceEndDate
                                    )
                                    VALUES (
                                        '運動', NULL, 1800, '軽い運動・ウォーキングなどの習慣化用（30分）', ${0xFFFF8A65.toInt()}, '水分補給を忘れずに。',
                                        NULL, NULL, NULL, NULL,
                                        NULL, NULL, NULL, 0, 0,
                                        NULL, NULL, NULL, NULL, NULL,
                                        2, NULL,
                                        NULL, NULL, NULL, NULL, NULL
                                    )
                                    """.trimIndent()
                                )
                                db.execSQL("INSERT OR IGNORE INTO template_tag (templateId, tagId) VALUES (3, 4)")

                                // サンプル3件を「最近使用したテンプレート」に表示させるため lastUsedAt を設定
                                db.execSQL("UPDATE templates SET lastUsedAt = ${nowEpoch * 1000} WHERE templateId = 3")
                                db.execSQL("UPDATE templates SET lastUsedAt = ${(nowEpoch - 3600) * 1000} WHERE templateId = 2")
                                db.execSQL("UPDATE templates SET lastUsedAt = ${(nowEpoch - 7200) * 1000} WHERE templateId = 1")
                            }
                        }
                    })
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}