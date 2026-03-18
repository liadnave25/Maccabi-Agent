package com.example.maccabidailynews.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.maccabidailynews.presentation.ContactViewModel
import com.example.maccabidailynews.presentation.SubmitState
import com.example.maccabidailynews.ui.theme.BackgroundLightGray
import com.example.maccabidailynews.ui.theme.MaccabiDeepBlue
import com.example.maccabidailynews.ui.theme.MaccabiSoftYellow
import kotlinx.coroutines.delay

@Composable
fun ContactScreen(viewModel: ContactViewModel = hiltViewModel()) {
    // משתנים לשמירת הטקסט שהמשתמש מקליד
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val submitState by viewModel.submitState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLightGray)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // כותרת המסך
        Text(
            text = "צור קשר",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaccabiDeepBlue
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "נשמח לשמוע ממך! הצעות, תקלות או סתם פידבק.",
            fontSize = 16.sp,
            color = MaccabiDeepBlue.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // שדה: שם מלא
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("שם מלא") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaccabiDeepBlue,
                focusedLabelColor = MaccabiDeepBlue
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // שדה: אימייל
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("אימייל (לא חובה)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaccabiDeepBlue,
                focusedLabelColor = MaccabiDeepBlue
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // שדה: הודעה (מוגדל)
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("הודעה") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            shape = RoundedCornerShape(12.dp),
            maxLines = 5,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaccabiDeepBlue,
                focusedLabelColor = MaccabiDeepBlue
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // לוגיקת הצגת מצב השליחה והכפתור
        when (submitState) {
            is SubmitState.Loading -> {
                CircularProgressIndicator(color = MaccabiDeepBlue)
            }
            is SubmitState.Success -> {
                Text(
                    text = "ההודעה נשלחה בהצלחה! תודה רבה.",
                    color = Color(0xFF4CAF50), // ירוק הצלחה
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                // מנקים את השדות אחרי 3 שניות ומחזירים את הכפתור
                LaunchedEffect(Unit) {
                    name = ""
                    email = ""
                    message = ""
                    delay(3000)
                    viewModel.resetState()
                }
            }
            is SubmitState.Error -> {
                Text(
                    text = (submitState as SubmitState.Error).message,
                    color = Color.Red
                )
                Spacer(modifier = Modifier.height(16.dp))
                SendButton { viewModel.submitFeedback(name, email, message) }
            }
            else -> {
                SendButton { viewModel.submitFeedback(name, email, message) }
            }
        }
    }
}

// קומפוננטת עזר לכפתור השליחה כדי לא לשכפל קוד
@Composable
fun SendButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaccabiSoftYellow,
            contentColor = MaccabiDeepBlue
        )
    ) {
        Text(text = "שלח הודעה", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}