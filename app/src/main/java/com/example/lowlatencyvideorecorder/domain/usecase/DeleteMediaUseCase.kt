package com.example.lowlatencyvideorecorder.domain.usecase

import com.example.lowlatencyvideorecorder.domain.model.MediaItem
import com.example.lowlatencyvideorecorder.domain.repository.MediaRepository

class DeleteMediaUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(mediaItem: MediaItem): Result<Unit> {
        return repository.deleteMedia(mediaItem)
    }
}

