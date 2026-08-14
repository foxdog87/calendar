package com.example.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.calendar.data.dao.ChecklistItemDao
import com.example.calendar.screens.CalendarScreen
import com.example.calendar.screens.TaskListScreen
import com.example.calendar.screens.taskcreate.TaskCreateScreen
import com.example.calendar.screens.DateDetailScreen
import com.example.calendar.screens.TaskDetailScreen
import com.example.calendar.screens.SettingsScreen
import com.example.calendar.viewmodel.CalendarViewModel
import com.example.calendar.viewmodel.DateDetailViewModel
import com.example.calendar.viewmodel.TaskDetailViewModel
import com.example.calendar.viewmodel.TaskListViewModel
import com.example.calendar.viewmodel.TaskCreateViewModel
import com.example.calendar.data.dao.TaskDao
import com.example.calendar.data.dao.TemplateChecklistItemDao
import com.example.calendar.data.osm.OsmRepository
import com.example.calendar.data.repository.TaskRepository
import com.example.calendar.data.repository.TagRepository
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import com.example.calendar.data.dao.TaskTagDao
import com.example.calendar.data.dao.TagDao
import com.example.calendar.data.dao.TagDisplayOrderDao
import com.example.calendar.data.dao.TemplateDao
import com.example.calendar.data.dao.TemplateTagDao
import com.example.calendar.data.dao.TemplateDisplayOrderDao
import com.example.calendar.data.AppDatabase
import com.example.calendar.data.repository.TemplateRepository
import com.example.calendar.data.repository.TemplateTagRepository
import com.example.calendar.screens.templatecreate.TemplateCreateScreen
import com.example.calendar.viewmodel.TemplateCreateViewModel

@Serializable
object CalendarRoute

@Serializable
data class DateDetailRoute(val dateMillis: Long)

@Serializable
object TaskListRoute
@Serializable
data class TaskCreateRoute(
    val dateMillis: Long? = null,
    val editTaskId: Long? = null
)
@Serializable
data class TemplateCreateRoute(
    val editTemplateId: Long? = null // ★ 追加
)

@Serializable
data class TaskDetailRoute(val taskId: Long)

@Serializable
object SettingsRoute




