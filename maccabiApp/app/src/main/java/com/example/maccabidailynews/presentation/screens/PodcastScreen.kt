package com.example.maccabidailynews.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.maccabidailynews.presentation.AudioPlayerViewModel
import com.example.maccabidailynews.ui.theme.MaccabiDeepBlue
import com.example.maccabidailynews.ui.theme.MaccabiSoftYellow
import kotlinx.coroutines.delay

@Composable
fun PodcastScreen(audioViewModel: AudioPlayerViewModel) {
    val isPlaying by audioViewModel.isPlaying.collectAsState()
    val currentTitle by audioViewModel.currentTitle.collectAsState()
    val player = audioViewModel.player

    var currentPosition by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(0L) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    val speedOptions = listOf(0.75f, 1.0f, 1.25f, 1.5f)

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = player.currentPosition
            totalDuration = player.duration.coerceAtLeast(0L)
            delay(1000L)
        }
    }

    fun formatTime(ms: Long): String {
        if (ms < 0) return "00:00"
        val totalSeconds = ms / 1000
        return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8EDF5),
                        Color(0xFFFFFFFF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Cover art card with golden ring border
            Card(
                modifier = Modifier.size(280.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                border = BorderStroke(3.dp, MaccabiSoftYellow),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaccabiDeepBlue.copy(alpha = 0.85f),
                                    MaccabiDeepBlue
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Podcasts,
                        contentDescription = "Podcast Cover",
                        tint = MaccabiSoftYellow,
                        modifier = Modifier.size(110.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Episode title
            Text(
                text = currentTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaccabiDeepBlue
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Progress slider
            val sliderPosition = if (totalDuration > 0) currentPosition.toFloat() / totalDuration.toFloat() else 0f
            Slider(
                value = sliderPosition,
                onValueChange = { newPos ->
                    val newTime = (newPos * totalDuration).toLong()
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatTime(currentPosition), color = Color.Gray, fontSize = 13.sp)
                Text(text = formatTime(totalDuration), color = Color.Gray, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Playback controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Rewind button with subtle circle background
                Surface(
                    shape = CircleShape,
                    color = MaccabiDeepBlue.copy(alpha = 0.08f),
                    modifier = Modifier.size(56.dp)
                ) {
                    IconButton(
                        onClick = { player.seekTo((player.currentPosition - 10000).coerceAtLeast(0)) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "אחורה 10 שניות",
                            tint = MaccabiDeepBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Play/Pause on dark circle
                Surface(
                    shape = CircleShape,
                    color = MaccabiDeepBlue,
                    modifier = Modifier.size(80.dp)
                ) {
                    IconButton(
                        onClick = { audioViewModel.togglePlayPause() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                            contentDescription = "Play/Pause",
                            tint = MaccabiSoftYellow,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Forward button with subtle circle background
                Surface(
                    shape = CircleShape,
                    color = MaccabiDeepBlue.copy(alpha = 0.08f),
                    modifier = Modifier.size(56.dp)
                ) {
                    IconButton(
                        onClick = { player.seekTo((player.currentPosition + 10000).coerceAtMost(player.duration)) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "קדימה 10 שניות",
                            tint = MaccabiDeepBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Speed chip
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaccabiDeepBlue.copy(alpha = 0.08f),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
            ) {
                TextButton(
                    onClick = {
                        val nextIndex = (speedOptions.indexOf(playbackSpeed) + 1) % speedOptions.size
                        playbackSpeed = speedOptions[nextIndex]
                        player.setPlaybackSpeed(playbackSpeed)
                    }
                ) {
                    Text(
                        text = "${if (playbackSpeed == playbackSpeed.toLong().toFloat()) playbackSpeed.toInt() else playbackSpeed}x",
                        color = MaccabiDeepBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
