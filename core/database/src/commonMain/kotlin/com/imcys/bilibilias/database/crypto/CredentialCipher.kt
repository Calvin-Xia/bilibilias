package com.imcys.bilibilias.database.crypto

/**
 * 凭据加解密。
 *
 * 用于账号 token 与 Cookie 值的落库保护：写入前加密、读取后解密。
 * 实现必须保证 [encrypt] 与 [decrypt] 互逆，且不得在异常时泄露明文。
 */
interface CredentialCipher {

    /** 加密明文。返回可安全落库的字符串。 */
    fun encrypt(plainText: String): String

    /**
     * 解密落库字符串。
     *
     * 解密失败（密钥失效、数据被篡改、格式不符）时必须返回 null，由调用方丢弃该凭据，
     * 不允许回退为明文。
     */
    fun decrypt(storedValue: String): String?
}

/** 当前平台的凭据加解密实现。 */
expect fun platformCredentialCipher(): CredentialCipher

/**
 * 不加密的实现，供 iOS 与测试使用。
 *
 * 注意：这是明文存储，仅因为当前定位为「仅维护 Android」而保留，不得用于 Android。
 */
object PassthroughCredentialCipher : CredentialCipher {
    override fun encrypt(plainText: String): String = plainText

    override fun decrypt(storedValue: String): String = storedValue
}
