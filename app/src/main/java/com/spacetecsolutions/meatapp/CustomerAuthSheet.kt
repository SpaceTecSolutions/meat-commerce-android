package com.spacetecsolutions.meatapp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.spacetecsolutions.meatapp.core.designsystem.component.AppAnimation
import com.spacetecsolutions.meatapp.core.designsystem.component.AppLottieAnimation

private val red = Color(0xFFD71920)
private val ink = Color(0xFF202936)
private val muted = Color(0xFF657083)
private val field = Color(0xFFF8F9FB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomerAuthSheet(state: CustomerAuthState, onDismiss: () -> Unit,
    onPhone: (String) -> Unit, onRequest: () -> Unit, onOtp: (String) -> Unit,
    onChangeNumber: () -> Unit, onRetryOtp: () -> Unit,
    onSuccessAnimationFinished: () -> Unit,
    onTerms: () -> Unit, onPrivacy: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(state.phase) {
        if (state.phase == CustomerAuthPhase.SUCCESS) {
            kotlinx.coroutines.delay(1400)
            sheetState.hide()
            onSuccessAnimationFinished()
        }
    }
    ModalBottomSheet(
        onDismissRequest = {
            if (state.phase !in setOf(
                    CustomerAuthPhase.CONVERGING,
                    CustomerAuthPhase.VERIFYING,
                    CustomerAuthPhase.SUCCESS,
                )) onDismiss()
        },
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { Box(Modifier.padding(top = 13.dp, bottom = 20.dp).size(48.dp, 5.dp)
            .background(Color(0xFFE0E4EA), RoundedCornerShape(50))) }) {
        Column(Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 440.dp).imePadding().navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (state.phase) {
                CustomerAuthPhase.PHONE, CustomerAuthPhase.REQUESTING -> PhoneContent(
                    state, onDismiss, onPhone, onRequest, onTerms, onPrivacy)
                else -> OtpContent(state, onOtp, onChangeNumber, onRequest, onRetryOtp)
            }
        }
    }
}

