package com.example.myapplication

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log

class MovementService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var firstRead = true

    companion object {
        var blockScreenShown: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()

        this.sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
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
                return
            }

            val dx = Math.abs(x - lastX)
            val dy = Math.abs(y - lastY)
            val dz = Math.abs(z - lastZ)

            lastX = x
            lastY = y
            lastZ = z

            // Detect large movement
            if (dx > 3 || dy > 3 || dz > 3) {
                Log.d("MovementService", "Movement detected!")

                if (!blockScreenShown) {
                    // Close the block screen if enough movement
                    sendBroadcast(Intent("com.example.motionblocker.CLOSE_BLOCK_SCREEN"))

                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun showBlockScreen() {
        val intent = Intent(this, BlockScreenActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        blockScreenShown = true
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }
}
