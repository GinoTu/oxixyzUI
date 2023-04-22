package com.dope.ooxixyz

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.dope.ooxixyz.Contracts.bluetooth_permission
import com.dope.ooxixyz.Contracts.currentdeviceaddr
import com.dope.ooxixyz.Contracts.location_permission
import com.dope.ooxixyz.Contracts.receiver
import com.dope.ooxixyz.Contracts.socket
import com.dope.ooxixyz.Extend.parcelable
import com.dope.ooxixyz.Extend.requestPermission
import com.dope.ooxixyz.databinding.ActivityBtconnectBinding
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*


class btconnect : AppCompatActivity() {
    //BT
    companion object {
        private const val DEVICE_SELECTED = "android.bluetooth.devicepicker.action.DEVICE_SELECTED"
        private const val BLE_LAUNCH = "android.bluetooth.devicepicker.action.LAUNCH"
    }

    private val btAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val binding: ActivityBtconnectBinding by lazy {
        ActivityBtconnectBinding.inflate(
            layoutInflater
        )
    }
    private var statecheck = 0
    private var pairDeviceList: MutableList<BLEDevice> = ArrayList()
    private lateinit var pairDeviceAdapter: ScanDeviceAdapter
    private var pairedDevices: Set<BluetoothDevice>? = null

    private var pairedDevice: BluetoothDevice? = null
    private var isConnectOther = true

