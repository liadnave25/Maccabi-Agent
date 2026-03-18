package com.example.maccabidailynews.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.maccabidailynews.domain.model.NewsItem
import com.example.maccabidailynews.ui.theme.MaccabiDeepBlue
import com.example.maccabidailynews.ui.theme.SurfaceWhite

@Composable
fun NewsCard(news: NewsItem) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // לחיצה תפתח את הכתבה בדפדפן
                if (news.link.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(news.link))
                    context.startActivity(intent)
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // שורה עליונה: מקור הכתבה
            Text(
                text = news.source,
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // כותרת הכתבה
            Text(
                text = news.title,
                color = MaccabiDeepBlue,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // תקציר הכתבה
            Text(
                text = news.content,
                color = Color.DarkGray,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // שורה תחתונה: אינדיקטור אמינות בצד שמאל
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End, // דוחף את התוכן שמאלה (מכיוון שהמכשיר בעברית)
                verticalAlignment = Alignment.CenterVertically
            ) {
                // פיענוח רמת האמינות לטקסט וצבע
                val reliabilityUpper = news.reliability.uppercase()
                val (indicatorColor, reliabilityText) = when {
                    reliabilityUpper.contains("CONFIRMED") || reliabilityUpper.contains("HIGH") ->
                        Pair(Color(0xFF4CAF50), "דיווח מאומת") // ירוק
                    reliabilityUpper.contains("RUMOR") || reliabilityUpper.contains("MEDIUM") ->
                        Pair(Color(0xFFFF9800), "שמועה") // כתום
                    reliabilityUpper.contains("FAKE") || reliabilityUpper.contains("LOW") ->
                        Pair(Color(0xFFF44336), "אמינות נמוכה") // אדום
                    else ->
                        Pair(Color.Gray, "לא ידוע") // ברירת מחדל אם מגיע טקסט לא צפוי
                }

                // העיגול הצבעוני
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // טקסט האמינות
                Text(
                    text = reliabilityText,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}