package com.example.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import com.example.calendar.viewmodel.DateDetailViewModel
import com.example.calendar.viewmodel.TaskDetailViewModel
import com.example.calendar.viewmodel.TaskListViewModel
import com.example.calendar.viewmodel.TaskCreateViewModel
import com.example.calendar.data.dao.TaskDao
import com.example.calendar.data.repository.TaskRepository
import com.example.calendar.data.repository.TagRepository
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId

// ==========================================
// ★型安全なルート定義（完全網羅）
// ==========================================
@Serializable
object CalendarRoute

@Serializable
data class DateDetailRoute(val dateMillis: Long)

@Serializable
object TaskListRoute

// ★ 修正1：予定作成画面ルートに日付（ミリ秒）を持たせるように変更
@Serializable
data class TaskCreateRoute(val dateMillis: Long)

@Serializable
data class TaskDetailRoute(val taskId: Long)

@Serializable
object SettingsRoute

/**
 * アプリ全体の全画面を一本の線で繋ぐ中央ナビゲーションハブ。
 * 根底からの修正として、ViewModelが要求するリポジトリ群を生成・分配します。
 */
@Composable
fun AppNavigation(
    taskDao: TaskDao,
    calendarViewModel: CalendarViewModel
) {
    val navController = rememberNavController()

    // ★ 永続化の核：Daoを元に、構成図通りのRepositoryインスタンスをナビゲーション層で準備
    val taskRepository = remember(taskDao) { TaskRepository(taskDao) }
    val tagRepository = remember(taskDao) { TagRepository(taskDao) }

    NavHost(
        navController = navController,
        startDestination = CalendarRoute
    ) {
// ------------------------------------------------------
// S1: カレンダー画面
// ------------------------------------------------------
        composable<CalendarRoute> {
            CalendarScreen(
                viewModel = calendarViewModel,
                // ★ 修正2：CalendarScreenから選択された日付（selectedDate）を受け取って遷移する
                onNavigateToTaskCreate = { selectedDate ->
                    val epochMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    navController.navigate(TaskCreateRoute(dateMillis = epochMillis))
                },
                onNavigateToTaskList = {
                    navController.navigate(TaskListRoute)
                },
                onNavigateToDateDetail = { selectedDate ->
                    val epochMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    navController.navigate(DateDetailRoute(dateMillis = epochMillis))
                },
                onNavigateToSettings = {
                    navController.navigate(SettingsRoute)
                }
            )
        }

        // ------------------------------------------------------
        // S2: 日付詳細画面
        // ------------------------------------------------------
        composable<DateDetailRoute> { backStackEntry ->
            val route: DateDetailRoute = backStackEntry.toRoute()

            val detailViewModel: DateDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return DateDetailViewModel(taskDao) as T
                    }
                }
            )

            DateDetailScreen(
                dateMillis = route.dateMillis,
                viewModel = detailViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTaskDetail = { taskId ->
                    navController.navigate(TaskDetailRoute(taskId = taskId))
                }
            )
        }

// ------------------------------------------------------
// S3: 予定一覧画面
// ------------------------------------------------------
        composable<TaskListRoute> {
            // ★ 修正：ファクトリ内で2つのRepositoryをコンストラクタに引き渡す形にリライト
            val listViewModel: TaskListViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return TaskListViewModel(
                            taskRepository = taskRepository,
                            tagRepository = tagRepository
                        ) as T
                    }
                }
            )

            TaskListScreen(
                viewModel = listViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToTaskDetail = { taskId ->
                    navController.navigate(TaskDetailRoute(taskId = taskId))
                }
            )
        }

        // ------------------------------------------------------
        // S4: 予定作成画面
        // ------------------------------------------------------
        composable<TaskCreateRoute> { backStackEntry ->
            // ナビゲーション経由で渡ってきた引数（日付のミリ秒）を取得
            val route: TaskCreateRoute = backStackEntry.toRoute()

            // ★ 修正：ファクトリ内で2つのRepositoryをコンストラクタに引き渡す形にリライト
            val TaskCreateViewModel: TaskCreateViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return TaskCreateViewModel(
                            taskRepository = taskRepository,
                            tagRepository = tagRepository
                        ) as T
                    }
                }
            )

            // ★ 修正3：画面が生成されたタイミングで、同じブロック内にある「TaskCreateViewModel」に日付をセット！
            LaunchedEffect(route.dateMillis) {
                val selectedDate = Instant.ofEpochMilli(route.dateMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                TaskCreateViewModel.prepareCreateTask(selectedDate)
            }

            TaskCreateScreen(
                viewModel = TaskCreateViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ------------------------------------------------------
        // S5: 予定詳細画面
        // ------------------------------------------------------
        composable<TaskDetailRoute> { backStackEntry ->
            val detailData = backStackEntry.toRoute<TaskDetailRoute>()

            val detailViewModel: TaskDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return TaskDetailViewModel(taskDao) as T
                    }
                }
            )

            TaskDetailScreen(
                taskId = detailData.taskId,
                viewModel = detailViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ------------------------------------------------------
        // S6: 設定画面
        // ------------------------------------------------------
        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}