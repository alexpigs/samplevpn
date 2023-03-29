package com.sample.vpn

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alps.vpnlib.VpnlibCore
import com.alps.vpnlib.bean.VpnServer
import com.alps.vpnlib.bean.VpnState
import java.security.MessageDigest
import java.util.*


val ByteArray.asHexLower
    inline get() = this.joinToString(separator = "") {
        String.format(
            Locale.ENGLISH,
            "%02x",
            (it.toInt() and 0xFF)
        )
    }

fun getSignatureMd5(context: Context): String? {
        try {
            /** 通过包管理器获得指定包名包含签名的包信息  */
            val packageInfo = context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)

            /******* 通过返回的包信息获得签名数组  */
            val signatures = packageInfo.signatures
            /******* 循环遍历签名数组拼接应用签名  */
            return MessageDigest.getInstance("MD5").digest(signatures[0].toByteArray()).asHexLower
            /************** 得到应用签名  */
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return null
    }


data class VpnCountryInfo(
    val countryName:String,
    val serverCount:Int
)

interface OnListFragmentInteractionCountryListener {
    fun onListFragmentInteractionCountry(countryName: String)
}

class VpnServerCountryRecyclerViewAdapter(
    private val mValues: ArrayList<VpnCountryInfo>,
    private val mListener: OnListFragmentInteractionCountryListener
) : RecyclerView.Adapter<VpnServerCountryRecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val  tv_country_name : TextView
        init{
            tv_country_name = view.findViewById(R.id.tv_country_name)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recyclerview_country_row, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < 0 || position >= mValues.size) return

        val item = mValues[position]

        holder.tv_country_name.text = "country=${item.countryName}: server count=${item.serverCount}"
        holder.itemView.setOnClickListener {
            mListener.onListFragmentInteractionCountry(item.countryName)
        }

    }

    override fun getItemCount(): Int {

        return mValues.size
    }

}


interface OnServerListFragmentInteracListener {
    fun onServerListFragmentInteracListener(vpnServer: VpnServer)
}
class VpnServerListRecyclerViewAdapter(
    private val mValues: ArrayList<VpnServer>,
    private val mListener: OnServerListFragmentInteracListener
) : RecyclerView.Adapter<VpnServerListRecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val  tv_server_info : TextView
        init{
            tv_server_info = view.findViewById(R.id.tv_server_info)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recyclerview_server_info_row, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < 0 || position >= mValues.size) return

        val item = mValues[position]

        holder.tv_server_info.text = "${item.country}/${item.title}"
        holder.itemView.setOnClickListener {
        }

    }

    override fun getItemCount(): Int {

        return mValues.size
    }

}


class HomeActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var connectBtn:Button
    private lateinit var tvStatus:TextView
    private lateinit var recyclerView:RecyclerView
    private lateinit var serverListRecyclerView:RecyclerView

    private var permissionResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if (it.resultCode == RESULT_OK) {
            Toast.makeText(this, "vpn permission granted, click to start", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "vpn permission denied", Toast.LENGTH_LONG).show()
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        Log.d("MD5", getSignatureMd5(this)!!)

        if (VpnService.prepare(this@HomeActivity) == null) {
            mainViewModel.vpnPermissionState = VpnPermissionState.Granted
        }

        setupUi()
        setupObserver()

        savedInstanceState?.let {
            if (it.getBoolean("from_notification",false)){
                Toast.makeText(this, " savedInstanceState from_notification", Toast.LENGTH_LONG).show()
            }
        }

        if (intent.getBooleanExtra("from_notification",false)){
            Toast.makeText(this, "intent from_notification", Toast.LENGTH_LONG).show()
        }
    }

//

    private fun setupUi(){
        connectBtn = findViewById(R.id.btn_start_stop_vpn)
        tvStatus = findViewById(R.id.tv_vpn_status)
        recyclerView = findViewById(R.id.list)
        serverListRecyclerView = findViewById(R.id.server_list)

        connectBtn.setOnClickListener {

            val intent: Intent? = VpnService.prepare(this@HomeActivity)
            if (intent == null) {
                mainViewModel.vpnPermissionState = VpnPermissionState.Granted
                if (mainViewModel.isVpnConnected()){
                    mainViewModel.stopVpn(this)
                }else if (mainViewModel.isVpnNotConnected()){
                    mainViewModel.startVpn(this, "all")
                }
            }else{
                permissionResult.launch(intent)
            }
        }
    }


    private fun setupObserver(){


        VpnlibCore.vpnConnectedEvent.observe(this, Observer {
            connectBtn.text = "Stop VPN"
            it.getContentIfNotHandled()?.let {
                Toast.makeText(this, " vpn connected", Toast.LENGTH_LONG).show()
            }
        })

        VpnlibCore.vpnDisconnectedEvent.observe(this, Observer {
            connectBtn.text = "Start VPN"
            tvStatus.text = "Not Connected"


            it.getContentIfNotHandled()?.let{ vpnStatistics ->
                vpnStatistics.maxRecvSpeed1S
                vpnStatistics.totalRecvBytes
                vpnStatistics.connectedTimestamp
                vpnStatistics.vpnServer
            }
        })

        VpnlibCore.vpnStatisticLiveData.observe(this, Observer {
            if (it.vpnState == VpnState.Connected){
                tvStatus.text = "Connected ${it.vpnServer!!.title} ${it.recvBytes1S} B/s"
                connectBtn.text = "Stop VPN"
            } else if (it.vpnState == VpnState.Connecting){
                tvStatus.text = "Connecting ${it.vpnServer?.title}"
                connectBtn.text = "..."
            }else if (it.vpnState == VpnState.Error){
                tvStatus.text = "Error"
                connectBtn.text = "Start VPN"
            }else if (it.vpnState == VpnState.Stopped){
                tvStatus.text = "Stopped"
                connectBtn.text = "Start VPN"
            }else if (it.vpnState == VpnState.Stopping){
                tvStatus.text = "Stopping ${it.vpnServer?.title}"
                connectBtn.text = "..."
            }
        })

        VpnlibCore.vpnStatisticLiveData.observeForever {
            Log.d("VPN", "${it.vpnState} ${it.vpnServer?.title} ${it.recvBytes1S} B/s")
        }


        VpnlibCore.vpnServerInfoByCountryLiveData.observe(this, Observer {
            if (it.isNotEmpty()){
                Log.d("SB", "${it.size} country available")
                val countryList = ArrayList<VpnCountryInfo>()
                it.forEach {serverCountry->
                    countryList.add(VpnCountryInfo(serverCountry.country, serverCountry.total))
                }

                recyclerView.layoutManager = LinearLayoutManager(this)
                recyclerView.adapter = VpnServerCountryRecyclerViewAdapter(countryList, object : OnListFragmentInteractionCountryListener{
                    override fun onListFragmentInteractionCountry(countryName: String) {
                        mainViewModel.startVpn(this@HomeActivity, countryName)
                    }
                })
            }
        })

        VpnlibCore.vpnServerListLiveData.observe(this, Observer {
            if (it.isNotEmpty()){

                serverListRecyclerView.layoutManager = LinearLayoutManager(this)
                serverListRecyclerView.adapter = VpnServerListRecyclerViewAdapter(it, object : OnServerListFragmentInteracListener{
                    override fun onServerListFragmentInteracListener(vpnServer: VpnServer) {
                    }
                })
            }
        })
    }
}