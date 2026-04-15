package com.example.maccabidailynews.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.maccabidailynews.presentation.components.MiniPlayer
import com.example.maccabidailynews.presentation.navigation.Screen
import com.example.maccabidailynews.presentation.screens.ContactScreen
import com.example.maccabidailynews.presentation.screens.NewsScreen
import com.example.maccabidailynews.presentation.screens.PodcastScreen
import com.example.maccabidailynews.presentation.screens.SplashScreen
import com.example.maccabidailynews.ui.theme.MaccabiDeepBlue
import com.example.maccabidailynews.ui.theme.MaccabiSoftYellow

@Composable
fun MainScreen(audioViewModel: AudioPlayerViewModel = viewModel()) {
    val navController = rememberNavController()
    // רשימת המסכים שיופיעו בסרגל התחתון
    val bottomNavItems = listOf(Screen.News, Screen.Podcast, Screen.Contact)

    // בדיקה באיזה מסך אנחנו נמצאים עכשיו
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // נראה את הסרגל התחתון רק אם אנחנו לא במסך הפתיחה
            if (currentRoute != Screen.Splash.route) {
                // עוטפים בעמודה כדי שהנגן המרחף יישב בדיוק מעל סרגל הניווט
                Column {
                    // הצגת הנגן המרחף רק אם אנחנו לא במסך הפודקאסט המלא
                    if (currentRoute != Screen.Podcast.route) {
                        MiniPlayer(
                            audioViewModel = audioViewModel,
                            onNavigateToPodcast = {
                                navController.navigate(Screen.Podcast.route) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    NavigationBar(
                        containerColor = MaccabiDeepBlue,
                        contentColor = MaccabiSoftYellow,
                        tonalElevation = 8.dp
                    ) {
                        bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(imageVector = screen.icon!!, contentDescription = screen.title) },
                                label = { Text(text = screen.title!!) },
                                selected = currentRoute == screen.route,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaccabiDeepBlue,
                                    unselectedIconColor = MaccabiSoftYellow.copy(alpha = 0.6f),
                                    selectedTextColor = MaccabiSoftYellow,
                                    indicatorColor = MaccabiSoftYellow
                                ),
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        // כאן יושבים המסכים עצמם
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route, // מתחילים ממסך הפתיחה
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToNews = {
                        navController.navigate(Screen.News.route) {
                            // ה-popUpTo מבטיח שנסגור את מסך הפתיחה לגמרי, כדי שאם
                            // המשתמש ילחץ על כפתור "אחורה" בטלפון, הוא יצא מהאפליקציה ולא יחזור ללוגו
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.News.route) { NewsScreen() }
            composable(Screen.Podcast.route) { PodcastScreen(audioViewModel = audioViewModel) }
            composable(Screen.Contact.route) { ContactScreen() }
        }
    }
}