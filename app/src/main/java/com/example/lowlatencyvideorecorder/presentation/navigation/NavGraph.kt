package com.example.lowlatencyvideorecorder.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.lowlatencyvideorecorder.presentation.gallery.GalleryScreen
import com.example.lowlatencyvideorecorder.presentation.recording.RecordingScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Recording.route
    ) {
        composable(Screen.Recording.route) {
            RecordingScreen(
                onNavigateToGallery = {
                    navController.navigate(Screen.Gallery.route) {
                        popUpTo(Screen.Recording.route) { inclusive = false }
                    }
                }
            )
        }
        composable(Screen.Gallery.route) {
            GalleryScreen(
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}

