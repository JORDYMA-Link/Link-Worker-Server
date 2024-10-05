package com.jordyma.blink.feed_summarizer.service

import com.jordyma.blink.feed_summarizer.listener.dto.FeedSummarizeMessage

interface FeedSummarizerService {

        fun summarizeFeed(payload: FeedSummarizeMessage): Unit

        fun refillToken(): Unit
}