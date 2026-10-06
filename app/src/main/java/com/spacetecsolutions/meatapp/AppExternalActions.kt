package com.spacetecsolutions.meatapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.spacetecsolutions.meatapp.tracking.DeliveryTrackingService
import com.spacetecsolutions.meatapp.feature.orders.TrackingCommand

internal fun openNavigation(context: Context, address: String) {
    val uri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
    val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
}

internal fun openDialer(context: Context, mobile: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(mobile)}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
}

internal fun openEmail(context: Context, email: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(email)}"))
        .putExtra(Intent.EXTRA_SUBJECT, "MeatBush customer support")
        .putExtra(Intent.EXTRA_TEXT, "Hello MeatBush Support,\n\nI need help with my account or order.\n\n")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
}

internal fun openWhatsApp(context: Context, mobile: String) {
    val number = mobile.filter(Char::isDigit)
    if (number.isBlank()) return
    val message = Uri.encode("Hello MeatBush Support, I need help with my account or order.")
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number?text=$message"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
}

internal fun handleTrackingCommand(context: Context, command: TrackingCommand) {
    when (command) {
        is TrackingCommand.Start -> DeliveryTrackingService.start(
            context, command.orderId, command.sessionId, command.actor,
        )
        TrackingCommand.Stop -> DeliveryTrackingService.stop(context)
    }
}
