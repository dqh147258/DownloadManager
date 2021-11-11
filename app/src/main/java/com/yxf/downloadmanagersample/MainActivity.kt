package com.yxf.downloadmanagersample

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.yxf.downloadmanager.DownloadManager
import com.yxf.downloadmanager.DownloadTask
import com.yxf.downloadmanager.DownloadTask.Companion.EVENT_PERCENT
import com.yxf.downloadmanager.DownloadTask.Companion.EVENT_SPEED
import com.yxf.downloadmanager.DownloadTask.Companion.EVENT_STATE
import com.yxf.downloadmanager.file.AndroidFileFactory
import com.yxf.downloadmanagersample.databinding.ActivityMainBinding
import com.yxf.rxandroidextensions.rxRequestSinglePermission

class MainActivity : AppCompatActivity() {

    private val vb by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val url =
        "https://books-1255703580.cos.ap-shanghai.myqcloud.com/OpenGL%E7%BC%96%E7%A8%8B%E6%8C%87%E5%8D%97%EF%BC%88%E7%AC%AC%E4%B9%9D%E7%89%88%E8%8B%B1%E6%96%87%EF%BC%89.pdf"

    private lateinit var task: DownloadTask

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(vb.root)
        DownloadManager.init(applicationContext)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            rxRequestSinglePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe()
        }

        getTask()
        vb.run {
            enqueueTask.setOnClickListener {
                task = DownloadManager.enqueueTask(task)
                observerTask()
            }
            enqueueTaskForce.setOnClickListener {
                task = DownloadManager.enqueueTaskForce(generateNewTask(), true)
                observerTask()
            }
            pauseTask.setOnClickListener {
                DownloadManager.pauseTask(task)
            }
            resumeTask.setOnClickListener {
                DownloadManager.resumeTask(task)
            }
            cancelTask.setOnClickListener {
                DownloadManager.cancelTask(task, true)
            }
        }
    }

    private fun observerTask() {
        task.dataChangedEventData.observe(this) {
            if (task.containEvent(EVENT_STATE)) {
                vb.stateView.text = "state: ${task.state}"
            }
            if (task.containEvent(EVENT_SPEED)) {
                vb.speedView.text = "speed: ${task.speedString}"
            }
            if (task.containEvent(EVENT_PERCENT)) {
                vb.percentView.text = "percent: ${task.percent}"
            }
        }
    }

    private fun getTask(): DownloadTask {
        if (!this::task.isInitialized) {
            generateNewTask()
        }
        return task
    }

    private fun generateNewTask(): DownloadTask {
        task = DownloadTask(url, url, AndroidFileFactory.createDownloadFile("Lawyer/download2.pdf"))
        return task
    }
}