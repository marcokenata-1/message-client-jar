package com.marcokenata.messagefetcher

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters

class FetcherWorkerFactory(private val fetcher: Fetcher) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return Fetcher.FetcherWork(appContext,workerParameters, fetcher)
    }
}