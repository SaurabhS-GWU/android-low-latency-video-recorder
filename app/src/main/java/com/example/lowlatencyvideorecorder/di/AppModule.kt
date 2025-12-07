package com.example.lowlatencyvideorecorder.di

import android.content.Context
import com.example.lowlatencyvideorecorder.data.repository.MediaRepositoryImpl
import com.example.lowlatencyvideorecorder.domain.repository.MediaRepository
import com.example.lowlatencyvideorecorder.domain.usecase.DeleteMediaUseCase
import com.example.lowlatencyvideorecorder.domain.usecase.GetAllMediaUseCase
import com.example.lowlatencyvideorecorder.domain.usecase.GetLatestMediaUseCase
import com.example.lowlatencyvideorecorder.domain.usecase.SaveVideoUseCase
import com.example.lowlatencyvideorecorder.presentation.gallery.GalleryViewModel
import com.example.lowlatencyvideorecorder.presentation.recording.RecordingViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<MediaRepository> { MediaRepositoryImpl(androidContext()) }
    
    factory { SaveVideoUseCase(get()) }
    factory { GetAllMediaUseCase(get()) }
    factory { GetLatestMediaUseCase(get()) }
    factory { DeleteMediaUseCase(get()) }
    
    viewModel { RecordingViewModel() }
    viewModel { GalleryViewModel() }
}

