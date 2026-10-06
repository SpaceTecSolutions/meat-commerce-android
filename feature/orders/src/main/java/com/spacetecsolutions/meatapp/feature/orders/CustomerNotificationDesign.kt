package com.spacetecsolutions.meatapp.feature.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.model.AppNotification
import com.spacetecsolutions.meatapp.core.model.NotificationCategory
import com.spacetecsolutions.meatapp.core.model.NotificationEvent

private object NoticeColors {
    val canvas = Color(0xFFF8F9FA)
    val ink = Color(0xFF191C1E)
    val muted = Color(0xFF626870)
    val red = Color(0xFFD71920)
    val border = Color(0xFFF0F1F3)
}

@Composable
internal fun CustomerNotificationHistory(
    state: NotificationHistoryState,
    back: () -> Unit,
    continueShopping: () -> Unit,
    emptyActionLabel: String,
    markAllRead: () -> Unit,
    clearAll: () -> Unit,
    retry: () -> Unit,
    open: (AppNotification) -> Unit,
    permissionGranted: Boolean,
    requestPermission: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(NoticeColors.canvas)
        .statusBarsPadding().navigationBarsPadding()) {
        CustomerNotificationTopBar(back, state.items.isNotEmpty(),
            state.items.any { !it.isRead }, clearAll, markAllRead)
        if (!permissionGranted) Surface(color = Color(0xFFFFF7ED), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Enable alerts for important updates", Modifier.weight(1f), fontSize = 12.sp)
                TextButton(onClick = requestPermission) { Text("Enable") }
            }
        }
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NoticeColors.red)
            }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error, color = NoticeColors.muted)
                    TextButton(onClick = retry) { Text("Retry") }
                }
            }
            state.items.isEmpty() -> CustomerNotificationsEmpty(continueShopping, emptyActionLabel)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = 440.dp),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = AppNotification::id) { item ->
                    CustomerNotificationItem(item) { open(item) }
                }
            }
        }
    }
}

@Composable
private fun CustomerNotificationTopBar(back: () -> Unit, hasItems: Boolean,
    hasUnread: Boolean, clearAll: () -> Unit, markAllRead: () -> Unit) {
    Surface(color = Color.White, border = BorderStroke(1.dp, NoticeColors.border)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back, modifier = Modifier.size(40.dp)) {
                Icon(AppIcons.Back, "Back", tint = NoticeColors.ink)
            }
            Text("Notifications", modifier = Modifier.weight(1f).padding(start = 7.dp),
                fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = NoticeColors.ink,
                maxLines = 1)
            if (hasItems) {
                TextButton(onClick = clearAll, contentPadding = PaddingValues(horizontal = 3.dp)) {
                    Text("Clear all", fontSize = 11.sp, color = NoticeColors.muted)
                }
                Text(" · ", color = Color(0xFFD1D5DB))
            }
            TextButton(onClick = markAllRead, enabled = hasUnread,
                contentPadding = PaddingValues(horizontal = 2.dp)) {
                Text("Mark all as read", fontSize = 11.sp,
                    color = if (hasUnread) NoticeColors.red else Color(0xFFC7C9CC),
                    maxLines = 1)
            }
        }
    }
}

@Composable
private fun CustomerNotificationsEmpty(continueShopping: () -> Unit, actionLabel: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(80.dp), shape = CircleShape,
                color = Color(0xFFF4F5F6), border = BorderStroke(6.dp, Color.White)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(AppIcons.NotificationsOutline, null, Modifier.size(36.dp),
                        tint = Color(0xFF9DA1A4))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("No notifications yet", fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                color = NoticeColors.ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text("Order updates, delivery tracking alerts, and account notices will appear here once you place an order.",
                fontSize = 14.sp, lineHeight = 22.sp, color = NoticeColors.muted,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            OutlinedButton(onClick = continueShopping, shape = CircleShape,
                border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                contentPadding = PaddingValues(horizontal = 25.dp, vertical = 9.dp)) {
                Text(actionLabel, color = NoticeColors.ink, fontWeight = FontWeight.Medium)
            }
        }
    }
}

private data class NoticeAppearance(val icon: ImageVector, val tint: Color,
    val iconBackground: Color, val unreadBackground: Color, val unreadBorder: Color)

private fun AppNotification.appearance(): NoticeAppearance = when {
    category == NotificationCategory.PAYMENT &&
        (title.contains("fail", true) || title.contains("cancel", true)) -> NoticeAppearance(
            AppIcons.Card, NoticeColors.red, Color(0xFFFEE2E2), Color(0xFFFFF8F8), Color(0xFFFECACA))
    category == NotificationCategory.PAYMENT -> NoticeAppearance(
        AppIcons.Card, Color(0xFF059669), Color(0xFFDDFBF0), Color(0xFFF5FFFB), Color(0xFFA7F3D0))
    event == NotificationEvent.DELIVERY_DELAYED -> NoticeAppearance(
        AppIcons.Pending, Color(0xFFB45309), Color(0xFFFEF9C3), Color(0xFFFFFDF0), Color(0xFFFDE68A))
    event == NotificationEvent.ORDER_OUT_FOR_DELIVERY || category == NotificationCategory.DELIVERY -> NoticeAppearance(
        AppIcons.DeliveryPerson, Color(0xFF2563EB), Color(0xFFDBEAFE), Color(0xFFF7FAFF), Color(0xFFBFDBFE))
    event == NotificationEvent.ORDER_DELIVERED || event == NotificationEvent.ORDER_CONFIRMED -> NoticeAppearance(
        AppIcons.Delivered, Color(0xFF16A34A), Color(0xFFDCFCE7), Color(0xFFF5FFF8), Color(0xFFBBF7D0))
    else -> NoticeAppearance(AppIcons.Orders, Color(0xFFD97706), Color(0xFFFEF3C7),
        Color(0xFFFFFCF5), Color(0xFFFDE68A))
}

@Composable
private fun CustomerNotificationItem(item: AppNotification, onClick: () -> Unit) {
    val visual = item.appearance()
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp),
        color = if (item.isRead) Color.White else visual.unreadBackground,
        border = BorderStroke(1.dp, if (item.isRead) NoticeColors.border else visual.unreadBorder),
        shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape,
                color = visual.iconBackground) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(visual.icon, null, Modifier.size(20.dp), tint = visual.tint)
                }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(item.title, Modifier.weight(1f), fontSize = 14.sp,
                        lineHeight = 18.sp, fontWeight = FontWeight.SemiBold,
                        color = NoticeColors.ink)
                    Text(notificationTime(item.createdAtEpochMillis).lowercase(),
                        fontSize = 11.sp, color = Color(0xFF92979C), maxLines = 1)
                }
                Text(item.body, fontSize = 12.sp, lineHeight = 18.sp,
                    color = NoticeColors.muted)
            }
            if (!item.isRead) Box(Modifier.padding(start = 9.dp, top = 6.dp)
                .size(8.dp).background(NoticeColors.red, CircleShape))
        }
    }
}
