package com.example.calendar.screens.templatecreate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.components.TagCreateDialog
import com.example.calendar.components.TagIconSource
import com.example.calendar.data.entity.Tag
import com.example.calendar.screens.taskcreate.AutoCompleteAndColorSection
import com.example.calendar.screens.taskcreate.ChecklistSection
import com.example.calendar.screens.taskcreate.MemoSection
import com.example.calendar.screens.taskcreate.TagSection
import com.example.calendar.screens.taskcreate.TitleSection
import com.example.calendar.state.TemplateInputState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateCreateContent(
    templateState: TemplateInputState,
    availableTags: List<Tag>,
    isTitleError: Boolean = false,
    onNavigateBack: () -> Unit,
    onSaveTemplate: () -> Unit,
    onUpdateInput: ((TemplateInputState) -> TemplateInputState) -> Unit,
    onToggleTagSelection: (Tag) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    onCreateTag: (Tag) -> Unit,
    onUpdateTagOrder: (List<Tag>) -> Unit,
) {
    val scrollState = rememberScrollState()
    var showTagCreateDialog by remember { mutableStateOf(false) }
    var isTagFolderExpanded by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<Tag?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("テンプレートを作成", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "閉じる", tint = Color(0xFF1C1B1F), modifier = Modifier.size(24.dp))
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSaveTemplate,
                        modifier = Modifier.padding(end = 8.dp).size(36.dp).background(Color(0xFF1A73E8), CircleShape)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "保存", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            TitleSection(
                title = templateState.title,
                isTitleError = isTitleError,
                label = "テンプレート名 (必須)",
                placeholder = "テンプレート名を入力",
                onTitleChange = { newTitle -> onUpdateInput { it.copy(title = newTitle) } }
            )

            DurationSection(
                durationMinutes = templateState.durationMinutes,
                onDurationChange = { minutes -> onUpdateInput { it.copy(durationMinutes = minutes) } }
            )

            TagSection(
                availableTags = availableTags,
                selectedTags = templateState.selectedTags,
                isTagFolderExpanded = isTagFolderExpanded,
                onExpandToggle = { isTagFolderExpanded = !isTagFolderExpanded },
                onToggleTagSelection = onToggleTagSelection,
                onUpdateTagOrder = onUpdateTagOrder,
                onDeleteTagRequest = { tag -> tagToDelete = tag },
                onAddTagClick = { showTagCreateDialog = true }
            )

            MemoSection(
                memo = templateState.memo,
                onMemoChange = { newMemo -> onUpdateInput { it.copy(memo = newMemo) } }
            )

            ChecklistSection(
                checkList = templateState.checkList,
                onCheckListChange = { newList -> onUpdateInput { it.copy(checkList = newList) } }
            )

            Spacer(modifier = Modifier.height(20.dp))

            AutoCompleteAndColorSection(
                isAutoCompleted = templateState.isAutoCompleted,
                onAutoCompletedChange = { isAuto -> onUpdateInput { it.copy(isAutoCompleted = isAuto) } },
                onInfoClick = {},
                selectedColor = templateState.color ?: Color(0xFF4285F4).toArgb(),
                onColorSelected = { color -> onUpdateInput { it.copy(color = color) } }
            )
        }
    }

    if (tagToDelete != null) {
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("タグの削除", fontWeight = FontWeight.Bold) },
            text = { Text("タグ「${tagToDelete?.name}」を削除しますか？\n(この操作は取り消せません)") },
            dismissButton = { TextButton(onClick = { tagToDelete = null }) { Text("キャンセル") } },
            confirmButton = {
                Button(
                    onClick = { tagToDelete?.let(onDeleteTag); tagToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("削除する") }
            }
        )
    }

    if (showTagCreateDialog) {
        TagCreateDialog(
            onDismissRequest = { showTagCreateDialog = false },
            onTagSave = { name, iconSource, color, _ ->
                val iconString = when (iconSource) {
                    is TagIconSource.InitialText -> null
                    is TagIconSource.Vector -> iconSource.iconId.id
                }
                onCreateTag(Tag(tagId = 0L, name = name, color = color.toArgb(), icon = iconString))
                showTagCreateDialog = false
            }
        )
    }
}