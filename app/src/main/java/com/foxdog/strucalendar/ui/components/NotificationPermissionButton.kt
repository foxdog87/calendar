package com.foxdog.strucalendar.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun NotificationPermissionButton(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
    canRequestPermission: Boolean,
    context: Context
) {

    Button(
        onClick = {

            Log.d(
                "PermissionDebug",
                "ボタン押下"
            )

            onRequestPermission()

        },

        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1565C0),
            contentColor = Color.White
        ),

        shape = RoundedCornerShape(8.dp),

        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)

    ) {

        Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Text(
            if (canRequestPermission)
                "タップして通知を許可する"
            else
                "通知設定を開く"
        )
    }
}