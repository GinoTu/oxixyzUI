package com.dope.ooxixyz

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class signup : AppCompatActivity() {
    var phoneNumber = ""
    var userName = ""
    var checkPassword = ""
    var password = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        //confirm button
        findViewById<ImageButton>(R.id.confirm).setOnClickListener{
            userName = findViewById<EditText>(R.id.nameTextField).text.toString()
            phoneNumber = findViewById<EditText>(R.id.phoneNumber).text.toString()
            password = findViewById<EditText>(R.id.password).text.toString()
            checkPassword = findViewById<EditText>(R.id.pwAgainTextField).text.toString()
            signup(phoneNumber, userName, password, checkPassword)
        }
    }
    private fun signup(InputphoneNumber: String, InputuserName: String, Inputpassword: String, InputcheckPassword: String) {
        val json = """
        {
            "phoneNumber": "$InputphoneNumber",
            "userName": "$InputuserName",
            "password": "$Inputpassword",
            "checkPassword": "$InputcheckPassword"
        }
    """.trimIndent()
        // 定义 JSON 格式的媒体类型
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        // 创建请求体
        val body = json.toRequestBody(JSON_MEDIA_TYPE)
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) // 连接超时时间为 10 秒
            .readTimeout(10, TimeUnit.SECONDS) // 读取超时时间为 10 秒
            .writeTimeout(10, TimeUnit.SECONDS) // 写入超时时间为 10 秒
            .build()
        val request = Request.Builder()
            .url("http:/192.168.103.159:3000/signup")
            .post(body)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val jsonResponse = JSONObject(response.body?.string())
                if (jsonResponse.getJSONObject("response").get("status").toString() == "201")
                {
                    runOnUiThread {
                        startActivity(Intent(applicationContext, login::class.java))
                    }
                }
                Log.e("signup", jsonResponse.getJSONObject("response").toString())
            }
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
        // 释放线程池
        client.dispatcher.executorService.shutdown()
    }

}