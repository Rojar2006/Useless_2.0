package com.example.myapplication

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class AppAccessibilityService : AccessibilityService() {
    private val motionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MotionService.ACTION_MOTION_DETECTED -> {
                    // Motion detected - close any existing block screen
                    sendBroadcast(Intent(BlockScreenActivity.ACTION_DISMISS))
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Register receiver for motion updates
        registerReceiver(motionReceiver, IntentFilter(MotionService.ACTION_MOTION_DETECTED))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()

            if (packageName == "com.instagram.android") {
                Log.d("Accessibility", "Instagram detected, motion state: ${MotionService.motionDetected}")
                if (!MotionService.motionDetected && !BlockScreenActivity.isActive) {
                    val intent = Intent(this, BlockScreenActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.w("Accessibility", "Service interrupted")
    }

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        serviceInfo = info
        Log.d("Accessibility", "Accessibility service connected")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(motionReceiver)
        } catch (e: Exception) {
            Log.e("Accessibility", "Error unregistering receiver", e)
        }
        Log.d("Accessibility", "Service destroyed")
    }

    companion object {
        const val TAG = "AppAccessibilityService"
    }
}
