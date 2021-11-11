package com.yxf.downloadmanager


internal fun String.fixPath(): String {
    var result = this.trim()
    if (result.startsWith("/")) {
        result = result.substringAfter("/")
    }
    if (result.endsWith("/")) {
        result = result.substringBeforeLast("/")
    }
    return result
}

internal fun String.appendPath(child: String): String {
    val parent = fixPath()
    if (parent.isNullOrEmpty()) {
        return child.fixPath()
    }
    val append = child.fixPath()
    if (append.isNullOrEmpty()) {
        return parent
    }
    return "${parent}/${append}"
}

internal fun String.addPathSuffix(): String {
    var result = this.trim()
    if (!result.endsWith("/")) {
        result += "/"
    }
    return result
}

internal fun String.addPathPrefix(): String {
    var result = this.trim()
    if (!result.startsWith("/")) {
        result = "/${result}"
    }
    return result
}

internal fun String.getNameFromPath(): String {
    return substringAfterLast("/")
}

internal fun String.getParentFromPath(): String {
    return substringBeforeLast("/", "")
}