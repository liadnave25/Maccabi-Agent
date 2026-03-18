package com.example.maccabidailynews.presentation

import com.example.maccabidailynews.domain.model.NewsItem

// אלו שלושת המצבים היחידים שהמסך שלנו יכול להיות בהם
sealed class NewsUiState {
    object Loading : NewsUiState() // מראה ספינר טעינה
    data class Success(val news: List<NewsItem>) : NewsUiState() // מראה את רשימת החדשות
    data class Error(val message: String) : NewsUiState() // מראה הודעת שגיאה
}