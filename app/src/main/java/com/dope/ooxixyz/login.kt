package com.dope.ooxixyz

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dope.ooxixyz.databinding.ActivityLoginBinding

class login : AppCompatActivity() {
    private val binding: ActivityLoginBinding by lazy {
        ActivityLoginBinding.inflate(
            layoutInflater
        )
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.also{
            //confirm button
            it.confirm.setOnClickListener{
                startActivity(Intent(this, btconnect::class.java))
            }
            //signup button
            it.newAccount.setOnClickListener{
                startActivity(Intent(this, signup::class.java))
            }
        }
    }
}