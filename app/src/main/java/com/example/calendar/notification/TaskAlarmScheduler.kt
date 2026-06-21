package com.example.calendar.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.calendar.data.entity.Task
import com.example.calendar.notification.TaskAlarmReceiver

class TaskAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @SuppressLint("ScheduleExactAlarm")
    fun schedule(task: Task) {
        // 通知設定（remindMinutes）が null の場合は何もしない
        val remindMinutes = task.remindMinutes ?: return

        // 実際の通知タイミング（エポック秒からミリ秒に変換。開始時間から差し引く）
        // startTime（秒） × 1000 = ミリ秒
        val startMillis = task.startTime * 1000
        val remindMillis = remindMinutes * 60 * 1000
        val triggerAtMillis = startMillis - remindMillis

        // すでに過去の時刻になっている場合はセットしない
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra("TASK_ID", task.taskId)
            putExtra("TASK_TITLE", task.title)
            putExtra("REMIND_MINUTES", remindMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.taskId.toInt(), // requestCodeをtaskIdにすることで、タスクごとに独立したアラームを確保
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Android 12 (API 31) 以上で正確なアラーム権限をチェック
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // 権限がない場合は不正確なアラームで代用するか、システム設定へ促す必要があります
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                return
            }
        }

        // 正確な時間（スリープ解除してでも鳴らす）にセット
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    fun cancel(task: Task) {
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        // 既存の登録があれば解除
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}