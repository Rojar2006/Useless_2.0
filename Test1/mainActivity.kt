// In MainActivity.kt
private lateinit var myReceiver: BroadcastReceiver
private val intentFilter = IntentFilter("com.example.myapplication.MOTION_SENSOR_DATA")

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
