package com.seamhealth.elsrt

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seamhealth.elsrt.data.LaunchState
import com.seamhealth.elsrt.data.LaunchViewModel
import com.seamhealth.elsrt.ui.navigation.FanBetsNavHost
import com.seamhealth.elsrt.ui.screens.browser.BrowserScreen
import com.seamhealth.elsrt.ui.screens.phone.OtpWaitingScreen
import com.seamhealth.elsrt.ui.screens.phone.PhoneEntryScreen
import com.seamhealth.elsrt.ui.screens.phone.PhoneVerificationState
import com.seamhealth.elsrt.ui.screens.phone.PhoneVerificationViewModel
import com.seamhealth.elsrt.ui.theme.FanBetsTheme
import com.seamhealth.elsrt.util.NotificationHelper
import com.seamhealth.elsrt.util.NotificationPermissionManager
import com.seamhealth.elsrt.util.StorageHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

private const val DEFAULT_PUSH_TIME_SECONDS = 20L

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val badgeClearHandler = Handler(Looper.getMainLooper())

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        NotificationPermissionManager.openNotificationSettingsAfterPermission(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.clearAllNotifications(this)
        scheduleBadgeReset()
        setContent {
            FanBetsTheme {
                val launchViewModel: LaunchViewModel = viewModel()
                val launchState by launchViewModel.launchState.collectAsState()

                val phoneViewModel: PhoneVerificationViewModel = viewModel()
                val phoneState by phoneViewModel.state.collectAsState()
                val selectedCountry by phoneViewModel.selectedCountry.collectAsState()
                val phoneNumber by phoneViewModel.phoneNumber.collectAsState()
                val isPhoneLoading by phoneViewModel.isLoading.collectAsState()

                val shouldPromptNotifications = launchState is LaunchState.Remote

                NotificationPermissionEffect(
                    enabled = shouldPromptNotifications,
                    onRequestPermission = {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                )

                when (val state = launchState) {
                    is LaunchState.Loading -> {
                        Surface(modifier = Modifier.fillMaxSize()) {}
                    }

                    is LaunchState.PhoneEntry -> {
                        when (val pState = phoneState) {
                            is PhoneVerificationState.PhoneEntry,
                            is PhoneVerificationState.Loading -> {
                                PhoneEntryScreen(
                                    selectedCountry = selectedCountry,
                                    phoneNumber = phoneNumber,
                                    isLoading = isPhoneLoading,
                                    onCountrySelected = { phoneViewModel.setSelectedCountry(it) },
                                    onPhoneNumberChanged = { phoneViewModel.setPhoneNumber(it) },
                                    onRegistrationClick = { phoneViewModel.submitPhone() }
                                )
                            }
                            is PhoneVerificationState.NetworkError -> {
                                PhoneEntryScreen(
                                    selectedCountry = selectedCountry,
                                    phoneNumber = phoneNumber,
                                    isLoading = false,
                                    onCountrySelected = { phoneViewModel.setSelectedCountry(it) },
                                    onPhoneNumberChanged = { phoneViewModel.setPhoneNumber(it) },
                                    onRegistrationClick = { phoneViewModel.submitPhone() }
                                )
                                AlertDialog(
                                    onDismissRequest = { phoneViewModel.dismissNetworkError() },
                                    title = { Text(stringResource(R.string.network_error_title)) },
                                    text = { Text(stringResource(R.string.network_error_message)) },
                                    confirmButton = {
                                        TextButton(onClick = { phoneViewModel.retryAfterNetworkError() }) {
                                            Text(stringResource(R.string.try_again))
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { phoneViewModel.dismissNetworkError() }) {
                                            Text(stringResource(R.string.cancel))
                                        }
                                    }
                                )
                            }
                            is PhoneVerificationState.OtpWaiting -> {
                                launchViewModel.updateState(LaunchState.OtpWaiting(pState.phone))
                            }
                            is PhoneVerificationState.Redirect -> {
                                launchViewModel.updateState(LaunchState.Remote(pState.link))
                            }
                            is PhoneVerificationState.GameAccess -> {
                                launchViewModel.updateState(LaunchState.Local)
                            }
                        }
                    }

                    is LaunchState.OtpWaiting -> {
                        OtpWaitingScreen(
                            phoneNumber = state.phone,
                            onConfirmCode = { code -> phoneViewModel.confirmCode(code) },
                            onResendCode = { phoneViewModel.resendCode() },
                            onBackClick = null
                        )
                    }

                    is LaunchState.Remote -> {
                        BrowserScreen(address = state.address)
                    }

                    is LaunchState.Local -> {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            FanBetsNavHost()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        NotificationHelper.clearAllNotifications(this)
        scheduleBadgeReset()
    }

    override fun onStart() {
        super.onStart()
        NotificationHelper.clearAllNotifications(this)
        scheduleBadgeReset()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        NotificationHelper.clearAllNotifications(this)
        scheduleBadgeReset()
    }

    override fun onPostResume() {
        super.onPostResume()
        NotificationHelper.clearAllNotifications(this)
    }

    override fun onDestroy() {
        badgeClearHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun scheduleBadgeReset() {
        badgeClearHandler.removeCallbacksAndMessages(null)
        badgeClearHandler.postDelayed({ NotificationHelper.clearAllNotifications(this) }, 500L)
        badgeClearHandler.postDelayed({ NotificationHelper.clearAllNotifications(this) }, 1500L)
    }
}

@Composable
private fun NotificationPermissionEffect(
    enabled: Boolean,
    onRequestPermission: () -> Unit,
    pushTimeSeconds: Long = DEFAULT_PUSH_TIME_SECONDS
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity ?: return
    val storage = remember { StorageHelper(context) }

    LaunchedEffect(enabled) {
        if (!enabled || storage.isNotificationDialogShown()) return@LaunchedEffect

        val delayMillis = if (pushTimeSeconds > 0) {
            pushTimeSeconds * 1000
        } else {
            DEFAULT_PUSH_TIME_SECONDS * 1000
        }
        delay(delayMillis)

        if (!activity.isFinishing && !activity.isDestroyed) {
            storage.setNotificationDialogShown()
            NotificationPermissionManager.showPrePermissionDialog(activity, onRequestPermission)
        }
    }
}
