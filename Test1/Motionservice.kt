package your.package.name

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.pow
import kotlin.math.sqrt

class MotionChallengeActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var shakeStartTime: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_motion_challenge)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val magnitude = sqrt(event.values[0].pow(2) + event.values[1].pow(2) + event.values[2].pow(2))
        if (magnitude > 15) {
            if (shakeStartTime == -1L) {
                shakeStartTime = System.currentTimeMillis()
            } else if (System.currentTimeMillis() - shakeStartTime > 5000) {
                passChallenge()
            }
        } else {
            shakeStartTime = -1
        }
    }

    private fun passChallenge() {
        val launchIntent = packageManager.getLaunchIntentForPackage("com.instagram.android")
        startActivity(launchIntent)
        finish()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
