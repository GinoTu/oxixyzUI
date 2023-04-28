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
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.dope.ooxixyz.Adapter.MemberListAdapter
import com.dope.ooxixyz.Adapter.MemberRequestListAdapter
import com.dope.ooxixyz.databinding.ActivityBottomSheetBinding
import com.dope.ooxixyz.databinding.ActivityTopSheetBinding
import com.dope.ooxixyz.sensorInResponse.sensorIn
import com.dope.ooxixyz.sensorShowResponse.sensorShow
import com.dope.ooxixyz.userInfoResponse.Members
import com.dope.ooxixyz.userInfoResponse.MembersRequest
import com.dope.ooxixyz.userInfoResponse.userInfoResponseFormat
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    //BT var set up
    private var input: InputStream? = null
    private val stringBuilder = StringBuilder()

    private var bottomSheetDialog: BottomSheetDialog? = null
    private var topSheetDialog: BottomSheetDialog? = null

    //參數
    private var membersList: MutableList<Members> = ArrayList() //儲存 response 回傳的 membersList
    private var membersRequestList: MutableList<MembersRequest> = ArrayList() //儲存 response 回傳的 membersList
    private lateinit var memberListAdapter: MemberListAdapter //儲存 Adapter 的變數
    private lateinit var memberRequestListAdapter: MemberRequestListAdapter //儲存 Adapter 的變數

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private var userId = ""
    private var userName = ""
    private var phoneNumber = ""
    private var token = ""
    private var getImportantFriend = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        input = socket?.inputStream
        startReadingFromSocket( /*開始監聽資料*/ )

        userId = getSharedPreferences("user_File", MODE_PRIVATE) //取得SharedPreferences物件
            .getString("user_id", "").toString() //取得USER的值 ""為預設回傳值

        userName = getSharedPreferences("user_File", MODE_PRIVATE) //取得SharedPreferences物件
            .getString("userName", "").toString() //取得USER的值 ""為預設回傳值

        phoneNumber = getSharedPreferences("user_File", MODE_PRIVATE) //取得SharedPreferences物件
            .getString("phoneNumber", "").toString() //取得USER的值 ""為預設回傳值

        getImportantFriend = getSharedPreferences("importantFile", MODE_PRIVATE) //取得SharedPreferences物件
            .getString("importantFriend", "").toString() //取得USER的值 ""為預設回傳值

        token = getSharedPreferences("tokenFile", MODE_PRIVATE).getString("TOKEN", "").toString()

        setListener( /*設定按鈕監聽器*/ )

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




    //顯示好友清單
    private fun displayFriendsList() {
        val view = ActivityBottomSheetBinding.inflate(layoutInflater)
        bottomSheetDialog = BottomSheetDialog(this)
        //friend list bottom sheet
        bottomSheetDialog?.setContentView(view.root)
        bottomSheetDialog?.show()

        // RecyclerView
        memberListAdapter = MemberListAdapter() //初始化 Adapter 物件
        memberListAdapter.setterData(membersList)
        Log.e("recyclerView", "count = ${memberListAdapter.itemCount}")

        view.rvMemberList.run {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
            adapter = memberListAdapter
            addItemDecoration(DividerItemDecoration(this@MainActivity, DividerItemDecoration.VERTICAL))
            memberListAdapter
        }.apply{
            onItemClickCallback = { position, item ->
                Toast.makeText(applicationContext, item.userName, Toast.LENGTH_SHORT).show()
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("是否設為重要聯絡人好友")
                    .setNeutralButton("否"){dialog, which ->
                    }
                    .setPositiveButton("是"){dialog, which ->
                        val important =
                            getSharedPreferences("importantFile", MODE_PRIVATE) //存成text.xml,MODE_PRIVATE方式存取

                        important.edit() //編輯pref
                            .putString("importantFriend", item.user_id) //將user字串的內容寫入設定檔，資料標籤為”USER”。
                            //.commit() //提交編輯
                            .apply() //提交編輯
                        getImportantFriend = getSharedPreferences("importantFile", MODE_PRIVATE) //取得SharedPreferences物件
                            .getString("importantFriend", "").toString() //取得USER的值 ""為預設回傳值

                        Log.e("是",getImportantFriend )
                    }.show()
            }
        }

        view.addFriend.setOnClickListener {

            /** 設定dialog **/
            lateinit var responseTo: String
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Enter phone number")

            val input = EditText(this)
            builder.setView(input)

            builder.setPositiveButton("OK") { _, _ ->
                responseTo = input.text.toString()
                membersRequest(responseTo)
            }

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }

            val dialog = builder.create()
            /** 設定dialog **/

            dialog.show()



            Toast.makeText(applicationContext, "Test", Toast.LENGTH_SHORT).show()
        }
    }

    //顯示好友請求清單
    //recycleView
    private fun displayFriendRequest() {
        val view = ActivityTopSheetBinding.inflate(layoutInflater)
        topSheetDialog = BottomSheetDialog(this)
        //friend list bottom sheet
        topSheetDialog?.setContentView(view.root)
        topSheetDialog?.show()

        // RecyclerView
        memberRequestListAdapter = MemberRequestListAdapter() //初始化 Adapter 物件
        memberRequestListAdapter.setterData(membersRequestList)
        Log.e("recyclerView", "count = ${memberRequestListAdapter.itemCount}")

        view.rvMemberRequestList.run {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
            adapter = memberRequestListAdapter
            addItemDecoration(DividerItemDecoration(this@MainActivity, DividerItemDecoration.VERTICAL))
            memberRequestListAdapter
        }.apply{
            onItemClickCallback = { position, item ->
                Toast.makeText(applicationContext, item.userName, Toast.LENGTH_SHORT).show()
                val responseTo = """
        {
            "user_id": "${item.user_id}",
            "userName": "${item.userName}",
            "phoneNumber": "${item.phoneNumber}"
        }""".trimIndent()
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("是否加為好友")
                    .setNeutralButton("拒絕"){dialog, which ->
                        membersDeny(responseTo)
                    }
                    .setPositiveButton("接受"){dialog, which ->
                        membersAccept(responseTo)
                    }.show()
            }
        }
    }

    //好友清單API
    private fun membersList(inputUserId: String){

        val json = """
        {
            "user_id": "$inputUserId"
        }
    """.trimIndent()

        // 定义 JSON 格式的媒体类型
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()

        // 创建请求体
        val requestBody = json.toRequestBody(jsonMediaType)
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) // 连接超时时间为 10 秒
            .readTimeout(10, TimeUnit.SECONDS) // 读取超时时间为 10 秒
            .writeTimeout(10, TimeUnit.SECONDS) // 写入超时时间为 10 秒
            .build()

        val request = Request.Builder()
            .url("http:/10.122.9.218:3000/userInfo")//記得改網址
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {

                //取得userInfo的Response
                val userInfoResponse = response.body?.string()
                Log.e("userInfoResponse", userInfoResponse.toString())

                //將userInfoResponse對應到userInfoResponseFormat的data class
                val userInfo = Gson().fromJson(userInfoResponse, userInfoResponseFormat::class.java)
                //印出userInfo的membersList
                Log.e("membersList", userInfo.response.userInfo.membersList.toString())
                membersList.clear()
                membersList.addAll(userInfo.response.userInfo.membersList)

            }
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
        // 释放线程池
        client.dispatcher.executorService.shutdown()
        //return membersList
    }

    //好友請求清單API
    private fun membersRequestList(inputUserId: String) {
        //Log.e("login", inputUserId)
        val json = """
        {
            "user_id": "$inputUserId"
        }
    """.trimIndent()
        // 定义 JSON 格式的媒体类型
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        // 创建请求体
        val requestBody = json.toRequestBody(jsonMediaType)
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) // 连接超时时间为 10 秒
            .readTimeout(10, TimeUnit.SECONDS) // 读取超时时间为 10 秒
            .writeTimeout(10, TimeUnit.SECONDS) // 写入超时时间为 10 秒
            .build()

        val request = Request.Builder()
            .url("http:/10.122.9.218.159:3000/userInfo")//記得改網址
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {

                //取得userInfo的Response
                val userInfoResponse = response.body?.string()
                Log.e("userInfoResponse", userInfoResponse.toString())

                //將userInfoResponse對應到userInfoResponseFormat的data class
                val userInfo = Gson().fromJson(userInfoResponse, userInfoResponseFormat::class.java)
                //印出userInfo的membersRequest
                Log.e("membersRequest", userInfo.response.userInfo.membersRequest.toString())

                membersRequestList.clear()
                membersRequestList.addAll(userInfo.response.userInfo.membersRequest)

            }
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
        // 释放线程池
        client.dispatcher.executorService.shutdown()
    }

    //送出好友請求
    private fun membersRequest(requestTo: String) {

        val userDetail = """
        {
            "user_id": "$userId",
            "userName": "$userName",
            "phoneNumber": "$phoneNumber"
        }""".trimIndent()

        val json = """
        {
            "user_id": "$userId",
            "userDetail": $userDetail,
            "requestTo": "$requestTo"
        }
    """.trimIndent()

        Log.e("membersRequest", json)
        // 定义 JSON 格式的媒体类型
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        // 创建请求体
        val requestBody = json.toRequestBody(jsonMediaType)
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) // 连接超时时间为 10 秒
            .readTimeout(10, TimeUnit.SECONDS) // 读取超时时间为 10 秒
            .writeTimeout(10, TimeUnit.SECONDS) // 写入超时时间为 10 秒
            .build()

        val request = Request.Builder()
            .url("http:/10.122.9.218:3000/membersRequest")//記得改網址
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {

                //取得userInfo的Response
                val membersRequestResponse = response.body?.string()

                //將userInfoResponse對應到userInfoResponseFormat的data class
                val membersRequest = Gson().fromJson(membersRequestResponse, userInfoResponseFormat::class.java)
                //印出userInfo的membersRequest
                Log.e("membersRequest", membersRequest.toString())

            }
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
        // 释放线程池
        client.dispatcher.executorService.shutdown()
    }

    //送出好友接受
    private fun membersAccept(responseTo: String) {
        Log.e("membersAccept",responseTo )

        val userDetail = """
        {
            "user_id": "$userId",
            "userName": "$userName",
            "phoneNumber": "$phoneNumber"
        }""".trimIndent()

        val json = """
        {
            "user_id": "$userId",
            "userDetail": $userDetail,
            "responseTo": $responseTo
        }
    """.trimIndent()

        // 定义 JSON 格式的媒体类型
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        // 创建请求体
        val requestBody = json.toRequestBody(jsonMediaType)
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) // 连接超时时间为 10 秒
            .readTimeout(10, TimeUnit.SECONDS) // 读取超时时间为 10 秒
            .writeTimeout(10, TimeUnit.SECONDS) // 写入超时时间为 10 秒
            .build()

        val request = Request.Builder()
            .url("http:/10.122.9.218:3000/membersResponse/accept")//記得改網址
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {

                val membersAcceptResponse = response.body?.string()
                Log.e("print2", membersAcceptResponse.toString())

                //將userInfoResponse對應到userInfoResponseFormat的data class
                //val membersAccept = Gson().fromJson(membersAcceptResponse, membersAccept::class.java)
                //印出userInfo的membersRequest
                //Log.e("membersAccept", membersAccept.toString())

            }
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
        // 释放线程池
        client.dispatcher.executorService.shutdown()
    }

    //送出拒絕好友6c
    private fun membersDeny(responseTo: String) {
        //Log.e("membersDeny",responseTo )

        val userDetail = """
        {
            "user_id": "$userId",
            "userName": "$userName",
            "phoneNumber": "$phoneNumber"
        }""".trimIndent()

        val json = """
        {
            "user_id": "$userId",
            "userDetail": $userDetail,
            "responseTo": $responseTo
        }
    """.trimIndent()

        Log.e("json",json )

        // 定义 JSON 格式的媒体类型
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        // 创建请求体
        val requestBody = json.toRequestBody(jsonMediaType)
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) // 连接超时时间为 10 秒
            .readTimeout(10, TimeUnit.SECONDS) // 读取超时时间为 10 秒
            .writeTimeout(10, TimeUnit.SECONDS) // 写入超时时间为 10 秒
            .build()

        val request = Request.Builder()
            .url("http:/10.122.9.218:3000/membersResponse/deny")//記得改網址
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {

                val membersDenyResponse = response.body?.string()
                Log.e("print", membersDenyResponse.toString())
                //將userInfoResponse對應到userInfoResponseFormat的data class
                //val membersDeny = Gson().fromJson(membersDenyResponse, membersDeny::class.java)
                //印出userInfo的membersRequest
                //Log.e("membersDeny", membersDeny.toString())
            }
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
        // 释放线程池
        client.dispatcher.executorService.shutdown()
    }

    //血氧資料輸入4a
    private fun sensorIn(inputUserId: String) {
        val sensorData = """
        {
            "Spo2": 20,
            "HR": 30
        }""".trimIndent()
        val json = """
        {
            "user_id": "$inputUserId",
            "sensorData": $sensorData
        }
    """.trimIndent()
        // 定义 JSON 格式的媒体类型
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        // 创建请求体
        val body = json.toRequestBody(jsonMediaType)
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) // 连接超时时间为 10 秒
            .readTimeout(10, TimeUnit.SECONDS) // 读取超时时间为 10 秒
            .writeTimeout(10, TimeUnit.SECONDS) // 写入超时时间为 10 秒
            .build()
        val request = Request.Builder()
            .url("http:/10.122.9.218:3000/sensorData/save")//記得改網址
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val sensorInResponse = response.body?.string()

                //將userInfoResponse對應到userInfoResponseFormat的data class
                val sensorIn = Gson().fromJson(sensorInResponse, sensorIn::class.java)
                //印出userInfo的membersRequest
                Log.e("sensorInResponse", sensorIn.toString())
            }
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
        // 释放线程池
        client.dispatcher.executorService.shutdown()
    }

    //血氧資料查看4b
    private fun sensorShow() {
        val json = """
        {
            "user_id": "$userId"
        }
    """.trimIndent()

        Log.e("sensorShow", json)
        // 定义 JSON 格式的媒体类型
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        // 创建请求体
        val requestBody = json.toRequestBody(jsonMediaType)
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) // 连接超时时间为 10 秒
            .readTimeout(10, TimeUnit.SECONDS) // 读取超时时间为 10 秒
            .writeTimeout(10, TimeUnit.SECONDS) // 写入超时时间为 10 秒
            .build()
        val request = Request.Builder()
            .url("http:/10.122.9.218:3000/sensorData/show")//記得改網址
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {

            override fun onResponse(call: Call, response: Response) {
                val sensorShowResponse = response.body?.string()
                Log.e("sensorShow","sensorShowResponse: $sensorShowResponse")
                //將userInfoResponse對應到userInfoResponseFormat的data class
                val sensorShow = Gson().fromJson(sensorShowResponse, sensorShow::class.java)
                //印出userInfo的membersRequest
                Log.e("sensorShow", sensorShow.toString())
            }
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
        // 释放线程池
        client.dispatcher.executorService.shutdown()
    }

    // 按鈕監聽器
    private fun setListener() {
        binding.run {

            friendList.setOnClickListener {
                if (userId != "") {
                    //從api取得好友清單
                    membersList(userId)
                    notification(userId,getImportantFriend)
                }
                displayFriendsList()
            }

            friendRequest.setOnClickListener {
                if (userId != "") {
                    //從api取得好友請求
                    membersRequestList(userId)
                }
                displayFriendRequest()
            }

        }
    }

    //跌倒通知
    private fun notification(inputUserId: String , sendTo: String) {

        val json = """
        {
            "user_id": "$inputUserId",
            "sendTo": "$sendTo"
        }
    """.trimIndent()
        // 定义 JSON 格式的媒体类型
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        // 创建请求体
        val requestBody = json.toRequestBody(jsonMediaType)
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) // 连接超时时间为 10 秒
            .readTimeout(10, TimeUnit.SECONDS) // 读取超时时间为 10 秒
            .writeTimeout(10, TimeUnit.SECONDS) // 写入超时时间为 10 秒
            .build()

        val request = Request.Builder()
            .url("http:/10.122.9.218:3000/notification")//記得改網址
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {

                //取得userInfo的Response
                val userInfoResponse = response.body?.string()
                Log.e("notification", userInfoResponse.toString())

            }
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
        // 释放线程池
        client.dispatcher.executorService.shutdown()
    }
}
