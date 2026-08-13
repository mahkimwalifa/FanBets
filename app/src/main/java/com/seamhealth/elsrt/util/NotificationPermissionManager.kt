package com.seamhealth.elsrt.util

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.seamhealth.elsrt.R
import java.util.Locale

object NotificationPermissionManager {

    fun showPrePermissionDialog(
        activity: Activity,
        requestPermission: () -> Unit
    ) {
        val localized = activity.createLocalizedContext()
        val dialog = AlertDialog.Builder(activity)
            .setTitle(localized.getString(R.string.notification_dialog_title))
            .setMessage(localized.getString(R.string.notification_dialog_message))
            .setPositiveButton(localized.getString(R.string.notification_dialog_enable)) { _, _ ->
                handlePermissionFlow(activity, requestPermission)
            }
            .setNegativeButton(localized.getString(R.string.notification_dialog_later), null)
            .create()

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    /** RU → русский, иначе EN. */
    private fun Activity.createLocalizedContext(): Context {
        val language = if (Locale.getDefault().language.equals("ru", ignoreCase = true)) {
            "ru"
        } else {
            "en"
        }
        val locale = Locale.forLanguageTag(language)
        val config = Configuration(resources.configuration)
        config.setLocales(LocaleList(locale))
        return createConfigurationContext(config)
    }

    fun openNotificationSettingsAfterPermission(activity: Activity) {
        openNotificationSettings(activity)
    }

    private fun handlePermissionFlow(activity: Activity, requestPermission: () -> Unit) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    openNotificationSettings(activity)
                } else {
                    requestPermission()
                }
            }

            else -> {
                openNotificationSettings(activity)
            }
        }
    }

    private fun openNotificationSettings(activity: Activity) {
        try {
            val intent = Intent().apply {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                    }

                    else -> {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.parse("package:${activity.packageName}")
                    }
                }
            }
            activity.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
        }
    }
}
