package com.xigong.xiaozhuan.channel.mi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.xigong.xiaozhuan.MoshiFactory
import com.xigong.xiaozhuan.channel.MarketInfo
import com.xigong.xiaozhuan.channel.ReviewState

@JsonClass(generateAdapter = false)
data class MiAppInfoResp(
    /**
     * 是否允许版本更新
     */
    @Json(name = "updateVersion")
    val updateVersion: Boolean,
    @Json(name = "packageInfo")
    val packageInfo: MiAppInfo
) {
    @JsonClass(generateAdapter = false)
    data class MiAppInfo(
        @Json(name = "appName")
        val appName: String,
        @Json(name = "versionName")
        val versionName: String,
        @Json(name = "versionCode")
        val versionCode: Long,
        @Json(name = "packageName")
        val packageName: String,
    )

    companion object {
        val adapter: JsonAdapter<MiAppInfoResp> = MoshiFactory.getAdapter()
    }

    fun toMarketState(): MarketInfo {
        val state = if (updateVersion) ReviewState.Online else ReviewState.UnderReviewXiaomi
        return MarketInfo(
            reviewState = state,
            enableSubmit = updateVersion,
            lastVersionCode = packageInfo.versionCode,
            lastVersionName = packageInfo.versionName
        )
    }
}
