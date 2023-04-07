package com.dope.ooxixyz

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.util.*
import com.dope.ooxixyz.Contracts.PERMISSION_CODE
import com.dope.ooxixyz.Contracts.PERMISSION_BLUETOOTH_CONNECT
import com.dope.ooxixyz.Contracts.PERMISSION_BLUETOOTH_SCAN
import com.dope.ooxixyz.Contracts.PERMISSION_COARSE_LOCATION
import com.dope.ooxixyz.Contracts.PERMISSION_FINE_LOCATION
import com.dope.ooxixyz.Contracts.bluetooth_permission
import com.dope.ooxixyz.Contracts.location_permission
import com.dope.ooxixyz.Extend.requestPermission
import com.dope.ooxixyz.databinding.ActivityBtconnectBinding


class btconnect : AppCompatActivity() {
    //BT
    private val btAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val binding: ActivityBtconnectBinding by lazy {
        ActivityBtconnectBinding.inflate(
            layoutInflater
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

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


    //nextstep button
    private fun nextstepbtn() {
        binding.nextStep.setOnClickListener {
            //判斷版本，要求權限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !requestLocationPermission()) {
                return@setOnClickListener
            initBluetooth()
            }
            if () {

            }
            if () {
                startActivity(Intent(this, MainActivity::class.java))
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
        }
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
    private fun initBluetooth(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
            requestBluetoothPermission()
            return
        }
    }
}
