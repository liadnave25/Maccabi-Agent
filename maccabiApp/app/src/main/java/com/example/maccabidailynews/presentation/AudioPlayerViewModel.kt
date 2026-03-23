package com.example.maccabidailynews.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AudioPlayerViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {

    // יצירת הנגן של גוגל
    val player = ExoPlayer.Builder(context).build()
    // חיבור למסד הנתונים
    private val firestore = FirebaseFirestore.getInstance()

    // שומרים את המצב: האם משהו מתנגן עכשיו?
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    // שומרים את שם הפודקאסט הנוכחי (למשל תאריך הכתבה)
    private val _currentTitle = MutableStateFlow("העדכון היומי של מכבי")
    val currentTitle = _currentTitle.asStateFlow()

    // שומרים את הלינק המעודכן של היום
    private val _latestAudioUrl = MutableStateFlow<String?>(null)
    val latestAudioUrl = _latestAudioUrl.asStateFlow()

    init {
        // ברגע שה-ViewModel נוצר, אנחנו מושכים את הלינק מה-Firestore
        fetchLatestPodcast()
    }

    // הפונקציה ששואבת את הלינק המעודכן ביותר מ-System/LatestPodcast
    private fun fetchLatestPodcast() {
        viewModelScope.launch {
            try {
                // ניגש למיקום הקבוע שיצרנו בפייתון
                val document = firestore.collection("System").document("LatestPodcast").get().await()
                if (document.exists()) {
                    val url = document.getString("audio_url")
                    val date = document.getString("date")
                    _latestAudioUrl.value = url
                    // עדכון הכותרת לתאריך של היום
                    _currentTitle.value = "פודקאסט - $date"
                }
            } catch (e: Exception) {
                // אם יש שגיאה (למשל אין אינטרנט), נתמודד איתה בשקט כרגע
                e.printStackTrace()
            }
        }
    }

    // הפונקציה שתטען את קובץ ה-MP3 ותתחיל לנגן
    fun playPodcast() {
        val url = _latestAudioUrl.value ?: return // אם אין לינק, לא עושים כלום

        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        _isPlaying.value = true
    }

    // פונקציית השהייה/המשך
    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
        } else {
            // אם ניסינו לנגן אבל הנגן ריק, נטען את הפודקאסט
            if (player.mediaItemCount == 0) {
                playPodcast()
            } else {
                player.play()
                _isPlaying.value = true
            }
        }
    }

    // חשוב מאוד: שחרור משאבים כשהאפליקציה נסגרת לחלוטין
    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}