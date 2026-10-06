package com.spacetecsolutions.meatapp.core.data.firebase.auth

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.AuthenticationRepository
import com.spacetecsolutions.meatapp.core.model.CustomerRegistration
import com.spacetecsolutions.meatapp.core.model.PasswordResetChallenge
import com.spacetecsolutions.meatapp.core.model.VerifiedResetChallenge
import com.spacetecsolutions.meatapp.core.model.RegistrationChallenge
import com.spacetecsolutions.meatapp.core.model.VerifiedRegistrationChallenge
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.suspendCancellableCoroutine
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.coroutines.resume

internal class FirebaseAuthenticationRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
    private val appCheck: FirebaseAppCheck,
    private val appCheckProviderFactory: AppCheckProviderFactory,
    private val defaultApp: FirebaseApp,
    private val activityProvider: PhoneAuthActivityProvider,
    @ApplicationContext private val context: Context,
) : AuthenticationRepository {
    private val automaticCredentials = ConcurrentHashMap<String, PhoneAuthCredential>()
    private val resendTokens = ConcurrentHashMap<String, PhoneAuthProvider.ForceResendingToken>()
    private val phoneAuth by lazy {
        val app = FirebaseApp.getApps(context).firstOrNull { it.name == PHONE_AUTH_APP }
            ?: FirebaseApp.initializeApp(context, defaultApp.options, PHONE_AUTH_APP)
        val phoneApp = requireNotNull(app)
        FirebaseAppCheck.getInstance(phoneApp).installAppCheckProviderFactory(appCheckProviderFactory)
        FirebaseAuth.getInstance(phoneApp)
    }
    override val authenticatedUserId: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(mobileNumber: String, password: String): AppResult<String> =
        authenticateWithCustomToken(
            functionName = "authSignInWithMobilePassword",
            payload = mapOf("mobileNumber" to mobileNumber, "password" to password),
        )

    override suspend fun requestCustomerOtp(mobileNumber: String): AppResult<RegistrationChallenge> =
        requestCustomerRegistration(mobileNumber)

    override suspend fun signInCustomerWithOtp(challengeId: String, code: String): AppResult<String> =
        verifiedPhoneToken(challengeId, code).flatMap { token ->
            authenticateWithCustomToken("authSignInCustomerWithPhone",
                mapOf("phoneVerificationIdToken" to token))
        }

    override suspend fun requestCustomerRegistration(
        mobileNumber: String,
    ): AppResult<RegistrationChallenge> = when (val result = requestPhoneCode(mobileNumber)) {
        is AppResult.Failure -> result
        is AppResult.Success -> AppResult.Success(RegistrationChallenge(
            result.value.id, maskMobile(mobileNumber), result.value.automatic,
        ))
    }

    override suspend fun verifyCustomerRegistration(
        challengeId: String,
        verificationCode: String,
    ): AppResult<VerifiedRegistrationChallenge> = verifiedPhoneToken(challengeId, verificationCode).flatMap { token -> call(
        "authVerifyCustomerRegistration", mapOf("phoneVerificationIdToken" to token),
    ) {
        VerifiedRegistrationChallenge(
            verificationToken = it.requiredString("verificationToken"),
            accountExists = it["accountExists"] as? Boolean ?: false,
        )
    } }

    override suspend fun registerCustomer(registration: CustomerRegistration): AppResult<String> =
        authenticateWithCustomToken(
            functionName = "authRegisterCustomer",
            payload = mapOf(
                "firstName" to registration.firstName,
                "lastName" to registration.lastName,
                "mobileNumber" to registration.mobileNumber,
                "password" to registration.password,
                "verificationToken" to registration.verificationToken,
            ),
        )

    override suspend fun requestPasswordReset(
        mobileNumber: String,
    ): AppResult<PasswordResetChallenge> = when (val result = requestPhoneCode(mobileNumber)) {
        is AppResult.Failure -> result
        is AppResult.Success -> AppResult.Success(PasswordResetChallenge(
            result.value.id, maskMobile(mobileNumber), result.value.automatic,
        ))
    }

    override suspend fun verifyPasswordResetCode(
        challengeId: String,
        verificationCode: String,
    ): AppResult<VerifiedResetChallenge> = verifiedPhoneToken(challengeId, verificationCode).flatMap { token -> call(
        "authVerifyPasswordResetCode", mapOf("phoneVerificationIdToken" to token),
    ) { VerifiedResetChallenge(it.requiredString("resetToken")) } }

    override suspend fun resetPassword(resetToken: String, newPassword: String): AppResult<Unit> =
        call(
            "authResetPassword",
            mapOf("resetToken" to resetToken, "newPassword" to newPassword),
        ) { Unit }

    override suspend fun signOut(): AppResult<Unit> = try {
        auth.signOut()
        AppResult.Success(Unit)
    } catch (error: Exception) {
        AppResult.Failure(error.toAppError())
    }

    private suspend fun requestPhoneCode(mobile: String): AppResult<PhoneChallenge> {
        val activity = activityProvider.currentActivity()
            ?: return AppResult.Failure(AppError.Validation(message = "Open the app and try phone verification again"))
        return suspendCancellableCoroutine { continuation ->
            val delivered = AtomicBoolean(false)
            fun complete(result: AppResult<PhoneChallenge>) {
                if (delivered.compareAndSet(false, true) && continuation.isActive) continuation.resume(result)
            }
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    resendTokens[mobile] = token
                    complete(AppResult.Success(PhoneChallenge(id, false)))
                }
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    val id = "automatic:${UUID.randomUUID()}"
                    if (delivered.compareAndSet(false, true) && continuation.isActive) {
                        automaticCredentials[id] = credential
                        continuation.resume(AppResult.Success(PhoneChallenge(id, true)))
                    }
                }
                override fun onVerificationFailed(error: com.google.firebase.FirebaseException) {
                    Log.e(AUTH_LOG_TAG, "Phone OTP request failed", error)
                    complete(AppResult.Failure(error.toAppError()))
                }
                override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                    complete(AppResult.Failure(AppError.Timeout))
                }
            }
            runCatching {
                val options = PhoneAuthOptions.newBuilder(phoneAuth)
                    .setPhoneNumber(mobile).setTimeout(60, TimeUnit.SECONDS)
                    .setActivity(activity).setCallbacks(callbacks)
                resendTokens[mobile]?.let(options::setForceResendingToken)
                PhoneAuthProvider.verifyPhoneNumber(options.build())
            }.onFailure {
                Log.e(AUTH_LOG_TAG, "Phone OTP request could not start", it)
                complete(AppResult.Failure(it.toAppError()))
            }
        }
    }

    private suspend fun verifiedPhoneToken(challengeId: String, code: String): AppResult<String> = try {
        val credential = automaticCredentials.remove(challengeId)
            ?: PhoneAuthProvider.getCredential(challengeId, code)
        val user = phoneAuth.signInWithCredential(credential).await().user
            ?: return AppResult.Failure(AppError.InvalidVerificationCode)
        val token = user.getIdToken(true).await().token
            ?: return AppResult.Failure(AppError.InvalidVerificationCode)
        phoneAuth.signOut()
        AppResult.Success(token)
    } catch (error: Exception) {
        phoneAuth.signOut()
        AppResult.Failure(error.toAppError())
    }

    private suspend fun authenticateWithCustomToken(
        functionName: String,
        payload: Map<String, Any>,
    ): AppResult<String> = when (val tokenResult = call(functionName, payload) { it.requiredString("customToken") }) {
        is AppResult.Failure -> tokenResult
        is AppResult.Success -> try {
            val userId = auth.signInWithCustomToken(tokenResult.value).await().user?.uid
                ?: return AppResult.Failure(AppError.Unauthorized)
            AppResult.Success(userId)
        } catch (error: Exception) {
            AppResult.Failure(error.toAppError())
        }
    }

    private suspend fun <T> call(
        name: String,
        payload: Map<String, Any>,
        transform: (Map<*, *>) -> T,
    ): AppResult<T> = try {
        // Resolve App Check first so emulator/provider failures are distinguishable from
        // invalid login credentials returned by the secured callable itself.
        appCheck.getAppCheckToken(false).await()
        val data = functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *>
            ?: return AppResult.Failure(AppError.Unknown())
        AppResult.Success(transform(data))
    } catch (error: Exception) {
        Log.e(AUTH_LOG_TAG, "Firebase callable $name failed", error)
        AppResult.Failure(error.toAppError())
    }
}

