package com.example.lowlatencyvideorecorder.domain.usecase

import com.example.lowlatencyvideorecorder.domain.model.MediaItem
import com.example.lowlatencyvideorecorder.domain.repository.MediaRepository

class GetLatestMediaUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(): MediaItem? {
        return repository.getLatestMedia()
    }
}

