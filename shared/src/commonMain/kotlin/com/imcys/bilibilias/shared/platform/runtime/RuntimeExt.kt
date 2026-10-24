package com.imcys.bilibilias.shared.platform.runtime

/**
 * 跳转网页
 */
expect fun openLink(url: String): Boolean

/**
 * 字符串模板
 */
expect fun format(format: String, vararg args: Any?): String

/**
 * 获取App版本
 */
expect fun getAppVersion(): Pair<Long, String>

/**
 * URL编码
 */
expect fun urlEncode(url: String): String

/**
 * URL解码
 */
expect fun urlDecode(url: String): String
