package com.example.lowlatencyvideorecorder.domain.repository

import com.example.lowlatencyvideorecorder.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    suspend fun saveVideo(uri: String, path: String): Result<MediaItem>
    fun getAllMedia(): Flow<List<MediaItem>>
    suspend fun getLatestMedia(): MediaItem?
    suspend fun deleteMedia(mediaItem: MediaItem): Result<Unit>
}

