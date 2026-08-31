package com.foxdog.strucalendar.screens.taskcreate

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.foxdog.strucalendar.components.NotificationPermissionButton
import com.foxdog.strucalendar.viewmodel.TaskCreateViewModel

@Composable
fun TaskCreateScreen(
    viewModel: TaskCreateViewModel,
    onNavigateBack: () -> Unit,
    onNotificationPermissionNeeded: () -> Unit = {},
    onNavigateToTemplateCreate: () -> Unit,
    onNavigateToTemplateEdit: (Long) -> Unit
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    fun openNotificationSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
        context.startActivity(intent)
    }

    // 現在の通知権限ステータスを確認
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Android 12以下の場合はデフォルトで許可扱い
            }
        )
    }

    val prefs = remember {
        context.getSharedPreferences("notification_permission", Context.MODE_PRIVATE)
    }

    var permissionRequestCount by remember {
        mutableIntStateOf(prefs.getInt("request_count", 0))
    }

    var showNotificationSettingDialog by remember { mutableStateOf(false) }

    val shouldOpenSettings =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                permissionRequestCount >= 2 &&
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                )

    // 権限リクエストダイアログのランチャー
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("PermissionDebug", "結果: granted=$isGranted")

        hasNotificationPermission = isGranted

        if (!isGranted) {
            permissionRequestCount++
            prefs.edit().putInt("request_count", permissionRequestCount).apply()
            Log.d("PermissionDebug", "拒否回数=$permissionRequestCount")
        }
    }

    NotificationPermissionButton(
        onRequestPermission = {
            println("通知ボタン押下")

            if (shouldOpenSettings) {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            } else {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        canRequestPermission = true,
        context = context
    )

    val lifecycleOwner = LocalLifecycleOwner.current

    androidx.compose.runtime.LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            hasNotificationPermission =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
        }
    }

    // 画面を開いたタイミングでだけ「最近使用したテンプレート」の順番を更新する。
    LaunchedEffect(Unit) {
        viewModel.refreshRecentTemplates()
    }

    val availableTags by viewModel.allTags.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val recentTemplates by viewModel.recentTemplates.collectAsState()
    val osmSearchResults = viewModel.osmSearchResults
    val isOsmSearching = viewModel.isOsmSearching

    // 予定作成画面のオンボーディング表示可否
    val settings by viewModel.settings.collectAsState()
    val showOnboarding = !settings.taskCreateOnboardingCompleted

    TaskCreateContent(
        taskState = viewModel.inputState,
        availableTags = availableTags,
        templates = templates,
        recentTemplates = recentTemplates,
        isTitleError = viewModel.isTitleError,
        isEditMode = viewModel.isEditMode,
        isDateTimeError = viewModel.isDateTimeError,
        isSaving = viewModel.isSaving,
        onNotificationPermissionNeeded = onNotificationPermissionNeeded,
        onNavigateBack = onNavigateBack,
        onSaveTask = { viewModel.saveTask(context = context, onSuccess = onNavigateBack) },
        onUpdateInput = { update -> viewModel.updateInput(update) },
        onToggleTagSelection = { tag -> viewModel.toggleTaskTagSelection(tag) },
        onDeleteTag = { tag -> viewModel.deleteTag(tag) },
        onCreateTag = { tag, customFieldNames ->
            viewModel.createTag(
                tag = tag,
                customFieldNames = customFieldNames
            )
        },
        onUpdateTag = { tag, customFieldNames ->
            viewModel.updateTag(
                tag = tag,
                customFieldNames = customFieldNames
            )
        },
        onLoadCustomFieldsForTag = { tagId -> viewModel.getCustomFieldNamesForTag(tagId) },
        onApplyTemplate = { template -> viewModel.applyTemplate(template) },
        onUpdateTagOrder = { tags -> viewModel.updateTagOrder(tags) },
        onUpdateTemplateOrder = { templates -> viewModel.updateTemplateOrder(templates) },
        onNavigateToTemplateCreate = onNavigateToTemplateCreate,
        osmSearchResults = osmSearchResults,
        isOsmSearching = isOsmSearching,
        onSearchOsmPoi = { keyword -> viewModel.searchOsmPoi(keyword) },
        onSelectOsmPoi = { poi -> viewModel.selectOsmPoi(poi) },
        onClearLocation = { viewModel.clearLocation() },
        onDeleteTemplate = { template -> viewModel.deleteTemplate(template) },
        onNavigateToTemplateEdit = onNavigateToTemplateEdit,
        onCustomFieldValueChange = { fieldId, value ->
            viewModel.updateCustomFieldValue(fieldId, value)
        },
        showOnboarding = showOnboarding,
        onOnboardingFinished = { viewModel.completeTaskCreateOnboarding() },
        alwaysShowDetailedSettings = settings.alwaysShowDetailedTaskSettings,
        showAllTutorialsCompletedDialog = viewModel.showAllTutorialsCompletedDialog,
        onDismissAllTutorialsCompletedDialog = { viewModel.dismissAllTutorialsCompletedDialog() },
        hasUnsavedChanges = viewModel.hasUnsavedChanges,
        confirmDiscardChanges = settings.confirmDiscardChanges,

    )

    if (showNotificationSettingDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationSettingDialog = false },
            title = { Text("通知設定について") },
            text = {
                Text(
                    """
                    通知の許可が拒否されています。

                    Androidの仕様により、これ以上アプリから許可を求めることができません。

                    設定画面から通知をONにしてください。
                    """.trimIndent()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationSettingDialog = false
                        openNotificationSettings()
                    }
                ) {
                    Text("設定を開く")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationSettingDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}