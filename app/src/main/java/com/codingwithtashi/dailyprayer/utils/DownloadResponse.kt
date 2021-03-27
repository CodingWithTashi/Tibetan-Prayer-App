package com.codingwithtashi.dailyprayer.utils

import java.io.File

class DownloadResponse(
    var status: STATUS = STATUS.PENDING,
    var progress: Int = 0,
    var error: String = "",
    var data: String = ""
) {


}

enum class STATUS {
    PENDING,
    SUCCESS,
    DOWNLOADING,
    ERROR
}