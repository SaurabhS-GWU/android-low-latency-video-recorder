package com.example.lowlatencyvideorecorder.presentation.recording

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.PendingRecording
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.koin.androidx.compose.koinViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.LottieCancellationBehavior
import com.example.lowlatencyvideorecorder.R
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RecordingScreen(
    onNavigateToGallery: () -> Unit,
    viewModel: RecordingViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, true)
            it.statusBarColor = android.graphics.Color.BLACK
            it.navigationBarColor = android.graphics.Color.BLACK
        }
    }

    var hasPermissions by remember {
        mutableStateOf(
            checkPermissions(context)
        )
    }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var preview: Preview? by remember { mutableStateOf(null) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    var recording: Recording? by remember { mutableStateOf(null) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var recordingStartTime by remember { mutableStateOf<Long?>(null) }
    var recordingDuration by remember { mutableStateOf(0L) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        hasPermissions = allGranted
        if (!allGranted) {
            Toast.makeText(context, "Permissions required", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(getRequiredPermissions())
        } else if (cameraProvider == null) {
            cameraProvider = context.getCameraProvider()
        }
    }
    
    LaunchedEffect(hasPermissions) {
        if (hasPermissions && cameraProvider == null) {
            cameraProvider = context.getCameraProvider()
        }
    }

    suspend fun bindCamera(provider: ProcessCameraProvider, facing: Int) {
        try {
            provider.unbindAll()
            kotlinx.coroutines.delay(100)
            
            val previewUseCase = Preview.Builder().build()
            
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    androidx.camera.video.QualitySelector.from(
                        androidx.camera.video.Quality.HIGHEST
                    )
                )
                .build()

            val videoCaptureUseCase = VideoCapture.Builder(recorder)
                .build()
                .also {
                    videoCapture = it
                }

            val imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(facing)
                .build()

            val cameraInstance = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                previewUseCase,
                videoCaptureUseCase,
                imageCapture
            )
            camera = cameraInstance
            preview = previewUseCase
            
            previewView?.let { view ->
                previewUseCase.setSurfaceProvider(view.surfaceProvider)
            }
            
            cameraInstance.cameraControl.enableTorch(isFlashOn)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(lensFacing, cameraProvider) {
        cameraProvider?.let { provider ->
            bindCamera(provider, lensFacing)
        }
    }
    
    LaunchedEffect(previewView, preview) {
        previewView?.let { view ->
            preview?.setSurfaceProvider(view.surfaceProvider)
        }
    }

    LaunchedEffect(uiState.isRecording) {
        if (uiState.isRecording) {
            recordingStartTime = System.currentTimeMillis()
            recordingDuration = 0L
        } else {
            recordingStartTime = null
            recordingDuration = 0L
            if (recording != null) {
                recording?.stop()
                recording = null
            }
        }
    }
    
    LaunchedEffect(uiState.isRecording, recordingStartTime) {
        while (uiState.isRecording && recordingStartTime != null) {
            recordingDuration = System.currentTimeMillis() - recordingStartTime!!
            kotlinx.coroutines.delay(100)
        }
    }

    LaunchedEffect(isFlashOn) {
        camera?.cameraControl?.enableTorch(isFlashOn)
    }

    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    val statusBarPadding = systemBarsPadding.calculateTopPadding()
    val navigationBarPadding = systemBarsPadding.calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusBarPadding + 16.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.sanas_logo),
                contentDescription = "Sanas Logo",
                modifier = Modifier.size(120.dp, 40.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = statusBarPadding + 80.dp,
                    bottom = 120.dp,
                    start = 16.dp,
                    end = 16.dp
                )
        ) {
            if (hasPermissions) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewView = this
                            preview?.setSurfaceProvider(surfaceProvider)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    update = { view ->
                        previewView = view
                        preview?.setSurfaceProvider(view.surfaceProvider)
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = "Camera permissions required",
                        color = Color.White
                    )
                }
            }

            IconButton(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.switch_camera),
                    contentDescription = "Switch Camera",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                IconButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    onClick = {
                        isFlashOn = !isFlashOn
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (isFlashOn) R.drawable.zap else R.drawable.zap_off
                        ),
                        contentDescription = "Flashlight",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        if (uiState.isRecording) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(
                        top = statusBarPadding + 80.dp + 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    )
                    .size(width = 92.dp, height = 27.dp)
                    .background(Color.Red, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatDuration(recordingDuration),
                    color = Color.White,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                )
            }
        }

        var shouldPlayStartAnimation by remember { mutableStateOf(false) }
        var shouldPlayStopAnimation by remember { mutableStateOf(false) }
        var previousRecordingState by remember { mutableStateOf(false) }
        
        LaunchedEffect(uiState.isRecording) {
            if (uiState.isRecording && !previousRecordingState) {
                shouldPlayStartAnimation = true
                shouldPlayStopAnimation = false
                delay(350)
                shouldPlayStartAnimation = false
            } else if (!uiState.isRecording && previousRecordingState) {
                shouldPlayStopAnimation = true
                shouldPlayStartAnimation = false
                delay(350)
                shouldPlayStopAnimation = false
            }
            previousRecordingState = uiState.isRecording
        }
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navigationBarPadding + 32.dp)
                .clickable {
                    if (uiState.isRecording) {
                        viewModel.stopRecording()
                    } else {
                        startRecording(
                            context = context,
                            videoCapture = videoCapture,
                            onRecordingStarted = { rec ->
                                recording = rec
                                viewModel.startRecording()
                            },
                            onVideoSaved = { uri, path ->
                                viewModel.onVideoSaved(uri, path)
                            }
                        )
                    }
                }
        ) {
            val startComposition by rememberLottieComposition(
                LottieCompositionSpec.Asset("record_button.json")
            )
            val stopComposition by rememberLottieComposition(
                LottieCompositionSpec.Asset("record_button_stop.json")
            )
            
            if (shouldPlayStopAnimation) {
                LottieAnimation(
                    composition = stopComposition,
                    iterations = 1,
                    modifier = Modifier.size(64.dp),
                    isPlaying = shouldPlayStopAnimation
                )
            } else {
                LottieAnimation(
                    composition = startComposition,
                    iterations = 1,
                    modifier = Modifier.size(64.dp),
                    isPlaying = shouldPlayStartAnimation
                )
            }
        }

        uiState.latestMedia?.let { media ->
            if (media.isVideo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = navigationBarPadding + 32.dp, end = 32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                            .clickable { onNavigateToGallery() }
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        VideoThumbnail(
                            videoUri = media.uri,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        }


    }
}

