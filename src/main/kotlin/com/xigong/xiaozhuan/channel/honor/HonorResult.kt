package com.xigong.xiaozhuan.channel.honor

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.xigong.xiaozhuan.channel.checkApiSuccess


@JsonClass(generateAdapter = false)
data class HonorResult<T>(
    @Json(name = "code")
    val code: Int,
    @Json(name = "msg")
    val msg: String,
    @Json(name = "data")
    val data: T?
) {
    fun throwOnFail(action: String) {
        checkApiSuccess(code, 0, action, msg)
    }
}