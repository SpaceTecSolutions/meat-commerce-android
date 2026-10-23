package com.spacetecsolutions.meatapp.feature.customerhome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons

@Composable
fun CustomerSupportRoute(back: () -> Unit, call: (String) -> Unit,
    whatsapp: (String) -> Unit, email: (String) -> Unit,
    onStaffSignIn: (() -> Unit)? = null,
    viewModel: CustomerSupportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(AccountStyle.canvas)
        .statusBarsPadding().navigationBarsPadding()) {
        AccountTopBar("Help & Support", back)
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            val contentMaxWidth = if (maxWidth >= 600.dp) 600.dp else 440.dp
            Column(Modifier.fillMaxSize().widthIn(max = contentMaxWidth).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)) {
            AccountCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(52.dp).background(Color(0xFFFFECEC), RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center) {
                        Icon(AppIcons.Contact, null, tint = AccountStyle.red)
                    }
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_need_some_help_f77b2b8), fontSize = 21.sp, fontWeight = FontWeight.Bold,
                            color = AccountStyle.ink)
                        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_we_re_here_to_assist_216fac5),
                            fontSize = 14.sp, lineHeight = 21.sp, color = AccountStyle.muted)
                    }
                }
                Spacer(Modifier.height(17.dp))
                HorizontalDivider(color = AccountStyle.line)
                Spacer(Modifier.height(15.dp))
                Text(listOfNotNull(state.support?.shopName?.takeIf(String::isNotBlank),
                    "Orders", "Delivery", "Account").joinToString("  ·  "),
                    fontSize = 12.sp, color = AccountStyle.muted)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AccountSectionLabel("Get in Touch")
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_direct_support_3750b67), fontSize = 12.sp, color = AccountStyle.muted)
            }
            when {
                state.loading -> Box(
                    Modifier.fillMaxWidth().heightIn(min = 180.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = AccountStyle.red) }
                state.error != null -> AccountCard(Modifier.fillMaxWidth()) {
                    Text(state.error.orEmpty())
                    TextButton(onClick = viewModel::refresh) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_retry_9f5cd8a)) }
                }
                else -> {
                    val support = state.support
                    /*support?.callNumber?.let { SupportContact(AppIcons.Call, "DIRECT PHONE", "Call Us",
                        it, AccountStyle.red, Color(0xFFFFEEEE)) { call(it) } }
                    support?.whatsappNumber?.let { SupportContact(AppIcons.WhatsApp, "WHATSAPP",
                        "Message on WhatsApp", it, Color(0xFF059669), Color(0xFFEFFDF6)) { whatsapp(it) } }
                    support?.email?.let { SupportContact(AppIcons.Email, "EMAIL SUPPORT", "Send Email",
                        it, Color(0xFF334155), Color(0xFFF4F7FB)) { email(it) } }*/
                    support?.callNumber?.let { SupportContact(AppIcons.Call, "Call Us", it,
                        it, Color(0xFF2563EB), Color(0xFFEFF6FF)) { call(it) } }
                    support?.whatsappNumber?.let { SupportContact(AppIcons.WhatsApp, "Message on WhatsApp",
                        it, it, Color(0xFF059669), Color(0xFFEFFDF6)) { whatsapp(it) } }
                    support?.email?.let { SupportContact(AppIcons.Email, "Send Email", it,
                        it, AccountStyle.red, Color(0xFFFFEEEE)) { email(it) } }
                    if (support?.callNumber == null && support?.whatsappNumber == null && support?.email == null)
                        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_support_contact_details_are_not_73c1d1a), color = AccountStyle.muted)
                }
            }
            Spacer(Modifier.height(28.dp))
            onStaffSignIn?.let { action -> TextButton(onClick = action) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_staff_sign_in_e04dc83)) } }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = CircleShape, color = Color.White,
                    border = BorderStroke(1.dp, AccountStyle.line)) {
                    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_customer_care_a44b64c), Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccountStyle.ink)
                }
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_direct_assistance_with_your_orders_6eb38ef),
                    Modifier.padding(top = 11.dp), fontSize = 12.sp, color = AccountStyle.muted)
            }
            }
        }
    }
}

@Composable
private fun SupportContact(icon: ImageVector, eyebrow: String, title: String, value: String,
    tint: Color, background: Color, action: () -> Unit) {
    Surface(onClick = action, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = Color.White, border = BorderStroke(1.dp, AccountStyle.line), shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(background, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint) }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(eyebrow, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = tint)
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AccountStyle.ink)
//                Text(value, fontSize = 14.sp, color = AccountStyle.ink)
            }
            Icon(AppIcons.ArrowRight, null, tint = AccountStyle.muted)
        }
    }
}
