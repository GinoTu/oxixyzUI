package com.dope.ooxixyz


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.dope.ooxixyz.databinding.ActivityBottomSheetBinding
import com.dope.ooxixyz.databinding.ActivityTopSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialog


class MainActivity : AppCompatActivity() {

//    private lateinit var bos: ConstraintLayout
//    private lateinit var tos: ConstraintLayout

    private var bottomSheetDialog: BottomSheetDialog? = null

    private var topSheetDialog: BottomSheetDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //friend list button
        findViewById<ImageButton>(R.id.friendList).setOnClickListener {

            displayFriendsList()
        }

        //friend request button
        findViewById<ImageButton>(R.id.friendRequest).setOnClickListener {

            displayFriendRquest()
        }

//        //friend request bottom sheet
//        val bottomSheetDialog2 = BottomSheetDialog(this)
//        bottomSheetDialog2.setContentView(R.layout.activity_top_sheet)
//
//        //friend request button
//        findViewById<ImageButton>(R.id.friendRequest).setOnClickListener {
//            bottomSheetDialog2.show()
//        }
    }

    private fun displayFriendsList() {
        val view = ActivityBottomSheetBinding.inflate(layoutInflater)
        bottomSheetDialog = BottomSheetDialog(this)
        //friend list bottom sheet
        bottomSheetDialog?.setContentView(view.root)
        bottomSheetDialog?.show()

        view.addFriend.setOnClickListener {
            Toast.makeText(applicationContext, "Test", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayFriendRquest() {
        val view = ActivityTopSheetBinding.inflate(layoutInflater)
        topSheetDialog = BottomSheetDialog(this)
        //friend list bottom sheet
        topSheetDialog?.setContentView(view.root)
        topSheetDialog?.show()
    }
}