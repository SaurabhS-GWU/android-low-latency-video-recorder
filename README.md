# LowLatencyVideoRecorder
Android MVVM + Clean Architecture - High Performance Video Recording App

A high-performance video recording and gallery viewing utility app built with Android's modern development stack, following Clean Architecture principles and MVVM design pattern.

## Tech Stack

- **Kotlin**: The programming language used for Android development
- **MVVM**: The Model-View-ViewModel architecture pattern
- **Clean Architecture**: A way to organize your code into layers to separate concerns
- **Jetpack Compose**: Modern declarative UI toolkit for building native Android UIs
- **CameraX**: Android library for camera operations and video recording
- **ExoPlayer (Media3)**: For video playback in the gallery
- **Kotlin Coroutines & Flow**: For managing background threads and asynchronous tasks
- **Koin**: Dependency injection framework
- **Navigation Compose**: For navigation between screens
- **Lottie**: For smooth animations
- **MediaStore**: For saving and retrieving media files

## Architecture

This project follows **Clean Architecture** with three main layers:

### Detailed Description of Each Folder:

- **`data/`**: Contains the data layer responsible for fetching and storing data
  - **`repository/`**: Implementation of the repository interface which abstracts data operations
    - `MediaRepositoryImpl.kt`: Handles saving, retrieving, and deleting media files using MediaStore

- **`domain/`**: Contains business logic and the domain layer
  - **`model/`**: Represents business-related data models that are decoupled from data models
    - `MediaItem.kt`: Domain model representing a media item (video)
  - **`repository/`**: Defines interfaces for repositories to interact with data
    - `MediaRepository.kt`: Interface defining media operations
  - **`usecase/`**: Contains use cases that encapsulate specific business logic
    - `SaveVideoUseCase.kt`: Handles saving videos to the gallery
    - `GetAllMediaUseCase.kt`: Retrieves all saved videos
    - `GetLatestMediaUseCase.kt`: Gets the most recently recorded video
    - `DeleteMediaUseCase.kt`: Handles deletion of media files

- **`presentation/`**: The UI layer that handles displaying data to the user
  - **`recording/`**: Recording screen components
    - `RecordingScreen.kt`: Main recording screen with camera preview
    - `RecordingViewModel.kt`: ViewModel for managing recording state
    - `VideoThumbnail.kt`: Component for displaying video thumbnails
  - **`gallery/`**: Gallery screen components
    - `GalleryScreen.kt`: Gallery screen for viewing recorded videos
    - `GalleryViewModel.kt`: ViewModel for managing gallery state
  - **`navigation/`**: Navigation setup
    - `NavGraph.kt`: Navigation graph configuration
    - `Screen.kt`: Screen route definitions

- **`di/`**: Contains the setup for dependency injection using Koin
  - `AppModule.kt`: Koin module defining all dependencies

- **`ui/theme/`**: Material Design 3 theme configuration
  - `Color.kt`: Color scheme definitions
  - `Theme.kt`: Theme composable
  - `Type.kt`: Typography definitions

- **`MainActivity.kt`**: The main activity that initializes the app and sets up navigation
- **`VideoRecorderApplication.kt**: Application class that initializes Koin

---

## Features

- **Video Recording**
  - Real-time camera preview with front and back camera support
  - Low-latency video recording with audio
  - Flashlight toggle (only available for back camera)
  - Recording timer display
  - Lottie animations for start/stop recording
  - Automatic video saving to gallery

- **Gallery Viewing**
  - Swipeable carousel of recorded videos
  - Video playback with ExoPlayer
  - Play/pause controls
  - Preview thumbnail of latest video on recording screen
  - Delete functionality

- **Architecture & Code Quality**
  - Follows **Clean Architecture** with clear separation of concerns
  - Implements **MVVM** design pattern for managing UI-related data
  - **Koin** for dependency injection
  - **Kotlin Coroutines & Flow** for reactive programming
  - Comprehensive unit tests with MockK and Turbine
  - Proper permission handling for camera, audio, and storage

---

## Setup

To set up this project locally, follow these steps:

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd LowLatencyVideoRecorder
   ```

2. **Open in Android Studio:**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

3. **Sync Gradle:**
   - Android Studio will automatically sync Gradle dependencies
   - If not, click "Sync Now" when prompted

4. **Build and Run:**
   - Connect an Android device or start an emulator (API 26+)
   - Click "Run" or press `Shift + F10`
   - Grant necessary permissions when prompted (Camera, Audio, Storage)

5. **Requirements:**
   - Android Studio Hedgehog or later
   - JDK 11 or higher
   - Android SDK 26 (Android 8.0) or higher
   - Target SDK 35

---

## App Recording

### Recording Screen Features

