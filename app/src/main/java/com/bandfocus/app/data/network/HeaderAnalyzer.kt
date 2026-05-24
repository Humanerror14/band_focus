package com.bandfocus.app.data.network

import com.bandfocus.app.core.security.DownloadSecurityPolicy
import com.bandfocus.app.core.util.IoDispatcher
import com.bandfocus.app.domain.model.DownloadMetadata
import com.bandfocus.app.domain.model.DownloadMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject

class HeaderAnalyzer @Inject constructor(
    private val client: OkHttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun analyze(url: String): Result<DownloadMetadata> = withContext(ioDispatcher) {
        runCatching {
            val normalizedUrl = url.trim()
            require(DownloadSecurityPolicy.isSecureDownloadUrl(normalizedUrl)) {
                "URL must start with https:// for secure downloads"
            }

            val headRequest = Request.Builder().url(normalizedUrl).head().build()
            client.newCall(headRequest).execute().use { headResponse ->
                val rangeProbe = if (headResponse.isSuccessful && !supportsRange(headResponse)) {
                    fallbackGet(normalizedUrl)
                } else {
                    null
                }
                val response = when {
                    rangeProbe?.isSuccessful == true -> rangeProbe
                    headResponse.isSuccessful -> headResponse
                    else -> fallbackGet(url)
                }
                val shouldCloseResponse = response !== headResponse && response !== rangeProbe
                try {
                    val fileSize = contentLength(response) ?: contentLength(headResponse)
                    val contentType = response.header("Content-Type") ?: headResponse.header("Content-Type")
                    val fileName = filenameFromHeaders(
                        url = normalizedUrl,
                        contentDisposition = response.header("Content-Disposition")
                            ?: headResponse.header("Content-Disposition")
                    )
                    val supportsRange = supportsRange(response) || supportsRange(headResponse)
                    val recommendedMode = recommendMode(fileSize, supportsRange)
                    DownloadMetadata(
                        url = normalizedUrl,
                        fileName = fileName,
                        fileSize = fileSize,
                        mimeType = contentType,
                        supportsRange = supportsRange,
                        recommendedMode = recommendedMode,
                        diagnosis = diagnosis(fileSize, supportsRange, recommendedMode)
                    )
                } finally {
                    if (shouldCloseResponse) response.close()
                    rangeProbe?.close()
                }
            }
        }
    }

    private fun fallbackGet(url: String) = client.newCall(
        Request.Builder()
            .url(url)
            .header("Range", "bytes=0-0")
            .get()
            .build()
    ).execute()

    private fun contentLength(response: Response): Long? {
        response.header("Content-Range")
            ?.substringAfter("/")
            ?.toLongOrNull()
            ?.let { return it }
        return response.header("Content-Length")?.toLongOrNull()
    }

    private fun supportsRange(response: Response): Boolean =
        response.code == 206 ||
            response.header("Accept-Ranges")?.equals("bytes", ignoreCase = true) == true

    private fun filenameFromHeaders(url: String, contentDisposition: String?): String {
        val headerName = contentDisposition
            ?.split(";")
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("filename=", ignoreCase = true) }
            ?.substringAfter("=")
            ?.trim('"')
        val pathName = url.toHttpUrlOrNull()
            ?.pathSegments
            ?.lastOrNull()
            ?.takeIf { it.isNotBlank() }
        return DownloadSecurityPolicy.sanitizeFileName(headerName ?: pathName ?: "download.bin")
    }

    private fun recommendMode(fileSize: Long?, supportsRange: Boolean): DownloadMode {
        if (!supportsRange) return DownloadMode.BALANCED
        return when {
            fileSize == null -> DownloadMode.BALANCED
            fileSize < 50L * 1024L * 1024L -> DownloadMode.ECO
            fileSize > 500L * 1024L * 1024L -> DownloadMode.TURBO
            else -> DownloadMode.BALANCED
        }
    }

    private fun diagnosis(fileSize: Long?, supportsRange: Boolean, mode: DownloadMode): String {
        if (!supportsRange) return "Server does not support multi-thread download. Switching to single-thread mode."
        if (fileSize != null && fileSize < 50L * 1024L * 1024L) return "File is small. Eco Mode is enough and avoids unnecessary connections."
        return "Server supports multi-thread download. ${mode.name.lowercase().replaceFirstChar { it.uppercase() }} Mode recommended."
    }
}
