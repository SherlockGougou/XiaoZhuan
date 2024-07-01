package apk.dispatcher.channel.vivo

import apk.dispatcher.channel.MarketState
import apk.dispatcher.channel.ReviewState
import com.google.gson.JsonObject

class VIVOAppInfo(obj: JsonObject) {
    /**
     * 应用分类（appClassify
     * https://dev.vivo.com.cn/documentCenter/doc/344
     */
    val onlineType: Int = obj.get("onlineType").asInt

    /**
     * 1 草稿
     * 2 待审核
     * 3 审核通过
     * 4 审核不通过
     */
    val reviewStatus: Int = obj.get("status").asInt
    val versionCode: Long = obj.get("versionCode").asLong
    val versionName: String = obj.get("versionName").asString

    override fun toString(): String {
        return "VIVOAppInfo(onlineType=$onlineType)"
    }

    fun toMarketState(): MarketState {
        val state = when (reviewStatus) {
            2 -> ReviewState.UnderReview
            3 -> ReviewState.Online
            4 -> ReviewState.Rejected
            else -> ReviewState.Unknown
        }
        return MarketState(reviewState = state, lastVersionCode = versionCode, lastVersionName = versionName)
    }

}