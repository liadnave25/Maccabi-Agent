package com.example.maccabidailynews // (ה-package שלך)

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import com.example.maccabidailynews.presentation.MainScreen
import com.example.maccabidailynews.ui.theme.MaccabiDailyNewsTheme // (העיצוב שלך)

@AndroidEntryPoint // <-- קריטי כדי ש-Hilt יעבוד!
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaccabiDailyNewsTheme { // תשאיר פה את מה שהיה לך
                // אובייקט Surface שנותן רקע על כל המסך
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // כאן קוראים למערכת הניווט שלנו!
                    MainScreen()
                }
            }
        }
    }
}