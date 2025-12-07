package com.example.lowlatencyvideorecorder.presentation.gallery

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import com.example.lowlatencyvideorecorder.R
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import org.koin.androidx.compose.koinViewModel
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onNavigateBack: () -> Unit,
    viewModel: GalleryViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val uiState by viewModel.uiState.collectAsState()
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, true)
            it.statusBarColor = android.graphics.Color.BLACK
            it.navigationBarColor = android.graphics.Color.BLACK
        }
    }
    var isLoadingVideo by remember { mutableStateOf(false) }
    var videoError by remember { mutableStateOf<String?>(null) }
    var isPlayerPlaying by remember { mutableStateOf(false) }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : androidx.media3.common.Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    videoError = error.message
                    android.util.Log.e("GalleryScreen", "ExoPlayer error", error)
                }
                
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isPlayerPlaying = isPlaying
                }
                
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    isPlayerPlaying = isPlaying
                    viewModel.setPlaying(isPlaying)
                }
            })
        }
    }

    LaunchedEffect(uiState.selectedMediaIndex, uiState.mediaList) {
        if (uiState.mediaList.isNotEmpty() && uiState.selectedMediaIndex < uiState.mediaList.size) {
            val selectedMedia = uiState.mediaList[uiState.selectedMediaIndex]
            isLoadingVideo = true
            videoError = null
            try {
                exoPlayer.apply {
                    stop()
                    clearMediaItems()
                    val uri = Uri.parse(selectedMedia.uri)
                    android.util.Log.d("GalleryScreen", "Loading video: $uri")
                    
                    try {
                        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                            android.util.Log.d("GalleryScreen", "Video file is accessible")
                        } ?: run {
                            throw Exception("Video file not accessible or not found")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("GalleryScreen", "Cannot access video file: $uri", e)
                        throw Exception("Video file not accessible: ${e.message}")
                    }
                    
                    val mediaItem = ExoMediaItem.fromUri(uri)
                    addMediaItem(mediaItem)
                    prepare()
                    playWhenReady = false
                }
                isLoadingVideo = false
            } catch (e: Exception) {
                android.util.Log.e("GalleryScreen", "Error loading video: ${selectedMedia.uri}", e)
                videoError = e.message ?: "Failed to load video"
                isLoadingVideo = false
            }
        }
    }

    LaunchedEffect(uiState.isPlaying) {
        if (uiState.isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "Gallery",
                        modifier = Modifier.size(width = 68.dp, height = 24.dp),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
            )
        }
    ) { paddingValues ->
        if (uiState.mediaList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No videos found")
            }
        } else {
            val pagerState = rememberPagerState(
                initialPage = uiState.selectedMediaIndex,
                pageCount = { uiState.mediaList.size }
            )

            LaunchedEffect(pagerState.currentPage) {
                viewModel.selectMedia(pagerState.currentPage)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val media = uiState.mediaList[page]
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (page == pagerState.currentPage) {
                            Box(
                                modifier = Modifier.size(width = 402.dp, height = 587.dp)
                            ) {
                                if (videoError != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black),
                                        contentAlignment = Alignment.Center
                                    ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "Error loading video",
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = videoError ?: "",
                                                    color = Color.Gray,
                                                    modifier = Modifier.padding(top = 8.dp)
                                                )
                                            }
                                    }
                                } else {
                                    AndroidView(
                                        factory = { ctx ->
                                            PlayerView(ctx).apply {
                                                player = exoPlayer
                                                useController = false // Disable default controls
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                            update = { view ->
                                                if (view.player != exoPlayer) {
                                                view.player = exoPlayer
                                            }
                                        }
                                    )
                                }

                                val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
                                val bottomPadding = systemBarsPadding.calculateBottomPadding()
                                
                                IconButton(
                                    onClick = {
                                        if (isPlayerPlaying) {
                                            viewModel.setPlaying(false)
                                            exoPlayer.pause()
                                        } else {
                                            viewModel.setPlaying(true)
                                            exoPlayer.play()
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = bottomPadding + 16.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (isPlayerPlaying) R.drawable.pause else R.drawable.play
                                        ),
                                        contentDescription = if (isPlayerPlaying) "Pause" else "Play",
                                        modifier = Modifier.size(64.dp),
                                        tint = Color.White
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

