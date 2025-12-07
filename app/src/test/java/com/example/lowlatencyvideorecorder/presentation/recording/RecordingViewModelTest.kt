package com.example.lowlatencyvideorecorder.presentation.recording

import app.cash.turbine.test
import com.example.lowlatencyvideorecorder.domain.model.MediaItem
import com.example.lowlatencyvideorecorder.domain.usecase.GetLatestMediaUseCase
import com.example.lowlatencyvideorecorder.domain.usecase.SaveVideoUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingViewModelTest {

    private lateinit var saveVideoUseCase: SaveVideoUseCase
    private lateinit var getLatestMediaUseCase: GetLatestMediaUseCase
    private lateinit var viewModel: RecordingViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        saveVideoUseCase = mockk()
        getLatestMediaUseCase = mockk()
        
        val testModule = module {
            factory { saveVideoUseCase }
            factory { getLatestMediaUseCase }
        }
        
        startKoin {
            modules(testModule)
        }
    }
    
    private fun createViewModel(latestMedia: MediaItem? = null) {
        coEvery { getLatestMediaUseCase() } returns latestMedia
        viewModel = RecordingViewModel()
    }
    
    @After
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have isRecording false`() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertFalse(initialState.isRecording)
            assertNull(initialState.currentVideoUri)
            assertNull(initialState.latestMedia)
            assertNull(initialState.error)
        }
    }

    @Test
    fun `startRecording should set isRecording to true`() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        viewModel.uiState.test {
            skipItems(1)
            viewModel.startRecording()
            val state = awaitItem()
            assertTrue(state.isRecording)
            assertNull(state.error)
        }
    }

    @Test
    fun `stopRecording should set isRecording to false`() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        viewModel.uiState.test {
            skipItems(1)
            viewModel.startRecording()
            skipItems(1)
            viewModel.stopRecording()
            val state = awaitItem()
            assertFalse(state.isRecording)
        }
    }

    @Test
    fun `onVideoSaved should update state with media item on success`() = runTest(testDispatcher) {
        val testUri = "content://test/video.mp4"
        val testPath = "/test/path/video.mp4"
        val testMediaItem = MediaItem(
            id = 1L,
            uri = testUri,
            path = testPath,
            dateCreated = Date(),
            duration = 1000L,
            size = 5000L,
            isVideo = true
        )

        createViewModel()
        coEvery { saveVideoUseCase(testUri, testPath) } returns Result.success(testMediaItem)
        coEvery { getLatestMediaUseCase() } returns testMediaItem

        advanceUntilIdle()
        viewModel.uiState.test {
            skipItems(1)
            viewModel.onVideoSaved(testUri, testPath)
            advanceUntilIdle()
            val state = awaitItem()
            assertEquals(testUri, state.currentVideoUri)
            assertEquals(testMediaItem, state.latestMedia)
            assertNull(state.error)
        }

        coVerify { saveVideoUseCase(testUri, testPath) }
    }

    @Test
    fun `onVideoSaved should set error on failure`() = runTest(testDispatcher) {
        val testUri = "content://test/video.mp4"
        val testPath = "/test/path/video.mp4"
        val errorMessage = "Failed to save video"
        val exception = Exception(errorMessage)

        createViewModel()
        coEvery { saveVideoUseCase(testUri, testPath) } returns Result.failure(exception)

        advanceUntilIdle()
        viewModel.uiState.test {
            skipItems(1)
            viewModel.onVideoSaved(testUri, testPath)
            advanceUntilIdle()
            val state = awaitItem()
            assertEquals(errorMessage, state.error)
        }

        coVerify { saveVideoUseCase(testUri, testPath) }
    }

    @Test
    fun `clearError should remove error from state`() = runTest(testDispatcher) {
        val testUri = "content://test/video.mp4"
        val testPath = "/test/path/video.mp4"
        val exception = Exception("Test error")

        createViewModel()
        coEvery { saveVideoUseCase(testUri, testPath) } returns Result.failure(exception)

        advanceUntilIdle()
        viewModel.uiState.test {
            skipItems(1)
            viewModel.onVideoSaved(testUri, testPath)
            advanceUntilIdle()
            skipItems(1)
            viewModel.clearError()
            val state = awaitItem()
            assertNull(state.error)
        }
    }

    @Test
    fun `startRecording should clear previous error`() = runTest(testDispatcher) {
        val testUri = "content://test/video.mp4"
        val testPath = "/test/path/video.mp4"
        val exception = Exception("Test error")

        createViewModel()
        coEvery { saveVideoUseCase(testUri, testPath) } returns Result.failure(exception)

        advanceUntilIdle()
        viewModel.uiState.test {
            skipItems(1)
            viewModel.onVideoSaved(testUri, testPath)
            advanceUntilIdle()
            skipItems(1)
            viewModel.startRecording()
            val state = awaitItem()
            assertTrue(state.isRecording)
            assertNull(state.error)
        }
    }
}

