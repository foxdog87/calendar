package com.example.calendar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.calendar.data.dao.TaskDao
import com.example.calendar.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId // ★ 追加
import java.time.ZoneOffset

@Database(
    entities = [
        Task::class,
        Tag::class,
        TaskTag::class,
        Template::class,
        TemplateTag::class,
        TagCustomField::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

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
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {

                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)

                            scope.launch(Dispatchers.IO) {

                                // ★ 修正：システムローカルタイムゾーンでEpoch秒を取得
                                val nowEpoch =
                                    LocalDateTime.now()
                                        .atZone(ZoneId.systemDefault())
                                        .toEpochSecond()

                                // タグ
                                db.execSQL(
                                    "INSERT INTO tags (name, icon, color) VALUES ('提出物', NULL, ${0xFF1A73E8.toInt()})"
                                )

                                db.execSQL(
                                    "INSERT INTO tags (name, icon, color) VALUES ('重要', NULL, ${0xFFD93025.toInt()})"
                                )

                                val memoStr =
                                    "問題集の第3章までを提出すること。ファイルはPDFで提出。"

                                val checklistStr =
                                    """[{"id":1,"text":"第1章の復習","isChecked":false},{"id":2,"text":"第2章の問題演習","isChecked":false}]"""

                                // サンプルタスク
                                db.execSQL(
                                    """
                                    INSERT INTO tasks (
                                        templateId,
                                        title,
                                        startTime,
                                        endTime,
                                        color,
                                        memo,
                                        checkList,
                                        latitude,
                                        longitude,
                                        dayCountTarget,
                                        url,
                                        attachmentPath,
                                        isAutoCompleted,
                                        completeState,
                                        remindMinutes,
                                        isAllDay
                                    )
                                    VALUES (
                                        NULL,
                                        '数学課題提出',
                                        $nowEpoch,
                                        ${nowEpoch + 7200},
                                        ${0xFF1A73E8.toInt()},
                                        '$memoStr',
                                        '$checklistStr',
                                        36.111,
                                        140.111,
                                        $nowEpoch,
                                        'https://example.com/assignment/3',
                                        '課題_第3章.pdf',
                                        0,
                                        'UNCOMPLETED',
                                        NULL,
                                        0
                                    )
                                    """.trimIndent()
                                )

                                // タグ紐付け
                                db.execSQL(
                                    "INSERT OR IGNORE INTO task_tag (taskId, tagId) VALUES (1, 1)"
                                )

                                db.execSQL(
                                    "INSERT OR IGNORE INTO task_tag (taskId, tagId) VALUES (1, 2)"
                                )
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