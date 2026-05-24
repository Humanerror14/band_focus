package com.bandfocus.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadSecurityPolicyTest {
    @Test
    fun secureDownloadUrl_acceptsHttpsWithHost() {
        assertTrue(DownloadSecurityPolicy.isSecureDownloadUrl("https://example.com/file.zip"))
    }

    @Test
    fun secureDownloadUrl_rejectsCleartextAndUserInfo() {
        assertFalse(DownloadSecurityPolicy.isSecureDownloadUrl("http://example.com/file.zip"))
        assertFalse(DownloadSecurityPolicy.isSecureDownloadUrl("https://user:pass@example.com/file.zip"))
        assertFalse(DownloadSecurityPolicy.isSecureDownloadUrl("not a url"))
    }

    @Test
    fun sanitizeFileName_removesPathsAndUnsafeCharacters() {
        assertEquals("report_.pdf", DownloadSecurityPolicy.sanitizeFileName("../unsafe/report?.pdf"))
        assertEquals("download.bin", DownloadSecurityPolicy.sanitizeFileName(".."))
        assertEquals("download.bin", DownloadSecurityPolicy.sanitizeFileName(""))
    }
}
