package com.spacetecsolutions.meatapp.feature.orders

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spacetecsolutions.meatapp.core.model.CodOrder
import com.spacetecsolutions.meatapp.core.designsystem.component.AppAnimation
import com.spacetecsolutions.meatapp.core.designsystem.component.AppImage
import com.spacetecsolutions.meatapp.core.designsystem.component.AppLottieAnimation
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun CustomerInvoiceDialog(order: CodOrder, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pdf = remember(order, context) { createInvoicePdf(context, order) }
    var downloaded by remember { mutableStateOf(false) }
    val saver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri -> uri?.let { target -> runCatching {
        context.contentResolver.openOutputStream(target)?.use { it.write(pdf) }; downloaded = true
    } } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invoice details", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (downloaded) item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    AppLottieAnimation(AppAnimation.OrderSuccess, Modifier.size(72.dp))
                }; Text("Invoice downloaded", Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                item { InvoicePreviewCard("Customer") {
                    Text(order.customerName, fontWeight = FontWeight.SemiBold)
                    Text(order.customerMobile, style = MaterialTheme.typography.bodySmall)
                    Text(order.addressSummary, style = MaterialTheme.typography.bodySmall)
                } }
                item { Text("Items", fontWeight = FontWeight.Bold) }
                items(order.items, key = { it.productId }) { line ->
                    Row(Modifier.fillMaxWidth()) {
                        Text("${line.name} × ${line.quantity}", Modifier.weight(1f))
                        Text(money(line.lineTotalMinor))
                    }
                }
                item { InvoicePreviewCard("Bill details") {
                    InvoiceAmountRow("Subtotal", order.subtotalMinor)
                    if (order.discountMinor > 0) InvoiceAmountRow("Discount", -order.discountMinor)
                    InvoiceAmountRow("Delivery", order.deliveryFeeMinor)
                    InvoiceAmountRow("Tax", order.taxMinor)
                    HorizontalDivider(); InvoiceAmountRow("Grand total", order.totalMinor, true)
                } }
            }
        },
        confirmButton = { Button({ saver.launch("invoice-${order.displayNumber}.pdf") }) { Text("Download PDF") } },
        dismissButton = { TextButton(onDismiss) { Text("Close") } },
    )
}

@Composable
private fun InvoicePreviewCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(
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

private fun createInvoicePdf(context: Context, order: CodOrder): ByteArray {
    val document = PdfDocument()
    val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val canvas = page.canvas
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 12f }
    canvas.drawColor(Color.WHITE)
    paint.color = Color.rgb(18, 18, 18); canvas.drawRect(0f, 0f, 595f, 105f, paint)
    paint.color = Color.rgb(215, 25, 32); canvas.drawRect(0f, 105f, 595f, 111f, paint)
    runCatching { context.applicationInfo.loadIcon(context.packageManager).let { drawable ->
        val bitmap = Bitmap.createBitmap(54, 54, Bitmap.Config.ARGB_8888)
        val iconCanvas = android.graphics.Canvas(bitmap); drawable.setBounds(0, 0, 54, 54); drawable.draw(iconCanvas)
        canvas.drawBitmap(bitmap, 38f, 24f, paint)
    } }
    paint.color = Color.WHITE; paint.typeface = Typeface.DEFAULT_BOLD; paint.textSize = 24f
    canvas.drawText(order.shopName.uppercase(), 105f, 48f, paint)
    paint.textSize = 30f; canvas.drawText("INVOICE", 410f, 57f, paint)
    paint.color = Color.BLACK; paint.textSize = 13f; canvas.drawText(order.displayNumber, 40f, 140f, paint)
    if (order.shopAddress.isNotBlank()) canvas.drawText(order.shopAddress.take(70), 40f, 160f, paint)
    paint.typeface = Typeface.DEFAULT; var y = 112f
    y = 195f; paint.typeface = Typeface.DEFAULT_BOLD; canvas.drawText("INVOICE TO", 40f, y, paint); y += 20f
    paint.typeface = Typeface.DEFAULT; canvas.drawText(order.customerName, 40f, y, paint); y += 18f
    canvas.drawText(order.customerMobile, 40f, y, paint); y += 20f
    order.addressSummary.chunked(72).take(3).forEach { canvas.drawText(it, 40f, y, paint); y += 17f }
    y += 18f; paint.color = Color.rgb(215, 25, 32); canvas.drawRect(35f, y - 17f, 560f, y + 7f, paint)
    paint.color = Color.WHITE; paint.typeface = Typeface.DEFAULT_BOLD
    canvas.drawText("Item", 45f, y, paint); canvas.drawText("Qty", 385f, y, paint); canvas.drawText("Amount", 475f, y, paint); y += 24f
    paint.color = Color.BLACK
    paint.typeface = Typeface.DEFAULT
    order.items.take(24).forEach { item ->
        canvas.drawText(item.name.take(48), 45f, y, paint); canvas.drawText(item.quantity.toString(), 390f, y, paint)
        canvas.drawText(money(item.lineTotalMinor), 470f, y, paint); y += 20f
    }
    y += 10f; paint.typeface = Typeface.DEFAULT_BOLD; paint.textSize = 16f
    canvas.drawText("Subtotal: ${money(order.subtotalMinor)}", 340f, y, paint); y += 22f
    paint.color = Color.rgb(215, 25, 32); canvas.drawRect(330f, y - 17f, 560f, y + 8f, paint)
    paint.color = Color.WHITE; canvas.drawText("TOTAL: ${money(order.totalMinor)}", 345f, y, paint)
    paint.color = Color.DKGRAY; paint.textSize = 10f; paint.typeface = Typeface.DEFAULT
    canvas.drawText("Thank you for shopping with ${order.shopName}.", 40f, 800f, paint)
    document.finishPage(page)
    return ByteArrayOutputStream().use { output -> document.writeTo(output); document.close(); output.toByteArray() }
}

private fun money(value: Long): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    .format(value / 100.0)
