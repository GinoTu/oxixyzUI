package com.dope.ooxixyz

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.dope.ooxixyz.databinding.ActivityBottomSheetBinding
import com.dope.ooxixyz.databinding.ActivityTopSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

//    private lateinit var bos: ConstraintLayout
//    private lateinit var tos: ConstraintLayout
    private var bottomSheetDialog: BottomSheetDialog? = null
    private var topSheetDialog: BottomSheetDialog? = null
    var membersList = ""
    var membersReq = ""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val gettoken = getSharedPreferences("tokenFile", MODE_PRIVATE)
            .getString("TOKEN", "") //取得USER的值 ""為預設回傳值
        val getid = getSharedPreferences("user_File", MODE_PRIVATE) //取得SharedPreferences物件
            .getString("user_id", "") //取得USER的值 ""為預設回傳值

        //friend list button
        findViewById<ImageButton>(R.id.friendList).setOnClickListener {
            if (getid != null) {
                userInfo(getid)
            }
            displayFriendsList()
        }

        //friend request button
        findViewById<ImageButton>(R.id.friendRequest).setOnClickListener {
            if (getid != null) {
                membersRequest(getid)
            }
            displayFriendRquest()
        }

//        //friend request bottom sheet
//        val bottomSheetDialog2 = BottomSheetDialog(this)
//        bottomSheetDialog2.setContentView(R.layout.activity_top_sheet)
//
//        //friend request button
//        findViewById<ImageButton>(R.id.friendRequest).setOnClickListener {
//            bottomSheetDialog2.show()
//        }
    }

    private fun displayFriendsList() {
        val view = ActivityBottomSheetBinding.inflate(layoutInflater)
        bottomSheetDialog = BottomSheetDialog(this)
        //friend list bottom sheet
        bottomSheetDialog?.setContentView(view.root)
        bottomSheetDialog?.show()

        view.addFriend.setOnClickListener {
            Toast.makeText(applicationContext, "Test", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayFriendRquest() {
        val view = ActivityTopSheetBinding.inflate(layoutInflater)
        topSheetDialog = BottomSheetDialog(this)
        //friend list bottom sheet
        topSheetDialog?.setContentView(view.root)
        topSheetDialog?.show()
    }
//清單
    private fun userInfo(Inputuser_id: String) {
        //Log.e("login", Inputuser_id)
        val json = """
        {
            "user_id": "$Inputuser_id"
        }
    """.trimIndent()
        // 定义 JSON 格式的媒体类型
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        // 创建请求体
        val requestBody = json.toRequestBody(JSON_MEDIA_TYPE)
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) // 连接超时时间为 10 秒
            .readTimeout(10, TimeUnit.SECONDS) // 读取超时时间为 10 秒
            .writeTimeout(10, TimeUnit.SECONDS) // 写入超时时间为 10 秒
            .build()

        val request = Request.Builder()
            .url("http:/192.168.150.159:3000/userInfo")//記得改網址
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val jsonResponse = JSONObject(response.body?.string())
                if (jsonResponse.getJSONObject("response").get("status").toString() == "200")
                {
                    membersList = jsonResponse.getJSONObject("response").getJSONObject("userInfo").get("membersList").toString()

                    Log.e("login", membersList)
                }
                else
                {
                    Log.e("no", "no")
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
        // 释放线程池
        client.dispatcher.executorService.shutdown()
    }
//請求
    private fun membersRequest(Inputuser_id: String) {
        //Log.e("login", Inputuser_id)
        val json = """
        {
            "user_id": "$Inputuser_id"
        }
    """.trimIndent()
        // 定义 JSON 格式的媒体类型
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        // 创建请求体
        val requestBody = json.toRequestBody(JSON_MEDIA_TYPE)
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) // 连接超时时间为 10 秒
            .readTimeout(10, TimeUnit.SECONDS) // 读取超时时间为 10 秒
            .writeTimeout(10, TimeUnit.SECONDS) // 写入超时时间为 10 秒
            .build()

        val request = Request.Builder()
            .url("http:/192.168.150.159:3000/userInfo")//記得改網址
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val jsonResponse = JSONObject(response.body?.string())
                if (jsonResponse.getJSONObject("response").get("status").toString() == "200")
                {
                    membersReq = jsonResponse.getJSONObject("response").getJSONObject("userInfo").get("membersRequest").toString()

                    Log.e("login", membersReq)
                }
                else
                {
                    Log.e("no", "no")
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
        // 释放线程池
        client.dispatcher.executorService.shutdown()
    }

    //申請好友
    /*private fun membersRequest(user_id: String,userDetail: String,userName: String
                               ,phoneNumber: String,requestTo: String) {
        //Log.e("login", Inputuser_id)
        val json = """
        {
            "user_id": "$user_id"
            "userDetail": "$userDetail"
            "userName": "$userName"
            "phoneNumber": "$phoneNumber"
            "requestTo": "$requestTo"
        }
    """.trimIndent()
        // 定义 JSON 格式的媒体类型
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        // 创建请求体
        val requestBody = json.toRequestBody(JSON_MEDIA_TYPE)
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) // 连接超时时间为 10 秒
            .readTimeout(10, TimeUnit.SECONDS) // 读取超时时间为 10 秒
            .writeTimeout(10, TimeUnit.SECONDS) // 写入超时时间为 10 秒
            .build()

        val request = Request.Builder()
            .url("http:/192.168.150.159:3000/membersRequest")//記得改網址
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val jsonResponse = JSONObject(response.body?.string())
                if (jsonResponse.getJSONObject("response").get("status").toString() == "200")
                {
                    membersList = jsonResponse.getJSONObject("response").getJSONObject("userInfo").get("membersList").toString()

                    Log.e("login", membersList)
                }
                else
                {
                    Log.e("no", "no")
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
        // 释放线程池
        client.dispatcher.executorService.shutdown()
    }

     */
}