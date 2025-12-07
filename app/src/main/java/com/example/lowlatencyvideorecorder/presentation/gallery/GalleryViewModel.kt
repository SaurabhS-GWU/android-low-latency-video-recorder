package com.example.lowlatencyvideorecorder.presentation.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lowlatencyvideorecorder.domain.model.MediaItem
import com.example.lowlatencyvideorecorder.domain.usecase.DeleteMediaUseCase
import com.example.lowlatencyvideorecorder.domain.usecase.GetAllMediaUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class GalleryUiState(
    val mediaList: List<MediaItem> = emptyList(),
    val selectedMediaIndex: Int = 0,
    val isPlaying: Boolean = false,
    val error: String? = null
)

class GalleryViewModel : ViewModel(), KoinComponent {
    private val getAllMediaUseCase: GetAllMediaUseCase by inject()
    private val deleteMediaUseCase: DeleteMediaUseCase by inject()

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        loadMedia()
    }

    private fun loadMedia() {
        viewModelScope.launch {
            getAllMediaUseCase().collect { mediaList ->
                _uiState.value = _uiState.value.copy(
                    mediaList = mediaList,
                    selectedMediaIndex = if (mediaList.isNotEmpty()) 0 else 0
                )
            }
        }
    }

    fun selectMedia(index: Int) {
        if (index >= 0 && index < _uiState.value.mediaList.size) {
            _uiState.value = _uiState.value.copy(selectedMediaIndex = index)
        }
    }

    fun setPlaying(isPlaying: Boolean) {
        _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
    }

    fun deleteMedia(mediaItem: MediaItem) {
        viewModelScope.launch {
            deleteMediaUseCase(mediaItem)
                .onSuccess {
                    loadMedia()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to delete media"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

