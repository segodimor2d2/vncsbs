package com.rec.vncsbs.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VncUiState(
    val connected: Boolean = false
)

class VncViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(VncUiState())

    val uiState: StateFlow<VncUiState> =
        _uiState.asStateFlow()
}