@Composable
fun AppNavigation(
    taskDao: TaskDao,
    tagDao: TagDao,
    tagDisplayOrderDao: TagDisplayOrderDao,
    taskTagDao: TaskTagDao,
    checklistItemDao: ChecklistItemDao,
    templateChecklistItemDao: TemplateChecklistItemDao,
    database: AppDatabase,
    templateDao: TemplateDao,
    templateTagDao: TemplateTagDao,
    templateDisplayOrderDao: TemplateDisplayOrderDao,
    calendarViewModel: CalendarViewModel,
    initialTaskId: Long? = null
) {
    val navController = rememberNavController()
    LaunchedEffect(initialTaskId) {

        if (initialTaskId != null) {

            navController.navigate(
                TaskDetailRoute(
                    taskId = initialTaskId
                )
            ) {
                launchSingleTop = true
            }
        }
    }

    val taskRepository = remember(
        taskDao,
        taskTagDao,
        checklistItemDao
    ) {

        TaskRepository(
            taskDao = taskDao,
            taskTagDao = taskTagDao,
            checklistItemDao = checklistItemDao
        )
    }

    val tagRepository = remember(
        tagDao,
        tagDisplayOrderDao,
        taskTagDao
    ) {
        TagRepository(
            tagDao = tagDao,
            tagDisplayOrderDao = tagDisplayOrderDao,
            taskTagDao = taskTagDao
        )
    }

    val templateRepository = remember(templateDao, templateTagDao, templateChecklistItemDao) {
        TemplateRepository(
            templateDao = templateDao,
            templateTagDao = templateTagDao,
            templateChecklistItemDao = templateChecklistItemDao
        )
    }

    val templateTagRepository = remember(templateTagDao) {
        TemplateTagRepository(
            templateTagDao = templateTagDao
        )
    }

    NavHost(
        navController = navController,
        startDestination =
            if (initialTaskId != null)
                TaskDetailRoute(initialTaskId)
            else
                CalendarRoute
    ) {
        // --- S1: カレンダー画面 ---
        composable<CalendarRoute> {
            CalendarScreen(
                viewModel = calendarViewModel,
                onNavigateToTaskCreate = { selectedDate ->
                    val epochMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    navController.navigate(TaskCreateRoute(dateMillis = epochMillis)) { launchSingleTop = true }
                },
                onNavigateToTaskList = {
                    navController.navigate(TaskListRoute) { launchSingleTop = true }
                },
                onNavigateToDateDetail = { selectedDate ->
                    val epochMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    navController.navigate(DateDetailRoute(dateMillis = epochMillis)) { launchSingleTop = true }
                },
                onNavigateToSettings = {
                    navController.navigate(SettingsRoute) { launchSingleTop = true }
                }
            )
        }

        // --- S2: 日付詳細画面 ---
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
                // ★ 白画面対策：戻り先が存在する場合のみpopBackStackを実行する
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                onNavigateToTaskDetail = { taskId ->
                    navController.navigate(TaskDetailRoute(taskId = taskId)) { launchSingleTop = true }
                },
                onNavigateToCreateTask = { dateMs ->
                    navController.navigate(TaskCreateRoute(dateMillis = dateMs)) { launchSingleTop = true }
                }
            )
        }

        // --- S3: 予定一覧画面 ---
        composable<TaskListRoute> {
            val listViewModel: TaskListViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return TaskListViewModel(taskRepository = taskRepository, tagRepository = tagRepository) as T
                    }
                }
            )

            TaskListScreen(
                viewModel = listViewModel,
                // ★ 白画面対策：戻り先が存在する場合のみpopBackStackを実行する
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                onNavigateToTaskDetail = { taskId ->
                    navController.navigate(TaskDetailRoute(taskId = taskId)) { launchSingleTop = true }
                }
            )
        }

        // --- S4: 予定作成画面 ---
        composable<TaskCreateRoute> { backStackEntry ->

            val route: TaskCreateRoute = backStackEntry.toRoute()

            val context = LocalContext.current

            val osmRepository = remember {
                OsmRepository(context)
            }

            val taskCreateViewModel: TaskCreateViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = object : ViewModelProvider.Factory {

                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(
                            modelClass: Class<T>
                        ): T {

                            return TaskCreateViewModel(
                                taskRepository = taskRepository,
                                tagRepository = tagRepository,
                                templateRepository = templateRepository, // ★ 追加
                                osmRepository = osmRepository,
                                checklistItemDao = checklistItemDao,
                                templateChecklistItemDao = templateChecklistItemDao,
                                templateTagRepository =  templateTagRepository
                            ) as T
                        }
                    }
                )

            LaunchedEffect(route) {

                if (route.editTaskId != null) {

                    taskCreateViewModel.loadTaskForEdit(
                        route.editTaskId
                    )

                } else if (route.dateMillis != null) {

                    val selectedDate =
                        Instant.ofEpochMilli(route.dateMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()

                    taskCreateViewModel.prepareCreateTask(
                        selectedDate
                    )
                }
            }

            TaskCreateScreen(
                viewModel = taskCreateViewModel,
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) navController.popBackStack()
                },
                onNavigateToTemplateCreate = {
                    navController.navigate(TemplateCreateRoute()) { launchSingleTop = true }
                },
                onNavigateToTemplateEdit = { templateId -> // ★ 追加
                    navController.navigate(TemplateCreateRoute(editTemplateId = templateId)) { launchSingleTop = true }
                }
            )
        }

        composable<TemplateCreateRoute> {
            val templateViewModel: TemplateCreateViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        TemplateCreateViewModel(templateRepository, tagRepository) as T
                }
            )
            TemplateCreateScreen(templateViewModel) { navController.popBackStack() }
        }

        // --- S5: 予定詳細画面 ---
        composable<TaskDetailRoute> { backStackEntry ->

            val detailData = backStackEntry.toRoute<TaskDetailRoute>()

            val detailViewModel: TaskDetailViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = object : ViewModelProvider.Factory {

                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(
                            modelClass: Class<T>
                        ): T {

                            return TaskDetailViewModel(
                                taskDao = taskDao,
                                checklistItemDao = checklistItemDao
                            ) as T
                        }
                    }
                )


            TaskDetailScreen(
                taskId = detailData.taskId,
                viewModel = detailViewModel,

                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },

                onNavigateToEditTask = {
                    navController.navigate(
                        TaskCreateRoute(
                            editTaskId = detailData.taskId
                        )
                    ) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // --- S6: 設定画面 ---
        composable<SettingsRoute> {
            SettingsScreen(
                // ★ 白画面対策：戻り先が存在する場合のみpopBackStackを実行する
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        }
        // --- S7: テンプレート作成画面 ---
        composable<TemplateCreateRoute> { backStackEntry ->
            val route: TemplateCreateRoute = backStackEntry.toRoute() // ★ 追加

            val templateCreateViewModel: TemplateCreateViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return TemplateCreateViewModel(
                                templateRepository = templateRepository,
                                tagRepository = tagRepository
                            ) as T
                        }
                    }
                )

            LaunchedEffect(route) { // ★ 追加
                if (route.editTemplateId != null) {
                    templateCreateViewModel.loadTemplateForEdit(route.editTemplateId)
                }
            }

            TemplateCreateScreen(
                viewModel = templateCreateViewModel,
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) navController.popBackStack()
                }
            )
        }
    }
}
