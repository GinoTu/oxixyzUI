package com.dope.ooxixyz

import android.content.ComponentCallbacks2
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dope.ooxixyz.Contracts.receiver
import com.dope.ooxixyz.Contracts.socket
import com.dope.ooxixyz.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private var input: InputStream? = null

    private val stringBuilder = StringBuilder()

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(
            layoutInflater
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        input = socket?.inputStream
        Extend.logE("OOOOOOOOO", "HIIIIII")
        Extend.logE("socket", socket.toString())
        Extend.logE("currentdeviceaddr", Contracts.currentdeviceaddr.toString())
        Extend.logE("receiver", receiver.toString())
        startReadingFromSocket( /*開始監聽資料*/ )
        bottomSheets()

    }

    private fun startReadingFromSocket() {
        CoroutineScope(Dispatchers.Default).launch {
            val buffer = ByteArray(4096)
            while (socket?.isConnected == true) {
                try {
                    withContext(Dispatchers.IO) {
                        input?.read(buffer)?.let { count ->
                            if (count > 0) {
                                Extend.logE("OOOOOOOOO", "GOOOO")
                                val receivedData = buffer.copyOf(count)
                                stringBuilder.append(receivedData.toString(Charsets.UTF_8))
                                if (stringBuilder.endsWith("H"))
                                    processDataH()
                                else if (stringBuilder.endsWith("S"))
                                    processDataS()
                                else if (stringBuilder.endsWith("F"))
                                    processDataF()
                                else
                                    stringBuilder.clear()
                            }
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    Extend.logE("startReadingFromSocket", "Error: ${e.message.toString()}")
                    break
                }
            }
            Extend.logE("startReadingFromSocket", "Socket disconnected")
        }
    }

    //HEART RATE display
    private fun processDataH() {
        CoroutineScope(Dispatchers.Main).launch {
            val receivedData = stringBuilder.removeSuffix("H").toString()
            stringBuilder.clear()
            binding.HRreceive.text = String.format(getString(R.string.ui_receive_data, receivedData))
            Extend.logE("HR", receivedData)
        }
    }

    //SPO2 display
    private fun processDataS() {
        CoroutineScope(Dispatchers.Main).launch {
            val receivedData = stringBuilder.removeSuffix("S").toString()
            stringBuilder.clear()
            binding.SPO2receive.text = String.format(getString(R.string.ui_receive_data, receivedData))
            Extend.logE("SPO2", receivedData)
        }
    }

    //跌倒資料
    private fun processDataF() {
        CoroutineScope(Dispatchers.Main).launch {
            val receivedData = stringBuilder.removeSuffix("F").toString()
            stringBuilder.clear()
            if (receivedData == "H1F")
                Extend.logE("FALL", "HELP!!!")
        }
    }



    override fun onDestroy() {
        if (receiver != null)
            unregisterReceiver(receiver)
        socket = null
        input = null
        super.onDestroy()
    }
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level <= ComponentCallbacks2.TRIM_MEMORY_MODERATE)
            System.gc()
    }

    private fun bottomSheets(){
        //friend list bottom sheet
        val bottomSheetDialog1 = BottomSheetDialog(this)
        bottomSheetDialog1.setContentView(R.layout.activity_bottom_sheet)

        //friend request bottom sheet
        val bottomSheetDialog2 = BottomSheetDialog(this)
        bottomSheetDialog2.setContentView(R.layout.activity_top_sheet)

        binding.also {
            //friend list button
            it.friendList.setOnClickListener {
                bottomSheetDialog1.show()
            }
            //friend request button
            it.friendRequest.setOnClickListener {
                bottomSheetDialog2.show()
            }
        }
    }

}


