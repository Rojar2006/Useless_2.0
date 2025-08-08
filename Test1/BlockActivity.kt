package com.example.myapplication

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class BlockActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.text = "Move your phone to access Instagram!"
        tv.textSize = 24f
        tv.setPadding(32, 64, 32, 64)
        setContentView(tv)
    }
}
