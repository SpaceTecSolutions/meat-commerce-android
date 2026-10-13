package com.spacetecsolutions.meatapp.feature.customerhome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import kotlinx.coroutines.launch

@Composable
fun CustomerLegalRoute(privacy: Boolean, back: () -> Unit,
    openOtherPolicy: () -> Unit, openSupport: () -> Unit,
    openDeleteAccount: () -> Unit) {
    val title = if (privacy) com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_privacy_policy_9db108b
        else com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_terms_conditions_d35f2b9
    val groups = if (privacy) CustomerLegalContent.privacy else CustomerLegalContent.terms
    val listState = rememberLazyListState()
    val tabState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val firstGroup = if (privacy) 3 else 2
    val selected by remember(listState, privacy) { derivedStateOf {
        ((listState.firstVisibleItemIndex - firstGroup).coerceAtLeast(0) / 2)
            .coerceIn(0, groups.lastIndex)
    } }
    LaunchedEffect(selected) { tabState.animateScrollToItem(selected) }
    Column(Modifier.fillMaxSize().background(AccountStyle.canvas)
        .statusBarsPadding().navigationBarsPadding()) {
        AccountTopBar(androidx.compose.ui.res.stringResource(title), back)
        LazyColumn(Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 600.dp), state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(17.dp)) {
            item { AccountCard(Modifier.fillMaxWidth()) {
                Text(if (privacy) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_consumer_privacy_trust_81c7f6e) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_legal_agreement_d1aed84),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccountStyle.red)
                Spacer(Modifier.height(8.dp))
                Text(androidx.compose.ui.res.stringResource(title), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AccountStyle.ink)
                Text(if (privacy) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_how_account_address_and_order_0a0516a) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_please_read_these_terms_before_8daac08),
                    Modifier.padding(top = 6.dp), fontSize = 13.sp, lineHeight = 19.sp,
                    color = AccountStyle.muted)
                HorizontalDivider(Modifier.padding(vertical = 15.dp), color = AccountStyle.line)
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_last_updated_customerlegalcontent_lastupdated_8babae6,
                    androidx.compose.ui.res.stringResource(CustomerLegalContent.lastUpdated),
                    androidx.compose.ui.res.stringResource(CustomerLegalContent.version)),
                    fontSize = 11.sp, color = AccountStyle.muted)
            } }
            item { LazyRow(Modifier.fillMaxWidth(), state = tabState,
                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                itemsIndexed(groups) { index, group ->
                    Surface(onClick = { scope.launch { listState.animateScrollToItem(firstGroup + index * 2) } },
                        shape = CircleShape,
//                        color = if (index == selected) AccountStyle.red else Color.White,
                        color = Color.White,
//                        border = BorderStroke(1.dp, if (index == selected) AccountStyle.red else AccountStyle.line)) {
                        border = BorderStroke(1.dp,  AccountStyle.line)) {
                        Text(androidx.compose.ui.res.stringResource(group.heading), Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
//                            fontSize = 11.sp, color = if (index == selected) Color.White else AccountStyle.ink)
                            fontSize = 11.sp, color = AccountStyle.ink)
                    }
                }
            } }
            if (privacy) item { AccountCard(Modifier.fillMaxWidth()) {
                AccountSectionLabel(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_summary_heading))
                Spacer(Modifier.height(10.dp))
                LegalSummary(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_summary_account,
                    com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_summary_account_detail)
                LegalSummary(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_summary_pin,
                    com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_summary_pin_detail)
                LegalSummary(com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_summary_deletion,
                    com.spacetecsolutions.meatapp.feature.customerhome.R.string.legal_summary_deletion_detail)
            } }
            groups.forEachIndexed { index, group ->
                item { Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(width = 4.dp, height = 16.dp)
                        .background(AccountStyle.red, RoundedCornerShape(2.dp)))
                    Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_group_index_1_group_heading_58aeae6,
                        index + 1, androidx.compose.ui.res.stringResource(group.heading).uppercase()),
                        Modifier.padding(start = 8.dp), fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, color = AccountStyle.muted)
                } }
                item { AccountCard(Modifier.fillMaxWidth()) {
                    group.clauses.forEachIndexed { clauseIndex, clause ->
                        if (clauseIndex > 0) HorizontalDivider(Modifier.padding(vertical = 13.dp),
                            color = AccountStyle.line)
                        Row(verticalAlignment = Alignment.Top) {
                            Text("${groups.take(index).sumOf { it.clauses.size } + clauseIndex + 1}.", Modifier.width(28.dp), fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, color = AccountStyle.red)
                            Column {
                                Text(androidx.compose.ui.res.stringResource(clause.heading), fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold, color = AccountStyle.ink)
                                Text(androidx.compose.ui.res.stringResource(clause.body), Modifier.padding(top = 4.dp), fontSize = 12.sp,
                                    lineHeight = 19.sp, color = AccountStyle.muted)
                            }
                        }
                    }
                } }
            }
            /*if (privacy) item { TextButton(onClick = openDeleteAccount,
                modifier = Modifier.fillMaxWidth()) { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_manage_account_deletion_e4fb039)) }
            }
            item { OutlinedButton(onClick = openOtherPolicy, modifier = Modifier.fillMaxWidth()) {
                Text(if (privacy) androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_terms_conditions_d35f2b9) else androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_privacy_policy_9db108b))
            } }
            item { Button(onClick = openSupport, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccountStyle.red)) {
                Icon(AppIcons.Contact, null)
                Spacer(Modifier.width(8.dp))
                Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_contact_customer_support_ef8fc8d))
            } }*/
            item { Text(androidx.compose.ui.res.stringResource(com.spacetecsolutions.meatapp.feature.customerhome.R.string.ui_legal_compliance_8fd0046), Modifier.fillMaxWidth(), fontSize = 11.sp,
                color = AccountStyle.muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun LegalSummary(title: Int, detail: Int) {
    Text(androidx.compose.ui.res.stringResource(title), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AccountStyle.ink)
    Text(androidx.compose.ui.res.stringResource(detail), Modifier.padding(bottom = 9.dp), fontSize = 12.sp, color = AccountStyle.muted)
}
