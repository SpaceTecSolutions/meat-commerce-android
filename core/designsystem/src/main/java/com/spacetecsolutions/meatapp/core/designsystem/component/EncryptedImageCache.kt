package com.spacetecsolutions.meatapp.core.designsystem.component

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.LruCache
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal object EncryptedImageCache {
    private const val KEY_ALIAS = "meatbush_image_cache_v1"
    private const val MAX_ENTRY_BYTES = 15 * 1024 * 1024
    private const val MAX_CACHE_BYTES = 120L * 1024 * 1024
    private const val MAX_MEMORY_BYTES = 20 * 1024 * 1024
    private const val VERSION: Byte = 1
    private val locks = mutableMapOf<String, Mutex>()
    private val memory = object : LruCache<String, ByteArray>(MAX_MEMORY_BYTES) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }

    fun peek(url: String): ByteArray? = memory.get(url.sha256())

    fun evict(context: Context, urls: Collection<String>) {
        val directory = File(context.noBackupFilesDir, "encrypted_image_cache")
        urls.forEach { url -> val name = url.sha256(); memory.remove(name); File(directory, "$name.img").delete() }
    }

    suspend fun load(context: Context, url: String): ByteArray? = withContext(Dispatchers.IO) {
        val name = url.sha256()
        memory.get(name)?.let { return@withContext it }
        val lock = synchronized(locks) { locks.getOrPut(name) { Mutex() } }
        lock.withLock {
            val directory = File(context.noBackupFilesDir, "encrypted_image_cache").apply { mkdirs() }
            val target = File(directory, "$name.img")
            read(target)?.also {
                target.setLastModified(System.currentTimeMillis())
                memory.put(name, it)
                return@withLock it
            }
            val downloaded = download(url) ?: return@withLock null
            write(target, downloaded)
            memory.put(name, downloaded)
            trim(directory)
            downloaded
        }
    }

    private fun download(source: String): ByteArray? {
        val connection = (URL(source).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "image/*")
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            val expected = connection.contentLengthLong
            if (expected > MAX_ENTRY_BYTES) return null
            connection.inputStream.use { input ->
                val output = ByteArrayOutputStream(
                    if (expected in 1L..MAX_ENTRY_BYTES.toLong()) expected.toInt() else 32_768,
                )
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_ENTRY_BYTES) return null
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun read(file: File): ByteArray? = runCatching {
        if (!file.isFile) return null
        val payload = file.readBytes()
        if (payload.size < 14 || payload[0] != VERSION) error("Invalid cache entry")
        val ivSize = payload[1].toInt()
        if (ivSize !in 12..16 || payload.size <= 2 + ivSize) error("Invalid cache entry")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, payload, 2, ivSize))
        cipher.doFinal(payload, 2 + ivSize, payload.size - 2 - ivSize)
    }.getOrElse { file.delete(); null }

    private fun write(target: File, bytes: ByteArray) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(bytes)
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.outputStream().buffered().use { output ->
            output.write(byteArrayOf(VERSION, cipher.iv.size.toByte()))
            output.write(cipher.iv)
            output.write(encrypted)
        }
        if (!temporary.renameTo(target)) {
            target.delete()
            check(temporary.renameTo(target)) { "Unable to store image cache" }
        }
    }

    @Synchronized
    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
            generateKey()
        }
    }

    private fun trim(directory: File) {
        val files = directory.listFiles { file -> file.extension == "img" }.orEmpty()
            .sortedByDescending(File::lastModified)
        var retained = 0L
        files.forEach { file ->
            retained += file.length()
            if (retained > MAX_CACHE_BYTES) file.delete()
        }
    }

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray()).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
