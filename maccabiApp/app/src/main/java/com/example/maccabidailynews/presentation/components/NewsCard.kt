package com.example.maccabidailynews.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
                if (news.link.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(news.link))
                    context.startActivity(intent)
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left accent border
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        color = MaccabiDeepBlue,
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )

            Column(modifier = Modifier.padding(start = 12.dp, top = 14.dp, end = 14.dp, bottom = 14.dp)) {
                // Source row: sport icon + source badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val sportIcon = when {
                        news.sport_type.contains("כדורסל") -> Icons.Default.SportsBasketball
                        else -> Icons.Default.SportsSoccer
                    }
                    Icon(
                        imageVector = sportIcon,
                        contentDescription = null,
                        tint = MaccabiDeepBlue.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaccabiDeepBlue.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = news.source,
                            color = MaccabiDeepBlue.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = news.title,
                    color = MaccabiDeepBlue,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(5.dp))

                // Content summary (max 3 lines)
                Text(
                    text = news.content,
                    color = Color.DarkGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Reliability pill badge
                val reliabilityUpper = news.reliability.uppercase()
                val (badgeColor, reliabilityText) = when {
                    reliabilityUpper.contains("CONFIRMED") || reliabilityUpper.contains("HIGH") ->
                        Pair(Color(0xFF4CAF50), "דיווח מאומת")
                    reliabilityUpper.contains("RUMOR") || reliabilityUpper.contains("MEDIUM") ->
                        Pair(Color(0xFFFF9800), "שמועה")
                    reliabilityUpper.contains("FAKE") || reliabilityUpper.contains("LOW") ->
                        Pair(Color(0xFFF44336), "אמינות נמוכה")
                    else ->
                        Pair(Color.Gray, "לא ידוע")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = badgeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = reliabilityText,
                            color = badgeColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
