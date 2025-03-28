package com.xigong.xiaozhuan.util

import java.util.Locale

/**
 * 当前系统是不是windows
 */
fun isWindows(): Boolean {
    return System.getProperty("os.name")
        .lowercase(Locale.getDefault())
        .contains("windows")
}
