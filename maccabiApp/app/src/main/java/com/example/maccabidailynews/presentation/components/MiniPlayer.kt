package com.example.maccabidailynews.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.maccabidailynews.presentation.AudioPlayerViewModel
import com.example.maccabidailynews.ui.theme.MaccabiDeepBlue
import com.example.maccabidailynews.ui.theme.MaccabiSoftYellow

@Composable
fun MiniPlayer(
    audioViewModel: AudioPlayerViewModel,
    onNavigateToPodcast: () -> Unit // פונקציה שתקפיץ אותנו למסך הפודקאסט המלא בלחיצה
) {
    // מאזינים למצבי הנגן (האם מנגן עכשיו? מה השם?)
    val isPlaying by audioViewModel.isPlaying.collectAsState()
    val currentTitle by audioViewModel.currentTitle.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(MaccabiSoftYellow) // צהוב יוקרתי כדי שיבלוט
            .clickable { onNavigateToPodcast() } // לחיצה על כל הנגן תפתח את המסך המלא
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // טקסט עם שם הפודקאסט
        Text(
            text = currentTitle,
            color = MaccabiDeepBlue,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )

        // כפתור פליי / פאוז
        IconButton(onClick = { audioViewModel.togglePlayPause() }) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause",
                tint = MaccabiDeepBlue,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}