private fun checkPermissions(context: android.content.Context): Boolean {
    val permissions = getRequiredPermissions()
    return permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

private fun getRequiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}

private fun formatDuration(milliseconds: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

private suspend fun android.content.Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { future ->
            future.addListener(
                {
                    continuation.resume(future.get())
                },
                ContextCompat.getMainExecutor(this)
            )
        }
    }

private fun startRecording(
    context: android.content.Context,
    videoCapture: VideoCapture<Recorder>?,
    onRecordingStarted: (Recording) -> Unit,
    onVideoSaved: (String, String) -> Unit
) {
    val hasAudioPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
    
    val hasCameraPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
    
    if (!hasCameraPermission) {
        android.util.Log.e("Recording", "Camera permission not granted")
        return
    }
    
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
        .format(System.currentTimeMillis())
    val contentValues = android.content.ContentValues().apply {
        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, "Movies/LowLatencyVideoRecorder")
        }
    }

    val mediaStoreOutputOptions = androidx.camera.video.MediaStoreOutputOptions.Builder(
        context.contentResolver,
        android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    )
        .setContentValues(contentValues)
        .build()

    try {
        val recordingBuilder = videoCapture?.output
            ?.prepareRecording(context, mediaStoreOutputOptions)
        
        val pendingRecording = if (hasAudioPermission) {
            recordingBuilder?.withAudioEnabled()
        } else {
            recordingBuilder
        }
        
        val activeRecording: Recording? = pendingRecording?.start(
            ContextCompat.getMainExecutor(context)
        ) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {}
                is VideoRecordEvent.Finalize -> {
                    if (event.hasError()) {
                        android.util.Log.e("Recording", "Video recording failed", event.cause)
                    } else {
                        val uri = event.outputResults.outputUri
                        android.util.Log.d("Recording", "Video saved to: $uri")
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            kotlinx.coroutines.delay(500)
                            onVideoSaved(uri.toString(), uri.toString())
                        }
                    }
                }
                else -> {}
            }
        }

        activeRecording?.let { onRecordingStarted(it) }
    } catch (e: SecurityException) {
        android.util.Log.e("Recording", "SecurityException: Permission denied", e)
    } catch (e: Exception) {
        android.util.Log.e("Recording", "Error starting recording", e)
    }
}

