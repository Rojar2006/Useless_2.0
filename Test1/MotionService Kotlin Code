package com.example.myapplication

import android.app.Notification
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
import android.os.IBinder
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import android.widget.TextView
import kotlin.math.sqrt

class MotionService : Service(), SensorEventListener {

    companion object {
        var isMoving: Boolean = false
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var measuredText: TextView
    private var lastCheckTime = 0L

    override fun onCreate() {
        super.onCreate()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        createNotificationChannel()
        startForeground(1, buildNotification())

        Thread { monitorInstagram() }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]
            val magnitude = sqrt((x * x + y * y + z * z).toDouble())
            measuredText.text = "X: %.2f\nY: %.2f\nZ: %.2f\nMagnitude: %.2f".format(
                x, y, z, magnitude
            )

            isMoving = magnitude > 1.2  // Threshold for movement; adjust as needed
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action needed here for now
    }

    private fun monitorInstagram() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        while (true) {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 1000
            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND &&
                    "com.instagram.android".equals(event.getPackageName())) {


                    if (!isMoving && System.currentTimeMillis() - lastCheckTime > 3000) {
                        lastCheckTime = System.currentTimeMillis()
                        // Launch BlockActivity to block Instagram when not moving
                        val intent = Intent(this, BlockActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                }
            }
            Thread.sleep(1000)  // Check every second
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "motion_channel",
                "Motion Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "motion_channel")
                .setContentTitle("Motion Lock Running")
                .setContentText("Instagram lock is active")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Motion Lock Running")
                .setContentText("Instagram lock is active")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()
        }
    }
}
