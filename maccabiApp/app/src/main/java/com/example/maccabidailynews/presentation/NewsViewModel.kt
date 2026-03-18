package com.example.maccabidailynews.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.maccabidailynews.domain.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

// האנוטציה הזו אומרת ל-Hilt להכין את ה-ViewModel הזה עבורנו מאחורי הקלעים
@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository // Hilt מזריק לכאן אוטומטית את ה-Repository שבנינו
) : ViewModel() {

    // כאן אנחנו שומרים את המצב הנוכחי של המסך. מתחילים תמיד במצב "טעינה"
    private val _uiState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    // הבלוק הזה רץ אוטומטית ברגע שהמסך נפתח
    init {
        fetchNews()
    }

    private fun fetchNews() {
        // מריצים את בקשת הרשת ברקע כדי לא לתקוע את המסך
        viewModelScope.launch {
            repository.getDailyNews()
                .catch { exception ->
                    // אם יש שגיאה (למשל אין אינטרנט בפעם הראשונה), נעדכן את המצב לשגיאה
                    _uiState.value = NewsUiState.Error(exception.message ?: "שגיאה לא ידועה התרחשה")
                }
                .collect { newsList ->
                    // ברגע שפיירבייס מחזיר לנו רשימה (או כשמשהו מתעדכן בענן), אנחנו בודקים אותה
                    if (newsList.isEmpty()) {
                        _uiState.value = NewsUiState.Error("אין עדיין חדשות להיום.")
                    } else {
                        // אם יש נתונים, מעדכנים את המסך למצב הצלחה ומעבירים לו את הרשימה
                        _uiState.value = NewsUiState.Success(newsList)
                    }
                }
        }
    }
}