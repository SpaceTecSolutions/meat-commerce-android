package com.spacetecsolutions.meatapp.feature.admin.settings

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentState
import com.spacetecsolutions.meatapp.core.designsystem.component.ContentStateView
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons

@Composable
fun AdminAboutRoute(back: () -> Unit, call: (String) -> Unit, email: (String) -> Unit,
    viewModel: AdminHelpViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(AdminAccountColors.canvas)) {
        AdminAccountTopBar("About Us", back)
        when {
            state.loading -> ContentStateView(ContentState.Loading)
            state.settings == null -> ContentStateView(
                ContentState.Error(description = state.error), onAction = viewModel::refresh)
            else -> {
                val shop = state.settings!!
                Column(Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = 440.dp).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    AboutCard { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(AppIcons.Shop, null, Modifier.size(48.dp), tint = AdminAccountColors.red)
                        Text(shop.shopName, style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = AdminAccountColors.ink)
                        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_fresh_quality_trusted_8848457), color = AdminAccountColors.muted)
                        Text(shop.address, color = AdminAccountColors.muted)
                    } }
                    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_about_us_b414b76), style = MaterialTheme.typography.labelLarge,
                        color = AdminAccountColors.muted, fontWeight = FontWeight.Bold)
                    AboutCard { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_shop_shopname_offers_fresh_meat_5d64b0d, shop.shopName),
                        color = AdminAccountColors.ink) }
                    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_shop_information_8fbe272), style = MaterialTheme.typography.labelLarge,
                        color = AdminAccountColors.muted, fontWeight = FontWeight.Bold)
                    AboutCard {
                        Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_shop_address_e41bed5), fontWeight = FontWeight.SemiBold)
                        Text(shop.address, color = AdminAccountColors.muted)
                        TextButton({ call(shop.contactPhone) }) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_call_shop_contactphone_d1d9be8, shop.contactPhone)) }
                        shop.contactEmail?.let { address -> TextButton({ email(address) }) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.admin.R.string.ui_email_address_b516f3b, address)) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White,
        border = BorderStroke(1.dp, AdminAccountColors.border), shadowElevation = 1.dp) {
        Column(Modifier.padding(16.dp), content = content)
    }
}
