package com.jordyma.blink.feed_summarizer.service

import com.jordyma.blink.feed.service.FeedService
import com.jordyma.blink.feed_summarizer.html_parser.HtmlParser
import com.jordyma.blink.feed_summarizer.listener.dto.FeedSummarizeMessage
import com.jordyma.blink.feed_summarizer.request_limiter.SummarizeRequestLimiter
import com.jordyma.blink.folder.service.FolderService
import com.jordyma.blink.gemini.GeminiService
import com.jordyma.blink.global.gemini.response.PromptResponse
import com.jordyma.blink.logger
import org.springframework.stereotype.Service

@Service
class FeedSummarizerServiceImpl(
    private val summarizeRequestLimiter: SummarizeRequestLimiter,
    private val htmlParser: HtmlParser,
    private val folderService: FolderService,
    private val geminiService: GeminiService,
    private val feedService: FeedService,
): FeedSummarizerService {

    override fun summarizeFeed(payload: FeedSummarizeMessage): Unit {
        val userId = payload.userId
        val link = payload.link

        val feedId = payload.feedId.toLong()
        val parseContent = htmlParser.fetchHtmlContent(link)
        val folderNames: List<String> = folderService.getFolders(userId=userId).folderList.map { it.name }

        try{
            val content = geminiService.getContents(
                link = link,
                folders = folderNames.joinToString(separator = " "),
                userId = userId,
                parseContent,
                feedId
            ) as PromptResponse
            val brunch = feedService.findBrunch(link)
            feedService.updateSummarizedFeed(content, brunch, feedId, userId)
        } catch (e: Exception){
            logger().error(e.message)
            logger().info("gemini exception: failed to summarize")
        }
    }

    override fun refillToken(): Unit {
        this.summarizeRequestLimiter.refillToken()
        logger().info("refill token")
    }
}
