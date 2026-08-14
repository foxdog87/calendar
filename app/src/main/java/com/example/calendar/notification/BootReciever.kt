package com.example.calendar.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.room.Room
import com.example.calendar.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }


        val pendingResult = goAsync()


        CoroutineScope(Dispatchers.IO).launch {

            try {

                Log.d(
                    "BOOT_ALARM",
                    "端末起動後通知復元開始"
                )


                val currentEpoch =
                    LocalDateTime.now()
                        .atZone(ZoneId.systemDefault())
                        .toEpochSecond()


                val database =
                    AppDatabase.getDatabase(
                        context.applicationContext,
                        this
                    )


                val taskDao =
                    database.taskDao()


                val futureTasks =
                    taskDao.getFutureTasksWithReminderDirect(
                        currentEpoch
                    )


                val scheduler =
                    TaskAlarmScheduler(context)


                futureTasks.forEach { task ->

                    Log.d(
                        "BOOT_ALARM",
                        "再登録 task=${task.taskId}"
                    )

                    scheduler.schedule(task)
                }


            } catch (e: Exception) {

                Log.e(
                    "BOOT_ALARM",
                    "通知復元失敗",
                    e
                )

            } finally {

                pendingResult.finish()

            }

        }
    }
}