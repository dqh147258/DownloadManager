package com.yxf.downloadmanager

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.yxf.downloadmanager.file.AndroidFile
import com.yxf.safelivedata.SafeLiveData
import com.yxf.safelivedata.setValueSync
import java.util.concurrent.ConcurrentHashMap

object DownloadManager {

    const val TAG = "DownloadManager"


    /**
     * 是否开启日志
     */
    var logEnable: Boolean
        get() {
            return log.enable
        }
        set(value) {
            log.enable = value
        }


    internal lateinit var applicationContext: Context
    private lateinit var storage: DownloadTaskStorage

    internal val handler = Handler(Looper.getMainLooper())

    @Volatile
    internal var initialized = false

    val executingTaskListData = SafeLiveData<MutableMap<String, DownloadTask>>().apply { value = ConcurrentHashMap() }
    val waitingTaskListData = SafeLiveData<MutableMap<String, DownloadTask>>().apply { value = ConcurrentHashMap() }

    /**
     * 初始化,必调,并且应该尽早
     */
    fun init(applicationContext: Context, storage: DownloadTaskStorage = SharePreferenceStorage(applicationContext)) {
        if (initialized) {
            log.w("download manager has been initialized already")
            return
        }
        initialized = true
        this.applicationContext = applicationContext
        this.storage = storage
    }

    /**
     * 强制插入任务,如果有之前任务存在则取消之前任务
     * @param deleteFile 如果任务已存在是否删除之前任务下载的文件
     */
    fun enqueueTaskForce(task: DownloadTask, deleteFile: Boolean = false): DownloadTask {
        val previous = getTaskById(task.id)
        if (previous != null) {
            cancelTask(previous, deleteFile)
        }
        return enqueueTask(task)
    }

    fun enqueueTaskForce(url: String, file: AndroidFile, id: String = url, deleteFile: Boolean = false): DownloadTask {
        val task = DownloadTask(id, url, file)
        return enqueueTaskForce(task, deleteFile)
    }

    /**
     * 插入新任务或者暂停中的任务
     * 如果任务正在执行,则返回正在执行的任务
     */
    fun enqueueTask(task: DownloadTask): DownloadTask {
        executingTaskListData.value!!.let {
            if (it.containsKey(task.id)) {
                log.w("task(${task.id}) is in executing already")
                return@enqueueTask it[task.id]!!
            }
        }
        waitingTaskListData.value!!.let {
            if (it.containsKey(task.id)) {
                log.w("task(${task.id}) is in waiting already")
                return@enqueueTask it[task.id]!!
            }
        }
        val savedTask = storage.getDownloadTaskById(task.id)
        if (savedTask != null) {
            log.w("task(${task.id}) is enqueued already")
        }
        val shouldStartTask = if (savedTask?.isWaitingDownload() == true) savedTask else task
        startTask(shouldStartTask)
        return task
    }

    fun enqueueTask(url: String, file: AndroidFile, id: String = url): DownloadTask {
        val task = DownloadTask(id, url, file)
        return enqueueTask(task)
    }

    internal fun startTask(task: DownloadTask) {
        if (task.state == DownloadState.Cancel) {
            log.w("task is canceled, can not start")
            return
        }
        task.state = DownloadState.Waiting
        storage.saveDownloadTask(task)
        DownloadWorker.start(task.id, applicationContext)
        insertWaitingTask(task)
    }

    fun isTaskExist(id: String): Boolean {
        return storage.getDownloadTaskById(id) != null
    }

    fun getTaskById(id: String): DownloadTask? {
        val map = executingTaskListData.value!!
        val waitingMap = waitingTaskListData.value!!
        return map[id] ?: waitingMap[id] ?: return storage.getDownloadTaskById(id)
    }

    fun getTaskCount(): Int {
        return storage.getTaskCount()
    }

    fun getAllTask(): List<DownloadTask> {
        return storage.getAllTask().map {
            return@map executingTaskListData.value!![it.id] ?: return@map waitingTaskListData.value!![it.id] ?: return@map it
        }
    }

    fun clearAllTask() {
        var map = executingTaskListData.value!!
        var it = map.iterator()
        while (it.hasNext()) {
            val task = it.next().value
            DownloadWorker.cancel(task.id, applicationContext)
        }
        map.clear()
        map = waitingTaskListData.value!!
        it = map.iterator()
        while (it.hasNext()) {
            val task = it.next().value
            DownloadWorker.cancel(task.id, applicationContext)
        }
        map.clear()
        storage.clearAllDownloadTask()
    }

    internal fun getTaskFromStorage(id: String): DownloadTask? {
        return storage.getDownloadTaskById(id)
    }

    internal fun getTaskFromWaiting(id: String): DownloadTask? {
        return waitingTaskListData.value!![id]
    }

    internal fun saveTask(task: DownloadTask) {
        storage.saveDownloadTask(task)
    }

    internal fun insertExecutingTask(task: DownloadTask) {
        val map = executingTaskListData.value!!
        map[task.id] = task
        executingTaskListData.postValue(map)
    }

    internal fun removeExecutingTask(task: DownloadTask) {
        val map = executingTaskListData.value!!
        map.remove(task.id)
        executingTaskListData.postValue(map)
    }

    internal fun insertWaitingTask(task: DownloadTask) {
        val map = waitingTaskListData.value!!
        map[task.id] = task
        waitingTaskListData.postValue(map)
    }

    internal fun removeWaitingTask(task: DownloadTask) {
        val map = waitingTaskListData.value!!
        map.remove(task.id)
        waitingTaskListData.postValue(map)
    }

    fun cancelTask(task: DownloadTask, deleteFile: Boolean = false) {
        val id = task.id
        task.state = DownloadState.Cancel
        DownloadWorker.cancel(id, applicationContext)
        storage.deleteDownloadTask(id)
        if (deleteFile) {
            task.file.delete()
        }
    }

    fun pauseTask(task: DownloadTask) {
        if (task.state == DownloadState.Finished) {
            log.v("task is finished, do not need pause")
            return
        }
        task.state = DownloadState.Pause
        DownloadWorker.cancel(task.id, applicationContext)
        storage.saveDownloadTask(task)
    }

    fun getAllFinishedTask(): List<DownloadTask> {
        return storage.getAllTask().filter { it.state == DownloadState.Finished }
    }

    fun clearAllFinishedTask() {
        storage.getAllTask().forEach {
            if (it.state == DownloadState.Finished) {
                storage.deleteDownloadTask(it.id)
            }
        }
    }

    fun resumeTask(task: DownloadTask) {
        if (task.state == DownloadState.Finished) {
            log.w("task is finished, can not resume")
            return
        }
        startTask(task)
    }


}