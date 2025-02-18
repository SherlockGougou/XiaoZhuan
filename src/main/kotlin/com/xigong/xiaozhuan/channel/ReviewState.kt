package com.xigong.xiaozhuan.channel

/**
 * 审核状态
 */
enum class ReviewState(val desc: String) {
    /** 已上线 */
    Online("已上线"),

    /** 审核中 */
    UnderReview("审核中"),

    /** 审核中 */
    UnderReviewXiaomi("审核中(此处版本不准确)"),

    /*** 被拒绝 */
    Rejected("被拒绝"),

    /** 未知状态 */
    Unknown("未知状态")
}