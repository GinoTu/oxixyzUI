package com.dope.ooxixyz


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import android.widget.ImageButton
import android.widget.Toast

import com.dope.ooxixyz.databinding.ActivityBottomSheetBinding
import com.dope.ooxixyz.databinding.ActivityTopSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody



import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager

import com.dope.ooxixyz.Adapter.MemberListAdapter

import com.dope.ooxixyz.userInfoResponse.Members
import com.dope.ooxixyz.userInfoResponse.userInfoResponseFormat

import com.google.gson.Gson

import java.io.IOException
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    //    private lateinit var bos: ConstraintLayout
//    private lateinit var tos: ConstraintLayout
    private var bottomSheetDialog: BottomSheetDialog? = null
    private var topSheetDialog: BottomSheetDialog? = null


    var membersReq = ""

    //參數
    private var membersList: MutableList<Members> = ArrayList() //儲存 response 回傳的 membersList
    private lateinit var memberListAdapter: MemberListAdapter //儲存 Adapter 的變數


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val getid = getSharedPreferences("user_File", MODE_PRIVATE) //取得SharedPreferences物件
            .getString("user_id", "") //取得USER的值 ""為預設回傳值

        //friend list button
        findViewById<ImageButton>(R.id.friendList).setOnClickListener {


            if (getid != null) {
                //從api取得好友清單
                membersList(getid)
            }
            displayFriendsList()


        }

        //friend request button
        findViewById<ImageButton>(R.id.friendRequest).setOnClickListener {
            if (getid != null) {

                //從api取得好友請求

                membersRequest(getid)
            }
            displayFriendRquest()
        }

    }

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
            layoutManager =
                LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
            adapter = memberListAdapter
            addItemDecoration(
                DividerItemDecoration(
                    this@MainActivity,
                    DividerItemDecoration.VERTICAL
                )
            )
            memberListAdapter
        }


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

    private fun membersList(inputUserId: String) {

        val json = """
        {
            "user_id": "$inputUserId"
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

            .url("http:/192.168.38.44:3000/userInfo")//記得改網址
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

            .url("http:/192.168.38.44:3000/userInfo")//記得改網址

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


            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
        // 释放线程池
        client.dispatcher.executorService.shutdown()
    }

}