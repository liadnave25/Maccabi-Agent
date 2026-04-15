package com.example.maccabidailynews.presentation.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private const val MAX_MESSAGE_LENGTH = 300

@Composable
fun ContactScreen(viewModel: ContactViewModel = hiltViewModel()) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val submitState by viewModel.submitState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLightGray)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Header icon in a circle
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaccabiDeepBlue.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MailOutline,
                contentDescription = null,
                tint = MaccabiDeepBlue,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "צור קשר",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaccabiDeepBlue
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "נשמח לשמוע ממך! הצעות, תקלות או סתם פידבק.",
            fontSize = 15.sp,
            color = MaccabiDeepBlue.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Form card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
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

                Spacer(modifier = Modifier.height(14.dp))

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

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = message,
                    onValueChange = { if (it.length <= MAX_MESSAGE_LENGTH) message = it },
                    label = { Text("הודעה") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 5,
                    supportingText = {
                        Text(
                            text = "${message.length}/$MAX_MESSAGE_LENGTH",
                            color = if (message.length >= MAX_MESSAGE_LENGTH) Color.Red
                                    else MaccabiDeepBlue.copy(alpha = 0.4f),
                            fontSize = 12.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaccabiDeepBlue,
                        focusedLabelColor = MaccabiDeepBlue
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Submit area with animated size
        Box(modifier = Modifier.animateContentSize()) {
            when (submitState) {
                is SubmitState.Loading -> {
                    CircularProgressIndicator(color = MaccabiDeepBlue)
                }
                is SubmitState.Success -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ההודעה נשלחה בהצלחה! תודה רבה.",
                            color = Color(0xFF388E3C),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    LaunchedEffect(Unit) {
                        name = ""; email = ""; message = ""
                        delay(3000)
                        viewModel.resetState()
                    }
                }
                is SubmitState.Error -> {
                    Column {
                        Text(
                            text = (submitState as SubmitState.Error).message,
                            color = Color.Red,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        SendButton { viewModel.submitFeedback(name, email, message) }
                    }
                }
                else -> {
                    SendButton { viewModel.submitFeedback(name, email, message) }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

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
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Text(text = "שלח הודעה", fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}
