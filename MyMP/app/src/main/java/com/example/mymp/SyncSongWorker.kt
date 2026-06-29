package com.example.mymp

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Worker che esegue sincronizzazione fra dati remoti e Room.
 * Viene schedulato in fase di connessione con il server.
 * Ritenta fino a 3 volte prima di dare errore
 */
class SyncSongWorker (
    context: Context,
    workerParams: WorkerParameters
): CoroutineWorker(context, workerParams){

    override suspend fun doWork(): Result {

        //Recupera il singleton del db dall' application
        val db = (applicationContext as MympApplication).database
        val repository = MympRepository(db.mympDao(), db.serverDao(), db.playlistDao())

        //Validazione degli oggetti in input. Passiamo oggetti in input al worker grazie a WorkDataOf
        val baseUrl = inputData.getString(KEY_IP) ?: return Result.failure()
        val serverId = inputData.getInt(KEY_SERVER_ID, -1)
        if (serverId == -1) return Result.failure()

        return try {
            repository.refreshSongs(serverId, baseUrl)
            Log.d("SyncWorker", "Sync OK per server $serverId")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync FAILED: ${e::class.simpleName}: ${e.message}", e)
            if (runAttemptCount < 3) Result.retry()
            else Result.failure()
        }

    }

    companion object {
        const val KEY_IP = "KEY_IP" //url base del server passato al worker
        const val KEY_SERVER_ID = "KEY_SERVER_ID" //server id passato al worker

    }

}