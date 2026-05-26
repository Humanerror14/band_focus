package com.bandfocus.app.data.network

import com.bandfocus.app.domain.model.DownloadMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HeaderAnalyzerTest {
    private lateinit var server: MockWebServer
    private lateinit var analyzer: HeaderAnalyzer

    @Before
    fun setUp() {
        val localhostCertificate = HeldCertificate.Builder()
            .addSubjectAlternativeName("localhost")
            .build()
        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(localhostCertificate)
            .build()
        val clientCertificates = HandshakeCertificates.Builder()
            .addTrustedCertificate(localhostCertificate.certificate)
            .build()

        server = MockWebServer().apply {
            useHttps(serverCertificates.sslSocketFactory(), false)
            start()
        }
        val client = OkHttpClient.Builder()
            .sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager)
            .build()
        analyzer = HeaderAnalyzer(client, Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun analyze_readsSecureHeadMetadata() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Accept-Ranges", "bytes")
                .setHeader("Content-Disposition", "attachment; filename=\"../unsafe?.zip\"")
                .setHeader("Content-Length", "10485760")
                .setHeader("Content-Type", "application/zip")
        )

        val metadata = analyzer.analyze(server.url("/downloads/file.zip").toString()).getOrThrow()

        assertEquals("unsafe_.zip", metadata.fileName)
        assertEquals(10L * 1024L * 1024L, metadata.fileSize)
        assertEquals("application/zip", metadata.mimeType)
        assertTrue(metadata.supportsRange)
        assertEquals(DownloadMode.ECO, metadata.recommendedMode)
        assertEquals("HEAD", server.takeRequest().method)
    }

    @Test
    fun analyze_probesRangeWhenHeadDoesNotExposeRangeSupport() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Length", "100")
                .setHeader("Content-Type", "application/octet-stream")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(206)
                .setHeader("Content-Range", "bytes 0-0/104857600")
                .setHeader("Content-Type", "application/octet-stream")
                .setBody("a")
        )

        val metadata = analyzer.analyze(server.url("/archive.bin").toString()).getOrThrow()

        assertEquals(100L * 1024L * 1024L, metadata.fileSize)
        assertTrue(metadata.supportsRange)
        assertEquals(DownloadMode.BALANCED, metadata.recommendedMode)
        assertEquals("HEAD", server.takeRequest().method)
        val probeRequest = server.takeRequest()
        assertEquals("GET", probeRequest.method)
        assertEquals("bytes=0-0", probeRequest.getHeader("Range"))
    }

    @Test
    fun analyze_rejectsCleartextUrlsBeforeNetworkCall() = runBlocking {
        val result = analyzer.analyze("http://example.com/file.zip")

        assertTrue(result.isFailure)
        assertEquals(0, server.requestCount)
    }
}
