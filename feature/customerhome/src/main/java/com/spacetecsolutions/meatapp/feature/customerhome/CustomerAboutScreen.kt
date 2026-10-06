package com.spacetecsolutions.meatapp.feature.customerhome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
fun CustomerAboutRoute(back: () -> Unit, call: (String) -> Unit, email: (String) -> Unit,
    openMap: (String) -> Unit,
    viewModel: CustomerSupportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shop = state.support
    val shopName = shop?.shopName?.takeIf(String::isNotBlank) ?: "Our Shop"
    Column(Modifier.fillMaxSize().background(AccountStyle.canvas)
        .statusBarsPadding().navigationBarsPadding()) {
        AccountTopBar("About Us", back)
        Column(Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 440.dp).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (state.error != null) TextButton(onClick = viewModel::refresh) { Text("Retry shop details") }
            AccountCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(70.dp).background(Color(0xFFFFE4E1), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center) {
                        Icon(AppIcons.Shop, null, Modifier.size(34.dp), tint = AccountStyle.red)
                    }
                    Spacer(Modifier.height(13.dp))
                    Text(shopName, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = AccountStyle.ink)
                    Text("Fresh  ·  Quality  ·  Trusted", fontSize = 13.sp,
                        letterSpacing = 1.sp, color = AccountStyle.muted)
                    shop?.address?.let {
                        Text(it, Modifier.padding(top = 11.dp), fontSize = 12.sp,
                            color = AccountStyle.muted)
                    }
                }
            }
            // Shop Settings has no image or logo field; do not substitute a screenshot's sample photo.
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFCEBE9), border = BorderStroke(1.dp, Color(0xFFFFDAD6))) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.Shop, null, Modifier.size(32.dp), tint = AccountStyle.red)
                    Text("Fresh cuts, prepared for your order", Modifier.padding(start = 13.dp),
                        fontWeight = FontWeight.SemiBold, color = AccountStyle.ink)
                }
            }
            AccountSectionLabel("About Us")
            AccountCard(Modifier.fillMaxWidth()) {
                Text("$shopName offers fresh meat with convenient ordering and scheduled doorstep delivery.",
                    fontSize = 15.sp, lineHeight = 24.sp, color = AccountStyle.ink)
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF4F6F8)) {
                    Text("Our goal is to make everyday ordering dependable and easy.",
                        Modifier.padding(13.dp), fontSize = 14.sp, color = AccountStyle.muted)
                }
            }
            AccountSectionLabel("Shop Information")
            AccountCard(Modifier.fillMaxWidth()) {
                shop?.address?.let { address ->
                    AboutContact(AppIcons.Location, "Shop Address", address, { openMap(address) })
                }
                shop?.callNumber?.let { AboutContact(AppIcons.Call, "Contact Number", it, { call(it) }) }
                shop?.email?.let { AboutContact(AppIcons.Contact, "Contact Email", it, { email(it) }) }
                if (shop?.address == null && shop?.callNumber == null && shop?.email == null)
                    Text("Shop details are not configured yet.", color = AccountStyle.muted)
            }
            Text(shopName, Modifier.fillMaxWidth(), fontSize = 12.sp,
                color = AccountStyle.muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AboutContact(icon: ImageVector, label: String, value: String, action: (() -> Unit)?) {
    Surface(onClick = action ?: {}, enabled = action != null, color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(Color(0xFFF4F6F8), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center) { Icon(icon, null, tint = AccountStyle.muted) }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(label, fontSize = 11.sp, color = AccountStyle.muted)
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = AccountStyle.ink)
            }
            if (action != null) Icon(AppIcons.ArrowRight, null, tint = AccountStyle.red)
        }
    }
}
