package com.marcokenata.messageclient

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.work.Configuration
import androidx.work.DelegatingWorkerFactory
import com.marcokenata.messagefetcher.Fetcher
import com.marcokenata.messagefetcher.FetcherWorkerFactory

class Application : Application(), Configuration.Provider {

    override fun getWorkManagerConfiguration(): Configuration {
        val workerFactory = DelegatingWorkerFactory()
        val appName: String = applicationInfo.loadLabel(packageManager).toString()
        val fetcher = Fetcher(R.drawable.ic_launcher_foreground, appName, Intent(this, com.marcokenata.messageclient.Application::class.java),this)
        fetcher.getChannel("test.*")
        Log.d("thread","worker factory setup")
        workerFactory.addFactory(FetcherWorkerFactory(fetcher))

        return Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .setWorkerFactory(workerFactory)
            .build()
    }
}