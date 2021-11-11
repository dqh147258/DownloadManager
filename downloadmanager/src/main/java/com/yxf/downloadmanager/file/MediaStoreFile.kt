package com.yxf.downloadmanager.file

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import com.yxf.downloadmanager.*
import com.yxf.downloadmanager.addPathPrefix
import com.yxf.downloadmanager.addPathSuffix
import com.yxf.downloadmanager.getNameFromPath
import com.yxf.downloadmanager.getParentFromPath
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

@RequiresApi(Build.VERSION_CODES.Q)
class MediaStoreFile(
    context: Context,
    private val relativePath: String,
    private val folderName: String = Environment.DIRECTORY_DOWNLOADS,
    private val uri: Uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
) : AndroidFile {


    private val context = context.applicationContext

    private val name = relativePath.getNameFromPath()
    private var path = relativePath.getParentFromPath()

    private var created = false

    private var size = 0L
    private val id by lazy { ContentUris.parseId(readFileUri) }

    private val desc by lazy {
        val jo = JSONObject()
        jo.put("relative_path", relativePath)
        jo.put("uri", uri.toString())
        jo.put("folder_name", folderName)
        return@lazy jo.toString()
    }

    private val fileUri by lazy {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.SIZE
        )
        val selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?"
        val args = arrayOf(folderName.appendPath(path).addPathSuffix())
        var fu = context.contentResolver.query(uri, projection, selection, args, null)?.use {
            if (it.count == 0) {
                return@use null
            } else {
                while (it.moveToNext()) {
                    val fileName = it.getString(it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME))
                    if (fileName == name || fileName.substringBeforeLast("(") == name) {
                        val id = it.getLong(it.getColumnIndex(MediaStore.MediaColumns._ID))
                        size = it.getLong(it.getColumnIndex(MediaStore.MediaColumns.SIZE))
                        created = true
                        return@use ContentUris.withAppendedId(uri, id)
                    }
                }
                return@use null
            }
        }
        if (fu == null) {

        }
        return@lazy fu
    }

    private val fileUriAutoCreate by lazy {
        if (fileUri == null) {
            //文件不存在,创建文件
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, folderName.appendPath(path).addPathSuffix())
            //values.put(MediaStore.Images.Media.TITLE, "title_1")
            /*values.put(
                MediaStore.Downloads.DISPLAY_NAME,
                "sample2.pdf"
            )
            values.put(
                MediaStore.Downloads.RELATIVE_PATH,
                "$folderName/sample"
            )*/
            val uri = context.contentResolver.insert(uri, values)
            if (uri != null) {
                created = true
            }
            return@lazy uri
        }
        return@lazy fileUri
    }

    private val readFileUri: Uri by lazy { fileUri ?: fileUriAutoCreate!! }


    override fun exist(): Boolean {
        if (created) return true

        return fileUri != null
    }

    override fun createIfNotExist() {
        if (!exist()) {
            //如果不存在调用下自动创建的fileUri即可
            val uri = fileUriAutoCreate
        }
    }

    override fun canOperate(): Boolean {
        return fileUriAutoCreate != null
    }

    override fun length(): Long {
        if (!exist()) {
            return 0
        }
        if (!canOperate()) {
            return 0
        }
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.SIZE)
        val selection = MediaStore.MediaColumns._ID + "=?"
        val args = arrayOf(id.toString())
        context.contentResolver.query(uri, projection, selection, args, null)?.use {
            if (it.count != 0) {
                while (it.moveToNext()) {
                    size = it.getLong(it.getColumnIndex(MediaStore.MediaColumns.SIZE))
                    break
                }
            }
        }
        return size
    }

    override fun getInputStream(): InputStream {
        return context.contentResolver.openInputStream(readFileUri)!!
    }

    override fun getOutputStream(): OutputStream {
        return context.contentResolver.openOutputStream(readFileUri, "wa")!!
    }

    override fun delete(): Boolean {
        var result = true
        try {
            context.contentResolver.delete(readFileUri, null, null)
        } catch (e: IOException) {
            e.printStackTrace()
            result = false
        }
        if (result) {
            created = false
        }
        return result
    }

    override fun getDescription(): String {
        return desc
    }
}