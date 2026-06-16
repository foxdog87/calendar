package com.example.calendar

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.calendar.screens.CalendarScreen
import com.example.calendar.screens.TaskListScreen
import com.example.calendar.screens.TaskCreateScreen
import com.example.calendar.screens.DateDetailScreen
import com.example.calendar.screens.TaskDetailScreen
import com.example.calendar.screens.SettingsScreen
import com.example.calendar.viewmodel.CalendarViewModel
import kotlinx.serialization.Serializable
import java.time.ZoneId

// ==========================================
// ★プログラミング原則：全画面の型安全なルート定義（完全網羅）
// ==========================================
@Serializable
object CalendarRoute     // S1: カレンダー画面（スタート地点）

@Serializable
data class DateDetailRoute(val dateMillis: Long) // S2: 日付詳細画面

@Serializable
object TaskListRoute     // S3: 予定一覧画面

@Serializable
object TaskCreateRoute   // S4: 予定作成画面

@Serializable
data class TaskDetailRoute(val taskId: Long) // S5: 予定詳細画面（どの予定かをIDで渡す）

@Serializable
object SettingsRoute     // S6: 設定画面

/**
 * アプリ全体の全画面（S1〜S6）を一本の線で繋ぐ中央ナビゲーションハブ。
 */
@Composable
fun AppNavigation(
    viewModel: CalendarViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = CalendarRoute // カレンダー画面からアプリ起動
    ) {
        // ------------------------------------------------------
        // S1: カレンダー画面
        // ------------------------------------------------------
        composable<CalendarRoute> {
            CalendarScreen(
                onNavigateToTaskCreate = {
                    navController.navigate(TaskCreateRoute)
                },
                onNavigateToTaskList = {
                    navController.navigate(TaskListRoute)
                },
                onNavigateToDateDetail = { selectedDate ->
                    val epochMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    navController.navigate(DateDetailRoute(dateMillis = epochMillis))
                },
                // モックアップ仕様：左上三本線（ハンバーガー）から設定へ
                onNavigateToSettings = {
                    navController.navigate(SettingsRoute)
                }
            )
        }

        // ------------------------------------------------------
        // S2: 日付詳細画面
        // ------------------------------------------------------
        composable<DateDetailRoute> { backStackEntry ->
            val dateDetail = backStackEntry.toRoute<DateDetailRoute>()
            DateDetailScreen(
                dateMillis = dateDetail.dateMillis,
                onNavigateBack = {
                    navController.popBackStack()
                },
                // モックアップ仕様：タイムラインの予定をタップして詳細へ
                onNavigateToTaskDetail = { taskId ->
                    navController.navigate(TaskDetailRoute(taskId = taskId))
                }
            )
        }

        // ------------------------------------------------------
        // S3: 予定一覧画面
        // ------------------------------------------------------
        composable<TaskListRoute> {
            TaskListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToTaskDetail = { taskId ->
                    navController.navigate(TaskDetailRoute(taskId = taskId))
                }
                // ※ 実際のデータをViewModel等から渡す場合は、ここに allTasksWithTags = viewModel.state... のように追加します
            )
        }

        // ------------------------------------------------------
        // S4: 予定作成画面
        // ------------------------------------------------------
        composable<TaskCreateRoute> {
            TaskCreateScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ------------------------------------------------------
        // S5: 予定詳細画面（★新規結合）
        // ------------------------------------------------------
        composable<TaskDetailRoute> { backStackEntry ->
            val detailData = backStackEntry.toRoute<TaskDetailRoute>()
            TaskDetailScreen(
                taskId = detailData.taskId,
                onNavigateBack = {
                    navController.popBackStack() // 詳細から前の画面（一覧 or 日付詳細）に戻る
                }
            )
        }

        // ------------------------------------------------------
        // S6: 設定画面（★新規結合）
        // ------------------------------------------------------
        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack() // 設定からカレンダーに戻る
                }
            )
        }
    }
}