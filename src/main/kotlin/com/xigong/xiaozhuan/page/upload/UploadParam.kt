package com.xigong.xiaozhuan.page.upload

import com.squareup.moshi.JsonClass
import com.xigong.xiaozhuan.MoshiFactory

@JsonClass(generateAdapter = false)
data class UploadParam(
    /**
     * ApplicationID
     */
    val appId: String,
    /**
     * 更新描述
     */
    val updateDesc: String,
    /**
     * 需要更新的Channel
     */
    val channels: List<String>,
    /**
     * 选中的Apk文件
     */
    val apkFile: String
) {
    companion object {
        val adapter = MoshiFactory.getAdapter<UploadParam>()
    }
}
