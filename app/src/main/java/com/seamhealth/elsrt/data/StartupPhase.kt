package com.seamhealth.elsrt.data

sealed class StartupPhase {
    data object CheckingPolicy : StartupPhase()
    data class PolicyLoaded(val isEnglishPolicy: Boolean) : StartupPhase()
}
