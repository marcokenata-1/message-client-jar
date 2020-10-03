package com.marcokenata.messagefetcher

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager


class FetcherIntentService : Service() {

    private lateinit var workManager: WorkManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val workRequest = OneTimeWorkRequestBuilder<Fetcher.FetcherWork>()
            .build()

        workManager = WorkManager.getInstance(this)
        workManager.enqueueUniqueWork("notif", ExistingWorkPolicy.REPLACE, workRequest)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val broadcastIntent = Intent()
        broadcastIntent.setClass(this, IntentReceiver::class.java)
        sendBroadcast(broadcastIntent)
        Log.d("thread","task removed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}