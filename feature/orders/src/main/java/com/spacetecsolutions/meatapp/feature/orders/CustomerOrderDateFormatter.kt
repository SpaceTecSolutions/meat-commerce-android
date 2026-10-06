package com.spacetecsolutions.meatapp.feature.orders

import com.spacetecsolutions.meatapp.core.model.CodOrder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object CustomerOrderDateFormatter {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val time = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    private val shortDate = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)
    private val fullDate = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

    fun created(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
        if (epochMillis <= 0) return "Date unavailable"
        val value = Instant.ofEpochMilli(epochMillis).atZone(zone)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val prefix = when (value.toLocalDate()) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> value.format(if (value.year == today.year) shortDate else fullDate)
        }
        return "$prefix, ${value.format(time)}"
    }

    fun delivery(order: CodOrder, now: Long = System.currentTimeMillis()): String {
        val date = order.deliveryDateIso?.let { value ->
            runCatching { LocalDate.parse(value) }.getOrNull()
        }
            ?: return order.deliveryLabelFallback()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val prefix = when (date) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(if (date.year == today.year) shortDate else fullDate)
        }
        return listOfNotNull(prefix, order.deliverySlotTimeLabel?.takeIf(String::isNotBlank))
            .joinToString(", ")
    }

    fun slotHasPassed(order: CodOrder, now: Long): Boolean {
        val date = order.deliveryDateIso?.let { value ->
            runCatching { LocalDate.parse(value) }.getOrNull()
        } ?: return false
        val end = order.deliverySlotEndMinutes ?: return false
        return now > date.atStartOfDay(zone).plusMinutes(end.toLong()).toInstant().toEpochMilli()
    }

    fun expectedBy(order: CodOrder): String? = order.deliverySlotEndMinutes?.let { minutes ->
        LocalTime.of(minutes / 60, minutes % 60).format(time)
    }
}

private fun CodOrder.deliveryLabelFallback() = listOfNotNull(
    deliverySlotDateLabel?.takeIf(String::isNotBlank),
    deliverySlotTimeLabel?.takeIf(String::isNotBlank),
).joinToString(", ")
