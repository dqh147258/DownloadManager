package com.yxf.downloadmanager

import com.yxf.downloadmanager.file.AndroidFile
import com.yxf.safelivedata.SafeLiveData

open class DownloadTask(val id: String, val url: String, val file: AndroidFile) {


    companion object {
        const val EVENT_NONE = 0x0000
        const val EVENT_STATE = 0x0001
        const val EVENT_PERCENT = EVENT_STATE shl 1
        const val EVENT_SPEED = EVENT_PERCENT shl 1
        const val EVENT_ALL = EVENT_NONE.inv()
    }

    /**
     * 数据变化事件
     */
    val dataChangedEventData = SafeLiveData<Int>().apply { postValue(0) }

    @Volatile
    private var shouldPostEvent = EVENT_NONE

    private var sendEventTask = Runnable {
        val postEvent: Int
        synchronized(shouldPostEvent) {
            if (shouldPostEvent != EVENT_NONE) {
                postEvent = shouldPostEvent
                shouldPostEvent = EVENT_NONE
            } else {
                postEvent = EVENT_NONE
            }
        }
        if (postEvent != EVENT_NONE) {
            dataChangedEventData.value = postEvent
        }
    }

    /**
     * 用于在特殊时期禁止事件,比如创建任务时
     */
    @Volatile
    var eventEnable = true

    /**
     * 任务状态
     */

    @Volatile
    var state = DownloadState.Create
        set(value) {
            field = value
            addEvent(EVENT_STATE)
        }

    /**
     * 下载进度, 范围0.0f -1.0f
     */
    @Volatile
    var percent = 0.0f
        internal set(value) {
            field = value
            addEvent(EVENT_PERCENT)
        }

    /**
     * 下载速率,单位: byte/s
     */
    @Volatile
    var speed = 0L
        internal set(value) {
            field = value
            addEvent(EVENT_SPEED)
        }

    val speedString: String
        get() {
            var v = speed.toFloat()
            var u = "B/s"
            if (v > 1024f) {
                v /= 1024f
                u = "KB/s"
                if (v > 1024f) {
                    v /= 1024f
                    u = "MB/s"
                }
            }
            return String.format("%.2f", v) + u
        }

    /**
     * 需要下载的文件总长度
     */
    @Volatile
    var totalLength = -1L
        set(value) {
            field = value
            val fileLength = getDownloadedLength()
            percent = when {
                value <= 0L -> {
                    0.0f
                }
                fileLength >= value -> {
                    1.0f
                }
                else -> {
                    fileLength / (value * 1.0f)
                }
            }
        }

    @Volatile
    var errorMessage = ""

    private fun addEvent(event: Int) {
        if (!eventEnable) {
            return
        }
        synchronized(shouldPostEvent) {
            val newEvent = shouldPostEvent or event
            if (newEvent != shouldPostEvent) {
                shouldPostEvent = newEvent
                val handler = DownloadManager.handler
                handler.removeCallbacks(sendEventTask)
                handler.post(sendEventTask)
            }
        }
    }

    fun containEvent(event: Int): Boolean {
        return dataChangedEventData.value!! and event != 0
    }

    fun getDownloadedLength(): Long {
        return file.length()
    }

    fun isWaitingDownload(): Boolean {
        return state == DownloadState.Waiting || state == DownloadState.Create
    }


}