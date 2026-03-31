package com.seamhealth.elsrt.ui.screens.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seamhealth.elsrt.data.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val repository: FootballRepository
) : ViewModel() {

    fun clearAllFavorites() {
        viewModelScope.launch {
            repository.clearAllFavorites()
        }
    }
}
