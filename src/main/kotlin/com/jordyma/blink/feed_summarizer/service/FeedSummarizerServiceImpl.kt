package com.jordyma.blink.feed_summarizer.service

import com.jordyma.blink.feed.entity.Status
import com.jordyma.blink.feed.repository.FeedRepository
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
    private val feedRepository: FeedRepository,
): FeedSummarizerService {

    override fun summarizeFeed(payload: FeedSummarizeMessage): PromptResponse? {
        val userId = payload.userId
        val link = payload.link
        val feedId = payload.feedId.toLong()

        try{
            val parseContent = htmlParser.fetchHtmlContent(link)
            val folderNames: List<String> = folderService.getFolders(userId=userId).folderList.map { it.name }
            val content = geminiService.getContents(
                link = link,
                folders = folderNames.joinToString(separator = " "),
                userId = userId,
                parseContent,
                feedId
            )
            if(content != null){
                val brunch = feedService.findBrunch(link)
                feedService.updateSummarizedFeed(
                    content.subject,
                    content.summary,
                    content.category,
                    content.keyword,
                    brunch,
                    feedId,
                    userId
                )
            }
        } catch (e: Exception){
            val feed = feedService.findFeedOrElseThrow(feedId)
            feed.updateStatus(Status.FAILED)
            feedRepository.save(feed)
            logger().error(e.message)
            logger().info("gemini exception: failed to summarize")
        }
        return null
    }

    override fun refillToken(): Unit {
        this.summarizeRequestLimiter.refillToken()
        logger().info("refill token")
    }
}
