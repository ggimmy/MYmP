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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Foreground Service responsabile della riproduzione audio in background.
 *
 * Gestisce il ciclo di vita del [MediaPlayer], la playlist corrente,
 * il progresso di riproduzione e la notifica di sistema con i controlli
 * play/pausa, skip e stop.
 *
 * Comunica lo stato corrente (brano attivo, isPlaying, progresso) al
 * [MympViewModel] tramite i [kotlinx.coroutines.flow.MutableStateFlow]
 * condivisi in [MympApplication], evitando accoppiamento diretto
 * tra Service e ViewModel.
 *
 * Viene avviato tramite [Context.startForegroundService] dal ViewModel,
 * che passa i parametri necessari (filePath, titolo, artista, playlist)
 * come extra dell'[Intent].
 */
class MusicService (

): Service() {

    //scope coroutine del service, cancellato in onDestroy
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var mediaPlayer: MediaPlayer? = null
    private var currentTitle: String = "Sconosciuto"
    private var currentArtist: String = "Sconosciuto"

    //lista dei brani in coda deserializzato dal json passato nell'intent
    private var playlist: List<Song> = emptyList()

    //indice del brano in playlist, aggiornato con skip
    private var currentIndex: Int = 0

    //job per polling del progresso
    private var progressJob: Job? = null

    //riferimento all'application per accedere agli stateFlow condivisi
    private val app get() = application as MympApplication

    override fun onCreate() {
        super.onCreate()
        Log.d("MusicService", "Service onCreate")
        createNotificationChannel()
    }

    /**
     * Punto di ingresso del Service. Riceve i comandi tramite Intent
     * e li smista al comportamento corretto.
     *
     * Restituisce [START_STICKY] per garantire che il sistema
     * riavvii il Service se viene terminato mentre è in foreground.
     */
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

                currentIndex = playlist.indexOfFirst { it.filePath == filePath }
                    .takeIf { it >= 0 } ?: 0 //aggiorna index per skip


                startForegroundWithNotification()

                if (filePath != null) {
                    playSong(filePath)
                }
            }

            ACTION_PAUSE_RESUME -> {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    stopProgressPolling()
                } else {
                    mediaPlayer?.start()
                    startProgressPolling()
                }
                updateSharedState()
                updateNotification()
            }

            ACTION_SKIP -> {
                skipToNext()
            }

            ACTION_SEEK -> {
                val progress = intent.getFloatExtra(EXTRA_SEEK_POSITION, 0f)
                val duration = mediaPlayer?.duration ?: 0
                if (duration > 0) {
                    val position = (progress * duration).toInt()
                    mediaPlayer?.seekTo(position)
                    app.playbackProgressState.value = progress
                }
            }

            ACTION_STOP -> {
                stopProgressPolling()
                mediaPlayer?.stop()
                app.currentSongState.value = null
                app.isPlayingState.value = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

        }

        return START_STICKY
    }

    private fun playSong(filePath: String) {
        stopProgressPolling()
        app.playbackProgressState.value = 0f

        //rilasciamo il player visto che questa funzione viene chiamata ricorsivamente
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            prepareAsync() //funzione del media player per svolgere in modo asincorno la riproduzione
            setOnPreparedListener { //preparedListener per comunicare con il thread lanciato da prepareAsync
                start()
                updateNotification()
                updateSharedState()
                startProgressPolling()
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
        updateSharedState()
        playSong(next.filePath)
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val player = mediaPlayer
                if (player != null && player.isPlaying) {
                    val duration = player.duration
                    val position = player.currentPosition
                    if (duration > 0) {
                        /*operazione aritmetica per determinare il progresso della canzone tramite i
                        dati recuperati dal player
                        */
                        app.playbackProgressState.value = position.toFloat() / duration.toFloat()
                    }
                }
                delay(1000L)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updateSharedState() {
        app.currentSongState.value = playlist.getOrNull(currentIndex)
        app.isPlayingState.value = mediaPlayer?.isPlaying == true
    }

    private fun startForegroundWithNotification() {
        Log.d("MusicService", "startForegroundWithNotification chiamata")
        Log.d("MusicService", "Channel ID: $CHANNEL_ID")
        Log.d("MusicService", "Notification: ${buildNotification()}")
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

    /**
     * Crea il canale di notifica richiesto da Android 8.0 (Oreo) e superiori.
     * Senza canale, la notifica non verrebbe mostrata su API 26+.
     */
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
        stopProgressPolling()
        mediaPlayer?.release()
        scope.cancel() //cancella tutte le coroutines del server
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
        const val ACTION_SEEK = "ACTION_SEEK"
        const val EXTRA_SEEK_POSITION = "seekPosition"

    }
}