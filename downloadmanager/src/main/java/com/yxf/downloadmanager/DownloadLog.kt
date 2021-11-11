package com.yxf.downloadmanager

import android.util.Log

internal val log = DownloadLog()

internal class DownloadLog {

    private val TAG = "DownloadManager"

    var enable = true

    fun d(message: String) {
        if (!enable) {
            return
        }
        Log.d(TAG, message)
    }

    fun v(message: String) {
        if (!enable) {
            return
        }
        Log.v(TAG, message)
    }

    fun w(message: String, e: Throwable? = null) {
        if (!enable) {
            return
        }
        if (e == null) {
            Log.w(TAG, message)
        } else
            Log.w(TAG, message, e)
    }

    fun e(message: String, e: Throwable? = null) {
        if (!enable) {
            return
        }
        if (e == null) {
            Log.e(TAG, message)
        } else {
            Log.e(TAG, message, e)
        }
    }

}
