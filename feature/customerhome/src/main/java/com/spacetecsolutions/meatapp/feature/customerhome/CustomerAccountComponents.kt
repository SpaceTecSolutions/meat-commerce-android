package com.spacetecsolutions.meatapp.feature.customerhome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons

internal object AccountStyle {
    val canvas = Color(0xFFF8F9FB)
    val ink = Color(0xFF191C1E)
    val muted = Color(0xFF64748B)
    val red = Color(0xFFD71920)
    val line = Color(0xFFE3E8EF)
}

@Composable
internal fun AccountTopBar(title: String, back: () -> Unit) {
    Surface(color = Color.White, border = BorderStroke(1.dp, AccountStyle.line)) {
        Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back, modifier = Modifier.size(42.dp)) {
                Icon(AppIcons.Back, "Back", tint = AccountStyle.ink)
            }
            Text(title, Modifier.padding(start = 9.dp), fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold, color = AccountStyle.ink)
        }
    }
}

@Composable
internal fun AccountCard(modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = Color.White,
        border = BorderStroke(1.dp, AccountStyle.line), shadowElevation = 1.dp) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
internal fun AccountSectionLabel(title: String) {
    Text(title.uppercase(), fontSize = 12.sp, letterSpacing = 1.sp,
        fontWeight = FontWeight.Bold, color = AccountStyle.muted)
}
