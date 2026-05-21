package com.bandfocus.app.data.download

import android.os.Environment
import com.bandfocus.app.core.util.IoDispatcher
import com.bandfocus.app.domain.model.DownloadMode
import com.bandfocus.app.domain.model.DownloadStatus
import com.bandfocus.app.domain.model.DownloadTask
import com.bandfocus.app.domain.repository.DownloadRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadEngine @Inject constructor(
    private val client: OkHttpClient,
    private val repository: DownloadRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val _activeDownloads = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, DownloadProgress>> = _activeDownloads.asStateFlow()

    suspend fun startDownload(
        url: String,
        fileName: String,
        fileSize: Long,
        supportsRange: Boolean,
        mode: DownloadMode
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val downloadId = UUID.randomUUID().toString()
            val threadCount = modeToThreadCount(mode, supportsRange, fileSize)
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val outputFile = File(downloadDir, fileName)

            val task = DownloadTask(
                id = downloadId,
                url = url,
                fileName = fileName,
                fileSize = fileSize,
                downloadedBytes = 0L,
                savedPath = outputFile.absolutePath,
                status = DownloadStatus.DOWNLOADING,
                mode = mode,
                threadCount = threadCount,
                averageSpeed = 0L,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                supportsRange = supportsRange
            )

            repository.upsert(task)

            _activeDownloads.value = _activeDownloads.value + (downloadId to DownloadProgress(
                downloadId = downloadId,
                downloadedBytes = 0L,
                totalBytes = fileSize,
                speed = 0L,
                eta = 0L
            ))

            if (supportsRange && threadCount > 1) {
                downloadMultiThread(downloadId, url, outputFile, fileSize, threadCount)
            } else {
                downloadSingleThread(downloadId, url, outputFile)
            }

            downloadId
        }
    }

    private suspend fun downloadSingleThread(downloadId: String, url: String, outputFile: File) = coroutineScope {
        val request = Request.Builder().url(url).build()
        val startTime = System.currentTimeMillis()
        var totalDownloaded = 0L

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Download failed: ${response.code}")

            val body = response.body ?: throw Exception("Empty response body")
            val totalBytes = body.contentLength()

            outputFile.parentFile?.mkdirs()
            outputFile.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead

                        val elapsed = System.currentTimeMillis() - startTime
                        val speed = if (elapsed > 0) (totalDownloaded * 1000L / elapsed) else 0L
                        val eta = if (speed > 0 && totalBytes > 0) ((totalBytes - totalDownloaded) / speed) else 0L

                        _activeDownloads.value = _activeDownloads.value + (downloadId to DownloadProgress(
                            downloadId = downloadId,
                            downloadedBytes = totalDownloaded,
                            totalBytes = totalBytes,
                            speed = speed,
                            eta = eta
                        ))

                        repository.upsert(
                            (repository.getById(downloadId) ?: return@use).copy(
                                downloadedBytes = totalDownloaded,
                                averageSpeed = speed,
                                updatedAt = System.currentTimeMillis()
                            )
                        )

                        delay(500)
                    }
                }
            }
        }

        repository.upsert(
            (repository.getById(downloadId) ?: return@coroutineScope).copy(
                status = DownloadStatus.COMPLETED,
                downloadedBytes = totalDownloaded,
                updatedAt = System.currentTimeMillis()
            )
        )

        _activeDownloads.value = _activeDownloads.value - downloadId
    }

    private suspend fun downloadMultiThread(
        downloadId: String,
        url: String,
        outputFile: File,
        fileSize: Long,
        threadCount: Int
    ) = coroutineScope {
        val chunkSize = fileSize / threadCount
        val tempDir = File(outputFile.parent, ".bandfocus_temp")
        tempDir.mkdirs()

        val startTime = System.currentTimeMillis()

        val jobs = (0 until threadCount).map { index ->
            async(ioDispatcher) {
                val start = index * chunkSize
                val end = if (index == threadCount - 1) fileSize - 1 else (start + chunkSize - 1)
                val partFile = File(tempDir, "${outputFile.name}.part$index")

                val request = Request.Builder()
                    .url(url)
                    .header("Range", "bytes=$start-$end")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.code != 206) throw Exception("Server does not support range for part $index")

                    val body = response.body ?: throw Exception("Empty body for part $index")
                    partFile.outputStream().use { output ->
                        body.byteStream().copyTo(output)
                    }
                }

                partFile
            }
        }

        val partFiles = jobs.awaitAll()

        outputFile.parentFile?.mkdirs()
        outputFile.outputStream().use { output ->
            partFiles.forEach { partFile ->
                partFile.inputStream().use { input ->
                    input.copyTo(output)
                }
                partFile.delete()
            }
        }

        tempDir.deleteRecursively()

        val elapsed = System.currentTimeMillis() - startTime
        val avgSpeed = if (elapsed > 0) (fileSize * 1000L / elapsed) else 0L

        repository.upsert(
            (repository.getById(downloadId) ?: return@coroutineScope).copy(
                status = DownloadStatus.COMPLETED,
                downloadedBytes = fileSize,
                averageSpeed = avgSpeed,
                updatedAt = System.currentTimeMillis()
            )
        )

        _activeDownloads.value = _activeDownloads.value - downloadId
    }

    private fun modeToThreadCount(mode: DownloadMode, supportsRange: Boolean, fileSize: Long): Int {
        if (!supportsRange) return 1
        return when (mode) {
            DownloadMode.ECO -> 2
            DownloadMode.BALANCED -> 4
            DownloadMode.TURBO -> 8
            DownloadMode.NIGHT -> 2
        }
    }
}

data class DownloadProgress(
    val downloadId: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speed: Long,
    val eta: Long
)