The recording screen provides a comprehensive video recording experience:

1. **Camera Preview**
   - Full-screen camera preview with rounded corners
   - Real-time preview of what will be recorded
   - Supports both front and back cameras

2. **Recording Controls**
   - **Record Button**: Tap to start/stop recording
     - Lottie animation plays when starting recording
     - Different animation when stopping
   - **Camera Switch Button**: Toggle between front and back cameras
   - **Flashlight Button**: Toggle flashlight (only visible for back camera)
   - **Preview Thumbnail**: Shows the latest recorded video thumbnail

3. **Recording Timer**
   - Displays recording duration in `00:00:00` format
   - Red text color for visibility
   - Appears at the top of the preview during recording

4. **Permissions**
   - Camera permission for video recording
   - Audio permission for recording sound
   - Storage permissions for saving videos

### Gallery Screen Features

The gallery screen allows users to view and manage recorded videos:

1. **Video Carousel**
   - Swipeable horizontal pager to browse through videos
   - Smooth transitions between videos

2. **Video Playback**
   - Tap to play/pause videos
   - ExoPlayer for high-quality playback
   - Play/pause button overlay on video

3. **Navigation**
   - Back button to return to recording screen
   - Centered "Gallery" title

4. **UI Design**
   - Black theme throughout
   - Clean, modern interface
   - Status and navigation bars styled to match

---

## Test Results

### Unit Tests

The project includes comprehensive unit tests for ViewModels using:
- **MockK**: For mocking dependencies
- **Turbine**: For testing Kotlin Flows
- **kotlinx-coroutines-test**: For testing coroutines

#### Test Coverage

**RecordingViewModel Tests:**
- ✅ Initial state should have empty media list
- ✅ Start recording should update state
- ✅ Stop recording should update state
- ✅ On video saved should update state with latest media
- ✅ Load latest media should update state

**GalleryViewModel Tests:**
- ✅ Initial state should have empty media list
- ✅ Load media should update state with media list
- ✅ Select media should update selectedMediaIndex
- ✅ Select media should not update index if out of bounds
- ✅ Select media should not update index if negative
- ✅ Set playing should update isPlaying state
- ✅ Delete media should reload media on success
- ✅ Delete media should set error on failure
- ✅ Clear error should remove error from state
- ✅ State should update when media list changes

### Running Tests

To run the unit tests:

```bash
./gradlew test
```

To run tests for a specific class:

```bash
./gradlew test --tests "com.example.lowlatencyvideorecorder.presentation.recording.RecordingViewModelTest"
./gradlew test --tests "com.example.lowlatencyvideorecorder.presentation.gallery.GalleryViewModelTest"
```

### Test Results Summary

All unit tests are passing with good coverage of:
- ViewModel state management
- Use case interactions
- Error handling
- Edge cases (out of bounds, negative indices, etc.)
- Flow emissions and state updates

The test suite ensures:
- Proper state transitions
- Correct error handling
- Repository interactions
- Use case execution
- Flow collection and updates

---

## Project Structure

```
app/src/main/java/com/example/lowlatencyvideorecorder/
├── data/
│   └── repository/
│       └── MediaRepositoryImpl.kt
├── domain/
│   ├── model/
│   │   └── MediaItem.kt
│   ├── repository/
│   │   └── MediaRepository.kt
│   └── usecase/
│       ├── SaveVideoUseCase.kt
│       ├── GetAllMediaUseCase.kt
│       ├── GetLatestMediaUseCase.kt
│       └── DeleteMediaUseCase.kt
├── presentation/
│   ├── recording/
│   │   ├── RecordingScreen.kt
│   │   ├── RecordingViewModel.kt
│   │   └── VideoThumbnail.kt
│   ├── gallery/
│   │   ├── GalleryScreen.kt
│   │   └── GalleryViewModel.kt
│   └── navigation/
│       ├── NavGraph.kt
│       └── Screen.kt
├── di/
│   └── AppModule.kt
├── ui/
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── MainActivity.kt
└── VideoRecorderApplication.kt
```

---

## Dependencies

Key dependencies used in this project:

- **AndroidX Core**: 1.16.0
- **Jetpack Compose**: 2024.09.00
- **CameraX**: 1.4.0
- **Navigation Compose**: 2.8.4
- **ExoPlayer (Media3)**: 1.1.1
- **Koin**: 3.5.6
- **Lottie**: 6.1.0
- **Coroutines**: 1.9.0
- **MockK**: 1.13.10 (testing)
- **Turbine**: 1.0.0 (testing)

---

## License

This project is licensed under the MIT License.

---

## App Recording



https://github.com/user-attachments/assets/6facfe6a-ec65-4fae-967e-da9a25dcadc9


