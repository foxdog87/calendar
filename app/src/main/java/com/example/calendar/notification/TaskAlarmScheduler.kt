package com.example.calendar.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import android.util.Log
import com.example.calendar.data.entity.Task
import com.example.calendar.notification.TaskAlarmReceiver

class TaskAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @SuppressLint("ScheduleExactAlarm")
    fun schedule(task: Task) {

        val remindMinutes = task.remindMinutes ?: return

        val startMillis = task.startTime * 1000
        val remindMillis = remindMinutes * 60 * 1000
        val triggerAtMillis = startMillis - remindMillis

        Log.d("ALARM_TEST", "schedule called")
        Log.d("ALARM_TEST", "taskId=${task.taskId}")
        Log.d("ALARM_TEST", "title=${task.title}")
        Log.d("ALARM_TEST", "remindMinutes=$remindMinutes")
        Log.d("ALARM_TEST", "startTime=${task.startTime}")
        Log.d("ALARM_TEST", "triggerAtMillis=$triggerAtMillis")

        Log.d(
            "ALARM_TEST",
            "triggerDate=${
                DateFormat.format(
                    "yyyy/MM/dd HH:mm:ss",
                    triggerAtMillis
                )
            }"
        )

        Log.d(
            "ALARM_TEST",
            "currentDate=${
                DateFormat.format(
                    "yyyy/MM/dd HH:mm:ss",
                    System.currentTimeMillis()
                )
            }"
        )

        if (triggerAtMillis <= System.currentTimeMillis()) {
            Log.d("ALARM_TEST", "alarm skipped because trigger time is in the past")
            return
        }

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra("TASK_ID", task.taskId)
            putExtra("TASK_TITLE", task.title)
            putExtra("REMIND_MINUTES", remindMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d(
                "ALARM_TEST",
                "canScheduleExact=${alarmManager.canScheduleExactAlarms()}"
            )
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )

        Log.d("ALARM_TEST", "alarm registered")
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