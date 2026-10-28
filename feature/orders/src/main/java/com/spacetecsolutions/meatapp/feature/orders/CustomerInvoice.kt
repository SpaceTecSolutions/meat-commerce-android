package com.spacetecsolutions.meatapp.feature.orders

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.designsystem.component.AppAnimation
import com.spacetecsolutions.meatapp.core.designsystem.component.AppImage
import com.spacetecsolutions.meatapp.core.designsystem.component.AppLottieAnimation
import com.spacetecsolutions.meatapp.core.model.CheckoutPaymentMethod
import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.model.ShopSupport
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun CustomerInvoiceDialog(order: CodOrder, shopDetails: ShopSupport?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val pdf = remember(order, shopDetails, context) { createInvoicePdf(context, order, shopDetails) }
    var downloaded by remember { mutableStateOf(false) }
    val saver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri -> uri?.let { target -> runCatching {
        context.contentResolver.openOutputStream(target)?.use { it.write(pdf) }; downloaded = true
    } } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ui_invoice_details_2325ca6), fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (downloaded) item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    AppLottieAnimation(AppAnimation.OrderSuccess, Modifier.size(72.dp))
                }; Text(stringResource(R.string.ui_invoice_downloaded_dae3c07), Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                item { Surface(shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        order.items.firstOrNull()?.let { AppImage(it.imageUrl, null, Modifier.size(52.dp), cornerRadius = 10.dp) }
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(order.displayNumber, fontWeight = FontWeight.Bold)
                            Text(order.shopName, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(money(order.totalMinor), fontWeight = FontWeight.ExtraBold)
                    }
                } }
                item { InvoicePreviewCard(stringResource(R.string.invoice_preview_customer)) {
                    Text(order.customerName, fontWeight = FontWeight.SemiBold)
                    Text(order.customerMobile, style = MaterialTheme.typography.bodySmall)
                    Text(order.addressSummary, style = MaterialTheme.typography.bodySmall)
                } }
                item { Text(stringResource(R.string.ui_items_44d25b5), fontWeight = FontWeight.Bold) }
                items(order.items, key = { it.productId }) { line ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.ui_line_name_line_quantity_c4f99dc, line.name, line.quantity), Modifier.weight(1f))
                        Text(money(line.lineTotalMinor))
                    }
                }
                item { InvoicePreviewCard(stringResource(R.string.invoice_preview_bill_details)) {
                    InvoiceAmountRow(stringResource(R.string.invoice_preview_subtotal), order.subtotalMinor)
                    if (order.discountMinor > 0) InvoiceAmountRow(stringResource(R.string.invoice_preview_discount), -order.discountMinor)
                    InvoiceAmountRow(stringResource(R.string.invoice_preview_delivery), order.deliveryFeeMinor)
                    InvoiceAmountRow(stringResource(R.string.invoice_preview_tax), order.taxMinor)
                    HorizontalDivider(); InvoiceAmountRow(stringResource(R.string.invoice_preview_grand_total), order.totalMinor, true)
                } }
            }
        },
        confirmButton = { Button({ saver.launch("invoice-${order.displayNumber}.pdf") }) { Text(stringResource(R.string.ui_download_pdf_98e5ef0)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.ui_close_bbfa773)) } },
    )
}

@Composable
private fun InvoicePreviewCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), border = BorderStroke(
        1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold); content()
        }
    }
}

