package com.dope.ooxixyz

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton

class btconnect : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_btconnect)

        //nextstep button
        findViewById<ImageButton>(R.id.nextStep).setOnClickListener{
            startActivity(Intent(this, MainActivity::class.java))
        }

    }
}