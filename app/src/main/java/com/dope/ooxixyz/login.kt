package com.dope.ooxixyz

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.dope.ooxixyz.loginResponse.loginResponseFormat
import com.google.gson.Gson
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        //confirm button
        findViewById<ImageButton>(R.id.confirm).setOnClickListener {


            phoneNumber = findViewById<EditText>(R.id.phoneNumber).text.toString()
            password = findViewById<EditText>(R.id.password).text.toString()
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
            .url("http:/192.168.0.136:3000/login")
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
}