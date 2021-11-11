package com.yxf.downloadmanager.file

import java.io.File

class JavaFileParser : AndroidFileParser {
    override fun parse(description: String): AndroidFile {
        return JavaFile(description)
    }

    override fun associatedType(): String {
        return JavaFile::class.simpleName!!
    }
}