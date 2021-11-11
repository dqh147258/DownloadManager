package com.yxf.downloadmanager.file

interface AndroidFileParser {

    fun parse(description: String): AndroidFile

    fun associatedType(): String

}