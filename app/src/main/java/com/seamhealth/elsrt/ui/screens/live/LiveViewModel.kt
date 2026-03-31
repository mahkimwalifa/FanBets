package com.seamhealth.elsrt.ui.screens.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seamhealth.elsrt.data.api.models.FixtureResponse
import com.seamhealth.elsrt.data.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveViewModel @Inject constructor(
    private val repository: FootballRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveUiState(isLoading = true))
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadLiveFixtures()
        startAutoRefresh()
    }

    fun loadLiveFixtures() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getLiveFixtures()
                .onSuccess { fixtures ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            fixtures = fixtures,
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Unknown error"
                        )
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.getLiveFixtures()
                .onSuccess { fixtures ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            fixtures = fixtures,
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Unknown error"
                        )
                    }
                }
            _isRefreshing.value = false
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(30_000L)
                repository.getLiveFixtures()
                    .onSuccess { fixtures ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                fixtures = fixtures,
                                error = null
                            )
                        }
                    }
            }
        }
    }
}

data class LiveUiState(
    val isLoading: Boolean = false,
    val fixtures: List<FixtureResponse> = emptyList(),
    val error: String? = null
)
