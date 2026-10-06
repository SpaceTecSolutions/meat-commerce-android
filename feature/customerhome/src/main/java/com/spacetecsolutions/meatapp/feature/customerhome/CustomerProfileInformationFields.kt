package com.spacetecsolutions.meatapp.feature.customerhome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import com.spacetecsolutions.meatapp.core.model.User
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val fieldBorder = Color(0xFFDDE4F0)
private val subtle = Color(0xFF8996AB)
private val green = Color(0xFF059669)

@Composable
internal fun ProfileInformationTopBar(back: () -> Unit, editing: Boolean, edit: () -> Unit) {
    Surface(color = Color.White, border = BorderStroke(1.dp, Color(0xFFF1F3F5))) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back, modifier = Modifier.size(40.dp)) {
                Icon(AppIcons.Back, "Back", Modifier.size(24.dp), tint = CustomerProfileColors.ink)
            }
            Text("Profile Information", Modifier.weight(1f).padding(start = 12.dp),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = CustomerProfileColors.ink)
            if (!editing) OutlinedButton(onClick = edit, shape = CircleShape,
                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                contentPadding = PaddingValues(horizontal = 12.dp)) {
                Icon(AppIcons.Edit, null, Modifier.size(17.dp), tint = CustomerProfileColors.red)
                Spacer(Modifier.width(5.dp))
                Text("Edit", color = CustomerProfileColors.red)
            }
        }
    }
}

@Composable
internal fun ProfileInformationIdentity(user: User) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = Color.White, border = BorderStroke(1.dp, fieldBorder), shadowElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).background(Color(0xFFFEE2E2), CircleShape),
                contentAlignment = Alignment.Center) {
                Text(user.displayName.trim().split(' ').filter(String::isNotBlank)
                    .take(2).joinToString("") { it.take(1).uppercase() }.ifBlank { "C" },
                    fontSize = 23.sp, fontWeight = FontWeight.Bold, color = CustomerProfileColors.red)
            }
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.displayName.ifBlank { "Customer" }, Modifier.weight(1f, false),
                        fontWeight = FontWeight.Bold, color = CustomerProfileColors.ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (user.active) Text("✓ Active", Modifier.padding(start = 7.dp)
                        .background(Color(0xFFECFDF5), RoundedCornerShape(5.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                        fontSize = 10.sp, color = green)
                }
                Text(user.mobileNumber, fontSize = 13.sp, color = CustomerProfileColors.muted)
                if (user.createdAtEpochMillis > 0) Text(
                    "Member since ${Instant.ofEpochMilli(user.createdAtEpochMillis)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()))}",
                    fontSize = 11.sp, color = subtle)
            }
        }
    }
}

@Composable
private fun ProfileFormCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = Color.White, border = BorderStroke(1.dp, fieldBorder), shadowElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp, color = subtle)
            HorizontalDivider(color = Color(0xFFF0F2F6))
            content()
        }
    }
}

@Composable
internal fun ProfileInformationPersonalCard(first: String, last: String, enabled: Boolean,
    firstError: Boolean, onFirst: (String) -> Unit, onLast: (String) -> Unit) {
    ProfileFormCard("Personal Details") {
        if (!enabled) Text("Read-Only  ·  Tap Edit to change", fontSize = 11.sp, color = subtle)
        ProfileEditableField("First Name *", first, onFirst, enabled,
            isError = firstError, imeAction = ImeAction.Next)
        if (firstError) Text("Enter a valid first name", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.error)
        ProfileEditableField("Last Name", last, onLast, enabled, imeAction = ImeAction.Done)
    }
}

@Composable
private fun ProfileEditableField(label: String, value: String, change: (String) -> Unit,
    enabled: Boolean, isError: Boolean = false, imeAction: ImeAction) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = Color(0xFF475569))
        OutlinedTextField(value, change, modifier = Modifier.fillMaxWidth(),
            singleLine = true, enabled = enabled, isError = isError,
            shape = RoundedCornerShape(12.dp),
            trailingIcon = { Icon(AppIcons.Profile, null, Modifier.size(18.dp), tint = subtle) },
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFFBFCFE),
                disabledContainerColor = Color(0xFFF7FAFE),
                disabledTextColor = CustomerProfileColors.ink,
                disabledBorderColor = fieldBorder,
                disabledTrailingIconColor = subtle,
                focusedBorderColor = CustomerProfileColors.red,
                unfocusedBorderColor = fieldBorder,
            ))
    }
}

@Composable
internal fun ProfileInformationDeleteEntry(onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp), color = Color(0xFFFFFAFA),
        border = BorderStroke(1.dp, Color(0xFFFFC8C8))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(Color(0xFFFFE5E5), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center) {
                Icon(AppIcons.Delete, null, tint = CustomerProfileColors.red)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Delete Account", fontWeight = FontWeight.Bold, color = CustomerProfileColors.red)
                Text("Permanently remove your account & data", fontSize = 12.sp,
                    color = CustomerProfileColors.red)
            }
            Icon(AppIcons.ArrowRight, null, tint = CustomerProfileColors.red)
        }
    }
}

@Composable
internal fun ProfileInformationSecurityCard(user: User) {
    ProfileFormCard("Security & Identifier") {
        ProfileReadOnlyField("Login Mobile Number", user.mobileNumber,
            AppIcons.Password, trailing = "Linked")
        Text("Used for OTP verification and delivery updates.", fontSize = 11.sp,
            lineHeight = 16.sp, color = subtle)
        ProfileReadOnlyField("Account Role", "Customer", AppIcons.Security)
    }
}

@Composable
private fun ProfileReadOnlyField(label: String, value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector, trailing: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = Color(0xFF475569))
        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF7FAFE),
            border = BorderStroke(1.dp, fieldBorder)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 54.dp)
                .padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(18.dp), tint = subtle)
                Text(value, Modifier.weight(1f).padding(start = 12.dp),
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = CustomerProfileColors.ink, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                trailing?.let { Text(it, Modifier.background(Color(0xFFDCFCEB), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, color = green) }
            }
        }
    }
}

@Composable
internal fun ProfileInformationSaveAction(busy: Boolean, editing: Boolean,
    onSave: () -> Unit, onCancel: () -> Unit) {
    Surface(color = Color.Transparent) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = editing && !busy, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CustomerProfileColors.red)) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                    color = Color.White)
                else {
                    Text("Save Profile", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(AppIcons.Check, null, Modifier.size(18.dp))
                }
            }
            TextButton(onClick = onCancel, enabled = !busy) {
                Text("Cancel", color = CustomerProfileColors.muted)
            }
        }
    }
}