private data class PhoneChallenge(val id: String, val automatic: Boolean)

private suspend inline fun <T, R> AppResult<T>.flatMap(
    crossinline transform: suspend (T) -> AppResult<R>,
): AppResult<R> = when (this) {
    is AppResult.Success -> transform(value)
    is AppResult.Failure -> this
}

private fun maskMobile(mobile: String) = "${mobile.take(3)} •••••• ${mobile.takeLast(2)}"

// Rotated after Android backup restored unusable Firebear/Keystore material for the
// previous named app. Keep this isolated from the signed-in default Firebase session.
private const val PHONE_AUTH_APP = "phone-verification-v2"
private const val AUTH_LOG_TAG = "FirebaseAuthentication"

private fun Map<*, *>.requiredString(key: String): String =
    this[key] as? String ?: error("Missing callable response field: $key")

private fun Throwable.toAppError(): AppError {
    val functionsError = this as? FirebaseFunctionsException
    val authErrorCode = (this as? FirebaseAuthException)?.errorCode
    val normalizedMessage = message.orEmpty().uppercase()
    val reason = (functionsError?.details as? Map<*, *>)?.get("reason") as? String
    return when {
        this is FirebaseTooManyRequestsException -> AppError.TooManyRequests
        this is FirebaseNetworkException -> AppError.Network
        this is FirebaseAuthMissingActivityForRecaptchaException ->
            AppError.Validation(message = "Phone verification could not open. Please try again")
        "APP ATTESTATION FAILED" in normalizedMessage ||
            "APP CHECK" in normalizedMessage ||
            "DEBUG TOKEN" in normalizedMessage ->
            AppError.Validation(
                message = "This emulator is not authorized by Firebase App Check. Register its debug token and try again",
            )
        authErrorCode == "ERROR_OPERATION_NOT_ALLOWED" ->
            AppError.Validation(message = "Phone verification is not enabled for this app")
        authErrorCode == "ERROR_APP_NOT_AUTHORIZED" ->
            AppError.Validation(message = "This app is not authorized for phone verification")
        authErrorCode == "ERROR_INVALID_APP_CREDENTIAL" || authErrorCode == "ERROR_INVALID_CERT_HASH" ->
            AppError.Validation(message = "Phone verification is not configured for this app build")
        authErrorCode == "ERROR_INVALID_PHONE_NUMBER" ->
            AppError.Validation(field = "mobile", message = "Enter a valid mobile number")
        authErrorCode in setOf("ERROR_QUOTA_EXCEEDED", "ERROR_TOO_MANY_REQUESTS") ->
            AppError.TooManyRequests
        authErrorCode == "ERROR_CAPTCHA_CHECK_FAILED" || "CAPTCHA_CHECK_FAILED" in normalizedMessage ->
            AppError.Validation(message = "Phone verification failed. Close the verification page and try again")
        authErrorCode == "ERROR_MISSING_CLIENT_IDENTIFIER" || "APP_NOT_AUTHORIZED" in normalizedMessage ->
            AppError.Validation(message = "This app is not configured correctly for phone verification")
        authErrorCode == "ERROR_SESSION_EXPIRED" -> AppError.InvalidVerificationCode
        this is FirebaseAuthInvalidCredentialsException -> AppError.InvalidVerificationCode
        reason == "ACCOUNT_NOT_FOUND" -> AppError.NotFound
        reason == "INVALID_PASSWORD" -> AppError.Validation("password", "Password does not meet the security requirements")
        reason == "INVALID_REGISTRATION" -> AppError.Validation(message = "Check your registration details and try again")
        reason == "RESET_EXPIRED" -> AppError.InvalidVerificationCode
        reason == "USER_DISABLED" -> AppError.DisabledUser
        reason == "DUPLICATE_ACCOUNT" -> AppError.DuplicateAccount
        reason == "INVALID_VERIFICATION_CODE" -> AppError.InvalidVerificationCode
        functionsError?.code == FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.InvalidCredentials
        functionsError?.code == FirebaseFunctionsException.Code.ALREADY_EXISTS -> AppError.DuplicateAccount
        functionsError?.code == FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> AppError.TooManyRequests
        functionsError?.code == FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
        functionsError?.code == FirebaseFunctionsException.Code.NOT_FOUND ->
            AppError.Validation(message = "Phone sign-in service is not deployed. Please contact support")
        else -> AppError.Unknown(this)
    }
}
