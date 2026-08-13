package com.seamhealth.elsrt

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seamhealth.elsrt.data.LaunchState
import com.seamhealth.elsrt.data.LaunchViewModel
import com.seamhealth.elsrt.data.StartupPhase
import com.seamhealth.elsrt.ui.navigation.FanBetsNavHost
import com.seamhealth.elsrt.ui.screens.browser.BrowserScreen
import com.seamhealth.elsrt.ui.screens.phone.OtpWaitingScreen
import com.seamhealth.elsrt.ui.screens.phone.PhoneEntryScreen
import com.seamhealth.elsrt.ui.screens.phone.PhoneVerificationState
import com.seamhealth.elsrt.ui.screens.phone.PhoneVerificationViewModel
import com.seamhealth.elsrt.ui.theme.FanBetsTheme
import com.seamhealth.elsrt.util.Country
import com.seamhealth.elsrt.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val badgeClearHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.clearAllNotifications(this)
        scheduleBadgeReset()
        setContent {
            FanBetsTheme {
                val launchViewModel: LaunchViewModel = viewModel()
                val startupPhase by launchViewModel.startupPhase.collectAsState()
                val launchState by launchViewModel.launchState.collectAsState()

                val phoneViewModel: PhoneVerificationViewModel = viewModel()
                val phoneState by phoneViewModel.state.collectAsState()
                val selectedCountry by phoneViewModel.selectedCountry.collectAsState()
                val phoneNumber by phoneViewModel.phoneNumber.collectAsState()
                val isPhoneLoading by phoneViewModel.isLoading.collectAsState()

                LaunchedEffect(startupPhase) {
                    val pl = startupPhase as? StartupPhase.PolicyLoaded ?: return@LaunchedEffect
                    phoneViewModel.initializeFromStorage()
                    if (!pl.isEnglishPolicy) {
                        phoneViewModel.submitAutoOtpForNonEnglishPolicy()
                    }
                }

                when (val phase = startupPhase) {
                    StartupPhase.CheckingPolicy -> {
                        LoadingScreen(modifier = Modifier.fillMaxSize())
                    }

                    is StartupPhase.PolicyLoaded -> {
                        if (!phase.isEnglishPolicy) {
                            NonEnglishPolicyContent(
                                phoneState = phoneState,
                                phoneViewModel = phoneViewModel,
                                selectedCountry = selectedCountry,
                                phoneNumber = phoneNumber,
                                isPhoneLoading = isPhoneLoading
                            )
                        } else {
                            EnglishPolicyContent(
                                launchState = launchState,
                                launchViewModel = launchViewModel,
                                phoneState = phoneState,
                                phoneViewModel = phoneViewModel,
                                selectedCountry = selectedCountry,
                                phoneNumber = phoneNumber,
                                isPhoneLoading = isPhoneLoading
                            )
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
private fun LoadingScreen(modifier: Modifier = Modifier) {
    val activityBg = colorResource(R.color.browser_activity_background)
    val loaderColor = colorResource(R.color.browser_loader_color)

    Surface(modifier = modifier, color = activityBg) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(activityBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = loaderColor
            )
        }
    }
}

@Composable
private fun NonEnglishPolicyContent(
    phoneState: PhoneVerificationState,
    phoneViewModel: PhoneVerificationViewModel,
    selectedCountry: Country,
    phoneNumber: String,
    isPhoneLoading: Boolean
) {
    when (phoneState) {
        is PhoneVerificationState.Loading -> {
            LoadingScreen(modifier = Modifier.fillMaxSize())
        }

        is PhoneVerificationState.Redirect -> {
            BrowserScreen(address = phoneState.link)
        }

        is PhoneVerificationState.GameAccess -> {
            Surface(modifier = Modifier.fillMaxSize()) {
                FanBetsNavHost()
            }
        }

        is PhoneVerificationState.OtpWaiting -> {
            OtpWaitingScreen(
                phoneNumber = phoneState.phone,
                onConfirmCode = { code -> phoneViewModel.confirmCode(code) },
                onResendCode = { phoneViewModel.resendCode() },
                onBackClick = null
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
                onDismissRequest = { phoneViewModel.dismissNetworkErrorAfterNonEnglishAuto() },
                title = { Text(stringResource(R.string.network_error_title)) },
                text = { Text(stringResource(R.string.network_error_message)) },
                confirmButton = {
                    TextButton(onClick = { phoneViewModel.retryNonEnglishAutoOtp() }) {
                        Text(stringResource(R.string.try_again))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { phoneViewModel.dismissNetworkErrorAfterNonEnglishAuto() }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        is PhoneVerificationState.PhoneEntry -> {
            PhoneEntryScreen(
                selectedCountry = selectedCountry,
                phoneNumber = phoneNumber,
                isLoading = isPhoneLoading,
                onCountrySelected = { phoneViewModel.setSelectedCountry(it) },
                onPhoneNumberChanged = { phoneViewModel.setPhoneNumber(it) },
                onRegistrationClick = { phoneViewModel.submitPhone() }
            )
        }
    }
}

@Composable
private fun EnglishPolicyContent(
    launchState: LaunchState,
    launchViewModel: LaunchViewModel,
    phoneState: PhoneVerificationState,
    phoneViewModel: PhoneVerificationViewModel,
    selectedCountry: Country,
    phoneNumber: String,
    isPhoneLoading: Boolean
) {
    when (val state = launchState) {
        is LaunchState.Loading -> {
            LoadingScreen(modifier = Modifier.fillMaxSize())
        }

        is LaunchState.PhoneEntry -> {
            LaunchedEffect(phoneState) {
                when (val ps = phoneState) {
                    is PhoneVerificationState.OtpWaiting ->
                        launchViewModel.updateState(LaunchState.OtpWaiting(ps.phone))
                    is PhoneVerificationState.Redirect ->
                        launchViewModel.updateState(LaunchState.Remote(ps.link))
                    is PhoneVerificationState.GameAccess ->
                        launchViewModel.updateState(LaunchState.Local)
                    else -> {}
                }
            }
            when (phoneState) {
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

                is PhoneVerificationState.OtpWaiting,
                is PhoneVerificationState.Redirect,
                is PhoneVerificationState.GameAccess -> {
                    LoadingScreen(modifier = Modifier.fillMaxSize())
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
