package com.example.maccabidailynews.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import com.example.maccabidailynews.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.maccabidailynews.ui.theme.MaccabiDeepBlue
import com.example.maccabidailynews.ui.theme.MaccabiSoftYellow
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToNews: () -> Unit) {
    LaunchedEffect(key1 = true) {
        delay(2800)
        onNavigateToNews()
    }

    // Logo: fade in + bounce scale
    var logoVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { logoVisible = true }

    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(700, easing = EaseOut),
        label = "logoAlpha"
    )
    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.7f,
        animationSpec = tween(700, easing = EaseOutBounce),
        label = "logoScale"
    )

    // Title + tagline: fade in after logo
    var showText by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(400)
        showText = true
    }
    val textAlpha by animateFloatAsState(
        targetValue = if (showText) 1f else 0f,
        animationSpec = tween(600),
        label = "textAlpha"
    )

    // Pulsing dots
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500), repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(0)
        ), label = "dot1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500), repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(170)
        ), label = "dot2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500), repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(340)
        ), label = "dot3"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaccabiDeepBlue,
                        MaccabiDeepBlue.copy(alpha = 0.82f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.maccabi_logo),
                contentDescription = "Maccabi Logo",
                modifier = Modifier
                    .size(150.dp)
                    .scale(logoScale)
                    .alpha(logoAlpha)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Maccabi Daily",
                color = MaccabiSoftYellow,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(textAlpha)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "חדשות | פודקאסטים | ספורט",
                color = MaccabiSoftYellow.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.alpha(textAlpha)
            )
        }

        // Pulsing loading dots at the bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(dot1Alpha, dot2Alpha, dot3Alpha).forEach { dotAlpha ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaccabiSoftYellow.copy(alpha = dotAlpha))
                )
            }
        }
    }
}
