package com.example.maccabidailynews.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AudioPlayerViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {

    // יצירת הנגן של גוגל
    val player = ExoPlayer.Builder(context).build()

    // שומרים את המצב: האם משהו מתנגן עכשיו?
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    // שומרים את שם הפודקאסט הנוכחי (למשל תאריך הכתבה)
    private val _currentTitle = MutableStateFlow("העדכון היומי של מכבי")
    val currentTitle = _currentTitle.asStateFlow()

    // הפונקציה שתטען את קובץ ה-MP3 מפיירבייס ותתחיל לנגן
    fun playPodcast(url: String, title: String) {
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        _isPlaying.value = true
        _currentTitle.value = title
    }

    // פונקציית השהייה/המשך
    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
        } else {
            player.play()
            _isPlaying.value = true
        }
    }

    // חשוב מאוד: שחרור משאבים כשהאפליקציה נסגרת לחלוטין
    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}