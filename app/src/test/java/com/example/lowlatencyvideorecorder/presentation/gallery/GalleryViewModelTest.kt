package com.example.lowlatencyvideorecorder.presentation.gallery

import app.cash.turbine.test
import com.example.lowlatencyvideorecorder.domain.model.MediaItem
import com.example.lowlatencyvideorecorder.domain.usecase.DeleteMediaUseCase
import com.example.lowlatencyvideorecorder.domain.usecase.GetAllMediaUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class GalleryViewModelTest {

    private lateinit var getAllMediaUseCase: GetAllMediaUseCase
    private lateinit var deleteMediaUseCase: DeleteMediaUseCase
    private lateinit var viewModel: GalleryViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testMediaList = listOf(
        MediaItem(
            id = 1L,
            uri = "content://test/video1.mp4",
            path = "/test/path/video1.mp4",
            dateCreated = Date(),
            duration = 1000L,
            size = 5000L,
            isVideo = true
        ),
        MediaItem(
            id = 2L,
            uri = "content://test/video2.mp4",
            path = "/test/path/video2.mp4",
            dateCreated = Date(),
            duration = 2000L,
            size = 10000L,
            isVideo = true
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        getAllMediaUseCase = mockk()
        deleteMediaUseCase = mockk()
        
        val testModule = module {
            factory { getAllMediaUseCase }
            factory { deleteMediaUseCase }
        }
        
        startKoin {
            modules(testModule)
        }
    }
    
    private fun createViewModel(mediaFlow: kotlinx.coroutines.flow.Flow<List<MediaItem>> = flowOf(emptyList())) {
        coEvery { getAllMediaUseCase() } returns mediaFlow
        viewModel = GalleryViewModel()
    }
    
    private fun createViewModelWithStateFlow(initialList: List<MediaItem> = emptyList()): MutableStateFlow<List<MediaItem>> {
        val mediaFlow = MutableStateFlow(initialList)
        coEvery { getAllMediaUseCase() } returns mediaFlow
        viewModel = GalleryViewModel()
        return mediaFlow
    }
    
    @After
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have empty media list`() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.mediaList.isEmpty())
            assertEquals(0, initialState.selectedMediaIndex)
            assertFalse(initialState.isPlaying)
            assertNull(initialState.error)
        }
    }

    @Test
    fun `loadMedia should update state with media list`() = runTest(testDispatcher) {
        createViewModel(flowOf(testMediaList))
        advanceUntilIdle()
        
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(testMediaList.size, state.mediaList.size)
            assertEquals(testMediaList[0], state.mediaList[0])
            assertEquals(0, state.selectedMediaIndex)
        }
    }

    @Test
    fun `selectMedia should update selectedMediaIndex`() = runTest(testDispatcher) {
        val mediaFlow = createViewModelWithStateFlow(testMediaList)
        advanceUntilIdle()
        
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertEquals(2, initialState.mediaList.size)
            assertEquals(0, initialState.selectedMediaIndex)
            
            viewModel.selectMedia(1)
            val state = awaitItem()
            assertEquals(1, state.selectedMediaIndex)
        }
    }

    @Test
    fun `selectMedia should not update index if out of bounds`() = runTest(testDispatcher) {
        val mediaFlow = createViewModelWithStateFlow(testMediaList)
        advanceUntilIdle()
        
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertEquals(2, initialState.mediaList.size)
            assertEquals(0, initialState.selectedMediaIndex)
        }
        
        viewModel.selectMedia(5)
        advanceUntilIdle()
        val currentState = viewModel.uiState.value
        assertEquals(0, currentState.selectedMediaIndex)
        assertEquals(2, currentState.mediaList.size)
    }

    @Test
    fun `selectMedia should not update index if negative`() = runTest(testDispatcher) {
        val mediaFlow = createViewModelWithStateFlow(testMediaList)
        advanceUntilIdle()
        
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertEquals(2, initialState.mediaList.size)
            assertEquals(0, initialState.selectedMediaIndex)
        }
        
        viewModel.selectMedia(-1)
        advanceUntilIdle()
        val currentState = viewModel.uiState.value
        assertEquals(0, currentState.selectedMediaIndex)
        assertEquals(2, currentState.mediaList.size)
    }

    @Test
    fun `setPlaying should update isPlaying state`() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertFalse(initialState.isPlaying)
            
            viewModel.setPlaying(true)
            val state = awaitItem()
            assertTrue(state.isPlaying)
        }
    }

    @Test
    fun `deleteMedia should reload media on success`() = runTest(testDispatcher) {
        val mediaToDelete = testMediaList[0]
        val mediaFlow = createViewModelWithStateFlow(testMediaList)
        advanceUntilIdle()
        
        coEvery { deleteMediaUseCase(mediaToDelete) } returns Result.success(Unit)

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertEquals(2, initialState.mediaList.size)
        }
        
        viewModel.deleteMedia(mediaToDelete)
        advanceUntilIdle()
        
        coVerify { deleteMediaUseCase(mediaToDelete) }
        
        val currentState = viewModel.uiState.value
        assertEquals(2, currentState.mediaList.size)
    }

    @Test
    fun `deleteMedia should set error on failure`() = runTest(testDispatcher) {
        val mediaToDelete = testMediaList[0]
        val errorMessage = "Failed to delete media"
        val exception = Exception(errorMessage)

        createViewModel(flowOf(testMediaList))
        advanceUntilIdle()
        coEvery { deleteMediaUseCase(mediaToDelete) } returns Result.failure(exception)

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertEquals(2, initialState.mediaList.size)
            assertNull(initialState.error)
            
            viewModel.deleteMedia(mediaToDelete)
            advanceUntilIdle()
            val state = awaitItem()
            assertEquals(errorMessage, state.error)
        }

        coVerify { deleteMediaUseCase(mediaToDelete) }
    }

    @Test
    fun `clearError should remove error from state`() = runTest(testDispatcher) {
        val mediaToDelete = testMediaList[0]
        val exception = Exception("Test error")

        createViewModel(flowOf(testMediaList))
        advanceUntilIdle()
        coEvery { deleteMediaUseCase(mediaToDelete) } returns Result.failure(exception)

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertEquals(2, initialState.mediaList.size)
            
            viewModel.deleteMedia(mediaToDelete)
            advanceUntilIdle()
            val errorState = awaitItem()
            assertEquals("Test error", errorState.error)
            
            viewModel.clearError()
            val clearedState = awaitItem()
            assertNull(clearedState.error)
        }
    }

    @Test
    fun `state should update when media list changes`() = runTest(testDispatcher) {
        val mediaFlow = createViewModelWithStateFlow(testMediaList)
        advanceUntilIdle()

        viewModel.uiState.test {
            val firstState = awaitItem()
            assertEquals(2, firstState.mediaList.size)
            
            mediaFlow.value = testMediaList.take(1)
            advanceUntilIdle()
            val secondState = awaitItem()
            assertEquals(1, secondState.mediaList.size)
        }
    }
}

