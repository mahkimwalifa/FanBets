package com.seamhealth.elsrt

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import com.onesignal.OneSignal
import dagger.hilt.android.HiltAndroidApp
import me.leolin.shortcutbadger.ShortcutBadger

@HiltAndroidApp
class FanBetsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        OneSignal.initWithContext(this, "21503fd9-40d8-4d13-a425-f945a447a514")
        clearAllNotifications()
    }

    private fun clearAllNotifications() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancelAll()
            ShortcutBadger.removeCount(this)
        } catch (_: Exception) {
        }
    }
}
