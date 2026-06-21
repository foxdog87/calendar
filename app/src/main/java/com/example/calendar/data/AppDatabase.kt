package com.example.calendar.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.calendar.data.dao.TaskDao
import com.example.calendar.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset

@Database(
    entities = [
        Task::class, Tag::class, TaskTag::class,
        Template::class, TemplateTag::class, TagCustomField::class
    ],
    version = 2, // ★ 1 から 2 に引き上げ
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // ★ 追加: バージョン1から2への移行処理（NullableなINTEGERとして追加）
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // NOT NULL を外すことで、既存データには自動的に null が格納されます
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `remindMinutes` INTEGER")
                db.execSQL("ALTER TABLE `templates` ADD COLUMN `remindMinutes` INTEGER")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calendar_database"
                )
                    .addMigrations(MIGRATION_1_2) // ★ マイグレーションを登録
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // デッドロック回避のため db を使って直接SQLを実行
                            scope.launch(Dispatchers.IO) {
                                val nowEpoch = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)

                                // 1. タグの挿入
                                db.execSQL("INSERT INTO tags (name, icon, color) VALUES ('提出物', NULL, ${0xFF1A73E8.toInt()})")
                                db.execSQL("INSERT INTO tags (name, icon, color) VALUES ('重要', NULL, ${0xFFD93025.toInt()})")

                                // 2. タスクの挿入（remindMinutesカラムは指定しない、または明示的にNULLにする。今回はカラム列から除外してSQLite側にデフォルトNULLを任せています）
                                val memoStr = "問題集の第3章までを提出すること。ファイルはPDFで提出。"
                                val checklistStr = """[{"id":1,"text":"第1章の復習","isChecked":false},{"id":2,"text":"第2章の問題演習","isChecked":false}]"""

                                db.execSQL("""
                                    INSERT INTO tasks (
                                        templateId, title, startTime, endTime, color, memo, checkList, 
                                        latitude, longitude, dayCountTarget, url, attachmentPath, isAutoCompleted, completeState
                                    ) VALUES (
                                        NULL, '数学課題提出', $nowEpoch, ${nowEpoch + 7200}, ${0xFF1A73E8.toInt()}, '$memoStr', '$checklistStr', 
                                        36.111, 140.111, $nowEpoch, 'https://example.com/assignment/3', '課題_第3章.pdf', 0, 'UNCOMPLETED'
                                    )
                                """.trimIndent())

                                // 3. 中間テーブルの紐付け
                                db.execSQL("INSERT OR IGNORE INTO task_tag (taskId, tagId) VALUES (1, 1)")
                                db.execSQL("INSERT OR IGNORE INTO task_tag (taskId, tagId) VALUES (1, 2)")
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