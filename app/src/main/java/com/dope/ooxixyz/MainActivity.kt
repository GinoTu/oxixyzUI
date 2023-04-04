package com.dope.ooxixyz


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomsheet.BottomSheetDialog


class MainActivity : AppCompatActivity() {

    private lateinit var bos: ConstraintLayout
    private lateinit var tos: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //friend list bottom sheet
        val bottomSheetDialog1 = BottomSheetDialog(this)
        bottomSheetDialog1.setContentView(R.layout.activity_bottom_sheet)

        //friend list button
        findViewById<ImageButton>(R.id.friendList).setOnClickListener {

            bottomSheetDialog1.show()
        }

        //friend request bottom sheet
        val bottomSheetDialog2 = BottomSheetDialog(this)
        bottomSheetDialog2.setContentView(R.layout.activity_top_sheet)

        //friend request button
        findViewById<ImageButton>(R.id.friendRequest).setOnClickListener {
            bottomSheetDialog2.show()
        }
    }
}