package com.yxf.downloadmanager

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class MaxThreadWorkManagerInitializer(private val maxThreadCount: Int) : WorkManagerInitializer {
    override fun initWorkManager(context: Context) {

        val executor = ThreadPoolExecutor(0, maxThreadCount, 30, TimeUnit.SECONDS, LinkedBlockingQueue())
        WorkManager.initialize(context, Configuration.Builder().setExecutor(executor).build())
    }
}