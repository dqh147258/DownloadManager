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

    private val url = "https://download.sanguosha.cn/sgswx/sgs3991.apk"

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
        task = DownloadTask(url, url, AndroidFileFactory.createDownloadFile("sgswx/sgs3991.apk"))
        return task
    }
}