package com.bandfocus.app.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

class FocusVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        vpnInterface = Builder()
            .setSession("BandFocus Focus Mode")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .establish()
        return START_STICKY
    }

    override fun onDestroy() {
        vpnInterface?.close()
        vpnInterface = null
        super.onDestroy()
    }
}
