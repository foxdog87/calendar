package com.example.calendar.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("TASK_ID", -1L)
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "予定の時間です"
        val remindMinutes = intent.getIntExtra("REMIND_MINUTES", 0)

        if (taskId == -1L) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "task_reminder_channel"

        // Android 8.0以上は通知チャンネルが必須
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "タスクリマインダー通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "予定の事前通知を行います"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 通知をタップしたときにアプリを開く設定（適切なActivityに書き換えてください）
        // ここでは仮に一般的な「MainActivity」宛てにしています
        val text = if (remindMinutes > 0) "開始の${remindMinutes}分前です" else "開始時間になりました"

        val contentIntent = Intent(context, Class.forName("${context.packageName}.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TARGET_TASK_ID", taskId) // アプリ起動時に詳細画面へ飛ばしたい場合用
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 通知のビルド
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // アプリ固有のアイコンがあれば差し替えてください
            .setContentTitle(taskTitle)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // 通知を発行（通知IDにtaskIdを使うことで重複を防ぐ）
        notificationManager.notify(taskId.toInt(), notification)
    }
}