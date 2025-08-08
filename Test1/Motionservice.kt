package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class MovementService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var firstRead = true

    private val noMovementTimeout = 5000L // 5 seconds
    private val handler = Handler(Looper.getMainLooper())
    private var noMovementRunnable: Runnable? = null

    companion object {
        var blockScreenShown: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "movement_channel",
                "Movement Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // Foreground notification
        val notification = NotificationCompat.Builder(this, "movement_channel")
            .setContentTitle("Movement Service Running")
            .setContentText("Detecting motion...")
            .setSmallIcon(R.drawable.ic_notification)
            .build()

        startForeground(1, notification)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        Log.d("MovementService", "Service started")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            if (firstRead) {
                lastX = x
                lastY = y
                lastZ = z
                firstRead = false
                startNoMovementTimer()
                return
            }

            val dx = abs(x - lastX)
            val dy = abs(y - lastY)
            val dz = abs(z - lastZ)

            lastX = x
            lastY = y
            lastZ = z

            if (dx > 3 || dy > 3 || dz > 3) {
                Log.d("MovementService", "Movement detected!")
                if (blockScreenShown) {
                    sendBroadcast(Intent("com.example.myapplication.CLOSE_BLOCK_SCREEN"))
                    blockScreenShown = false
                }
                startNoMovementTimer() // reset the no movement timer
            }
        }
    }

    private fun startNoMovementTimer() {
        noMovementRunnable?.let { handler.removeCallbacks(it) }
        noMovementRunnable = Runnable {
            if (!blockScreenShown) {
                Log.d("MovementService", "No movement detected for $noMovementTimeout ms")
                showBlockScreen()
            }
        }
        handler.postDelayed(noMovementRunnable!!, noMovementTimeout)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun showBlockScreen() {
        val intent = Intent(this, BlockScreenActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        blockScreenShown = true
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
