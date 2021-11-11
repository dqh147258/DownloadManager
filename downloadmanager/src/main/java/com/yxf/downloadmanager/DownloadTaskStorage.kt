package com.yxf.downloadmanager

interface DownloadTaskStorage {


    /**
     * 返回下载任务,如果为空表示不存在
     */
    fun getDownloadTaskById(id: String): DownloadTask?


    /**
     * 存储任务信息,包括插入和更新
     */
    fun saveDownloadTask(task: DownloadTask)

    /**
     * 删除存储的下载任务
     */
    fun deleteDownloadTask(id: String)

    /**
     * 清空所有任务
     */
    fun clearAllDownloadTask()

    fun getTaskCount(): Int

    fun getAllTask(): List<DownloadTask>



}