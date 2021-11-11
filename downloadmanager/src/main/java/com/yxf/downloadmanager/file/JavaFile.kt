package com.yxf.downloadmanager.file

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception

class JavaFile(private val file: File) : AndroidFile {


    constructor(path: String) : this(File(path))


    override fun exist(): Boolean = file.exists()
    override fun createIfNotExist() {
        file.parentFile.let {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
        if (!file.exists()) {
            file.createNewFile()
        }
    }

    override fun canOperate(): Boolean {
        return file.exists() && file.canWrite() && file.canRead()
    }

    override fun length(): Long = file.length()

    override fun getInputStream(): InputStream = file.inputStream()

    override fun getOutputStream(): OutputStream = FileOutputStream(file, true)

    override fun delete(): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    override fun getDescription(): String {
        return file.absolutePath
    }

}