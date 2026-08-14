package com.example.calendar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import com.example.calendar.data.dao.TaskDao
import com.example.calendar.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import androidx.room.TypeConverters
import com.example.calendar.data.converter.ReminderTypeConverter
import com.example.calendar.data.dao.ChecklistItemDao
import com.example.calendar.data.dao.TagDao
import com.example.calendar.data.dao.TagDisplayOrderDao
import com.example.calendar.data.dao.TaskTagDao
import com.example.calendar.data.dao.TemplateChecklistItemDao
import com.example.calendar.data.dao.TemplateDao
import com.example.calendar.data.dao.TemplateTagDao
import com.example.calendar.data.dao.TemplateDisplayOrderDao

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
        TemplateDisplayOrder::class
    ],
    version = 14, // ★ バージョンを更新（DBの再作成を促すため）
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
    abstract fun tagDao(): TagDao
    abstract fun tagDisplayOrderDao(): TagDisplayOrderDao
    abstract fun taskTagDao(): TaskTagDao

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
                    .addMigrations(MIGRATION_9_10)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {

                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)

                            scope.launch(Dispatchers.IO) {
                                val nowEpoch = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond()

                                // ==========================================
                                // 1. サンプルタグの作成
                                // ==========================================
                                db.execSQL("INSERT INTO tags (name, icon, color) VALUES ('提出物', NULL, ${0xFF1A73E8.toInt()})")
                                db.execSQL("INSERT INTO tags (name, icon, color) VALUES ('重要', NULL, ${0xFFD93025.toInt()})")
                                db.execSQL("INSERT INTO tags (name, icon, color) VALUES ('買い物', NULL, ${0xFF34A853.toInt()})")


                                // ==========================================
                                // 2. サンプルタスクの作成
                                // ==========================================
                                val memoStr = "問題集の第3章までを提出すること。ファイルはPDFで提出。"

                                // ★ 修正：latitude と longitude カラムを追加してエラーを解消
                                db.execSQL(
                                    """
                                    INSERT INTO tasks (
                                        templateId, title, startTime, endTime, color, memo,
                                        locationName, locationAddress, latitude, longitude,
                                        dayCountTarget, url, attachmentPath, isAutoCompleted,
                                        completeState, isAllDay,
                                        reminderType, reminderOffsetMinutes, reminderDayOffset, reminderHour, reminderMinute
                                    )
                                    VALUES (
                                        NULL, '数学課題提出', $nowEpoch, ${nowEpoch + 7200}, ${0xFF1A73E8.toInt()}, '$memoStr',
                                        '筑波大学', '茨城県つくば市天王台1-1-1', 36.0825, 140.1114,
                                        $nowEpoch, 'https://example.com/assignment/3', '課題_第3章.pdf', 0,
                                        'UNCOMPLETED', 0,
                                        NULL, NULL, NULL, NULL, NULL
                                    )
                                    """.trimIndent()
                                )

                                // タグ紐付け
                                db.execSQL("INSERT OR IGNORE INTO task_tag (taskId, tagId) VALUES (1, 1)")
                                db.execSQL("INSERT OR IGNORE INTO task_tag (taskId, tagId) VALUES (1, 2)")


                                // ==========================================
                                // 3. サンプルテンプレートの作成
                                // ==========================================

                                // テンプレート①：大学の授業
                                db.execSQL(
                                    """
                                    INSERT INTO templates (
                                        title, icon, timeLength, description, color, memo,
                                        locationName, locationAddress, latitude, longitude,
                                        dayCountTarget, url, attachmentPath, isAutoCompleted,
                                        reminderType, reminderOffsetMinutes, reminderDayOffset, reminderHour, reminderMinute,
                                        position, lastUsedAt
                                    )
                                    VALUES (
                                        '【講義】', NULL, 5400, '通常の大学講義用（90分）', ${0xFF81C784.toInt()}, '持ち物：ノートPC、配布資料',
                                        '3C棟 3C201', '筑波大学 第3エリア', 36.0825, 140.1114,
                                        NULL, 'https://manaba.tsukuba.ac.jp', NULL, 1,
                                        'BEFORE', 10, NULL, NULL, NULL,
                                        0, NULL
                                    )
                                    """.trimIndent()
                                )

                                // テンプレート②：日用品の買い出し
                                db.execSQL(
                                    """
                                    INSERT INTO templates (
                                        title, icon, timeLength, description, color, memo,
                                        locationName, locationAddress, latitude, longitude,
                                        dayCountTarget, url, attachmentPath, isAutoCompleted,
                                        reminderType, reminderOffsetMinutes, reminderDayOffset, reminderHour, reminderMinute,
                                        position, lastUsedAt
                                    )
                                    VALUES (
                                        '日用品の買い出し', NULL, 3600, 'スーパーやドラッグストアでの買い物', ${0xFFFFB74D.toInt()}, 'ポイントカードを忘れないこと！',
                                        '近くのスーパー', NULL, NULL, NULL,
                                        NULL, NULL, NULL, 0,
                                        NULL, NULL, NULL, NULL, NULL,
                                        1, NULL
                                    )
                                    """.trimIndent()
                                )

                                // テンプレート②に「買い物」タグを紐付ける
                                db.execSQL("INSERT OR IGNORE INTO template_tags (templateId, tagId) VALUES (2, 3)")

                                // テンプレート②用のチェックリスト（TemplateChecklistItem）を登録
                                db.execSQL("INSERT INTO template_checklist_items (templateId, text, isChecked, position) VALUES (2, 'トイレットペーパー', 0, 0)")
                                db.execSQL("INSERT INTO template_checklist_items (templateId, text, isChecked, position) VALUES (2, '洗剤', 0, 1)")
                                db.execSQL("INSERT INTO template_checklist_items (templateId, text, isChecked, position) VALUES (2, 'シャンプーの詰め替え', 0, 2)")
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