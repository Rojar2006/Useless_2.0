package com.example.myapplication

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.WindowManager

class BlockScreenActivity : Activity() {

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the screen on and visible even on lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_block_screen)
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(
            closeReceiver,
            IntentFilter("com.example.motionblocker.CLOSE_BLOCK_SCREEN"),
            Context.RECEIVER_NOT_EXPORTED // Important for Android 13+
        )
        MovementService.blockScreenShown = true
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(closeReceiver)
        MovementService.blockScreenShown = false // mark overlay as inacti
    }

    override fun onBackPressed() {
        // Disable back button
    }
}
