package com.example.maccabidailynews.domain.repository

import com.example.maccabidailynews.domain.model.NewsItem
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    // Flow אומר שאנחנו מאזינים לזרם של נתונים - ברגע שסוכן ה-AI בשרת ידחוף כתבה חדשה,
    // האפליקציה תתעדכן אוטומטית בזמן אמת בלי שהמשתמש יצטרך לרענן!
    fun getDailyNews(): Flow<List<NewsItem>>
}