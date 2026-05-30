package com.example.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class NocTunePlayerService : Service() {

    companion object {
        const val CHANNEL_ID = "noctune_playback_channel"
        const val NOTIFICATION_ID = 404
        
        const val ACTION_PLAY_PAUSE = "com.example.noctune.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.noctune.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.noctune.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.example.noctune.ACTION_STOP"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        MusicPlayerManager.init(this)
        createNotificationChannel()
        
        // Start foreground immediately in onCreate with initial notification to satisfy Android requirements
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        
        // Listen to state changes to update the notification dynamically
        MusicPlayerManager.isPlaying.onEach { updateNotification() }.launchIn(serviceScope)
        MusicPlayerManager.currentSong.onEach { updateNotification() }.launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                if (MusicPlayerManager.isPlaying.value) {
                    MusicPlayerManager.pausePlayback()
                } else {
                    MusicPlayerManager.resumePlayback()
                }
            }
            ACTION_NEXT -> MusicPlayerManager.nextSong()
            ACTION_PREVIOUS -> MusicPlayerManager.prevSong()
            ACTION_STOP -> {
                MusicPlayerManager.pausePlayback()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        
        // Ensure starting foreground again safely if restarted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NocTune Playback Control",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Provides notification player controls for NocTune Music Lounge"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val song = MusicPlayerManager.currentSong.value
        val isPlaying = MusicPlayerManager.isPlaying.value
        
        val title = song?.title ?: "No Song Loaded"
        val artist = song?.artist ?: "Relax in the NocTune Espresso lounge"
        
        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Previous Action
        val prevIntent = Intent(this, NocTunePlayerService::class.java).apply { action = ACTION_PREVIOUS }
        val prevPendingIntent = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Play/Pause Action
        val playIntent = Intent(this, NocTunePlayerService::class.java).apply { action = ACTION_PLAY_PAUSE }
        val playPendingIntent = PendingIntent.getService(this, 2, playIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Next Action
        val nextIntent = Intent(this, NocTunePlayerService::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Stop Action
        val stopIntent = Intent(this, NocTunePlayerService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 4, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val playIconRes = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        // Standard notification builder loaded gracefully
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(mainPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(playIconRes, if (isPlaying) "Pause" else "Play", playPendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Exit", stopPendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setColor(0xFF1E1814.toInt()) // Deep Espresso Accent Theme Colour for notification
        
        return builder.build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }
}
