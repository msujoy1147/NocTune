package com.example.player

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.db.AppDatabase
import com.example.data.model.SongEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.io.IOException

enum class RepeatMode {
    OFF, ALL, ONE
}

object MusicPlayerManager {
    private const val PREFS_NAME = "noctune_player_prefs"
    private const val KEY_LAST_SONG_ID = "last_song_id"
    private const val KEY_LAST_POSITION = "last_position"

    private var context: Context? = null
    private var mediaPlayer: MediaPlayer? = null
    private val generativeSynth = ProceduralAudioSynthesizer()
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressTrackerJob: Job? = null
    private var sleepTimerJob: Job? = null
    
    // Core playback flows
    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong = _currentSong.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()
    
    private val _playbackQueue = MutableStateFlow<List<SongEntity>>(emptyList())
    val playbackQueue = _playbackQueue.asStateFlow()
    
    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled = _shuffleEnabled.asStateFlow()
    
    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode = _repeatMode.asStateFlow()
    
    private val _sleepTimerRemaining = MutableStateFlow(0L) // Remaining time in ms
    val sleepTimerRemaining = _sleepTimerRemaining.asStateFlow()
    
    private val _stopAfterCurrentSong = MutableStateFlow(false)
    val stopAfterCurrentSong = _stopAfterCurrentSong.asStateFlow()
    
    private var currentIndex = -1
    private var originalQueue = listOf<SongEntity>()

    fun init(ctx: Context) {
        if (context != null) return
        this.context = ctx.applicationContext
        loadSavedState()
    }

    private fun loadSavedState() {
        val ctx = context ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSongId = prefs.getString(KEY_LAST_SONG_ID, null)
        val lastPos = prefs.getLong(KEY_LAST_POSITION, 0L)
        
        if (lastSongId != null) {
            coroutineScope.launch {
                val db = AppDatabase.getDatabase(ctx)
                try {
                    val foundSong = withContext(Dispatchers.IO) {
                        db.songDao().getAllSongs().first().find { it.id == lastSongId }
                    }
                    if (foundSong != null) {
                        _currentSong.value = foundSong
                        _currentPosition.value = lastPos
                        // Enqueue song
                        setQueue(listOf(foundSong))
                        Log.d("NocTunePlayer", "Restored matching last song: ${foundSong.title} to position: ${lastPos}ms")
                    }
                } catch (e: Exception) {
                    Log.e("NocTunePlayer", "Error loading saved song", e)
                }
            }
        }
    }

    private fun savePlaybackState() {
        val ctx = context ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val song = _currentSong.value
        prefs.edit().apply {
            if (song != null) {
                putString(KEY_LAST_SONG_ID, song.id)
                putLong(KEY_LAST_POSITION, _currentPosition.value)
            } else {
                remove(KEY_LAST_SONG_ID)
                remove(KEY_LAST_POSITION)
            }
            apply()
        }
    }

    fun setQueue(songs: List<SongEntity>, startIndex: Int = 0) {
        originalQueue = songs
        currentIndex = startIndex
        if (_shuffleEnabled.value) {
            val shuffled = songs.shuffled()
            _playbackQueue.value = shuffled
            currentIndex = shuffled.indexOf(songs.getOrNull(startIndex))
        } else {
            _playbackQueue.value = songs
        }
        
        if (_playbackQueue.value.isNotEmpty() && currentIndex in _playbackQueue.value.indices) {
            _currentSong.value = _playbackQueue.value[currentIndex]
        }
    }

    fun playSong(song: SongEntity) {
        val index = _playbackQueue.value.indexOfFirst { it.id == song.id }
        if (index != -1) {
            currentIndex = index
        } else {
            // Append and play
            val currentList = _playbackQueue.value.toMutableList()
            currentList.add(song)
            _playbackQueue.value = currentList
            currentIndex = currentList.size - 1
        }
        
        _currentSong.value = song
        _currentPosition.value = 0L
        startPlayback()
    }

    fun startPlayback() {
        val song = _currentSong.value ?: return
        val ctx = context ?: return
        
        // Stop current
        stopAllPlayers()
        
        if (song.isGenerative) {
            startGenerativeAudio(song)
        } else {
            startLocalAudio(song)
        }
        
        _isPlaying.value = true
        startProgressTracker()
        savePlaybackState()
        
        // Log in Database play history
        coroutineScope.launch {
            val db = AppDatabase.getDatabase(ctx)
            db.songDao().incrementPlayCount(song.id)
        }
        
        // Start foreground service
        startForegroundService()
    }

    private fun startLocalAudio(song: SongEntity) {
        val ctx = context ?: return
        mediaPlayer = MediaPlayer().apply {
            try {
                if (song.path.startsWith("content://")) {
                    setDataSource(ctx, android.net.Uri.parse(song.path))
                } else {
                    setDataSource(song.path)
                }
                prepare()
                if (_currentPosition.value > 0) {
                    seekTo(_currentPosition.value.toInt())
                }
                start()
                
                setOnCompletionListener {
                    onSongCompleted()
                }
            } catch (e: Exception) {
                Log.e("NocTunePlayer", "Error preparing local MediaPlayer", e)
                onSongCompleted() // Skip to next if corrupt
            }
        }
    }

    private fun startGenerativeAudio(song: SongEntity) {
        generativeSynth.start(song.generativePreset)
        // Set synth auto-timer if there's no completion callback
        // Generative songs are endless, but we cap display / completion ticks at the designated song.duration.
    }