@Composable
private fun InvoiceAmountRow(label: String, amount: Long, total: Boolean = false) {
    Row(Modifier.fillMaxWidth()) { Text(label, Modifier.weight(1f), fontWeight = if (total) FontWeight.Bold else FontWeight.Normal)
        Text(money(amount), fontWeight = FontWeight.Bold, color = if (total) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
}

private fun createInvoicePdf(context: Context, order: CodOrder, shopDetails: ShopSupport?): ByteArray {
    val document = PdfDocument()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var pageNumber = 1
    var page = startInvoicePage(context, document, order, shopDetails, pageNumber, paint, continuation = false)
    var y = drawInvoiceDetails(context, page.canvas, order, paint, page.y)
    y = drawInvoiceTableHeader(context, page.canvas, paint, y)

    order.items.forEachIndexed { index, item ->
        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        val nameLines = wrapForWidth(item.name, paint, 292f)
        val rowHeight = maxOf(32f, nameLines.size * 14f + 12f)
        if (y + rowHeight > 770f) {
            drawInvoiceFooter(context, page.canvas, order, paint, pageNumber)
            document.finishPage(page.page)
            pageNumber++
            page = startInvoicePage(context, document, order, shopDetails, pageNumber, paint, continuation = true)
            y = drawInvoiceTableHeader(context, page.canvas, paint, page.y)
        }
        if (index % 2 == 1) {
            paint.color = Color.rgb(247, 248, 250)
            page.canvas.drawRect(40f, y - 17f, 555f, y + rowHeight - 4f, paint)
        }
        paint.color = Color.rgb(35, 39, 45)
        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        paint.textAlign = Paint.Align.CENTER
        page.canvas.drawText((index + 1).toString(), 52f, y, paint)
        paint.textAlign = Paint.Align.LEFT
        nameLines.forEachIndexed { lineIndex, line ->
            page.canvas.drawText(line, 72f, y + lineIndex * 14f, paint)
        }
        paint.textAlign = Paint.Align.CENTER
        page.canvas.drawText(item.quantityLabel, 412f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        page.canvas.drawText(money(item.lineTotalMinor), 545f, y, paint)
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.rgb(226, 229, 233)
        page.canvas.drawLine(40f, y + rowHeight - 4f, 555f, y + rowHeight - 4f, paint)
        y += rowHeight
    }

    val totalRows = 4 + if (order.discountMinor > 0) 1 else 0
    val totalsHeight = totalRows * 23f + 40f
    if (y + totalsHeight > 770f) {
        drawInvoiceFooter(context, page.canvas, order, paint, pageNumber)
        document.finishPage(page.page)
        pageNumber++
        page = startInvoicePage(context, document, order, shopDetails, pageNumber, paint, continuation = true)
        y = page.y
    }
    drawInvoiceTotals(context, page.canvas, order, paint, y + 16f)
    drawInvoiceFooter(context, page.canvas, order, paint, pageNumber)
    document.finishPage(page.page)
    return ByteArrayOutputStream().use { output -> document.writeTo(output); document.close(); output.toByteArray() }
}

private data class InvoicePage(val page: PdfDocument.Page, val canvas: Canvas, val y: Float)

private fun startInvoicePage(
    context: Context,
    document: PdfDocument,
    order: CodOrder,
    shopDetails: ShopSupport?,
    pageNumber: Int,
    paint: Paint,
    continuation: Boolean,
): InvoicePage {
    val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
    val canvas = page.canvas
    canvas.drawColor(Color.WHITE)
    paint.color = Color.rgb(215, 25, 32)
    canvas.drawRect(0f, 0f, 595f, 7f, paint)
    if (continuation) {
        paint.color = Color.rgb(35, 39, 45)
        paint.textSize = 18f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(context.getString(R.string.invoice_title), 40f, 42f, paint)
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(context.getString(R.string.invoice_order_number) + ": " + order.displayNumber, 555f, 41f, paint)
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.rgb(226, 229, 233)
        canvas.drawLine(40f, 61f, 555f, 61f, paint)
        return InvoicePage(page, canvas, 88f)
    }

    val logoId = context.resources.getIdentifier("meat_station_app_logo", "drawable", context.packageName)
    if (logoId != 0) runCatching {
        context.getDrawable(logoId)?.let { drawable -> drawInvoiceLogo(canvas, drawable, 40, 27, 102, 68) }
    }
    val infoX = if (logoId != 0) 154f else 40f
    val shopInfoWidth = if (logoId != 0) 190f else 340f
    paint.color = Color.rgb(28, 31, 35)
    paint.typeface = Typeface.DEFAULT_BOLD
    paint.textSize = 18f
    val shopName = shopDetails?.shopName?.takeIf(String::isNotBlank) ?: order.shopName
    canvas.drawText(fitText(shopName, paint, shopInfoWidth), infoX, 39f, paint)
    paint.typeface = Typeface.DEFAULT
    paint.textSize = 9.5f
    paint.color = Color.rgb(92, 99, 108)
    var detailY = 57f
    val shopAddress = shopDetails?.address?.takeIf(String::isNotBlank) ?: order.shopAddress
    shopAddress.takeIf(String::isNotBlank)?.let { address ->
        val addressText = context.getString(R.string.invoice_shop_address, address)
        wrapForWidth(addressText, paint, shopInfoWidth).take(3).forEach { line ->
            canvas.drawText(line, infoX, detailY, paint)
            detailY += 13f
        }
    }

    shopDetails?.callNumber?.takeIf(String::isNotBlank)?.let { phone ->
        canvas.drawText(context.getString(R.string.invoice_shop_contact, phone), infoX, detailY, paint)
    }

    paint.color = Color.rgb(215, 25, 32)
    paint.textSize = 20f
    paint.typeface = Typeface.DEFAULT_BOLD
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText(context.getString(R.string.invoice_title), 555f, 39f, paint)
    paint.color = Color.rgb(92, 99, 108)
    paint.textSize = 9.5f
    paint.typeface = Typeface.DEFAULT
    canvas.drawText(context.getString(R.string.invoice_number) + ": " + order.displayNumber, 555f, 57f, paint)
    val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        .format(Date(order.createdAtEpochMillis.takeIf { it > 0 } ?: System.currentTimeMillis()))
    canvas.drawText(context.getString(R.string.invoice_date) + ": " + date, 555f, 72f, paint)
    paint.textAlign = Paint.Align.LEFT
    paint.color = Color.rgb(226, 229, 233)
    canvas.drawLine(40f, 112f, 555f, 112f, paint)
    paint.color = Color.rgb(215, 25, 32)
    canvas.drawRect(40f, 110f, 115f, 114f, paint)
    return InvoicePage(page, canvas, 136f)
}

private fun drawInvoiceLogo(canvas: Canvas, drawable: Drawable, left: Int, top: Int, maxWidth: Int, maxHeight: Int) {
    val ratio = drawable.intrinsicWidth.takeIf { it > 0 }?.toFloat()?.div(
        drawable.intrinsicHeight.takeIf { it > 0 } ?: 1,
    ) ?: 1f
    val width = minOf(maxWidth.toFloat(), maxHeight * ratio)
    val height = width / ratio
    val x = left + (maxWidth - width) / 2f
    val y = top + (maxHeight - height) / 2f
    drawable.setBounds(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt())
    drawable.draw(canvas)
}

private fun drawInvoiceDetails(context: Context, canvas: Canvas, order: CodOrder, paint: Paint, top: Float): Float {
    paint.color = Color.rgb(247, 248, 250)
    canvas.drawRoundRect(40f, top, 555f, top + 104f, 8f, 8f, paint)
    paint.color = Color.rgb(215, 25, 32)
    paint.textSize = 8.5f
    paint.typeface = Typeface.DEFAULT_BOLD
    canvas.drawText(context.getString(R.string.invoice_customer).uppercase(Locale.getDefault()), 54f, top + 18f, paint)
    canvas.drawText(context.getString(R.string.invoice_order_details), 325f, top + 18f, paint)
    paint.color = Color.rgb(35, 39, 45)
    paint.textSize = 11.5f
    canvas.drawText(fitText(order.customerName, paint, 245f), 54f, top + 38f, paint)
    paint.textSize = 9.5f
    paint.typeface = Typeface.DEFAULT
    canvas.drawText(order.customerMobile, 54f, top + 54f, paint)
    val addressLines = wrapForWidth(order.addressSummary, paint, 242f).take(2)
    addressLines.forEachIndexed { index, line -> canvas.drawText(line, 54f, top + 69f + index * 13f, paint) }
    paint.color = Color.rgb(92, 99, 108)
    canvas.drawText(context.getString(R.string.invoice_order_number) + ": " + order.displayNumber, 325f, top + 39f, paint)
    val paymentMethod = context.getString(when (order.paymentMethod) {
        CheckoutPaymentMethod.COD -> R.string.invoice_payment_cod
        CheckoutPaymentMethod.RAZORPAY -> R.string.invoice_payment_razorpay
        CheckoutPaymentMethod.UPI -> R.string.invoice_payment_upi
    })
    canvas.drawText(context.getString(R.string.invoice_payment_method) + ": " + paymentMethod, 325f, top + 56f, paint)
    order.deliverySlotTimeLabel?.takeIf(String::isNotBlank)?.let {
        canvas.drawText(it, 325f, top + 73f, paint)
    }
    return top + 123f
}

private fun drawInvoiceTableHeader(context: Context, canvas: Canvas, paint: Paint, top: Float): Float {
    paint.color = Color.rgb(35, 39, 45)
    canvas.drawRoundRect(40f, top, 555f, top + 30f, 5f, 5f, paint)
    paint.color = Color.WHITE
    paint.textSize = 9.5f
    paint.typeface = Typeface.DEFAULT_BOLD
    canvas.drawText(context.getString(R.string.invoice_serial_number), 50f, top + 19f, paint)
    canvas.drawText(context.getString(R.string.invoice_item), 72f, top + 19f, paint)
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText(context.getString(R.string.invoice_quantity), 412f, top + 19f, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText(context.getString(R.string.invoice_amount), 545f, top + 19f, paint)
    paint.textAlign = Paint.Align.LEFT
    return top + 47f
}

private fun drawInvoiceTotals(context: Context, canvas: Canvas, order: CodOrder, paint: Paint, top: Float) {
    var y = top
    fun row(label: String, value: Long) {
        paint.color = Color.rgb(76, 82, 90)
        paint.textSize = 10.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(label, 355f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(money(value), 545f, y, paint)
        paint.textAlign = Paint.Align.LEFT
        y += 21f
    }
    row(context.getString(R.string.invoice_preview_subtotal), order.subtotalMinor)
    if (order.discountMinor > 0) row(context.getString(R.string.invoice_discount), -order.discountMinor)
    row(context.getString(R.string.invoice_preview_delivery), order.deliveryFeeMinor)
    row(context.getString(R.string.invoice_tax), order.taxMinor)
    paint.color = Color.rgb(215, 25, 32)
    canvas.drawRoundRect(330f, y - 4f, 555f, y + 31f, 5f, 5f, paint)
    paint.color = Color.WHITE
    paint.textSize = 12f
    paint.typeface = Typeface.DEFAULT_BOLD
    canvas.drawText(context.getString(R.string.invoice_grand_total), 344f, y + 18f, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText(money(order.totalMinor), 541f, y + 18f, paint)
    paint.textAlign = Paint.Align.LEFT
}

private fun drawInvoiceFooter(context: Context, canvas: Canvas, order: CodOrder, paint: Paint, pageNumber: Int) {
    paint.color = Color.rgb(226, 229, 233)
    canvas.drawLine(40f, 790f, 555f, 790f, paint)
    paint.color = Color.rgb(92, 99, 108)
    paint.textSize = 8.5f
    paint.typeface = Typeface.DEFAULT
    canvas.drawText(context.getString(R.string.invoice_thank_you, order.shopName), 40f, 807f, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText(context.getString(R.string.invoice_page, pageNumber), 555f, 807f, paint)
    paint.textAlign = Paint.Align.LEFT
}

private fun wrapForWidth(text: String, paint: Paint, width: Float): List<String> {
    if (text.isBlank()) return emptyList()
    val lines = mutableListOf<String>()
    var line = StringBuilder()
    text.trim().split(Regex("\\s+")).forEach { word ->
        val candidate = if (line.isEmpty()) word else "$line $word"
        if (paint.measureText(candidate) <= width) line = StringBuilder(candidate)
        else {
            if (line.isNotEmpty()) lines += line.toString()
            line = StringBuilder()
            var remainder = word
            while (paint.measureText(remainder) > width && remainder.length > 1) {
                var split = remainder.length - 1
                while (split > 1 && paint.measureText(remainder.substring(0, split)) > width) split--
                lines += remainder.substring(0, split)
                remainder = remainder.substring(split)
            }
            line.append(remainder)
        }
    }
    if (line.isNotEmpty()) lines += line.toString()
    return lines
}

private fun fitText(text: String, paint: Paint, width: Float): String {
    if (paint.measureText(text) <= width) return text
    var shortened = text
    while (shortened.isNotEmpty() && paint.measureText("$shortened…") > width) shortened = shortened.dropLast(1)
    return "$shortened…"
}

private fun money(value: Long): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    .format(value / 100.0)
