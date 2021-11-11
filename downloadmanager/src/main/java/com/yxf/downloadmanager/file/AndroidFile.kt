package com.yxf.downloadmanager.file

import java.io.InputStream
import java.io.OutputStream

interface AndroidFile {

    fun exist(): Boolean

    fun createIfNotExist()

    fun canOperate(): Boolean

    fun length(): Long

    fun getInputStream(): InputStream

    /**
     * 这里需要返回追加写入的os
     */
    fun getOutputStream(): OutputStream

    fun delete(): Boolean

    /**
     * 文件描述,用于追踪和创建此文件对象
     */
    fun getDescription(): String

    /**
     * 文件类名
     */
    fun getFileType(): String {
        return this::class.simpleName!!
    }

}