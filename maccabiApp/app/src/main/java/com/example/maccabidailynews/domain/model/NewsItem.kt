package com.example.maccabidailynews.domain.model

data class NewsItem(
    val title: String = "",        // כותרת הכתבה
    val content: String = "",      // תקציר הכתבה
    val source: String = "",       // מקור (למשל: ערוץ הספורט, One)
    val reliability: String = "",  // ציון אמינות (למשל: CONFIRMED או RUMOR)
    val link: String = "",         // קישור לכתבה המלאה
    val sport_type: String = "כדורסל", // "כדורסל" או "כדורגל"
    val date: String = "",
    val audio_url: String = ""
)