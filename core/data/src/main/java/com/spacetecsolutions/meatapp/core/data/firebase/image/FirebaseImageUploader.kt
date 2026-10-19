package com.spacetecsolutions.meatapp.core.data.firebase.image

import android.net.Uri
import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.spacetecsolutions.meatapp.core.data.firebase.di.FirebaseRegionConfig
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

internal class FirebaseImageUploader @Inject constructor(
    private val storage: FirebaseStorage,
    private val processor: FirebaseImageProcessor,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val appCheck: FirebaseAppCheck,
    private val region: FirebaseRegionConfig,
) {
    suspend fun upload(localUri: String, storagePath: String, kind: ImageUploadKind) {
        val processed = processor.process(localUri, kind)
        var task: com.google.firebase.storage.UploadTask? = null
        val uid = auth.currentUser?.uid
        val bucket = storage.reference.bucket
        val contentType = JPEG_CONTENT_TYPE
        logUploadContext(uid, bucket, storagePath, processed.file.length(), contentType)
        try {
            task = storage.reference.child(storagePath).putFile(
                Uri.fromFile(processed.file),
                StorageMetadata.Builder().setContentType(contentType).build(),
            )
            task.await()
            Log.i(TAG, "Storage upload succeeded bucket=$bucket path=$storagePath")
        } catch (cancelled: CancellationException) {
            task?.cancel()
            throw cancelled
        } catch (error: StorageException) {
            Log.e(TAG, "Storage upload failed uid=$uid bucket=$bucket path=$storagePath " +
                "sizeBytes=${processed.file.length()} contentType=$contentType " +
                "storageErrorCode=${error.errorCode} httpResultCode=${error.httpResultCode} " +
                "message=${error.message}", error)
            throw error
        } finally {
            processed.file.delete()
        }
    }

    private suspend fun logUploadContext(uid: String?, bucket: String, path: String, sizeBytes: Long, contentType: String) {
        val app = storage.app
        Log.i(TAG, "Storage upload context uid=$uid projectId=${app.options.projectId} bucket=$bucket " +
            "database=${region.firestoreDatabaseId} path=$path sizeBytes=$sizeBytes contentType=$contentType " +
            "mode=CREATE_ONLY sessionId=${path.substringAfterLast('/').substringBeforeLast('.')}")

        runCatching { appCheck.getAppCheckToken(false).await() }
            .onSuccess { token ->
                Log.i(TAG, "App Check token available projectId=${app.options.projectId} " +
                    "expiresAtMillis=${token.expireTimeMillis}")
            }
            .onFailure { error ->
                Log.e(TAG, "App Check token unavailable projectId=${app.options.projectId} " +
                    "error=${error.javaClass.simpleName}:${error.message}")
            }

        if (uid == null) {
            Log.w(TAG, "Storage rule comparison: signedIn=false; profile not queried")
            return
        }
        runCatching { firestore.collection("users").document(uid).get(Source.SERVER).await() }
            .onSuccess { profile ->
                val active = profile.getBoolean("active")
                val role = profile.getString("role")
                val profileShopId = profile.getString("shopId")
                val permissions = profile.get("permissions") as? List<*>
                val pathShopId = path.split('/').getOrNull(1)
                val catalogStaff = role == "STAFF" && permissions?.contains("MANAGE_PRODUCTS") == true
                Log.i(TAG, "Storage rule comparison uid=$uid profileExists=${profile.exists()} " +
                    "active=$active role=$role profileShopId=$profileShopId pathShopId=$pathShopId " +
                    "isAdmin=${role == "ADMIN"} isSuperAdmin=${role == "SUPER_ADMIN"} " +
                    "catalogStaff=$catalogStaff activeRule=${active == true} " +
                    "roleRule=${role == "ADMIN" || catalogStaff} " +
                    "shopRule=${profileShopId != null && profileShopId == pathShopId} " +
                    "sizeRule=${sizeBytes in 1..MAX_UPLOAD_BYTES} mimeRule=${contentType == JPEG_CONTENT_TYPE} " +
                    "createOnlyPathUsesSessionId=${path.contains("/uploads/")}")
            }
            .onFailure { error ->
                Log.e(TAG, "Could not read Storage-rule profile uid=$uid database=${region.firestoreDatabaseId} " +
                    "error=${error.javaClass.simpleName}:${error.message}")
            }
    }

    private companion object {
        const val TAG = "FirebaseImageUpload"
        const val JPEG_CONTENT_TYPE = "image/jpeg"
        const val MAX_UPLOAD_BYTES = 5L * 1024 * 1024
    }
}
