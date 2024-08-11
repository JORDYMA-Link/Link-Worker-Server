package com.jordyma.blink.feed_summarizer.service

import com.jordyma.blink.feed_summarizer.listener.dto.FeedSummarizeMessage
import com.jordyma.blink.feed_summarizer.request_limiter.SummarizeRequestLimiter
import org.springframework.stereotype.Service

@Service
class FeedSummarizerServiceImpl(
    private val summarizeRequestLimiter: SummarizeRequestLimiter
): FeedSummarizerService {

    override fun summarizeFeed(payload: FeedSummarizeMessage): Unit {
        //TODO Implement this method
    }

    override fun refillToken(): Unit {
        this.summarizeRequestLimiter.refillToken()
    }
}