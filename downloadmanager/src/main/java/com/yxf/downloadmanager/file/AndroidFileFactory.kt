package com.yxf.downloadmanager.file

import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.yxf.downloadmanager.DownloadManager
import java.lang.RuntimeException

object AndroidFileFactory {

    private val parserMap = HashMap<String, AndroidFileParser>()

    init {
        addParser(JavaFileParser())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            addParser(MediaStoreFileParser())
        }
    }

    fun createFileByPath(path: String): AndroidFile {
        return create(JavaFile::class.simpleName!!, path)
    }

    /**
     * 创建下载文件夹中的文件
     */
    fun createDownloadFile(relativePath: String): AndroidFile {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStoreFile(
                DownloadManager.applicationContext,
                relativePath,
                Environment.DIRECTORY_DOWNLOADS
            )
        } else {
            var downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
            if (!downloadPath.endsWith("/")) {
                downloadPath += "/"
            }
            var path = downloadPath + relativePath
            return createFileByPath(path)
        }
    }

    /**
     * 创建应用内部文件,其它应用无法访问,卸载时删除
     */
    fun createExternalFile(relativePath: String): AndroidFile {
        var externalPath = DownloadManager.applicationContext.getExternalFilesDir(null)!!.absolutePath
        if (!externalPath.endsWith("/")) {
            externalPath += "/"
        }
        var path = externalPath + relativePath
        return createFileByPath(path)
    }

    fun create(type: String, description: String): AndroidFile {
        val parser = parserMap[type] ?: throw RuntimeException("not support file type: $type")
        return parser.parse(description)
    }

    fun addParser(parser: AndroidFileParser) {
        parserMap[parser.associatedType()] = parser
    }


}