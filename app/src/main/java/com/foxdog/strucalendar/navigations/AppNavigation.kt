package com.foxdog.strucalendar

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.foxdog.strucalendar.data.dao.ChecklistItemDao
import com.foxdog.strucalendar.screens.calendar.CalendarScreen
import com.foxdog.strucalendar.screens.TaskListScreen
import com.foxdog.strucalendar.screens.taskcreate.TaskCreateScreen
import com.foxdog.strucalendar.screens.DateDetailScreen
import com.foxdog.strucalendar.screens.TaskDetailScreen
import com.foxdog.strucalendar.screens.SettingsScreen
import com.foxdog.strucalendar.viewmodel.CalendarViewModel
import com.foxdog.strucalendar.viewmodel.DateDetailViewModel
import com.foxdog.strucalendar.viewmodel.TaskDetailViewModel
import com.foxdog.strucalendar.viewmodel.TaskListViewModel
import com.foxdog.strucalendar.viewmodel.TaskCreateViewModel
import com.foxdog.strucalendar.data.dao.TaskDao
import com.foxdog.strucalendar.data.dao.TemplateChecklistItemDao
import com.foxdog.strucalendar.data.osm.OsmRepository
import com.foxdog.strucalendar.data.repository.TaskRepository
import com.foxdog.strucalendar.data.repository.TagRepository
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import com.foxdog.strucalendar.data.dao.TaskTagDao
import com.foxdog.strucalendar.data.dao.TagDao
import com.foxdog.strucalendar.data.dao.TagDisplayOrderDao
import com.foxdog.strucalendar.data.dao.TemplateDao
import com.foxdog.strucalendar.data.dao.TemplateTagDao
import com.foxdog.strucalendar.data.dao.TemplateDisplayOrderDao
import com.foxdog.strucalendar.data.AppDatabase
import com.foxdog.strucalendar.data.dao.TagCustomFieldDao
import com.foxdog.strucalendar.data.repository.TemplateRepository
import com.foxdog.strucalendar.data.repository.TemplateTagRepository
import com.foxdog.strucalendar.data.settings.SettingsRepository
import com.foxdog.strucalendar.screens.templatecreate.TemplateCreateScreen
import com.foxdog.strucalendar.viewmodel.SettingViewModel
import com.foxdog.strucalendar.viewmodel.TemplateCreateViewModel

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
    val editTemplateId: Long? = null
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
    tagCustomFieldDao: TagCustomFieldDao,
    calendarViewModel: CalendarViewModel,
    settingsRepository: SettingsRepository,
    countryCode: String,
    onNotificationPermissionNeeded: () -> Unit = {},
    initialTaskId: Long? = null
) {
    val navController = rememberNavController()

    LaunchedEffect(initialTaskId) {
        if (initialTaskId != null) {
            navController.navigate(
                TaskDetailRoute(taskId = initialTaskId)
            ) {
                launchSingleTop = true
            }
        }
    }

    val context = LocalContext.current

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
        taskTagDao,
        tagCustomFieldDao
    ) {
        TagRepository(
            tagDao = tagDao,
            tagDisplayOrderDao = tagDisplayOrderDao,
            taskTagDao = taskTagDao,
            tagCustomFieldDao = tagCustomFieldDao
        )
    }

    val templateRepository = remember(
        templateDao,
        templateTagDao,
        templateChecklistItemDao
    ) {
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
        startDestination = if (initialTaskId != null) {
            TaskDetailRoute(initialTaskId)
        } else {
            CalendarRoute
        },
        modifier = Modifier.background(
            MaterialTheme.colorScheme.background
        )
    ) {

        // --- S1: カレンダー画面 ---
        composable<CalendarRoute> {
            CalendarScreen(
                viewModel = calendarViewModel,
                onNavigateToTaskCreate = { selectedDate ->
                    val epochMillis =
                        selectedDate
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()

                    navController.navigate(
                        TaskCreateRoute(dateMillis = epochMillis)
                    ) {
                        launchSingleTop = true
                    }
                },
                onNavigateToTaskList = {
                    navController.navigate(TaskListRoute) {
                        launchSingleTop = true
                    }
                },
                onNavigateToDateDetail = { selectedDate ->
                    val epochMillis =
                        selectedDate
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()

                    navController.navigate(
                        DateDetailRoute(dateMillis = epochMillis)
                    ) {
                        launchSingleTop = true
                    }
                },
                // タスク詳細へのナビゲーションを追加
                onNavigateToTaskDetail = { taskId ->
                    navController.navigate(
                        TaskDetailRoute(taskId = taskId)
                    ) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(SettingsRoute) {
                        launchSingleTop = true
                    }
                },
            )
        }

        // --- S2: 日付詳細画面 ---
        composable<DateDetailRoute> { backStackEntry ->
            val route: DateDetailRoute = backStackEntry.toRoute()

            val detailViewModel: DateDetailViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(
                            modelClass: Class<T>
                        ): T {
                            return DateDetailViewModel(
                                taskDao = taskDao,
                                holidayDao = database.holidayDao(),
                                settingsRepository = settingsRepository,
                                deviceCountryCode = countryCode
                            ) as T
                        }
                    }
                )

            DateDetailScreen(
                dateMillis = route.dateMillis,
                viewModel = detailViewModel,
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                onNavigateToTaskDetail = { taskId ->
                    navController.navigate(
                        TaskDetailRoute(taskId = taskId)
                    ) {
                        launchSingleTop = true
                    }
                },
                onNavigateToCreateTask = { dateMs ->
                    navController.navigate(
                        TaskCreateRoute(dateMillis = dateMs)
                    ) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // --- S3: 予定一覧画面 ---
        composable<TaskListRoute> {
            val listViewModel: TaskListViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(
                            modelClass: Class<T>
                        ): T {
                            return TaskListViewModel(
                                taskRepository = taskRepository,
                                tagRepository = tagRepository,
                                settingsRepository = settingsRepository
                            ) as T
                        }
                    }
                )

            TaskListScreen(
                viewModel = listViewModel,
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                onNavigateToTaskDetail = { taskId ->
                    navController.navigate(
                        TaskDetailRoute(taskId = taskId)
                    ) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // --- S4: 予定作成画面 ---
        composable<TaskCreateRoute> { backStackEntry ->
            val route: TaskCreateRoute = backStackEntry.toRoute()
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
                                templateRepository = templateRepository,
                                osmRepository = osmRepository,
                                checklistItemDao = checklistItemDao,
                                templateChecklistItemDao = templateChecklistItemDao,
                                templateTagRepository = templateTagRepository,
                                settingsRepository = settingsRepository,
                                taskCustomFieldValueDao = database.taskCustomFieldValueDao(),
                                templateCustomFieldValueDao = database.templateCustomFieldValueDao(),
                                tagCustomFieldDao = tagCustomFieldDao
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
                        Instant
                            .ofEpochMilli(route.dateMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()

                    taskCreateViewModel.prepareCreateTask(
                        selectedDate
                    )
                }
            }

            TaskCreateScreen(
                viewModel = taskCreateViewModel,
                onNotificationPermissionNeeded = onNotificationPermissionNeeded,
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                onNavigateToTemplateCreate = {
                    navController.navigate(TemplateCreateRoute()) {
                        launchSingleTop = true
                    }
                },
                onNavigateToTemplateEdit = { templateId ->
                    navController.navigate(
                        TemplateCreateRoute(
                            editTemplateId = templateId
                        )
                    ) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // --- S5: 予定詳細画面 ---
        composable<TaskDetailRoute> { backStackEntry ->
            val detailData =
                backStackEntry.toRoute<TaskDetailRoute>()

            val detailViewModel: TaskDetailViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(
                            modelClass: Class<T>
                        ): T {
                            return TaskDetailViewModel(
                                taskDao = taskDao,
                                checklistItemDao = checklistItemDao,
                                taskTagDao = taskTagDao,
                                taskCustomFieldValueDao =
                                    database.taskCustomFieldValueDao(),
                                tagCustomFieldDao = tagCustomFieldDao,
                                settingsRepository = settingsRepository
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
            val settingViewModel: SettingViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(
                            modelClass: Class<T>
                        ): T {
                            return SettingViewModel(
                                settingsRepository
                            ) as T
                        }
                    }
                )

            SettingsScreen(
                viewModel = settingViewModel,
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        }

        // --- S7: テンプレート作成画面 ---
        composable<TemplateCreateRoute> { backStackEntry ->
            val route: TemplateCreateRoute =
                backStackEntry.toRoute()

            val templateCreateViewModel: TemplateCreateViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(
                            modelClass: Class<T>
                        ): T {
                            return TemplateCreateViewModel(
                                templateRepository = templateRepository,
                                tagRepository = tagRepository,
                                templateCustomFieldValueDao =
                                    database.templateCustomFieldValueDao(),
                                tagCustomFieldDao = tagCustomFieldDao,
                                settingsRepository = settingsRepository
                            ) as T
                        }
                    }
                )

            LaunchedEffect(route) {
                if (route.editTemplateId != null) {
                    templateCreateViewModel.loadTemplateForEdit(
                        route.editTemplateId
                    )
                }
            }

            TemplateCreateScreen(
                viewModel = templateCreateViewModel,
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}