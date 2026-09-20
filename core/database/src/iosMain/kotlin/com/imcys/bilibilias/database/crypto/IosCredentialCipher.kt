package com.imcys.bilibilias.database.crypto

/**
 * iOS 侧凭据仍为明文存储。
 *
 * iOS 目标当前已冻结（仅维护 Android），因此这里不做加密，也不申请 Keychain 权限。
 * 若后续恢复 iOS 维护，需要改为 Keychain 保管密钥的实现，并设计存量数据迁移。
 */
actual fun platformCredentialCipher(): CredentialCipher = PassthroughCredentialCipher
