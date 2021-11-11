package com.yxf.downloadmanager

import java.lang.RuntimeException
import java.text.ParseException
import java.util.*

enum class DownloadState {

    Create,
    Waiting,
    Downloading,
    Pause,
    Finished,
    Cancel,
    Error;

}