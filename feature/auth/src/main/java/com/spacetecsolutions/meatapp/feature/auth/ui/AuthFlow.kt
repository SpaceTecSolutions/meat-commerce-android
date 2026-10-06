package com.spacetecsolutions.meatapp.feature.auth.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppMotion
import com.spacetecsolutions.meatapp.core.designsystem.theme.AppSpacing
import com.spacetecsolutions.meatapp.feature.auth.presentation.AuthFeedback

private const val LOGIN = "login"
private const val REGISTER = "register_customer"
private const val RESET = "reset_password"

@Composable
fun AuthenticationFlow(
    appName: String,
    @DrawableRes bgRes: Int,
    @DrawableRes logoRes: Int,
    modifier: Modifier = Modifier,
    sessionMessage: String? = null,
    staffOnly: Boolean = false,
    onFeedback: (AuthFeedback) -> Unit = {},
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = LOGIN,
        modifier = modifier,
        enterTransition = { fadeIn(tween(AppMotion.STANDARD_MILLIS)) },
        exitTransition = { fadeOut(tween(AppMotion.QUICK_MILLIS)) },
        popEnterTransition = { fadeIn(tween(AppMotion.MICRO_MILLIS)) },
        popExitTransition = { fadeOut(tween(AppMotion.QUICK_MILLIS)) },
    ) {
        composable(LOGIN) {
            LoginScreen(
                appName = appName,
                bgRes = bgRes,
                logoRes = logoRes,
                onRegister = { navController.navigate(REGISTER) },
                onForgotPassword = { navController.navigate(RESET) },
                onFeedback = onFeedback,
                sessionMessage = sessionMessage,
                allowRegistration = !staffOnly,
            )
        }
        composable(REGISTER) {
            RegisterScreen(appName, bgRes = bgRes, logoRes = logoRes,
                onLogin = navController::popBackStack, onFeedback = onFeedback)
        }
        composable(RESET) {
            ResetPasswordScreen(appName, bgRes = bgRes, logoRes = logoRes,
                onLogin = navController::popBackStack, onFeedback = onFeedback)
        }
    }
}

@Composable
fun SplashScreen(appName: String, @DrawableRes bgRes: Int,@DrawableRes logoRes: Int, modifier: Modifier = Modifier) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(bgRes),
            contentDescription = appName,
            contentScale = ContentScale.Crop
        )
    }
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(logoRes),
            contentDescription = appName,
            modifier = Modifier.size(180.dp),
        )
        /*Text(
            text = appName,
            modifier = Modifier.padding(top = AppSpacing.medium),
            style = MaterialTheme.typography.headlineSmall,
        )*/
        CircularProgressIndicator(modifier = Modifier.padding(top = AppSpacing.large).size(28.dp))
    }
}
