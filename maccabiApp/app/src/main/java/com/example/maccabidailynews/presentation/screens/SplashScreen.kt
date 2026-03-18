package com.example.maccabidailynews.presentation.screens

import androidx.compose.foundation.Image
import com.example.maccabidailynews.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.maccabidailynews.ui.theme.MaccabiDeepBlue
import com.example.maccabidailynews.ui.theme.MaccabiSoftYellow
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToNews: () -> Unit) {
    // ה-LaunchedEffect מריץ את הקוד שבתוכו פעם אחת ברגע שהמסך עולה
    LaunchedEffect(key1 = true) {
        delay(2500) // ממתינים 2.5 שניות (2500 מילישניות)
        onNavigateToNews() // מפעילים את פונקציית המעבר למסך החדשות
    }

    // ה-Box מאפשר לנו למרכז דברים על המסך ולתת רקע מלא
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaccabiDeepBlue), // רקע כחול עמוק ויוקרתי
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // זה הפלייסחולדר ללוגו שלך
            Image(
                painter = painterResource(id = R.drawable.maccabi_logo),
                contentDescription = "Maccabi Logo",
                modifier = Modifier.size(150.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // טקסט מתחת ללוגו
            Text(
                text = "Maccabi Daily",
                color = MaccabiSoftYellow,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}