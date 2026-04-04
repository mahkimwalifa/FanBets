package com.seamhealth.elsrt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FanBetsTheme {
                val launchViewModel: LaunchViewModel = viewModel()
                val launchState by launchViewModel.launchState.collectAsState()

                val phoneViewModel: PhoneVerificationViewModel = viewModel()
                val phoneState by phoneViewModel.state.collectAsState()
                val selectedCountry by phoneViewModel.selectedCountry.collectAsState()
                val phoneNumber by phoneViewModel.phoneNumber.collectAsState()
                val isPhoneLoading by phoneViewModel.isLoading.collectAsState()

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
}
