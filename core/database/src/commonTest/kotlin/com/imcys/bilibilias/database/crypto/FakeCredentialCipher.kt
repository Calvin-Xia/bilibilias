package com.imcys.bilibilias.database.crypto

/**
 * 测试用加解密：给明文加 `enc:` 前缀，能识别的才还原。
 *
 * 与 Android 实现不同，它不依赖 Keystore，因此可以在 host test 中运行；
 * 同时用「前缀不匹配则解密失败」模拟密钥失效场景。
 */
class FakeCredentialCipher(private val prefix: String = "enc:") : CredentialCipher {
    override fun encrypt(plainText: String): String = prefix + plainText

    override fun decrypt(storedValue: String): String? =
        storedValue.removePrefix(prefix).takeIf { storedValue.startsWith(prefix) }
}
