package com.sample.vpn

import android.app.Application
import com.alps.vpnlib.VpnlibCore

class VpnApp : Application() {
    override fun onCreate() {
        super.onCreate()

        VpnlibCore.initialize(this)
    }
}