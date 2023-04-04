package com.dope.ooxixyz

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton

class login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //confirm button
        findViewById<ImageButton>(R.id.confirm).setOnClickListener{
            startActivity(Intent(this, btconnect::class.java))
        }

        //signup button
        findViewById<ImageButton>(R.id.newAccount).setOnClickListener{
            startActivity(Intent(this, signup::class.java))
        }

    }
}