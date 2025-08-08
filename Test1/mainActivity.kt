package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private lateinit var measuredText: TextView
    private lateinit var myReceiver: BroadcastReceiver
    private val intentFilter = IntentFilter("com.example.myapplication.MOTION_SENSOR_DATA")
    private val sensorDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val data = intent?.getStringExtra("sensor_data")
            measuredText.text = data
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        measuredText = findViewById(R.id.measuredText)
        val btnEnableService: Button = findViewById(R.id.btnEnableService)
        val btnStartService: Button = findViewById(R.id.btnStartService)

        btnEnableService.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Enable the Motion Blocker Accessibility Service", Toast.LENGTH_LONG).show()
        }

        btnStartService.setOnClickListener {
            val serviceIntent = Intent(this, MovementService::class.java)
            startForegroundService(serviceIntent)
            Toast.makeText(this, "Motion detection service started", Toast.LENGTH_SHORT).show()
        }
    }

    // In MainActivity.kt

    override fun onResume() {
        super.onResume()
        myReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Handle the broadcast
            }
        }
        // Add the flag for API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(myReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(myReceiver, intentFilter)
        }
    }

    override fun onPause() {
        super.onPause()
        // It's good practice to check if the receiver was actually registered
        // though in this simple onResume/onPause pairing, it likely was.
        // For more complex scenarios, you might add a flag.
        try {
            unregisterReceiver(myReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered or already unregistered.
            Log.w("MainActivity", "Receiver not registered or already unregistered.")
        }
    }

}
