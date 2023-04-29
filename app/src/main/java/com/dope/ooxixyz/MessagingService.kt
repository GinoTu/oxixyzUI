package com.dope.ooxixyz

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dope.ooxixyz.Contracts.ipconfig
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class MessagingService : FirebaseMessagingService() {
    private var userId = ""

    // Token 被更新時觸發
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d(TAG, "Refreshed token: $token")

        userId = getSharedPreferences("user_File", MODE_PRIVATE) //取得SharedPreferences物件
            .getString("user_id", "").toString() //取得USER的值 ""為預設回傳值
        // 將 Token 存入 Database
        //sendTokenToServer(token)
        firebaseSave(userId,token)
    }

    // 收到 Message 時觸發
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it)
        }
    }

    private fun sendNotification(notification: RemoteMessage.Notification){
        val intent = Intent(this, MainActivity::class.java)
        // 如果啟動 Intent 會回到舊的 Activity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_IMMUTABLE)

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // 設定通知屬性
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setAutoCancel(true) // 點擊後移除通知
            .setSound(defaultSoundUri) // 不可以設定超過一秒的音效
            .setContentIntent(pendingIntent) // 點擊通知後要執行的 PendingIntent

        // 建立 NotificationManager 物件
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0 以上需要設定 Notification Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }


    companion object {
        private const val TAG = "MessagingService"
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

        val token = getSharedPreferences("tokenFile", MODE_PRIVATE).getString("TOKEN", "").toString()

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