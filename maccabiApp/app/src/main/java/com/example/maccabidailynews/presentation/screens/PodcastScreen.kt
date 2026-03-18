package com.example.maccabidailynews.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.maccabidailynews.presentation.AudioPlayerViewModel
import com.example.maccabidailynews.ui.theme.BackgroundLightGray
import com.example.maccabidailynews.ui.theme.MaccabiDeepBlue
import com.example.maccabidailynews.ui.theme.MaccabiSoftYellow
import kotlinx.coroutines.delay

@Composable
fun PodcastScreen(audioViewModel: AudioPlayerViewModel) {
    // משיכת הנתונים מה-ViewModel
    val isPlaying by audioViewModel.isPlaying.collectAsState()
    val currentTitle by audioViewModel.currentTitle.collectAsState()
    val player = audioViewModel.player

    // משתנים מקומיים שישמרו את זמן הניגון הנוכחי והזמן הכולל
    var currentPosition by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(0L) }

    // לולאה שרצה ברקע רק כשהפודקאסט מתנגן, ומעדכנת את הסרגל כל שנייה
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = player.currentPosition
            totalDuration = player.duration.coerceAtLeast(0L)
            delay(1000L)
        }
    }

    // פונקציית עזר להמרת מילישניות לפורמט של דקות ושניות (למשל 01:25)
    fun formatTime(ms: Long): String {
        if (ms < 0) return "00:00"
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLightGray)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // תמונת הקאבר המרכזית (פלייסחולדר עד שיהיה AI)
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaccabiDeepBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Podcasts,
                contentDescription = "Podcast Cover",
                tint = MaccabiSoftYellow,
                modifier = Modifier.size(120.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // כותרת הפודקאסט
        Text(
            text = currentTitle,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaccabiDeepBlue
        )

        Spacer(modifier = Modifier.height(32.dp))

        // סרגל ההתקדמות (Slider)
        val sliderPosition = if (totalDuration > 0) (currentPosition.toFloat() / totalDuration.toFloat()) else 0f
        Slider(
            value = sliderPosition,
            onValueChange = { newPosition ->
                // כשהמשתמש גורר את הסרגל, אנחנו מעבירים את הנגן לזמן החדש
                val newTime = (newPosition * totalDuration).toLong()
                player.seekTo(newTime)
                currentPosition = newTime
            },
            colors = SliderDefaults.colors(
                thumbColor = MaccabiSoftYellow,
                activeTrackColor = MaccabiDeepBlue,
                inactiveTrackColor = Color.LightGray
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // טקסטים של זמן (מימין לשמאל: זמן כולל וזמן נוכחי)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatTime(currentPosition), color = Color.Gray, fontSize = 14.sp)
            Text(text = formatTime(totalDuration), color = Color.Gray, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // שורת כפתורי השליטה
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // אחורה 10 שניות
            IconButton(
                onClick = { player.seekTo((player.currentPosition - 10000).coerceAtLeast(0)) },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FastRewind,
                    contentDescription = "אחורה 10 שניות",
                    tint = MaccabiDeepBlue,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // כפתור Play / Pause ענק
            IconButton(
                onClick = { audioViewModel.togglePlayPause() },
                modifier = Modifier.size(96.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                    contentDescription = "Play/Pause",
                    tint = MaccabiSoftYellow,
                    modifier = Modifier.size(96.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // קדימה 10 שניות
            IconButton(
                onClick = { player.seekTo((player.currentPosition + 10000).coerceAtMost(player.duration)) },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FastForward,
                    contentDescription = "קדימה 10 שניות",
                    tint = MaccabiDeepBlue,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}