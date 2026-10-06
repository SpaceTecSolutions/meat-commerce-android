package com.spacetecsolutions.meatapp.feature.superadmin.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing
import com.spacetecsolutions.meatapp.core.model.ReportPeriodType

@Composable
fun ReportPeriodSelector(
    state: ReportsUiState,
    onPeriodSelected: (ReportPeriodType) -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onApplyCustom: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(Modifier.padding(3.dp)) {
                listOf(
                    ReportPeriodType.WEEKLY to "Weekly",
                    ReportPeriodType.MONTHLY to "Monthly",
                    ReportPeriodType.YEARLY to "Yearly",
                ).forEach { (period, label) ->
                    val selected = state.selectedPeriod == period ||
                        (period == ReportPeriodType.MONTHLY &&
                            state.selectedPeriod == ReportPeriodType.CURRENT_MONTH_BY_WEEK)
                    Box(
                        modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .clickable { onPeriodSelected(period) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
        AnimatedVisibility(state.selectedPeriod == ReportPeriodType.CUSTOM) {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                    DateField("Start", state.startDate, onStartChange, Modifier.weight(1f))
                    DateField("End", state.endDate, onEndChange, Modifier.weight(1f))
                }
                state.rangeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = onApplyCustom, modifier = Modifier.fillMaxWidth()) { Text("Apply range") }
            }
        }
        if (state.selectedPeriod == ReportPeriodType.MONTHLY) {
            TextButton(
                onClick = { onPeriodSelected(ReportPeriodType.CURRENT_MONTH_BY_WEEK) },
                modifier = Modifier.align(Alignment.End).heightIn(min = 32.dp),
                contentPadding = PaddingValues(horizontal = AppSpacing.small),
            ) { Text("View month by week", style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun DateField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        label = { Text(label) },
        placeholder = { Text("YYYY-MM-DD") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
    )
}
