package com.example.lowlatencyvideorecorder.domain.model

import java.util.Date

data class MediaItem(
    val id: Long,
    val uri: String,
    val path: String,
    val dateCreated: Date,
    val duration: Long = 0L, // in milliseconds
    val size: Long = 0L, // in bytes
    val isVideo: Boolean = true
)

