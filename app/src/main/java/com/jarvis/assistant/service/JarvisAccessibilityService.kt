package com.jarvis.assistant.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        var isServiceActive = false
            private set

        var lastObservedPackage: String? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceActive = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        lastObservedPackage = event.packageName?.toString()
    }

    override fun onInterrupt() {
        isServiceActive = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceActive = false
    }
}
