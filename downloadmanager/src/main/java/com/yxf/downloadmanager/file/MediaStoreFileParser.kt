package com.yxf.downloadmanager.file

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.yxf.downloadmanager.DownloadManager
import org.json.JSONObject

@RequiresApi(Build.VERSION_CODES.Q)
class MediaStoreFileParser : AndroidFileParser {
    override fun parse(description: String): AndroidFile {
        val jo = JSONObject(description)
        val relativePath = jo.getString("relative_path")
        val uriString = jo.getString("uri")
        val folderName = jo.getString("folder_name")
        val uri = Uri.parse(uriString)
        return MediaStoreFile(DownloadManager.applicationContext, relativePath, folderName, uri)
    }

    override fun associatedType(): String {
        return MediaStoreFile::class.simpleName!!
    }
}