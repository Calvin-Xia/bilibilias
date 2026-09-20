package com.imcys.bilibilias.database.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 基于 Android Keystore 的 AES-GCM 凭据加密。
 *
 * 密钥由 Keystore 生成并保管，应用进程无法导出明文密钥；密文格式为
 * `v1:` + Base64(IV || 密文)。IV 每次随机，因此同一明文两次加密结果不同。
 *
 * 任何解密失败（密钥被系统清除、数据损坏、格式不符）都返回 null，由上层丢弃凭据。
 */
object AndroidKeystoreCredentialCipher : CredentialCipher {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "bilibilias_credential_cipher"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH_BYTES = 12
    private const val TAG_LENGTH_BITS = 128
    private const val VERSION_PREFIX = "v1:"

    override fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, obtainKey())
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val payload = cipher.iv + cipherText
        return VERSION_PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    override fun decrypt(storedValue: String): String? {
        if (!storedValue.startsWith(VERSION_PREFIX)) return null
        return runCatching {
            val payload = Base64.decode(storedValue.removePrefix(VERSION_PREFIX), Base64.NO_WRAP)
            if (payload.size <= IV_LENGTH_BYTES) return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                obtainKey(),
                GCMParameterSpec(TAG_LENGTH_BITS, payload, 0, IV_LENGTH_BYTES),
            )
            cipher.doFinal(payload, IV_LENGTH_BYTES, payload.size - IV_LENGTH_BYTES)
                .toString(Charsets.UTF_8)
        }.getOrNull()
    }

    private fun obtainKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return synchronized(this) {
            val reloaded = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            (reloaded.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).apply {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                )
            }.generateKey()
        }
    }
}

actual fun platformCredentialCipher(): CredentialCipher = AndroidKeystoreCredentialCipher
