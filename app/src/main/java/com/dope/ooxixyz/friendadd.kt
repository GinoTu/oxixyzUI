package com.dope.ooxixyz

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton

class friendadd : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bottom_sheet)

        //confirm button
        findViewById<ImageButton>(R.id.addFriend).setOnClickListener{
            startActivity(Intent(this, btconnect::class.java))
        }

    }
}