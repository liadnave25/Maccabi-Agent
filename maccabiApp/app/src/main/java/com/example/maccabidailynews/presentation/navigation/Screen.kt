package com.example.maccabidailynews.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector

// מגדיר את כל המסכים האפשריים באפליקציה
sealed class Screen(val route: String, val title: String? = null, val icon: ImageVector? = null) {
    object Splash : Screen("splash_screen")
    object News : Screen("news_screen", "חדשות", Icons.Default.Home)
    object Podcast : Screen("podcast_screen", "פודקאסט", Icons.Default.PlayArrow)
    object Contact : Screen("contact_screen", "צור קשר", Icons.Default.Email)
}