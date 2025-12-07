package com.example.lowlatencyvideorecorder.presentation.navigation

sealed class Screen(val route: String) {
    object Recording : Screen("recording")
    object Gallery : Screen("gallery")
}

