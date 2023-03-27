package com.sample.vpn

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.alps.vpnlib.VpnlibCore
import com.alps.vpnlib.repo.MainRepo
import kotlinx.coroutines.*

enum class VpnPermissionState {
    NotGrant, Granted, Deny
}
class MainViewModel : ViewModel() {

    var vpnPermissionState: VpnPermissionState = VpnPermissionState.NotGrant

    fun startVpn(context: Context, country:String){
        GlobalScope.launch(Dispatchers.IO) {

            if (vpnPermissionState != VpnPermissionState.Granted) {
                Toast.makeText(context, "no vpn permission", Toast.LENGTH_SHORT)
                return@launch
            }

            VpnlibCore.connectAutoVpn(context, country)
        }
    }

    fun stopVpn(context: Context){
        VpnlibCore.disconnectVpn(context)
    }


    fun getVpnServerTitle() = VpnlibCore.getVpnServerTitle()
    fun getVpnServerCountry() = VpnlibCore.getVpnServerCountry()
    fun isVpnConnecting() = VpnlibCore.isVpnConnecting()
    fun isVpnNotConnected() = VpnlibCore.isVpnNotConnected()
    fun isVpnConnected() = VpnlibCore.isVpnConnected()

    fun getDisableVpnPackageList():Set<String>{
        return VpnlibCore.getDisableVpnPackageList()
    }
    fun setDisableVpnPackageList(list:Set<String>){
        VpnlibCore.setDisableVpnPackageList(list)
    }

}