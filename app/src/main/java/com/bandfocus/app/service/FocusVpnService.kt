package com.bandfocus.app.service

import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class FocusVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var packetDropThread: Thread? = null
    private val isRunning = AtomicBoolean(false)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val blockedPackages = intent
            ?.getStringArrayListExtra(EXTRA_BLOCKED_PACKAGES)
            .orEmpty()
            .distinct()

        if (blockedPackages.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        stopTunnel()
        val builder = Builder()
            .setSession("BandFocus Focus Mode")
            .setMtu(MTU)
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .setBlocking(false)

        runCatching {
            builder
                .addAddress("fd00:1:2:3::2", 128)
                .addRoute("::", 0)
        }

        var validBlockedApps = 0
        blockedPackages.forEach { packageName ->
            try {
                builder.addAllowedApplication(packageName)
                validBlockedApps++
            } catch (_: PackageManager.NameNotFoundException) {
                // Ignore stale rules for apps that are no longer installed.
            }
        }

        if (validBlockedApps == 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        vpnInterface = builder.establish()
        if (vpnInterface == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startPacketDropLoop()
        return START_STICKY
    }

    override fun onRevoke() {
        stopTunnel()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopTunnel()
        super.onDestroy()
    }

    private fun startPacketDropLoop() {
        val descriptor = vpnInterface ?: return
        isRunning.set(true)
        packetDropThread = Thread {
            val buffer = ByteArray(MTU)
            FileInputStream(descriptor.fileDescriptor).use { input ->
                while (isRunning.get()) {
                    try {
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read == 0) Thread.sleep(IDLE_SLEEP_MS)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    } catch (_: IOException) {
                        if (isRunning.get()) Thread.sleep(IDLE_SLEEP_MS)
                    }
                }
            }
        }.apply {
            name = "BandFocusPacketDropper"
            isDaemon = true
            start()
        }
    }

    private fun stopTunnel() {
        isRunning.set(false)
        packetDropThread?.interrupt()
        packetDropThread = null
        runCatching { vpnInterface?.close() }
        vpnInterface = null
    }

    companion object {
        const val ACTION_STOP = "com.bandfocus.app.action.STOP_FOCUS_VPN"
        const val EXTRA_BLOCKED_PACKAGES = "blocked_packages"
        private const val MTU = 1500
        private const val IDLE_SLEEP_MS = 20L
    }
}
