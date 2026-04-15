package com.example.maccabidailynews.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.maccabidailynews.presentation.AudioPlayerViewModel
import com.example.maccabidailynews.ui.theme.MaccabiDeepBlue
import com.example.maccabidailynews.ui.theme.MaccabiSoftYellow
import kotlinx.coroutines.delay

@Composable
fun MiniPlayer(
    audioViewModel: AudioPlayerViewModel,
    onNavigateToPodcast: () -> Unit
) {
    val isPlaying by audioViewModel.isPlaying.collectAsState()
    val currentTitle by audioViewModel.currentTitle.collectAsState()
    val player = audioViewModel.player

    // Track progress for the thin progress bar
    var currentPosition by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(0L) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = player.currentPosition
            totalDuration = player.duration.coerceAtLeast(0L)
            delay(1000L)
        }
    }

    val progress = if (totalDuration > 0) currentPosition.toFloat() / totalDuration.toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp)
            .background(MaccabiSoftYellow)
            .clickable { onNavigateToPodcast() }
    ) {
        // Thin progress bar at the very top
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = MaccabiDeepBlue,
            trackColor = MaccabiDeepBlue.copy(alpha = 0.2f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Small thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaccabiDeepBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Podcasts,
                    contentDescription = null,
                    tint = MaccabiSoftYellow,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Episode title (ellipsis on overflow)
            Text(
                text = currentTitle,
                color = MaccabiDeepBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Play / Pause button
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
}
