package com.dope.ooxixyz

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class login : AppCompatActivity() {
    var phoneNumber = ""
    var password = ""
    var userName = ""
    var user_id = ""
    var tonk = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        //confirm button
        findViewById<ImageButton>(R.id.confirm).setOnClickListener{


            phoneNumber = findViewById<EditText>(R.id.phoneNumber).text.toString()
            password = findViewById<EditText>(R.id.password).text.toString()
            login(phoneNumber,password)
        //startActivity(Intent(this, btconnect::class.java))
        }

        //signup button
        findViewById<ImageButton>(R.id.newAccount).setOnClickListener{
            startActivity(Intent(this, signup::class.java))
        }

    }
    private fun login(InputphoneNumber: String, Inputpassword: String) {
        Log.e("login", InputphoneNumber)
        val json = """
        {
            "phoneNumber": "$InputphoneNumber",
            "password": "$Inputpassword"
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
            .url("http:/192.168.103.159:3000/login")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val jsonResponse = JSONObject(response.body?.string())

                if (jsonResponse.getJSONObject("response").get("status").toString() == "200")
                {
                    userName = jsonResponse.getJSONObject("response").getJSONObject("userDetail").get("userName").toString()
                    user_id = jsonResponse.getJSONObject("response").getJSONObject("userDetail").get("user_id").toString()
                    phoneNumber = jsonResponse.getJSONObject("response").getJSONObject("userDetail").get("phoneNumber").toString()
                    tonk = jsonResponse.getJSONObject("response").get("token").toString()
                    Log.e("yes", "yes")

                    runOnUiThread {
                        startActivity(Intent(applicationContext, btconnect::class.java))
                    }
                    //startActivity(Intent(this, btconnect::class.java))

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
}