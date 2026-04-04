package com.seamhealth.elsrt.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.seamhealth.elsrt.util.StorageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LaunchViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = StorageHelper(application)

    private val _launchState = MutableStateFlow<LaunchState>(LaunchState.Loading)
    val launchState: StateFlow<LaunchState> = _launchState.asStateFlow()

    init {
        checkInitialState()
    }

    private fun checkInitialState() {
        val redirectLink = storage.getRedirectLink()
        if (!redirectLink.isNullOrEmpty()) {
            _launchState.value = LaunchState.Remote(redirectLink)
            return
        }

        if (storage.hasGameAccess()) {
            _launchState.value = LaunchState.Local
            return
        }

        if (storage.isOtpMode()) {
            val countryCode = storage.getSavedCountryCode() ?: ""
            val phone = storage.getSavedPhone() ?: ""
            _launchState.value = LaunchState.OtpWaiting("$countryCode$phone")
            return
        }

        _launchState.value = LaunchState.PhoneEntry
    }

    fun updateState(newState: LaunchState) {
        _launchState.value = newState
    }

    fun getPolicyPath(): String? = storage.getPolicyPath()
}
