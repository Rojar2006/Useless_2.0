package com.example.myapplication

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import kotlin.math.abs
import kotlin.math.sqrt
import android.view.WindowManager

// Action strings
private const val ACTION_CLOSE_BLOCK = "com.example.myapplication.CLOSE_BLOCK_SCREEN"
private const val ACTION_SENSOR_DATA = "com.example.myapplication.MOTION_SENSOR_DATA"
private const val CHANNEL_ID = "movement_channel"

////////////////////////////////////////////////////////////////////////////////
// MainActivity (UI + controls)
////////////////////////////////////////////////////////////////////////////////
class MainActivity : AppCompatActivity() {

    private lateinit var measuredText: TextView
    private lateinit var btnStartService: Button
    private val sensorReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val s = intent?.getStringExtra("sensor_data")
            measuredText.text = s ?: "no data"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }


        measuredText = findViewById(R.id.measuredText)
        btnStartService = findViewById(R.id.btnStartService)

        btnStartService.setOnClickListener {
            val serviceIntent = Intent(this, MovementService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // register to receive sensor data for debugging UI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(sensorReceiver, IntentFilter(ACTION_SENSOR_DATA), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(sensorReceiver, IntentFilter(ACTION_SENSOR_DATA),Context.RECEIVER_EXPORTED)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(sensorReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w("MainActivity", "Receiver not registered")
        }
    }
}

////////////////////////////////////////////////////////////////////////////////
// MovementService (background sensor + overlay control)
////////////////////////////////////////////////////////////////////////////////
class MovementService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var firstRead = true

    // tune these values
    private val movementThreshold = 5f     // per-sample delta threshold
    private val noMovementTimeout = 5000L    // ms of stillness to show block screen

    private val handler = Handler(Looper.getMainLooper())
    private var noMovementRunnable: Runnable? = null

    companion object {
        // state flag shared between Activity and Service
        @JvmStatic
        var blockScreenShown: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()

        // create notification channel (Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Movement Service", NotificationManager.IMPORTANCE_LOW)
            val mgr = getSystemService(NotificationManager::class.java)
            mgr?.createNotificationChannel(ch)
        }

        // build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Motion Lock Running")
            .setContentText("Monitoring motion...")
            .setSmallIcon(R.drawable.ic_notification) // ensure you have this drawable
            .build()

        startForeground(1, notification)

        // setup sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accel?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: Log.w("MovementService", "No accelerometer available")

        // start the no-motion timer initially (so block only appears after timeout)
        startNoMovementTimer()
        Log.d("MovementService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // keep running
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // (optional) compute magnitude for debugging
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat() - 10

        // broadcast sensor data string for UI debug
        val sensorString = "x: %.2f y: %.2f z: %.2f | mag: %.2f".format(x, y, z, magnitude)
        sendBroadcast(Intent(ACTION_SENSOR_DATA).setPackage(packageName).putExtra("sensor_data", sensorString))

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

        // if we detect movement above threshold:
        if (magnitude > movementThreshold) {
            Log.d("MovementService", "Movement detected (dx=$dx, dy=$dy, dz=$dz)")
            // if overlay is shown, close it
            if (blockScreenShown) {
                sendBroadcast(Intent(ACTION_CLOSE_BLOCK).setPackage(packageName))
                // let activity toggle the flag in its lifecycle; but set false to avoid re-show race
                blockScreenShown = false
            }
            // reset the no-motion timer so we wait again after movement stops
            startNoMovementTimer()
        }
    }

    private fun startNoMovementTimer() {
        noMovementRunnable?.let { handler.removeCallbacks(it) }
        noMovementRunnable = Runnable {
            // timeout fired — user has been still for noMovementTimeout ms
            if (!blockScreenShown) {
                Log.d("MovementService", "No movement for $noMovementTimeout ms — showing block screen")
                showBlockScreen()
            } else {
                Log.d("MovementService", "No movement timeout but block already shown")
            }
        }
        handler.postDelayed(noMovementRunnable!!, noMovementTimeout)
    }

    private fun showBlockScreen() {
        val i = Intent(this, BlockScreenActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(i)
        blockScreenShown = true
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}

////////////////////////////////////////////////////////////////////////////////
// BlockScreenActivity (finishes when it receives CLOSE broadcast)
////////////////////////////////////////////////////////////////////////////////
class BlockScreenActivity : Activity() {

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // keep visible on lock screen and keep display on
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_block_screen)
    }

    override fun onStart() {
        super.onStart()
        // dynamic registration while activity visible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(closeReceiver, IntentFilter(ACTION_CLOSE_BLOCK), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(closeReceiver, IntentFilter(ACTION_CLOSE_BLOCK), Context.RECEIVER_EXPORTED)
        }
        // mark shown
        MovementService.blockScreenShown = true
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(closeReceiver)
        } catch (e: Exception) {
            Log.w("BlockScreenActivity", "Receiver not registered")
        }
        // do NOT set blockScreenShown = false here. Service controls that when sending CLOSE.
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Do nothing
    }

}
