package com.example.lowlatencyvideorecorder.presentation.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lowlatencyvideorecorder.domain.model.MediaItem
import com.example.lowlatencyvideorecorder.domain.usecase.GetLatestMediaUseCase
import com.example.lowlatencyvideorecorder.domain.usecase.SaveVideoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class RecordingUiState(
    val isRecording: Boolean = false,
    val currentVideoUri: String? = null,
    val latestMedia: MediaItem? = null,
    val error: String? = null
)

class RecordingViewModel : ViewModel(), KoinComponent {
    private val saveVideoUseCase: SaveVideoUseCase by inject()
    private val getLatestMediaUseCase: GetLatestMediaUseCase by inject()

    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    init {
        loadLatestMedia()
    }

    fun startRecording() {
        _uiState.value = _uiState.value.copy(isRecording = true, error = null)
    }

    fun stopRecording() {
        _uiState.value = _uiState.value.copy(isRecording = false)
    }

    fun onVideoSaved(uri: String, path: String) {
        viewModelScope.launch {
            saveVideoUseCase(uri, path)
                .onSuccess { mediaItem ->
                    _uiState.value = _uiState.value.copy(
                        currentVideoUri = uri,
                        latestMedia = mediaItem
                    )
                    loadLatestMedia()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to save video"
                    )
                }
        }
    }

    private fun loadLatestMedia() {
        viewModelScope.launch {
            getLatestMediaUseCase()?.let { media ->
                _uiState.value = _uiState.value.copy(latestMedia = media)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

