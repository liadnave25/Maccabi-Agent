package com.example.maccabidailynews.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.maccabidailynews.R
import com.example.maccabidailynews.presentation.NewsUiState
import com.example.maccabidailynews.presentation.NewsViewModel
import com.example.maccabidailynews.presentation.components.NewsCard
import com.example.maccabidailynews.ui.theme.BackgroundLightGray
import com.example.maccabidailynews.ui.theme.MaccabiDeepBlue
import com.example.maccabidailynews.ui.theme.MaccabiSoftYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(viewModel: NewsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedSport by remember { mutableStateOf("כדורסל") }
    val tabs = listOf("כדורסל", "כדורגל")
    val tabIcons = listOf(Icons.Default.SportsBasketball, Icons.Default.SportsSoccer)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLightGray)
    ) {
        // Top App Bar — force LTR regardless of device locale
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.maccabi_logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Maccabi Daily",
                            color = MaccabiSoftYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaccabiDeepBlue
                )
            )
        }

        // Sport tabs with icons
        TabRow(
            selectedTabIndex = tabs.indexOf(selectedSport),
            containerColor = MaccabiDeepBlue,
            contentColor = MaccabiSoftYellow
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSport == title,
                    onClick = { selectedSport = title },
                    icon = {
                        Icon(
                            imageVector = tabIcons[index],
                            contentDescription = title,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    text = { Text(title, fontWeight = FontWeight.Bold) },
                    selectedContentColor = MaccabiSoftYellow,
                    unselectedContentColor = MaccabiSoftYellow.copy(alpha = 0.5f)
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (val state = uiState) {
                is NewsUiState.Loading -> {
                    ShimmerNewsList()
                }
                is NewsUiState.Error -> {
                    Text(text = state.message, color = MaccabiDeepBlue)
                }
                is NewsUiState.Success -> {
                    val filteredNews = state.news.filter { it.sport_type == selectedSport }

                    if (filteredNews.isEmpty()) {
                        // Styled empty state
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (selectedSport == "כדורסל")
                                    Icons.Default.SportsBasketball
                                else
                                    Icons.Default.SportsSoccer,
                                contentDescription = null,
                                tint = MaccabiDeepBlue.copy(alpha = 0.25f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "אין כרגע חדשות עבור $selectedSport",
                                color = MaccabiDeepBlue.copy(alpha = 0.5f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredNews) { newsItem ->
                                NewsCard(news = newsItem)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShimmerNewsList() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaccabiDeepBlue.copy(alpha = 0.06f),
            MaccabiDeepBlue.copy(alpha = 0.14f),
            MaccabiDeepBlue.copy(alpha = 0.06f)
        ),
        start = Offset(shimmerOffset, 0f),
        end = Offset(shimmerOffset + 300f, 300f)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(3) {
            ShimmerCard(brush = shimmerBrush)
        }
    }
}

@Composable
private fun ShimmerCard(brush: Brush) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(brush)
            )
        }
    }
}