    fun pausePlayback() {
        if (!_isPlaying.value) return
        _isPlaying.value = false
        
        val song = _currentSong.value
        if (song != null) {
            if (song.isGenerative) {
                generativeSynth.stop()
            } else {
                mediaPlayer?.apply {
                    if (isPlaying) {
                        pause()
                    }
                }
            }
        }
        savePlaybackState()
        stopProgressTracker()
        startForegroundService() // Updates service state
    }

    fun resumePlayback() {
        if (_isPlaying.value) return
        val song = _currentSong.value ?: return
        
        if (song.isGenerative) {
            generativeSynth.start(song.generativePreset)
        } else {
            try {
                mediaPlayer?.start() ?: run {
                    // Rebuild player if killed
                    startLocalAudio(song)
                }
            } catch (e: Exception) {
                Log.e("NocTunePlayer", "Error resuming MediaPlayer", e)
            }
        }
        _isPlaying.value = true
        startProgressTracker()
        startForegroundService()
    }

    fun nextSong() {
        val queue = _playbackQueue.value
        if (queue.isEmpty()) return
        
        if (_stopAfterCurrentSong.value) {
            _stopAfterCurrentSong.value = false
            pausePlayback()
            return
        }

        if (_repeatMode.value == RepeatMode.ONE) {
            _currentPosition.value = 0L
            startPlayback()
            return
        }

        currentIndex++
        if (currentIndex >= queue.size) {
            if (_repeatMode.value == RepeatMode.ALL) {
                currentIndex = 0
            } else {
                currentIndex = queue.size - 1
                pausePlayback()
                return
            }
        }
        
        _currentSong.value = queue[currentIndex]
        _currentPosition.value = 0L
        startPlayback()
    }

    fun prevSong() {
        val queue = _playbackQueue.value
        if (queue.isEmpty()) return
        
        // If the current track is past 3 seconds, restart it first
        if (_currentPosition.value > 3000L) {
            seekTo(0L)
            return
        }

        currentIndex--
        if (currentIndex < 0) {
            if (_repeatMode.value == RepeatMode.ALL) {
                currentIndex = queue.size - 1
            } else {
                currentIndex = 0
                seekTo(0L)
                return
            }
        }
        
        _currentSong.value = queue[currentIndex]
        _currentPosition.value = 0L
        startPlayback()
    }

    fun seekTo(positionMs: Long) {
        val current = _currentSong.value ?: return
        _currentPosition.value = positionMs
        
        if (!current.isGenerative) {
            try {
                mediaPlayer?.seekTo(positionMs.toInt())
            } catch (e: Exception) {
                Log.e("NocTunePlayer", "Error seeking MediaPlayer", e)
            }
        } else {
            // Generative tracker just warps display clocks
        }
        savePlaybackState()
    }

    fun toggleShuffle() {
        val isEn = !_shuffleEnabled.value
        _shuffleEnabled.value = isEn
        
        val song = _currentSong.value
        if (isEn) {
            val shuffled = originalQueue.shuffled()
            _playbackQueue.value = shuffled
            if (song != null) {
                currentIndex = shuffled.indexOf(song)
            }
        } else {
            _playbackQueue.value = originalQueue
            if (song != null) {
                currentIndex = originalQueue.indexOf(song)
            }
        }
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleFavorite() {
        val song = _currentSong.value ?: return
        val ctx = context ?: return
        val newFav = !song.isFavorite
        _currentSong.value = song.copy(isFavorite = newFav)
        
        coroutineScope.launch {
            val db = AppDatabase.getDatabase(ctx)
            db.songDao().updateFavorite(song.id, newFav)
        }
    }

    fun toggleStopAfterCurrent() {
        _stopAfterCurrentSong.value = !_stopAfterCurrentSong.value
    }

    // Dynamic countdown Sleep Timer
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemaining.value = 0L
            return
        }
        
        val durationMs = minutes * 60 * 1000L
        _sleepTimerRemaining.value = durationMs
        
        sleepTimerJob = coroutineScope.launch {
            while (_sleepTimerRemaining.value > 0) {
                delay(1000)
                _sleepTimerRemaining.value -= 1000L
            }
            // Timer expired! Stop audio.
            pausePlayback()
            Log.d("NocTunePlayer", "Sleep timer expired. Playback paused.")
        }
    }

    private fun onSongCompleted() {
        nextSong()
    }

    private fun stopAllPlayers() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("NocTunePlayer", "Error clearing MediaPlayer", e)
        }
        mediaPlayer = null
        generativeSynth.stop()
    }

    private fun startProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = coroutineScope.launch {
            while (isActive) {
                val song = _currentSong.value
                if (song != null && _isPlaying.value) {
                    if (song.isGenerative) {
                        val nextPos = _currentPosition.value + 1000L
                        if (nextPos >= song.duration) {
                            _currentPosition.value = song.duration
                            onSongCompleted()
                        } else {
                            _currentPosition.value = nextPos
                        }
                    } else {
                        mediaPlayer?.let { mp ->
                            try {
                                if (mp.isPlaying) {
                                    _currentPosition.value = mp.currentPosition.toLong()
                                }
                            } catch (e: Exception) {
                                // Ignore transient media player exceptions
                            }
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = null
    }

    private fun startForegroundService() {
        val ctx = context ?: return
        val serviceIntent = Intent(ctx, NocTunePlayerService::class.java)
        ContextCompat.startForegroundService(ctx, serviceIntent)
    }

    fun release() {
        stopAllPlayers()
        stopProgressTracker()
        sleepTimerJob?.cancel()
        coroutineScope.cancel()
    }
}
