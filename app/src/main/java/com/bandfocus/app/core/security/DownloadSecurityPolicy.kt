package com.bandfocus.app.core.security

import java.net.URI

object DownloadSecurityPolicy {
    fun isSecureDownloadUrl(url: String): Boolean =
        runCatching {
            val uri = URI(url.trim())
            uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo.isNullOrBlank()
        }.getOrDefault(false)

    fun sanitizeFileName(fileName: String): String {
        val safeName = fileName
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace(Regex("""[^\w.\- ()]"""), "_")
            .take(120)
            .trim()

        return safeName
            .takeIf { it.isNotBlank() && it != "." && it != ".." }
            ?: "download.bin"
    }
}
