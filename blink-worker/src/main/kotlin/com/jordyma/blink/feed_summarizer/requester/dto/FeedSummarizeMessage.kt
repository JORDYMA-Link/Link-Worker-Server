package com.jordyma.blink.feed_summarizer.requester.dto

data class FeedSummarizeMessage (
    // TODO 메세지 형식에 따라 변경가능 지금은 임시로 넣어둔 것.
    val link: String,
    val userId: Long,
)