package com.example.calendar.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class TagIconId(
    val id: String,
    val vector: ImageVector
) {
    BOOK("book", Icons.Default.MenuBook),
    WARNING("warning", Icons.Default.Warning),
    FIRE("fire", Icons.Default.LocalFireDepartment),
    SCHOOL("school", Icons.Default.School),
    WORK("work", Icons.Default.Work),
    STAR("star", Icons.Default.Star),
    FAVORITE("favorite", Icons.Default.Favorite);
    }