    private var paired = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initRv()
        StateTransform()
        Extend.logE("statecheck", statecheck.toString())
        if(statecheck == 1)
            rcv()
        nextstepbtn()
    }

    //nextstep button
    @SuppressLint("SuspiciousIndentation")
    private fun nextstepbtn() {
        binding.run {
            nextStep.setOnClickListener {
                StateTransform()
                when (statecheck)
                {   //開啟權限
                    0 -> {
                        if(!requestBluetoothPermission())
                            requestBluetoothPermission()
                        if(!requestLocationPermission())
                            requestLocationPermission()
                        if(!requestLocationPermission() || !requestBluetoothPermission())
                            return@setOnClickListener
                        StateTransform()
                    }
                    // 開啟藍芽//尋找裝置
                    1 -> {
                        if(paired)
                        {
                            checkDevice()
                        }
                        else {
                            if (!btAdapter.isEnabled)
                                launchBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                            if (btAdapter.isEnabled) {
                                Intent(BLE_LAUNCH).apply { startActivity(this) }
                            } else {
                                displayShortToast("請不要關閉藍芽")
                                launchBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                            }
                        }
                    }
                    //切換頁面
                    2 ->{
                        if (btAdapter.isEnabled)
                        {
                            Extend.logE("pairedDevice", pairedDevice.toString())
                            Extend.logE("socket", socket.toString())
                            Extend.logE("currentdeviceaddr", currentdeviceaddr.toString())
                            Extend.logE("receiver", receiver.toString())
                            startActivity(Intent(this@btconnect, MainActivity::class.java))
                        }else{
                            displayShortToast("請不要關閉藍芽")
                            launchBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                        }
                    }

                    3 -> {

                    }
                    else -> {
                        displayShortToast("請試著重開此程式")
                    }
                }
            }
        }
    }
    private fun StateTransform(){
        if(requestBluetoothPermission() && requestLocationPermission() && !paired) {
            statecheck = 1
            Extend.logE("statecheck", statecheck.toString())
            if(btAdapter.isEnabled){
                val hintl1: ImageView = binding.hintl1
                hintl1.setImageResource(R.drawable.igreenlight)

                val bttext: ImageView = binding.bttext
                bttext.setImageResource(R.drawable.tbtiscon)
            }
            else{
                val hintl1: ImageView = binding.hintl1
                hintl1.setImageResource(R.drawable.iredlight)

                val bttext: ImageView = binding.bttext
                bttext.setImageResource(R.drawable.tbtnotcon)
            }
        }
    }

    private fun checkDevice(){
            if (currentdeviceaddr.toString() == "00:18:E5:03:70:51" && paired) {
                statecheck = 2
                val hintl2: ImageView = binding.hintl2
                hintl2.setImageResource(R.drawable.igreenlight)

                val pairtext: ImageView = binding.pairtext
                pairtext.setImageResource(R.drawable.tdeviceiscon)
                Toast.makeText(this@btconnect, "已配對至指定裝置!!", Toast.LENGTH_SHORT).show()
            } else {
                val hintl2: ImageView = binding.hintl2
                hintl2.setImageResource(R.drawable.iredlight)

                val pairtext: ImageView = binding.pairtext
                pairtext.setImageResource(R.drawable.tdevicenotcon)
                Toast.makeText(this@btconnect, "請配對指定裝置!", Toast.LENGTH_SHORT).show()
            }

    }

    //開BT
    private val launchBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result?.let {
                if (result.resultCode != RESULT_OK) {
                    Toast.makeText(this@btconnect, "請開啟您的藍芽!", Toast.LENGTH_SHORT).show()
                } else {
                    rcv()
                }
            }
        }

    //recycle view
    private fun rcv()
    {
        //抓取已配對過的資料已建立 recycle view
        btAdapter.let {
            // 先找已配對過的
            pairedDevices = it.bondedDevices
            pairDeviceList.clear()
            pairedDevices?.forEach { device ->
                pairDeviceList.add(BLEDevice(device.name, device.address))
                currentdeviceaddr = device
            }.also { pairDeviceAdapter.setterData(pairDeviceList) }
            it
        }.also {
            initService( /*初始化BroadcastReceiver*/)
            val filter = IntentFilter(DEVICE_SELECTED)
            registerReceiver(receiver, filter)
        }

    }

    //短暫顯示toast
    fun Context.displayShortToast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    //要位置權限
    fun Activity.requestLocationPermission(): Boolean{
        if(!requestPermission(this, *location_permission)){
            displayShortToast("請開啟定位權限!")
            return false
        }
        return true
    }

    //要藍芽權限
    @RequiresApi(Build.VERSION_CODES.S)
    fun Activity.requestBluetoothPermission(): Boolean {
        if(!requestPermission(this, *bluetooth_permission)){
            displayShortToast("請開啟藍芽權限!")
            return false
        }
    return true
    }

    private fun initService() {
        receiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context?, intent: Intent?) {
                pairedDevice = intent?.parcelable(BluetoothDevice.EXTRA_DEVICE)
                pairedDevice?.let { pairDevice(it) }
            }
        }
    }

    private fun pairDevice(device: BluetoothDevice) {
        // 確定權限開啟
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
            requestBluetoothPermission()
            return
        }
        try {
            isConnectOther = true
            currentdeviceaddr = device
            Extend.logE("statecheck", statecheck.toString())
            Extend.logE("currentdevice", currentdeviceaddr.toString())
            device.createBond()
            initSocket()
        } catch (e: Exception) {
            e.printStackTrace()
            Extend.logE("Pair", "Connect Error: ${e.message.toString()}")
            finish()
        }
    }

    //初始化recycleview
    private fun initRv() {
        pairDeviceAdapter = ScanDeviceAdapter()
        binding.rvListPair.run {
            layoutManager = LinearLayoutManager(this@btconnect, LinearLayoutManager.VERTICAL, false)
            adapter = pairDeviceAdapter
            addItemDecoration(DividerItemDecoration(baseContext, DividerItemDecoration.VERTICAL))
            pairDeviceAdapter
        }.apply {
            if(statecheck != 2) {
                onItemClickCallback = { _, item ->
                    // 連線藍芽
                    pairedDevices?.find { it.address == item.address }?.let { device ->
                        pairedDevice = device
                        pairDevice(device)

                    }
                }
            }
        }
    }

    private fun initSocket() {
        if(btAdapter.isEnabled) {
            paired = false
            Toast.makeText(this@btconnect, "Connecting...", Toast.LENGTH_LONG).show()
            // 確定權限開啟
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.BLUETOOTH_SCAN
                        ) != PackageManager.PERMISSION_GRANTED)) {
                requestBluetoothPermission()
                return
            }
            try {
                // Socket 連線
                socket =
                    pairedDevice?.createRfcommSocketToServiceRecord(pairedDevice?.uuids?.get(0)?.uuid)
                        ?: return
                isConnectOther = false
                CoroutineScope(Dispatchers.Default).launch {
                    if (socket?.isConnected == false) {
                        try {
                            socket?.connect()
                            if (socket?.isConnected == true) {
                                Extend.logE("initSocket", "Connect Success")
                                paired = true
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                            Extend.logE("initSocket", "Connect Error: ${e.message.toString()}")
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                            Extend.logE("initSocket", "Connect Error: ${e.message.toString()}")
                        }
                    }
                }
                Extend.logE("paired", paired.toString())
                Extend.logE("currentdevice", currentdeviceaddr.toString())
                checkDevice()
            } catch (e: Exception) {
                e.printStackTrace()
                Extend.logE("initSocket", "Error: ${e.message.toString()}")
            }
        }else
        {
            launchBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    override fun onDestroy() {
        if (receiver != null)
            unregisterReceiver(receiver)
        socket = null
        super.onDestroy()
    }
}
