package com.bandfocus.app.data.download

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.ContextCompat
import com.bandfocus.app.core.security.DownloadSecurityPolicy
import com.bandfocus.app.core.util.IoDispatcher
import com.bandfocus.app.domain.model.DownloadMode
import com.bandfocus.app.domain.model.DownloadStatus
import com.bandfocus.app.domain.model.DownloadTask
import com.bandfocus.app.domain.repository.DownloadRepository
import com.bandfocus.app.service.DownloadForegroundService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
    private val repository: DownloadRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val _activeDownloads = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, DownloadProgress>> = _activeDownloads.asStateFlow()
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val engineScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    suspend fun startDownload(
        url: String,
        fileName: String,
        fileSize: Long,
        supportsRange: Boolean,
        mode: DownloadMode
    ): Result<String> = withContext(ioDispatcher) {
        val downloadId = UUID.randomUUID().toString()
        runCatching {
            val threadCount = modeToThreadCount(mode, supportsRange, fileSize)
            val outputFile = createOutputFile(fileName)

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
            startDownloadService()

            _activeDownloads.value = _activeDownloads.value + (downloadId to DownloadProgress(
                downloadId = downloadId,
                downloadedBytes = 0L,
                totalBytes = fileSize,
                speed = 0L,
                eta = 0L
            ))

            val job = engineScope.launch {
                runCatching {
                    if (supportsRange && threadCount > 1) {
                        downloadMultiThread(downloadId, url, outputFile, fileSize, threadCount)
                    } else {
                        downloadSingleThread(downloadId, url, outputFile)
                    }
                }.onFailure { throwable ->
                    markDownloadFailed(downloadId, throwable)
                }.also {
                    activeJobs.remove(downloadId)
                    stopDownloadServiceIfIdle()
                }
            }
            activeJobs[downloadId] = job

            downloadId
        }
    }

    suspend fun pauseDownload(downloadId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            repository.getById(downloadId)?.let { task ->
                repository.upsert(
                    task.copy(
                        status = DownloadStatus.PAUSED,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            activeJobs.remove(downloadId)?.cancel(CancellationException("Paused by user"))
            _activeDownloads.value = _activeDownloads.value - downloadId
            stopDownloadServiceIfIdle()
        }
    }

    suspend fun cancelDownload(downloadId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            repository.getById(downloadId)?.let { task ->
                repository.upsert(
                    task.copy(
                        status = DownloadStatus.CANCELED,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            activeJobs.remove(downloadId)?.cancel(CancellationException("Canceled by user"))
            _activeDownloads.value = _activeDownloads.value - downloadId
            stopDownloadServiceIfIdle()
        }
    }

    private suspend fun downloadSingleThread(downloadId: String, url: String, outputFile: File) = coroutineScope {
        val request = Request.Builder().url(url).build()
        val startTime = System.currentTimeMillis()
        var totalDownloaded = 0L
        var lastProgressUpdate = 0L
        var lastPersistUpdate = 0L

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Download failed: ${response.code}")

            val body = response.body ?: throw Exception("Empty response body")
            val totalBytes = body.contentLength()

            outputFile.parentFile?.mkdirs()
            outputFile.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastProgressUpdate >= PROGRESS_UPDATE_MS) {
                            lastProgressUpdate = now
                            publishProgress(downloadId, totalDownloaded, totalBytes, startTime)
                        }
                        if (now - lastPersistUpdate >= PERSIST_UPDATE_MS) {
                            lastPersistUpdate = now
                            persistProgress(downloadId, totalDownloaded, startTime)
                        }
                    }
                }
            }
        }

        repository.upsert(
            (repository.getById(downloadId) ?: return@coroutineScope).copy(
                status = DownloadStatus.COMPLETED,
                downloadedBytes = totalDownloaded,
                averageSpeed = calculateSpeed(totalDownloaded, startTime),
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
        val totalDownloaded = AtomicLong(0L)
        val lastProgressUpdate = AtomicLong(0L)
        val lastPersistUpdate = AtomicLong(0L)

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
                        body.byteStream().use { input ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                val downloaded = totalDownloaded.addAndGet(bytesRead.toLong())
                                val now = System.currentTimeMillis()
                                if (now - lastProgressUpdate.get() >= PROGRESS_UPDATE_MS &&
                                    lastProgressUpdate.compareAndSet(lastProgressUpdate.get(), now)
                                ) {
                                    publishProgress(downloadId, downloaded, fileSize, startTime)
                                }
                                if (now - lastPersistUpdate.get() >= PERSIST_UPDATE_MS &&
                                    lastPersistUpdate.compareAndSet(lastPersistUpdate.get(), now)
                                ) {
                                    persistProgress(downloadId, downloaded, startTime)
                                }
                            }
                        }
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
        if (!supportsRange || fileSize <= 0L) return 1
        return when (mode) {
            DownloadMode.ECO -> 2
            DownloadMode.BALANCED -> 4
            DownloadMode.TURBO -> 8
            DownloadMode.NIGHT -> 2
        }
    }

    private fun createOutputFile(fileName: String): File {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, Environment.DIRECTORY_DOWNLOADS)
        downloadDir.mkdirs()

        val safeName = DownloadSecurityPolicy.sanitizeFileName(fileName)

        val baseName = safeName.substringBeforeLast('.', safeName)
        val extension = safeName.substringAfterLast('.', missingDelimiterValue = "")
        var candidate = File(downloadDir, safeName)
        var suffix = 1
        while (candidate.exists()) {
            val nextName = if (extension.isBlank()) {
                "$baseName ($suffix)"
            } else {
                "$baseName ($suffix).$extension"
            }
            candidate = File(downloadDir, nextName)
            suffix++
        }
        return candidate
    }

    private suspend fun markDownloadFailed(downloadId: String, throwable: Throwable) {
        val task = repository.getById(downloadId) ?: return
        if (throwable is CancellationException) {
            if (task.status == DownloadStatus.DOWNLOADING) {
                repository.upsert(
                    task.copy(
                        status = DownloadStatus.PAUSED,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        } else if (task.status == DownloadStatus.DOWNLOADING) {
            repository.upsert(
                task.copy(
                    status = DownloadStatus.FAILED,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        _activeDownloads.value = _activeDownloads.value - downloadId
    }

    private fun calculateSpeed(downloadedBytes: Long, startTime: Long): Long {
        val elapsed = System.currentTimeMillis() - startTime
        return if (elapsed > 0) (downloadedBytes * 1000L / elapsed) else 0L
    }

    private suspend fun publishProgress(
        downloadId: String,
        downloadedBytes: Long,
        totalBytes: Long,
        startTime: Long
    ) {
        val speed = calculateSpeed(downloadedBytes, startTime)
        val eta = if (speed > 0 && totalBytes > 0) ((totalBytes - downloadedBytes) / speed) else 0L

        _activeDownloads.value = _activeDownloads.value + (downloadId to DownloadProgress(
            downloadId = downloadId,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            speed = speed,
            eta = eta
        ))
    }

    private suspend fun persistProgress(
        downloadId: String,
        downloadedBytes: Long,
        startTime: Long
    ) {
        val speed = calculateSpeed(downloadedBytes, startTime)
        repository.upsert(
            (repository.getById(downloadId) ?: return).copy(
                downloadedBytes = downloadedBytes,
                averageSpeed = speed,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private fun startDownloadService() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, DownloadForegroundService::class.java)
        )
    }

    private fun stopDownloadServiceIfIdle() {
        if (activeJobs.isNotEmpty()) return
        context.stopService(Intent(context, DownloadForegroundService::class.java))
    }

    private companion object {
        const val DEFAULT_BUFFER_SIZE = 64 * 1024
        const val PROGRESS_UPDATE_MS = 250L
        const val PERSIST_UPDATE_MS = 1_000L
    }
}

data class DownloadProgress(
    val downloadId: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speed: Long,
    val eta: Long
)
