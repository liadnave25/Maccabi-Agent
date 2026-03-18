package com.example.maccabidailynews.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.maccabidailynews.presentation.NewsUiState
import com.example.maccabidailynews.presentation.NewsViewModel
import com.example.maccabidailynews.presentation.components.NewsCard
import com.example.maccabidailynews.ui.theme.BackgroundLightGray
import com.example.maccabidailynews.ui.theme.MaccabiDeepBlue
import com.example.maccabidailynews.ui.theme.MaccabiSoftYellow

@Composable
fun NewsScreen(viewModel: NewsViewModel = hiltViewModel()) {
    // מאזינים למצב הנתונים מה-ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // שומרים את מצב הטאב הנבחר (ברירת מחדל: כדורסל)
    var selectedSport by remember { mutableStateOf("כדורסל") }
    val tabs = listOf("כדורסל", "כדורגל")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLightGray)
    ) {
        // סרגל הניווט העליון (Tabs)
        TabRow(
            selectedTabIndex = tabs.indexOf(selectedSport),
            containerColor = MaccabiDeepBlue,
            contentColor = MaccabiSoftYellow
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSport == title,
                    onClick = { selectedSport = title },
                    text = { Text(title, fontWeight = FontWeight.Bold) },
                    selectedContentColor = MaccabiSoftYellow,
                    unselectedContentColor = MaccabiSoftYellow.copy(alpha = 0.5f)
                )
            }
        }

        // הצגת התוכן לפי המצב (Loading / Success / Error)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (val state = uiState) {
                is NewsUiState.Loading -> {
                    CircularProgressIndicator(color = MaccabiDeepBlue)
                }
                is NewsUiState.Error -> {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
                is NewsUiState.Success -> {
                    // סינון הרשימה לפי הספורט שנבחר בסרגל
                    val filteredNews = state.news.filter { it.sport_type == selectedSport }

                    if (filteredNews.isEmpty()) {
                        Text("אין כרגע חדשות עבור $selectedSport", color = MaccabiDeepBlue)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp) // רווח בין הכרטיסיות
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