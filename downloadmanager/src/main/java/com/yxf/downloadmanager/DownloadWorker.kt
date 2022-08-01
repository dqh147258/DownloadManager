package com.yxf.downloadmanager

import android.content.Context
import android.util.Log
import androidx.work.*
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.http2.StreamResetException
import java.io.IOException
import java.net.ProtocolException
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap

internal class DownloadWorker(private val appContext: Context, workParams: WorkerParameters) : Worker(appContext, workParams) {

    companion object {

        private const val KEY_ID = "id"

        private val client by lazy { OkHttpClient.Builder().build() }

        private val callMap by lazy { ConcurrentHashMap<String, Call>() }

        fun start(id: String, context: Context) {
            val data = Data.Builder().putString(KEY_ID, id).build()
            val request = OneTimeWorkRequest.Builder(DownloadWorker::class.java)
                .setInputData(data)
                .addTag(id)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }

        fun cancel(id: String, context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(id)
            callMap[id]?.let {
                it.cancel()
            }
        }

    }


    override fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: throw RuntimeException("get id from input data failed")
        val task = DownloadManager.getTaskFromWaiting(id) ?: DownloadManager.getTaskFromStorage(id)
        ?: throw RuntimeException("get task from storage failed")
        val map = DownloadManager.executingTaskListData.value!!
        if (map.containsKey(task.id)) {
            val executingTask = map[task.id]!!
            if (executingTask.state == DownloadState.Cancel) {
                map.remove(executingTask.id)
            } else {
                log.e("task(id = ${task.id}) is in executing")
            }
            return Result.success()
        }
        //文件是否可用检查
        val file = task.file
        file.createIfNotExist()
        if (!file.canOperate()) {
            task.state = DownloadState.Error
            task.errorMessage = "can not access file(description = ${file.getDescription()}) in task(id = ${task.id})"
            DownloadManager.saveTask(task)
            return Result.failure()
        }


        val totalLength = getContentLength(task.url)
        if (totalLength <= 0) {
            task.errorMessage =
                "download task(id = ${task.id}) execute failed caused by get file total length failed(length = ${totalLength})"
            task.state = DownloadState.Error
            DownloadManager.saveTask(task)
            return Result.failure()
        }
        task.totalLength = totalLength
        task.state = DownloadState.Downloading
        DownloadManager.initialized
        DownloadManager.saveTask(task)
        DownloadManager.insertExecutingTask(task)
        DownloadManager.removeWaitingTask(task)
        var downloadedLength = task.getDownloadedLength()
        val request: Request = Request.Builder() //确定下载的范围,添加此头,则服务器就可以跳过已经下载好的部分
            .addHeader("RANGE", "bytes=${downloadedLength}-$totalLength")
            .url(task.url)
            .build()
        val call = client.newCall(request)
        callMap[task.id] = call
        val pauseSet by lazy { setOf(DownloadState.Pause, DownloadState.Cancel) }
        try {
            val response = call.execute()
            val ins = response.body()!!.byteStream()
            val os = file.getOutputStream()
            os.use {
                ins.use {
                    val buffer = ByteArray(2048)
                    var len: Int = 0
                    val startTime = System.currentTimeMillis()
                    var lastTime = startTime
                    var lastLength = downloadedLength
                    while (ins.read(buffer).also { len = it } != -1) {
                        os.write(buffer, 0, len)
                        downloadedLength += len
                        task.percent = downloadedLength / (totalLength * 1.0f)
                        //val usedTime = (lastTime - startTime) / 1000
                        val current = System.currentTimeMillis()
                        val dTime = current - lastTime
                        if (dTime >= 1000) {
                            val dLength = downloadedLength - lastLength
                            val speed = dLength * 1000 / dTime
                            lastTime = current
                            lastLength = downloadedLength
                            task.speed = speed
                        }
                    }
                    //TODO : 根据长度决定是暂停状态还是结束状态
                    if (downloadedLength >= totalLength) {
                        task.state = DownloadState.Finished
                        DownloadManager.saveTask(task)
                    } else {
                        if (pauseSet.contains(task.state)) {
                            log.v("task(id = ${task.id}) state is ${task.state}")
                        } else {
                            task.errorMessage =
                                "download task(id = ${task.id}) execute failed, downloaded length(${downloadedLength}) is less than the total length(${totalLength}) of the file"
                            task.state = DownloadState.Error
                            DownloadManager.saveTask(task)
                        }
                    }
                    return Result.success()
                }
            }
        } catch (e: Exception) {
            return if (pauseSet.contains(task.state)) {
                log.v("task(id = ${task.id}) state is ${task.state}")
                Result.success()
            } else {
                task.errorMessage = "download task execute failed caused by ${e.message}"
                task.state = DownloadState.Error
                DownloadManager.saveTask(task)
                when (e) {
                    is SocketException, is ProtocolException, is StreamResetException -> {
                        e.printStackTrace()
                    }
                    else -> {
                        throw e
                    }
                }
                Result.failure()
            }
        } finally {
            callMap.remove(task.id)
            DownloadManager.removeExecutingTask(task)
        }
    }

    /**
     * 获取下载长度
     *
     * @param url
     * @return
     */
    private fun getContentLength(url: String): Long {
        val request: Request = Request.Builder()
            .url(url)
            .build()
        try {
            val response: Response = client.newCall(request).execute()
            if (response != null && response.isSuccessful) {
                val contentLength: Long = response.body()?.contentLength() ?: 0
                response.close()
                return contentLength
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return -1
    }
}