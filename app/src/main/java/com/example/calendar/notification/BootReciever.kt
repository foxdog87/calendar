package com.example.calendar.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.calendar.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId // ★ 追加
import java.time.ZoneOffset

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // ★ 修正：システムローカルタイムゾーンでEpoch秒を取得
                    val currentEpoch = LocalDateTime.now()
                        .atZone(ZoneId.systemDefault())
                        .toEpochSecond()

                    val database = AppDatabase.getDatabase(context.applicationContext, this)
                    val taskDao = database.taskDao()

                    val futureTasks = taskDao.getFutureTasksWithReminderDirect(currentEpoch)
                    val scheduler = TaskAlarmScheduler(context)

                    futureTasks.forEach { task ->
                        scheduler.schedule(task)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}