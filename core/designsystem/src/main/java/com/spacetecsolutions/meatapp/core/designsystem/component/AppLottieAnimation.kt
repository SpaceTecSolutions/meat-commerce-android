package com.spacetecsolutions.meatapp.core.designsystem.component

import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.spacetecsolutions.meatapp.core.designsystem.R

enum class AppAnimation(@param:RawRes val resourceId: Int) {
    Loading(R.raw.loading),
    EmptyData(R.raw.empty_data_cat),
    OrderSuccess(R.raw.order_success),
    OrderCancelled(R.raw.order_cancel),
    AddToCart(R.raw.add_to_cart),
}

@Composable
fun AppLottieAnimation(
    animation: AppAnimation,
    modifier: Modifier = Modifier,
    loop: Boolean = false,
    tint: Color = Color.Unspecified,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animation.resourceId))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = if (loop) LottieConstants.IterateForever else 1,
        restartOnPlay = true,
    )
    val dynamicProperties = if (tint != Color.Unspecified) {
        rememberLottieDynamicProperties(
            rememberLottieDynamicProperty(
                property = LottieProperty.COLOR,
                value = tint.toArgb(),
                keyPath = arrayOf("**"),
            ),
        )
    } else null
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier,
        dynamicProperties = dynamicProperties,
    )
}
