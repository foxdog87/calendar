package com.example.calendar.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateFormat
import android.util.Log
import com.example.calendar.data.entity.Task
import java.time.Instant
import java.time.ZoneId

class TaskAlarmScheduler(
    private val context: Context
) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @SuppressLint("ScheduleExactAlarm")
    fun schedule(task: Task) {

        val now = System.currentTimeMillis()

        // 1. Entityから型安全なドメインモデルへ変換
        val reminderSetting = task.getReminderSetting()

        // 通知設定なしの場合は即終了
        if (reminderSetting is ReminderSetting.None) {
            return
        }

        // 2. ドメインモデルを使ってトリガー時間を計算
        val triggerAtMillis = calculateTriggerTime(task, reminderSetting) ?: return

        // 過去時刻の場合は登録しない
        if (triggerAtMillis <= now) {
            Log.d("ALARM_TEST", "通知時刻が過去 taskId=${task.taskId}")
            return
        }

        Log.d("ALARM_TEST", "schedule taskId=${task.taskId}")
        Log.d("ALARM_TEST", "trigger=${DateFormat.format("yyyy/MM/dd HH:mm:ss", triggerAtMillis)}")

        // Android12 Exact Alarm確認
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e("ALARM_TEST", "Exact Alarm権限なし")
                return
            }
        }

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra("TASK_ID", task.taskId)
            putExtra("TASK_TITLE", task.title)
            // Receiver側での表示用に、nullにならないよう0をフォールバック
            putExtra("REMIND_MINUTES", task.reminderOffsetMinutes ?: 0)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )

        Log.d("ALARM_TEST", "alarm registered")
    }

    /**
     * 通知時間計算
     * Domain Model (ReminderSetting) の型安全な when 分岐で計算する。
     * isAllDay などの泥臭い分岐は不要になり、設定そのものが意味を持つ。
     */
    private fun calculateTriggerTime(task: Task, setting: ReminderSetting): Long? {
        val zone = ZoneId.systemDefault()
        val startDateTime = Instant.ofEpochSecond(task.startTime).atZone(zone).toLocalDateTime()

        return when (setting) {
            is ReminderSetting.None -> null

            is ReminderSetting.AtStartTime -> {
                startDateTime.atZone(zone).toInstant().toEpochMilli()
            }

            is ReminderSetting.Before -> {
                startDateTime
                    .minusMinutes(setting.minutes.toLong())
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
            }

            is ReminderSetting.DayBefore -> {
                startDateTime
                    .minusDays(setting.daysBack.toLong())
                    .withHour(setting.hour)
                    .withMinute(setting.minute)
                    .withSecond(0)
                    .withNano(0)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
            }
        }
    }

    fun cancel(task: Task) {
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("ALARM_TEST", "alarm cancelled taskId=${task.taskId}")
        }
    }
}