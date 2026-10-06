package com.spacetecsolutions.meatapp.feature.admin.reports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.icon.AppIcons
import androidx.compose.ui.text.style.TextOverflow
import com.spacetecsolutions.meatapp.core.designsystem.theme.*
import com.spacetecsolutions.meatapp.core.model.Product
import com.spacetecsolutions.meatapp.core.model.ReportPeriodType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val adminPeriods = listOf(
    ReportPeriodType.WEEKLY,
    ReportPeriodType.MONTHLY,
    ReportPeriodType.YEARLY,
    ReportPeriodType.CUSTOM,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportFilterBottomSheet(
    state: AdminReportsState,
    dismiss: () -> Unit,
    apply: (ReportPeriodType, String?, String?, String?) -> Unit,
) {
    var period by remember(state.filter) { mutableStateOf(state.filter.period) }
    var start by remember(state.filter) { mutableStateOf(state.filter.startDateIso) }
    var end by remember(state.filter) { mutableStateOf(state.filter.endDateIso) }
    var datePickerOpen by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = dismiss, containerColor = ReportsStyle.surface) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = AppDimensions.formMaxWidth)
                .align(Alignment.CenterHorizontally).padding(horizontal = 24.dp)
                .navigationBarsPadding(),
        ) {
            Text("Report Filters", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = ReportsStyle.ink)
            Spacer(Modifier.height(AppSpacing.medium))
            Text("PERIOD", style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold, color = ReportsStyle.muted)
            adminPeriods.forEach { option ->
                val selected = period == option
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        .background(if (selected) ReportsStyle.rose else ReportsStyle.surface, RoundedCornerShape(12.dp))
                        .border(1.dp, if (selected) ReportsStyle.rose else ReportsStyle.surface, RoundedCornerShape(12.dp))
                        .clickable {
                        period = option
                        if (option == ReportPeriodType.CUSTOM) datePickerOpen = true
                    }.padding(vertical = 8.dp, horizontal = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = ReportsStyle.red))
                    Text(option.filterLabel(), Modifier.padding(start = AppSpacing.small),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = ReportsStyle.ink)
                }
            }
            if (period == ReportPeriodType.CUSTOM) {
                OutlinedButton({ datePickerOpen = true }, Modifier.fillMaxWidth()) {
                    Text(if (start != null && end != null) "Custom Date" else "Select start and end date")
                }
            }
            Spacer(Modifier.height(AppSpacing.medium))
            state.filterError?.let {
                Spacer(Modifier.height(AppSpacing.small)); Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(AppSpacing.large))
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                OutlinedButton({
                    period = ReportPeriodType.MONTHLY; start = null; end = null
                }, Modifier.weight(1f).heightIn(min = 48.dp), shape = ReportsStyle.pillShape) { Text("Reset") }
                Button({ apply(period, null, start, end) }, Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = ReportsStyle.pillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = ReportsStyle.red)) {
                    Text("Apply Filters")
                }
            }
            Spacer(Modifier.height(AppSpacing.medium))
        }
    }
    if (datePickerOpen) ReportDateRangePicker(start, end, { datePickerOpen = false }) { from, to ->
        start = from; end = to; period = ReportPeriodType.CUSTOM; datePickerOpen = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportDateRangePicker(
    start: String?,
    end: String?,
    dismiss: () -> Unit,
    select: (String, String) -> Unit,
) {
    val picker = rememberDateRangePickerState(
        initialSelectedStartDateMillis = start?.toEpochMillis(),
        initialSelectedEndDateMillis = end?.toEpochMillis(),
    )
    DatePickerDialog(
        onDismissRequest = dismiss,
        confirmButton = {
            TextButton({
                val from = picker.selectedStartDateMillis?.toIsoDate()
                val to = picker.selectedEndDateMillis?.toIsoDate()
                if (from != null && to != null) select(from, to)
            }, enabled = picker.selectedStartDateMillis != null && picker.selectedEndDateMillis != null) {
                Text("Apply")
            }
        },
        dismissButton = { TextButton(dismiss) { Text("Cancel") } },
    ) {
        DateRangePicker(
            state = picker,
            modifier = Modifier.heightIn(max = AppDimensions.dashboardMaxWidth),
            title = { Text("Select report dates", Modifier.padding(horizontal = AppSpacing.large)) },
            headline = null,
            showModeToggle = false,
        )
    }
}

private fun ReportPeriodType.filterLabel() = when (this) {
    ReportPeriodType.WEEKLY -> "Weekly"
    ReportPeriodType.MONTHLY, ReportPeriodType.CURRENT_MONTH_BY_WEEK -> "Monthly"
    ReportPeriodType.YEARLY -> "Yearly"
    ReportPeriodType.CUSTOM -> "Custom range"
}
private fun String.toEpochMillis() = LocalDate.parse(this).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.toIsoDate() = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate().toString()
