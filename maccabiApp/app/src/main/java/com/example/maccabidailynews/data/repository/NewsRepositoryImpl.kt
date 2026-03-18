package com.example.maccabidailynews.data.repository

import com.example.maccabidailynews.domain.model.NewsItem
import com.example.maccabidailynews.domain.repository.NewsRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

// ה-Inject אומר ל-Hilt (מערכת הזרקת התלויות) שהיא אחראית לספק לנו את פיירבייס
class NewsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NewsRepository {

    override fun getDailyNews(): Flow<List<NewsItem>> = callbackFlow {
        // פנייה לאוסף "DailyNews" שלנו בענן, ומיון מהחדש לישן
        val subscription = firestore.collection("DailyNews")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error) // במקרה של שגיאה (למשל אין אינטרנט בפעם הראשונה), נסגור את הזרם
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // המרה קסומה של כל המסמכים מפיירבייס ישירות לאובייקטי NewsItem שלנו
                    val newsList = snapshot.documents.mapNotNull { it.toObject(NewsItem::class.java) }
                    trySend(newsList) // שליחת הרשימה המעודכנת ל-UI
                }
            }

        // ברגע שהמשתמש סוגר את המסך, אנחנו מנתקים את ההאזנה כדי לחסוך סוללה
        awaitClose { subscription.remove() }
    }
}