package com.dope.ooxixyz

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
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
import com.dope.ooxixyz.Contracts.PERMISSION_BLUETOOTH_CONNECT
import com.dope.ooxixyz.Contracts.PERMISSION_BLUETOOTH_SCAN
import com.dope.ooxixyz.Contracts.PERMISSION_COARSE_LOCATION
import com.dope.ooxixyz.Contracts.PERMISSION_CODE
import com.dope.ooxixyz.Contracts.PERMISSION_FINE_LOCATION
import com.dope.ooxixyz.Contracts.bluetooth_permission
import com.dope.ooxixyz.Contracts.location_permission
import com.dope.ooxixyz.Extend.parcelable
import com.dope.ooxixyz.Extend.requestPermission
import com.dope.ooxixyz.databinding.ActivityBtconnectBinding
import java.io.InputStream
import java.io.OutputStream
import java.util.*


class btconnect : AppCompatActivity() {
    //BT
    companion object {
        private const val DEVICE_SELECTED = "android.bluetooth.devicepicker.action.DEVICE_SELECTED"
        private const val BLE_LAUNCH = "android.bluetooth.devicepicker.action.LAUNCH"
    }
    private val btAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
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
    private var receiver: BroadcastReceiver? = null

    private var pairedDevices: Set<BluetoothDevice>? = null
    private var pairedDevice: BluetoothDevice? = null

    private var isConnectOther = true

    private var socket: BluetoothSocket? = null
    private var output: OutputStream? = null
    private var input: InputStream? = null

    private val stringBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initRv()
        nextstepbtn()
    }

    //要求權限
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_CODE -> {
                for (result in grantResults)
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        when {
                            permissions.any { it == PERMISSION_FINE_LOCATION || it == PERMISSION_COARSE_LOCATION }
                            -> displayShortToast("請開啟定位的權限!")
                            permissions.any { it == PERMISSION_BLUETOOTH_SCAN || it == PERMISSION_BLUETOOTH_CONNECT }
                            -> displayShortToast("請開啟藍芽的權限!")
                        }
                        return
                    }
            }
        }
    }
    //開BT
    private val launchBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result?.let {
            if (result.resultCode != RESULT_OK) {
                Toast.makeText(this@btconnect, "請開啟您的藍芽!", Toast.LENGTH_SHORT).show()

                val hintl1: ImageView = binding.hintl1
                hintl1.setImageResource(R.drawable.iredlight)

                val bttext: ImageView = binding.bttext
                bttext.setImageResource(R.drawable.tbtnotcon)
            }else{

                val hintl1: ImageView = binding.hintl1
                hintl1.setImageResource(R.drawable.igreenlight)

                val bttext: ImageView = binding.bttext
                bttext.setImageResource(R.drawable.tbtiscon)
                statecheck =1
            }
        }
    }


    //nextstep button
    private fun nextstepbtn() {
        binding.run {
            nextStep.setOnClickListener {
                //判斷版本，要求權限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !requestLocationPermission())
                    return@setOnClickListener
                    initBluetooth()
            }

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
        if(!Extend.requestPermission(this, *bluetooth_permission)){
            displayShortToast("請開啟藍芽權限!")
            return false
        }
    return true
    }


    //初始化藍芽
    private fun initBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED)
        ) {
            requestBluetoothPermission()
            return
        }

        //取得藍芽開啟
        if (!btAdapter.isEnabled)
            launchBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))

        btAdapter.let {
            // 先找已配對過的
            pairedDevices = it.bondedDevices
            pairDeviceList.clear()
            pairedDevices?.forEach { device ->
                pairDeviceList.add(BLEDevice(device.name, device.address))
            }.also { pairDeviceAdapter.setterData(pairDeviceList) }
            it
        }.also {

            initService( /*初始化BroadcastReceiver*/)
            val filter = IntentFilter(DEVICE_SELECTED)
            registerReceiver(receiver, filter)
        }
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
        // 連線藍芽
        try {
            isConnectOther = true
            device.createBond()
            Toast.makeText(this@btconnect, "Connect ${device.name}", Toast.LENGTH_SHORT).show()

            val hintl2: ImageView = binding.hintl2
            hintl2.setImageResource(R.drawable.igreenlight)

            val pairtext: ImageView = binding.pairtext
            pairtext.setImageResource(R.drawable.tdeviceiscon)

        } catch (e: Exception) {
            e.printStackTrace()
            Extend.logE("Pair", "Connect Error: ${e.message.toString()}")

            val hintl2: ImageView = binding.hintl2
            hintl2.setImageResource(R.drawable.iredlight)

            val pairtext: ImageView = binding.pairtext
            pairtext.setImageResource(R.drawable.tdevicenotcon)

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
