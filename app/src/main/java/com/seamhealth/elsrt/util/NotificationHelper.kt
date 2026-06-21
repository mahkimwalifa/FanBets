package com.seamhealth.elsrt.util

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.onesignal.OneSignal
import me.leolin.shortcutbadger.ShortcutBadger

object NotificationHelper {
    fun clearAllNotifications(context: Context) {
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancelAll()
            NotificationManagerCompat.from(context).cancelAll()
            OneSignal.Notifications.clearAllNotifications()
            ShortcutBadger.applyCount(context, 0)
            ShortcutBadger.removeCount(context)
        } catch (_: Exception) {
        }
    }
}
