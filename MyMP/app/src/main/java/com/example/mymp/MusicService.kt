package com.example.mymp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.Json

class MusicService (

): Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var mediaPlayer: MediaPlayer? = null
    private var currentTitle: String = "Sconosciuto"
    private var currentArtist: String = "Sconosciuto"
    private var playlist: List<Song> = emptyList()
    private var currentIndex: Int = 0

    override fun onCreate() {
        super.onCreate()
        Log.d("MusicService", "Service onCreate")
        createNotificationChannel()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {

            ACTION_PLAY -> {
                val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
                currentTitle = intent.getStringExtra(EXTRA_TITLE) ?: "Sconosciuto"
                currentArtist = intent.getStringExtra(EXTRA_ARTIST) ?: "Sconosciuto"
                val songJson = intent.getStringExtra(EXTRA_PLAYLIST) ?: "empty"

                if ( songJson != "empty"){
                    playlist = Json.decodeFromString(songJson)
                }


                startForegroundWithNotification()

                if (filePath != null) {
                    playSong(filePath)
                }
            }

            ACTION_PAUSE_RESUME -> {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                } else {
                    mediaPlayer?.start()
                }
                updateNotification()
            }

            ACTION_SKIP -> {
                skipToNext()
            }

            ACTION_STOP -> {
                mediaPlayer?.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

        }

        return START_STICKY
    }

    private fun playSong(filePath: String) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            prepareAsync()
            setOnPreparedListener {
                start()
                updateNotification()
               // startForegroundWithNotification()
            }
            setOnCompletionListener {
                skipToNext()
            }
        }
    }

    private fun skipToNext() {
        if (playlist.isEmpty()) {
            Log.d("skip", "PLAYLIST VUOTA!")
            return
        }
        currentIndex = (currentIndex + 1) % playlist.size
        val next = playlist[currentIndex]
        currentTitle = next.title
        currentArtist = next.artist
        playSong(next.filePath)
    }

    private fun startForegroundWithNotification() {
        Log.d("MusicService", "startForegroundWithNotification chiamata")
        Log.d("MusicService", "Channel ID: $CHANNEL_ID")
        Log.d("MusicService", "Notification: ${buildNotification()}")
        //createNotificationChannel()
        ServiceCompat.startForeground(
            this,
            PLAYBACK_NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
        Log.d("MusicService", "startForeground completato")
    }

    private fun updateNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(PLAYBACK_NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val name = "Riproduzione musicale"
            val descriptionText = "Canale notifica riproduzione musicale"

            val importance = NotificationManager.IMPORTANCE_LOW

            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                importance
            ).apply {description = descriptionText}
            /*val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)*/
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)

        }
    }

    private fun buildNotification(): Notification {
        val openPlayer = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResume = PendingIntent.getService(
            this, 1,
            Intent(this, MusicService::class.java).apply { action = ACTION_PAUSE_RESUME },
            PendingIntent.FLAG_IMMUTABLE
        )

        val skip = PendingIntent.getService(
            this, 2,
            Intent(this, MusicService::class.java).apply { action = ACTION_SKIP },
            PendingIntent.FLAG_IMMUTABLE
        )

        val stop = PendingIntent.getService(
            this, 3,
            Intent(this, MusicService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIcon = if (mediaPlayer?.isPlaying == true)
            android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTitle)
            .setContentText(currentArtist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openPlayer)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(
                android.graphics.drawable.Icon.createWithResource(this, pauseResumeIcon),
                if (mediaPlayer?.isPlaying == true) "Pausa" else "Play",
                pauseResume
            ).build())
            .addAction(Notification.Action.Builder(
                android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_media_next),
                "Skip",
                skip
            ).build())
            .addAction(Notification.Action.Builder(
                android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_media_ff),
                "Stop",
                stop
            ).build())
            .build()
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null

    companion object {
        const val PLAYBACK_NOTIFICATION_ID = 1
        const val CHANNEL_ID = "CHANNEL_PLAYBACK"
        const val ACTION_PLAY = "ACTION_PLAY"
        const val ACTION_PAUSE_RESUME = "ACTION_PAUSE_RESUME"
        const val ACTION_SKIP = "ACTION_SKIP"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_FILE_PATH = "filePath"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
        const val EXTRA_PLAYLIST = "playlist"

    }
}