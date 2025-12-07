package com.example.lowlatencyvideorecorder.domain.usecase

import com.example.lowlatencyvideorecorder.domain.model.MediaItem
import com.example.lowlatencyvideorecorder.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow

class GetAllMediaUseCase(
    private val repository: MediaRepository
) {
    operator fun invoke(): Flow<List<MediaItem>> {
        return repository.getAllMedia()
    }
}

