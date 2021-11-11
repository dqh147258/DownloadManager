package com.yxf.downloadmanager

import android.content.Context
import android.util.Log
import com.yxf.downloadmanager.file.AndroidFileFactory
import org.json.JSONObject

internal class SharePreferenceStorage(private val context: Context) : DownloadTaskStorage {

    private val PREFERENCE_NAME = "download_manager_storage"

    private val TAG = "DownloadManager.SharePreferenceStorage"

    private val sp by lazy { context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE) }


    override fun getDownloadTaskById(id: String): DownloadTask? {
        val js = sp.getString(id, null) ?: return null
        return parseStringToDownloadTask(js)
    }

    private fun parseStringToDownloadTask(js: String): DownloadTask {
        val jo = JSONObject(js)
        val id = jo.getString("id")
        val url = jo.getString("url")
        val fileType = jo.getString("file_type")
        val fileDescription = jo.getString("file_description")
        val state = DownloadState.valueOf(jo.getString("state"))
        val totalLength = jo.getLong("total_length")
        val errorMessage = jo.getString("error_message")
        val file = AndroidFileFactory.create(fileType, fileDescription)
        return DownloadTask(id, url, file).also { task ->
            task.eventEnable = false
            task.state = when (state) {
                DownloadState.Downloading -> {
                    //下载中意外停止
                    log.w("haa a task(id = ${task.id}) is stopped when in downloading")
                    DownloadState.Pause
                }
                else -> state
            }
            task.totalLength = totalLength
            task.errorMessage = errorMessage
            task.eventEnable = true
        }
    }

    override fun saveDownloadTask(task: DownloadTask) {
        val jo = JSONObject()
        jo.put("id", task.id)
        jo.put("url", task.url)
        val file = task.file
        jo.put("file_type", file.getFileType())
        jo.put("file_description", file.getDescription())
        jo.put("state", task.state.toString())
        jo.put("total_length", task.totalLength)
        jo.put("error_message", task.errorMessage)
        val js = jo.toString()
        sp.edit().putString(task.id, js).apply()
    }

    override fun deleteDownloadTask(id: String) {
        sp.edit().remove(id).apply()
    }

    override fun clearAllDownloadTask() {
        sp.edit().clear().apply()
    }

    override fun getTaskCount(): Int {
        return sp.all.size
    }

    override fun getAllTask(): List<DownloadTask> {
        return sp.all.values.mapTo(ArrayList()) {
            parseStringToDownloadTask(it as String)
        }
    }
}