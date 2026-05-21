package com.bandfocus.app.data.network

import android.net.Uri
import com.bandfocus.app.core.util.IoDispatcher
import com.bandfocus.app.domain.model.DownloadMetadata
import com.bandfocus.app.domain.model.DownloadMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class HeaderAnalyzer @Inject constructor(
    private val client: OkHttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun analyze(url: String): Result<DownloadMetadata> = withContext(ioDispatcher) {
        runCatching {
            require(isValidHttpUrl(url)) { "URL must start with http:// or https://" }

            val headRequest = Request.Builder().url(url).head().build()
            val response = client.newCall(headRequest).execute().use { headResponse ->
                if (headResponse.isSuccessful) headResponse else fallbackGet(url)
            }

            response.use { res ->
                val fileSize = res.header("Content-Length")?.toLongOrNull()
                val contentType = res.header("Content-Type")
                val fileName = filenameFromHeaders(url, res.header("Content-Disposition"))
                val supportsRange = res.header("Accept-Ranges")?.equals("bytes", ignoreCase = true) == true
                val recommendedMode = recommendMode(fileSize, supportsRange)
                DownloadMetadata(
                    url = url,
                    fileName = fileName,
                    fileSize = fileSize,
                    mimeType = contentType,
                    supportsRange = supportsRange,
                    recommendedMode = recommendedMode,
                    diagnosis = diagnosis(fileSize, supportsRange, recommendedMode)
                )
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

    private fun isValidHttpUrl(url: String): Boolean {
        val uri = Uri.parse(url)
        return uri.scheme == "http" || uri.scheme == "https"
    }

    private fun filenameFromHeaders(url: String, contentDisposition: String?): String {
        val headerName = contentDisposition
            ?.split(";")
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("filename=", ignoreCase = true) }
            ?.substringAfter("=")
            ?.trim('"')
        return headerName ?: Uri.parse(url).lastPathSegment?.takeIf { it.isNotBlank() } ?: "download.bin"
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
