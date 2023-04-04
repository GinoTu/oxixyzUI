package com.dope.ooxixyz

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class signup : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        //confirm button
        findViewById<ImageButton>(R.id.confirm).setOnClickListener{
            startActivity(Intent(this, login::class.java))
        }
    }
}