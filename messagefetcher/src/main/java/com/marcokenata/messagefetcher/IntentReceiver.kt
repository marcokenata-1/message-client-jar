package com.marcokenata.messagefetcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager


class IntentReceiver : BroadcastReceiver() {

    private lateinit var workManager: WorkManager

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.intent.action.BOOT_COMPLETED" ){
            val workRequest = OneTimeWorkRequestBuilder<Fetcher.FetcherWork>()
                .build()

            workManager = WorkManager.getInstance(context!!)
            workManager.enqueueUniqueWork("notif", ExistingWorkPolicy.REPLACE, workRequest)
        } else {
            Log.d("thread", "stay alive")
        }
    }
}