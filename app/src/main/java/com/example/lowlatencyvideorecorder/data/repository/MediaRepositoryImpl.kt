package com.example.lowlatencyvideorecorder.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.example.lowlatencyvideorecorder.domain.model.MediaItem
import com.example.lowlatencyvideorecorder.domain.repository.MediaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Date

class MediaRepositoryImpl(
    private val context: Context
) : MediaRepository {

    override suspend fun saveVideo(uri: String, path: String): Result<MediaItem> {
        return try {
            val videoUri = android.net.Uri.parse(uri)
            val videoId = android.content.ContentUris.parseId(videoUri)
            
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION
            )
            
            val selection = "${MediaStore.Video.Media._ID} = ?"
            val selectionArgs = arrayOf(videoId.toString())
            
            var mediaItem: MediaItem? = null
            var retries = 5
            var delayMs = 200L
            
            while (retries > 0 && mediaItem == null) {
                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                        
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn)
                        val dateAdded = cursor.getLong(dateColumn)
                        val size = cursor.getLong(sizeColumn)
                        val duration = cursor.getLong(durationColumn)
                        
                        if (size > 0) {
                            mediaItem = MediaItem(
                                id = id,
                                uri = uri,
                                path = path,
                                dateCreated = Date(dateAdded * 1000),
                                duration = duration,
                                size = size,
                                isVideo = true
                            )
                        }
                    }
                }
                
                if (mediaItem == null) {
                    retries--
                    if (retries > 0) {
                        delay(delayMs)
                        delayMs *= 2 // Exponential backoff
                    }
                }
            }
            
            if (mediaItem != null) {
                Result.success(mediaItem!!)
            } else {
                val fallbackItem = MediaItem(
                    id = videoId,
                    uri = uri,
                    path = path,
                    dateCreated = Date(),
                    isVideo = true
                )
                Result.success(fallbackItem)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override fun getAllMedia(): Flow<List<MediaItem>> = flow {
        val mediaList = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION
        )

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        } else {
            null
        }

        val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf("%LowLatencyVideoRecorder%")
        } else {
            null
        }

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn)
                        val dateAdded = cursor.getLong(dateColumn)
                        val size = cursor.getLong(sizeColumn)
                        val duration = cursor.getLong(durationColumn)

                        // Only include videos that have been fully written (size > 0)
                        if (size > 0) {
                            val uri = android.content.ContentUris.withAppendedId(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                id
                            )

                            mediaList.add(
                                MediaItem(
                                    id = id,
                                    uri = uri.toString(),
                                    path = uri.toString(),
                                    dateCreated = Date(dateAdded * 1000),
                                    duration = duration,
                                    size = size,
                                    isVideo = true
                                )
                            )
                        }
                    }
        }

        emit(mediaList)
    }

    override suspend fun getLatestMedia(): MediaItem? {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION
        )

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        } else {
            null
        }

        val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf("%LowLatencyVideoRecorder%")
        } else {
            null
        }

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val dateAdded = cursor.getLong(dateColumn)
                val size = cursor.getLong(sizeColumn)
                val duration = cursor.getLong(durationColumn)

                // Only return video if it has been fully written (size > 0)
                if (size > 0) {
                    val uri = android.content.ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    return MediaItem(
                        id = id,
                        uri = uri.toString(),
                        path = uri.toString(),
                        dateCreated = Date(dateAdded * 1000),
                        duration = duration,
                        size = size,
                        isVideo = true
                    )
                }
            }
        }
        return null
    }

    override suspend fun deleteMedia(mediaItem: MediaItem): Result<Unit> {
        return try {
            val uri = android.net.Uri.parse(mediaItem.uri)
            val deleted = context.contentResolver.delete(uri, null, null)
            if (deleted > 0) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete media"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

