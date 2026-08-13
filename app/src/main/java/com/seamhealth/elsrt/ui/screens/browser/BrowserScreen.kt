package com.seamhealth.elsrt.ui.screens.browser

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.seamhealth.elsrt.R

@Composable
fun BrowserScreen(
    address: String
) {
    val hostContext = LocalContext.current

    LaunchedEffect(address) {
        val portalIntent = Intent(hostContext, BrowserActivity::class.java).apply {
            putExtra(BrowserActivity.EXTRA_SEAM_HREF, address)
        }

        hostContext.startActivity(portalIntent)

        if (hostContext is Activity) {
            applyBrowserFadeTransition(hostContext, closing = false)
            hostContext.finish()
            applyBrowserFadeTransition(hostContext, closing = true)
        }
    }
}

@Suppress("DEPRECATION")
private fun applyBrowserFadeTransition(activity: Activity, closing: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val type = if (closing) {
            Activity.OVERRIDE_TRANSITION_CLOSE
        } else {
            Activity.OVERRIDE_TRANSITION_OPEN
        }
        activity.overrideActivityTransition(
            type,
            R.anim.browser_fade_in,
            R.anim.browser_fade_out
        )
    } else {
        activity.overridePendingTransition(R.anim.browser_fade_in, R.anim.browser_fade_out)
    }
}
