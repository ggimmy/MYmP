package com.example.mymp

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncSongWorker (
    context: Context,
    workerParams: WorkerParameters
): CoroutineWorker(context, workerParams){

    override suspend fun doWork(): Result {

        val db = (applicationContext as MympApplication).database
        val repository = MympRepository(db.mympDao(), db.serverDao(), db.playlistDao())

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
        const val KEY_IP = "KEY_IP"
        const val KEY_SERVER_ID = "KEY_SERVER_ID"

    }

}