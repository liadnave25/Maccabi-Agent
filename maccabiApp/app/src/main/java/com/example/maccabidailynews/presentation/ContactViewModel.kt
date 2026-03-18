package com.example.maccabidailynews.presentation

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

// הגדרת המצבים האפשריים בעת שליחת ההודעה
sealed class SubmitState {
    object Idle : SubmitState() // ממתין להזנה
    object Loading : SubmitState() // שולח...
    object Success : SubmitState() // נשלח בהצלחה
    data class Error(val message: String) : SubmitState() // שגיאה
}

@HiltViewModel
class ContactViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _submitState = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submitState = _submitState.asStateFlow()

    fun submitFeedback(name: String, email: String, message: String) {
        // ולידציה בסיסית
        if (name.isBlank() || message.isBlank()) {
            _submitState.value = SubmitState.Error("חובה למלא שם והודעה")
            return
        }

        _submitState.value = SubmitState.Loading

        // הכנת המידע לשליחה לפיירבייס
        val feedback = hashMapOf(
            "name" to name,
            "email" to email,
            "message" to message,
            "timestamp" to System.currentTimeMillis()
        )

        // יצירת קולקשיין חדש בשם Feedbacks ודחיפת המסמך
        firestore.collection("Feedbacks")
            .add(feedback)
            .addOnSuccessListener {
                _submitState.value = SubmitState.Success
            }
            .addOnFailureListener { e ->
                _submitState.value = SubmitState.Error(e.message ?: "שגיאה בשליחת ההודעה")
            }
    }

    // איפוס המצב לאחר שליחה מוצלחת
    fun resetState() {
        _submitState.value = SubmitState.Idle
    }
}