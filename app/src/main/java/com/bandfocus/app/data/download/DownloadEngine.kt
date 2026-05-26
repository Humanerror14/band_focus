package com.bandfocus.app.data.download

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.StatFs
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
import kotlinx.coroutines.CoroutineStart
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
import java.io.FileOutputStream
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
            prepareFreshOutput(outputFile)

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

            ensureEnoughDiskSpaceForDownload(task, outputFile, existingBytes = 0L)
            repository.upsert(task)
            launchDownloadJob(task, outputFile)

            downloadId
        }
    }

    suspend fun resumeDownload(downloadId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            require(!activeJobs.containsKey(downloadId)) { "Download is already running" }

            val existingTask = repository.getById(downloadId)
                ?: throw IllegalArgumentException("Download not found")
            require(existingTask.status != DownloadStatus.COMPLETED) { "Download already completed" }

            val threadCount = modeToThreadCount(
                mode = existingTask.mode,
                supportsRange = existingTask.supportsRange,
                fileSize = existingTask.fileSize
            )
            val outputFile = existingTask.savedPath?.let(::File) ?: createOutputFile(existingTask.fileName)
            val canUseMultiThread = existingTask.supportsRange && threadCount > 1 && existingTask.fileSize > 0L
            val existingBytes = when {
                canUseMultiThread -> {
                    outputFile.delete()
                    existingPartBytes(outputFile, existingTask.fileSize, threadCount)
                }

                existingTask.supportsRange -> existingFileBytes(outputFile, existingTask.fileSize)

                else -> {
                    prepareFreshOutput(outputFile)
                    0L
                }
            }

            val task = existingTask.copy(
                savedPath = outputFile.absolutePath,
                status = DownloadStatus.DOWNLOADING,
                threadCount = threadCount,
                downloadedBytes = existingBytes,
                updatedAt = System.currentTimeMillis()
            )

            ensureEnoughDiskSpaceForDownload(task, outputFile, existingBytes)
            repository.upsert(task)
            launchDownloadJob(task, outputFile)
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

    private fun launchDownloadJob(task: DownloadTask, outputFile: File) {
        startDownloadService()

        _activeDownloads.value = _activeDownloads.value + (task.id to DownloadProgress(
            downloadId = task.id,
            downloadedBytes = task.downloadedBytes,
            totalBytes = task.fileSize,
            speed = 0L,
            eta = 0L
        ))

        val job = engineScope.launch(start = CoroutineStart.LAZY) {
            runCatching {
                if (task.supportsRange && task.threadCount > 1 && task.fileSize > 0L) {
                    downloadMultiThread(task.id, task.url, outputFile, task.fileSize, task.threadCount)
                } else {
                    downloadSingleThread(task.id, task.url, outputFile, task.supportsRange, task.fileSize)
                }
            }.onFailure { throwable ->
                markDownloadFailed(task.id, throwable)
            }.also {
                activeJobs.remove(task.id)
                stopDownloadServiceIfIdle()
            }
        }
        activeJobs[task.id] = job
        job.start()
    }

    private suspend fun downloadSingleThread(
        downloadId: String,
        url: String,
        outputFile: File,
        supportsRange: Boolean,
        expectedFileSize: Long
    ) = coroutineScope {
        val startTime = System.currentTimeMillis()
        var lastProgressUpdate = 0L
        var lastPersistUpdate = 0L
        var sessionDownloaded = 0L
        val initialOffset = if (supportsRange) existingFileBytes(outputFile, expectedFileSize) else 0L

        if (!supportsRange && outputFile.exists()) outputFile.delete()
        if (expectedFileSize > 0L && initialOffset >= expectedFileSize) {
            markDownloadCompleted(downloadId, initialOffset, averageSpeed = 0L, resolvedFileSize = expectedFileSize)
            return@coroutineScope
        }

        suspend fun executeFrom(offset: Long): Pair<Long, Long> {
            val requestBuilder = Request.Builder().url(url)
            if (offset > 0L) requestBuilder.header("Range", "bytes=$offset-")

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (offset > 0L && response.code != 206) throw ResumeNotSupportedException()
                if (!response.isSuccessful) throw Exception("Download failed: ${response.code}")

                val body = response.body ?: throw Exception("Empty response body")
                val responseLength = body.contentLength()
                val totalBytes = when {
                    expectedFileSize > 0L -> expectedFileSize
                    responseLength > 0L -> offset + responseLength
                    else -> 0L
                }
                var totalDownloaded = offset

                publishProgress(downloadId, totalDownloaded, totalBytes, sessionDownloaded, startTime)
                outputFile.parentFile?.mkdirs()
                FileOutputStream(outputFile, offset > 0L).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalDownloaded += bytesRead
                            sessionDownloaded += bytesRead

                            val now = System.currentTimeMillis()
                            if (now - lastProgressUpdate >= PROGRESS_UPDATE_MS) {
                                lastProgressUpdate = now
                                publishProgress(downloadId, totalDownloaded, totalBytes, sessionDownloaded, startTime)
                            }
                            if (now - lastPersistUpdate >= PERSIST_UPDATE_MS) {
                                lastPersistUpdate = now
                                persistProgress(downloadId, totalDownloaded, sessionDownloaded, startTime)
                            }
                        }
                    }
                }

                if (expectedFileSize > 0L && totalDownloaded < expectedFileSize) {
                    throw Exception("Download incomplete: expected $expectedFileSize bytes, received $totalDownloaded bytes")
                }
                return totalDownloaded to totalBytes
            }
        }

        val (totalDownloaded, resolvedTotalBytes) = try {
            executeFrom(initialOffset)
        } catch (_: ResumeNotSupportedException) {
            outputFile.delete()
            publishProgress(downloadId, 0L, expectedFileSize, 0L, startTime)
            executeFrom(0L)
        }

        markDownloadCompleted(
            downloadId = downloadId,
            downloadedBytes = totalDownloaded,
            averageSpeed = calculateSpeed(sessionDownloaded, startTime),
            resolvedFileSize = resolvedTotalBytes.takeIf { it > 0L } ?: totalDownloaded
        )
    }

    private suspend fun downloadMultiThread(
        downloadId: String,
        url: String,
        outputFile: File,
        fileSize: Long,
        threadCount: Int
    ) = coroutineScope {
        val chunkSize = fileSize / threadCount
        val tempDir = tempDirFor(outputFile)
        tempDir.mkdirs()
        outputFile.delete()

        val startTime = System.currentTimeMillis()
        val totalDownloaded = AtomicLong(existingPartBytes(outputFile, fileSize, threadCount))
        val sessionDownloaded = AtomicLong(0L)
        val lastProgressUpdate = AtomicLong(0L)
        val lastPersistUpdate = AtomicLong(0L)

        publishProgress(downloadId, totalDownloaded.get(), fileSize, sessionDownloaded.get(), startTime)

        val jobs = (0 until threadCount).map { index ->
            async(ioDispatcher) {
                val start = index.toLong() * chunkSize
                val end = if (index == threadCount - 1) fileSize - 1 else (start + chunkSize - 1)
                val partFile = tempPartFile(outputFile, index)
                val expectedPartSize = end - start + 1
                val existingPartLength = existingPartBytes(partFile, expectedPartSize)
                if (existingPartLength >= expectedPartSize) return@async partFile

                val request = Request.Builder()
                    .url(url)
                    .header("Range", "bytes=${start + existingPartLength}-$end")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.code != 206) throw Exception("Server does not support range for part $index")

                    val body = response.body ?: throw Exception("Empty body for part $index")
                    FileOutputStream(partFile, existingPartLength > 0L).use { output ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                val downloaded = totalDownloaded.addAndGet(bytesRead.toLong())
                                val sessionBytes = sessionDownloaded.addAndGet(bytesRead.toLong())
                                val now = System.currentTimeMillis()
                                if (now - lastProgressUpdate.get() >= PROGRESS_UPDATE_MS &&
                                    lastProgressUpdate.compareAndSet(lastProgressUpdate.get(), now)
                                ) {
                                    publishProgress(downloadId, downloaded, fileSize, sessionBytes, startTime)
                                }
                                if (now - lastPersistUpdate.get() >= PERSIST_UPDATE_MS &&
                                    lastPersistUpdate.compareAndSet(lastPersistUpdate.get(), now)
                                ) {
                                    persistProgress(downloadId, downloaded, sessionBytes, startTime)
                                }
                            }
                        }
                    }
                }

                if (partFile.length() != expectedPartSize) {
                    throw Exception("Part $index incomplete: expected $expectedPartSize bytes, received ${partFile.length()} bytes")
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

        deleteTempDirIfEmpty(tempDir)

        markDownloadCompleted(
            downloadId = downloadId,
            downloadedBytes = fileSize,
            averageSpeed = calculateSpeed(sessionDownloaded.get(), startTime),
            resolvedFileSize = fileSize
        )
    }

    private fun modeToThreadCount(mode: DownloadMode, supportsRange: Boolean, fileSize: Long): Int {
        if (!supportsRange || fileSize <= 0L) return 1
        val requestedThreadCount = when (mode) {
            DownloadMode.ECO -> 2
            DownloadMode.BALANCED -> 4
            DownloadMode.TURBO -> 8
            DownloadMode.NIGHT -> 2
        }
        return minOf(requestedThreadCount, fileSize.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()).coerceAtLeast(1)
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

    private fun prepareFreshOutput(outputFile: File) {
        outputFile.delete()
        deleteTempParts(outputFile)
    }

    private fun ensureEnoughDiskSpaceForDownload(task: DownloadTask, outputFile: File, existingBytes: Long) {
        if (task.fileSize <= 0L) return

        val remainingPayloadBytes = (task.fileSize - existingBytes).coerceAtLeast(0L)
        val mergeBytes = if (task.supportsRange && task.threadCount > 1) task.fileSize else 0L
        val requiredBytes = safeAdd(safeAdd(remainingPayloadBytes, mergeBytes), STORAGE_HEADROOM_BYTES)
        val targetDir = outputFile.parentFile ?: context.filesDir
        targetDir.mkdirs()

        val availableBytes = StatFs(targetDir.absolutePath).availableBytes
        if (availableBytes < requiredBytes) {
            throw IllegalStateException(
                "Not enough storage. Need ${formatBytes(requiredBytes)}, available ${formatBytes(availableBytes)}."
            )
        }
    }

    private fun existingFileBytes(outputFile: File, expectedFileSize: Long): Long {
        if (!outputFile.exists()) return 0L
        val length = outputFile.length()
        if (expectedFileSize > 0L && length > expectedFileSize) {
            outputFile.delete()
            return 0L
        }
        return length
    }

    private fun existingPartBytes(outputFile: File, fileSize: Long, threadCount: Int): Long =
        (0 until threadCount).sumOf { index ->
            val chunkSize = fileSize / threadCount
            val start = index.toLong() * chunkSize
            val end = if (index == threadCount - 1) fileSize - 1 else (start + chunkSize - 1)
            existingPartBytes(tempPartFile(outputFile, index), end - start + 1)
        }

    private fun existingPartBytes(partFile: File, expectedPartSize: Long): Long {
        if (!partFile.exists()) return 0L
        val length = partFile.length()
        if (length > expectedPartSize) {
            partFile.delete()
            return 0L
        }
        return length
    }

    private fun tempDirFor(outputFile: File): File = File(outputFile.parentFile, TEMP_DIR_NAME)

    private fun tempPartFile(outputFile: File, index: Int): File =
        File(tempDirFor(outputFile), "${outputFile.name}.part$index")

    private fun deleteTempParts(outputFile: File) {
        val tempDir = tempDirFor(outputFile)
        if (!tempDir.exists()) return
        tempDir.listFiles { file -> file.name.startsWith("${outputFile.name}.part") }
            ?.forEach { it.delete() }
        deleteTempDirIfEmpty(tempDir)
    }

    private fun deleteTempDirIfEmpty(tempDir: File) {
        if (tempDir.exists() && tempDir.listFiles().isNullOrEmpty()) tempDir.delete()
    }

    private suspend fun markDownloadCompleted(
        downloadId: String,
        downloadedBytes: Long,
        averageSpeed: Long,
        resolvedFileSize: Long
    ) {
        val task = repository.getById(downloadId) ?: return
        repository.upsert(
            task.copy(
                fileSize = resolvedFileSize.takeIf { it > 0L } ?: task.fileSize,
                status = DownloadStatus.COMPLETED,
                downloadedBytes = downloadedBytes,
                averageSpeed = averageSpeed,
                updatedAt = System.currentTimeMillis()
            )
        )

        _activeDownloads.value = _activeDownloads.value - downloadId
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
        sessionDownloadedBytes: Long,
        startTime: Long
    ) {
        val speed = calculateSpeed(sessionDownloadedBytes, startTime)
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
        sessionDownloadedBytes: Long,
        startTime: Long
    ) {
        val speed = calculateSpeed(sessionDownloadedBytes, startTime)
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

    private fun safeAdd(left: Long, right: Long): Long {
        if (Long.MAX_VALUE - left < right) return Long.MAX_VALUE
        return left + right
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = listOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024.0 && unitIndex < units.lastIndex) {
            value /= 1024.0
            unitIndex++
        }
        return if (unitIndex == 0) {
            "${value.toLong()} ${units[unitIndex]}"
        } else {
            "%.1f %s".format(value, units[unitIndex])
        }
    }

    private companion object {
        const val DEFAULT_BUFFER_SIZE = 64 * 1024
        const val PROGRESS_UPDATE_MS = 250L
        const val PERSIST_UPDATE_MS = 1_000L
        const val STORAGE_HEADROOM_BYTES = 10L * 1024L * 1024L
        const val TEMP_DIR_NAME = ".bandfocus_temp"
    }
}

private class ResumeNotSupportedException : Exception()

data class DownloadProgress(
    val downloadId: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speed: Long,
    val eta: Long
)
