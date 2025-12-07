package com.example.lowlatencyvideorecorder.domain.usecase

import com.example.lowlatencyvideorecorder.domain.model.MediaItem
import com.example.lowlatencyvideorecorder.domain.repository.MediaRepository

class SaveVideoUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(uri: String, path: String): Result<MediaItem> {
        return repository.saveVideo(uri, path)
    }
}

