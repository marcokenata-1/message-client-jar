package com.marcokenata.messagefetcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class Broadcaster : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("thread", "broadcaster jalan")
        val fetcher = Fetcher()

        context?.let {
            intent?.let { intent ->
                fetcher.enqueueWork(it, intent)
            }
        }

        Log.d("thread", "broadcaster berhenti")
    }
}