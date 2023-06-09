package com.dope.ooxixyz

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dope.ooxixyz.Contracts.ipconfig
import com.dope.ooxixyz.loginResponse.loginResponseFormat
import com.dope.ooxixyz.userInfoResponse.userInfoResponseFormat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit


class login : AppCompatActivity() {

    private var phoneNumber = ""
    private var password = ""
    private var userName = ""
    private var userId = ""
    private var token = ""
    private var firebaseToken =""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        //confirm button
        findViewById<ImageButton>(R.id.confirm).setOnClickListener {


            phoneNumber = findViewById<EditText>(R.id.phoneNumber).text.toString()
            password = findViewById<EditText>(R.id.password).text.toString()

//            runBlocking{
//                async { login(phoneNumber, password) }.await()
//                async { firebaseSave(userId, firebaseToken) }.await()
//            }
            login(phoneNumber, password)

        }

        //signup button
        findViewById<ImageButton>(R.id.newAccount).setOnClickListener {
            startActivity(Intent(this, signup::class.java))
        }

  }
    private fun login(inputPhoneNumber: String, inputPassword: String) {
        //印出inputPhoneNumber和inputPassword
        Log.e("login", "inputPhoneNumber: $inputPhoneNumber, inputPassword: $inputPassword")

        val json = """
        {
            "phoneNumber": "$inputPhoneNumber",
            "password": "$inputPassword"
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
            .url("http:/"+getString(R.string.ipconfig)+":3000/login")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {

                //取得login的Response
                val loginResponse = response.body?.string()
                Log.e("loginResponse", loginResponse.toString())

                //將loginResponse對應到loginResponseFormat的data class
                val login = Gson().fromJson(loginResponse, loginResponseFormat::class.java)

                // 判斷 loginResponse 的 status 是否為 200
                if (login.response.status == 200) {

                    userName = login.response.userDetail.userName
                    userId = login.response.userDetail.user_id
                    phoneNumber = login.response.userDetail.phoneNumber.toString()
                    token = login.response.token

                    val pref =
                        getSharedPreferences("tokenFile", MODE_PRIVATE) //存成text.xml,MODE_PRIVATE方式存取

                    pref.edit() //編輯pref
                        .putString("TOKEN", token) //將user字串的內容寫入設定檔，資料標籤為”USER”。
                        //.commit() //提交編輯
                        .apply() //提交編輯
                    val pid =
                        getSharedPreferences("user_File", MODE_PRIVATE) //存成text.xml,MODE_PRIVATE方式存取

                    pid.edit() //編輯pref
                        .putString("user_id", userId)
                        .putString("userName", userName)
                        .putString("phoneNumber", phoneNumber)
                        //.commit() //提交編輯
                        .apply() //提交編輯

                    val getId = getSharedPreferences("user_File", MODE_PRIVATE) //取得SharedPreferences物件
                        .getString("user_id", "") //取得USER的值 ""為預設回傳值

                    Firebase.messaging.token.addOnCompleteListener(OnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                            return@OnCompleteListener
                        }

                        // Get new FCM registration token
                        firebaseToken = task.result

                        // Log and toast
                        val msg = getString(R.string.msg_token_fmt, firebaseToken)
                        Log.d(TAG, msg)
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        firebaseSave(userId, firebaseToken)
//                        // 將 Token 存入 Database
//                        //sendTokenToServer(token)
//                        val pref =
//                            getSharedPreferences("tokenFile", MODE_PRIVATE) //存成text.xml,MODE_PRIVATE方式存取
//
//                        pref.edit() //編輯pref
//                            .putString("firebaseTOKEN", firebaseToken) //將user字串的內容寫入設定檔，資料標籤為”USER”。
//                            //.commit() //提交編輯
//                            .apply() //提交編輯
//                        //firebaseSave(userId,token)
                    })

                    runOnUiThread {
                        startActivity(Intent(applicationContext, btconnect::class.java))
                    }
                    if (getId != null) {
                        Log.e("yes", getId)
                    }
                }



            }

            override fun onFailure(call: Call, e: IOException) {
                // 印出錯誤訊息
                Log.e("error", "onFailure:$e")
                e.printStackTrace()
            }
        })
        // 释放线程池
        client.dispatcher.executorService.shutdown()
    }

    companion object{
        private const val TAG = "login"
    }
    private fun firebaseSave(inputUserId: String , fireToken: String) {

        val json = """
        {
            "user_id": "$inputUserId",
            "firebaseToken": "$fireToken"
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
            .url("http:/"+getString(R.string.ipconfig)+":3000/userInfo")//記得改網址
            .addHeader("Authorization", "Bearer $token")
            .patch(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {

                //取得userInfo的Response
                val userInfoResponse = response.body?.string()
                Log.e("userInfoResponse", userInfoResponse.toString())
//                Log.e("firebaseTOKEN", getSharedPreferences("tokenFile", MODE_PRIVATE) //取得SharedPreferences物件
//                    .getString("firebaseTOKEN", "").toString())

            }
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
        // 释放线程池
        client.dispatcher.executorService.shutdown()
    }
}