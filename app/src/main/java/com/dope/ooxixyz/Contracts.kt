package com.dope.ooxixyz

import android.os.Build
import androidx.annotation.RequiresApi

object Contracts {
    const val PERMISSION_CODE = 0

    const val PERMISSION_FINE_LOCATION =  android.Manifest.permission.ACCESS_FINE_LOCATION
    const val PERMISSION_COARSE_LOCATION =  android.Manifest.permission.ACCESS_COARSE_LOCATION
    @RequiresApi(Build.VERSION_CODES.S)
    const val PERMISSION_BLUETOOTH_SCAN = android.Manifest.permission.BLUETOOTH_SCAN
    @RequiresApi(Build.VERSION_CODES.S)
    const val PERMISSION_BLUETOOTH_CONNECT = android.Manifest.permission.BLUETOOTH_CONNECT

    val location_permission = arrayOf(PERMISSION_FINE_LOCATION, PERMISSION_COARSE_LOCATION)
    @RequiresApi(Build.VERSION_CODES.S)
    val bluetooth_permission = arrayOf(PERMISSION_BLUETOOTH_SCAN, PERMISSION_BLUETOOTH_CONNECT)
}