@Composable
private fun PhoneContent(state: CustomerAuthState, close: () -> Unit,
    phone: (String) -> Unit, request: () -> Unit, terms: () -> Unit, privacy: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Continue with mobile", Modifier.weight(1f), fontSize = 23.sp, color = ink, fontWeight = FontWeight.ExtraBold)
        TextButton(onClick = close) { Text("✕", color = muted) }
    }
    Text("Enter your 10-digit mobile number to access your account, saved cuts, and orders.",
        fontSize = 13.sp, lineHeight = 18.sp, color = muted)
    Spacer(Modifier.height(8.dp))
    Text("MOBILE NUMBER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = muted)
    OutlinedTextField(value = state.phone, onValueChange = phone, modifier = Modifier.fillMaxWidth(),
        prefix = { Text("IN  +91  │  ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ink) },
        placeholder = { Text("Enter mobile number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true, isError = state.error != null, shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = red, unfocusedContainerColor = field,
            focusedContainerColor = field))
    Text("♙  We'll send a 6-digit verification code via SMS", fontSize = 11.sp, color = muted)
    state.error?.let { Text(it, color = red, fontSize = 12.sp) }
    var accepted by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.Top) {
        Checkbox(checked = accepted, onCheckedChange = { accepted = it }, modifier = Modifier.size(40.dp),
            colors = CheckboxDefaults.colors(checkedColor = red))
        FlowRow(Modifier.padding(top = 9.dp)) {
            Text("I agree to the", fontSize = 12.sp, color = muted)
            Row { Text("Terms & Conditions", Modifier.clickable(onClick = terms), fontSize = 12.sp,
                color = ink, fontWeight = FontWeight.Bold)
                Text(" and acknowledge the ", fontSize = 12.sp, color = muted) }
            Text("Privacy Policy", Modifier.clickable(onClick = privacy), fontSize = 12.sp,
                color = ink, fontWeight = FontWeight.Bold)
        }
    }
    Button(onClick = request, enabled = accepted && state.phone.length == 10 &&
        state.phase != CustomerAuthPhase.REQUESTING, modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = red)) {
        if (state.phase == CustomerAuthPhase.REQUESTING) CircularProgressIndicator(Modifier.size(18.dp),
            strokeWidth = 2.dp, color = Color.White)
        else Text("Get OTP  →", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OtpContent(state: CustomerAuthState, otp: (String) -> Unit,
    changeNumber: () -> Unit, resend: () -> Unit, retryOtp: () -> Unit) {
    val result = when (state.phase) {
        CustomerAuthPhase.SUCCESS -> true
        CustomerAuthPhase.INVALID -> false
        else -> null
    }
    AnimatedContent(
        targetState = result,
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = .9f)) togetherWith
                (fadeOut() + scaleOut(targetScale = .88f))
        },
        label = "OTP validation result",
    ) { validationResult ->
        if (validationResult != null) {
            OtpValidationResult(
                success = validationResult,
                message = state.error,
                onRetry = retryOtp,
            )
        } else {
            OtpEntryContent(state, otp, changeNumber, resend)
        }
    }
}

@Composable
private fun OtpEntryContent(state: CustomerAuthState, otp: (String) -> Unit,
    changeNumber: () -> Unit, resend: () -> Unit) {
    val converging = state.phase == CustomerAuthPhase.CONVERGING
    val verifying = state.phase in setOf(CustomerAuthPhase.CONVERGING, CustomerAuthPhase.VERIFYING)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!verifying) TextButton(onClick = changeNumber) { Text("‹", fontSize = 22.sp, color = muted) }
            Text(if (verifying) "Verifying security code" else "Verify your number",
                Modifier.weight(1f), fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold, color = ink)
            if (!verifying) Text("Step 2 of 2", fontSize = 11.sp,
                fontWeight = FontWeight.Bold, color = red)
        }
        if (!verifying) {
            Surface(color = field, shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE9ECF1))) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("OTP sent to +91 ••••• ••${state.phone.takeLast(3)}", Modifier.weight(1f),
                        fontSize = 12.sp, color = muted)
                    Text("Change", Modifier.clickable(onClick = changeNumber), fontSize = 12.sp,
                        color = red, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("ENTER 6-DIGIT CODE", Modifier.fillMaxWidth(), fontSize = 11.sp,
                fontWeight = FontWeight.Bold, color = muted, textAlign = TextAlign.Center)
        } else {
            Text("Please wait while we securely validate your OTP.", Modifier.fillMaxWidth(),
                fontSize = 12.sp, color = muted, textAlign = TextAlign.Center)
        }
        Box(Modifier.fillMaxWidth()) {
            OtpTiles(state.otp, converging || state.phase == CustomerAuthPhase.VERIFYING)
            if (!verifying) {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(state.phase) { focusRequester.requestFocus() }
                BasicTextField(value = state.otp, onValueChange = otp,
                    modifier = Modifier.matchParentSize().focusRequester(focusRequester)
                        .semantics { contentDescription = "Six digit verification code" }.alpha(0.01f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true)
            }
        }
        if (verifying) {
            Text("Verifying...", Modifier.fillMaxWidth(), fontSize = 15.sp,
                fontWeight = FontWeight.Bold, color = ink, textAlign = TextAlign.Center)
            Text("Please do not close this window", Modifier.fillMaxWidth(), fontSize = 11.sp,
                color = muted, textAlign = TextAlign.Center)
        } else {
            state.error?.let { Text("❗ $it", fontSize = 12.sp, color = red,
                fontWeight = FontWeight.SemiBold) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Didn't receive the OTP?", fontSize = 12.sp, color = muted)
                if (state.secondsUntilResend > 0) Text("Resend in 00:${state.secondsUntilResend.toString().padStart(2, '0')}",
                    fontSize = 12.sp, color = muted)
                else Text("Resend OTP", Modifier.clickable(onClick = resend), fontSize = 12.sp,
                    color = red, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OtpTiles(code: String, verifying: Boolean) {
    val convergence by animateFloatAsState(
        targetValue = if (verifying) 1f else 0f,
        animationSpec = tween(durationMillis = 820, easing = FastOutSlowInEasing),
        label = "OTP tile convergence",
    )

    val infiniteTransition = rememberInfiniteTransition(
        label = "OTP continuous rotation"
    )

    val continuousRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "OTP spinner rotation"
    )

    val density = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxWidth().height(76.dp), contentAlignment = Alignment.Center) {
        val tileWidth = ((maxWidth - 30.dp) / 6).coerceAtMost(48.dp)
        val stepPx = with(density) { (tileWidth + 6.dp).toPx() }
        val rotations = floatArrayOf(-90f, -74f, -42f, 38f, 72f, 90f)
        repeat(6) { index ->
            val initialOffset = (index - 2.5f) * stepPx
            Surface(
                Modifier
                    .align(Alignment.Center)
                    .size(tileWidth, 56.dp)
                    .zIndex(index.toFloat())
                    .graphicsLayer {
                        // Move all tiles toward center
                        translationX = initialOffset * (1f - convergence)
                        // Initial fan animation
                        val fanRotation = rotations[index] * convergence
                        // Keep rotating once convergence is complete
                        val spin =
                            if (verifying && convergence > 0.99f) {
                                continuousRotation
                            } else {
                                0f
                            }
                        rotationZ = fanRotation + spin
                        scaleX = 1f - (.035f * convergence)
                        scaleY = scaleX
                    }
                    .shadow(5.dp,
                    RoundedCornerShape(16.dp), ambientColor = red.copy(alpha = .28f),
                    spotColor = red.copy(alpha = .35f)),
                shape = RoundedCornerShape(16.dp),
                color = field,
                border = BorderStroke(2.dp, if (verifying) red else Color(0xFFD9DEE8)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(code.getOrNull(index)?.toString().orEmpty(),
                        fontSize = 20.sp, fontWeight = FontWeight.Black, color = ink)
                }
            }
        }
    }
}

@Composable
private fun OtpValidationResult(success: Boolean, message: String?, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppLottieAnimation(
            animation = if (success) AppAnimation.OrderSuccess else AppAnimation.OrderCancelled,
            modifier = Modifier.size(168.dp),
        )
        Text(if (success) "OTP verified" else "Verification failed", fontSize = 23.sp,
            color = ink, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text(if (success) "Signing you in securely…" else
            (message ?: "The code could not be verified. Please try again."),
            modifier = Modifier.fillMaxWidth(), fontSize = 12.sp, lineHeight = 18.sp,
            color = if (success) muted else red, textAlign = TextAlign.Center)
        if (!success) {
            Spacer(Modifier.height(4.dp))
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = red)) {
                Text("Try OTP again", fontWeight = FontWeight.Bold)
            }
        }
    }
}
