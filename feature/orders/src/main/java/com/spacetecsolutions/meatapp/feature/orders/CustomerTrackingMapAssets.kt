package com.spacetecsolutions.meatapp.feature.orders

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.animation.ValueAnimator
import androidx.annotation.ColorInt
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

internal fun deliveryBikeMarkerBitmap(context: Context, @ColorInt color: Int): Bitmap {
    val density = context.resources.displayMetrics.density
    val width = (52 * density).toInt().coerceAtLeast(52)
    val height = (62 * density).toInt().coerceAtLeast(62)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val w = width.toFloat(); val h = height.toFloat()
    val pin = Path().apply {
        moveTo(w * .28f, h * .52f); lineTo(w * .5f, h * .96f)
        lineTo(w * .72f, h * .52f); close()
    }
    paint.setShadowLayer(w * .07f, 0f, w * .035f, 0x55000000)
    paint.color = color
    canvas.drawPath(pin, paint)
    canvas.drawCircle(w / 2, h * .37f, w * .42f, paint)
    paint.clearShadowLayer()
    context.getDrawable(R.drawable.ic_delivery_dining_marker)?.apply {
        setBounds(
            (w * .22f).toInt(), (h * .12f).toInt(),
            (w * .78f).toInt(), (h * .48f).toInt(),
        )
        draw(canvas)
    }
    return bitmap
}

internal fun animateRiderMarker(
    marker: Marker,
    destination: LatLng,
    headingDegrees: Float?,
): ValueAnimator {
    val start = marker.position
    val startRotation = marker.rotation
    val targetRotation = headingDegrees ?: startRotation
    val rotationDelta = ((targetRotation - startRotation + 540f) % 360f) - 180f
    return ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 700
        addUpdateListener { animation ->
            val value = animation.animatedFraction.toDouble()
            marker.position = LatLng(
                start.latitude + (destination.latitude - start.latitude) * value,
                start.longitude + (destination.longitude - start.longitude) * value,
            )
            marker.rotation = startRotation + rotationDelta * animation.animatedFraction
        }
        start()
    }
}

internal fun decodeRoutePolyline(encoded: String): List<LatLng> {
    val points = mutableListOf<LatLng>()
    var index = 0; var latitude = 0; var longitude = 0
    while (index < encoded.length) {
        val lat = decodeValue(encoded, index); index = lat.second; latitude += lat.first
        val lng = decodeValue(encoded, index); index = lng.second; longitude += lng.first
        points += LatLng(latitude / 1e5, longitude / 1e5)
    }
    return points
}

private fun decodeValue(encoded: String, start: Int): Pair<Int, Int> {
    var index = start; var result = 0; var shift = 0; var value: Int
    do {
        value = encoded[index++].code - 63
        result = result or ((value and 0x1f) shl shift)
        shift += 5
    } while (value >= 0x20 && index < encoded.length)
    return (if (result and 1 != 0) (result shr 1).inv() else result shr 1) to index
}
