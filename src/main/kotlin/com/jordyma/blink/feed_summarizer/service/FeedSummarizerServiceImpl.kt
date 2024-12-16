package com.jordyma.blink.feed_summarizer.service

import com.jordyma.blink.feed.entity.Source
import com.jordyma.blink.feed.entity.Status
import com.jordyma.blink.feed.repository.FeedRepository
import com.jordyma.blink.feed.service.FeedService
import com.jordyma.blink.feed_summarizer.html_parser.HtmlParser
import com.jordyma.blink.feed_summarizer.listener.dto.FeedSummarizeMessage
import com.jordyma.blink.feed_summarizer.request_limiter.SummarizeRequestLimiter
import com.jordyma.blink.folder.service.FolderService
import com.jordyma.blink.gemini.GeminiService
import com.jordyma.blink.global.exception.ApplicationException
import com.jordyma.blink.global.exception.ErrorCode
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
            var thumbnailImage = parseContent.thumbnailImage
            val folderNames: List<String> = folderService.getFolders(userId=userId).folderList.map { it.name }
            val content = geminiService.getContents(
                link = link,
                folders = folderNames.joinToString(separator = " "),
                userId = userId,
                parseContent.content,
                feedId
            )
            if (content == null){
                throw ApplicationException(ErrorCode.JSON_PARSING_FAILED, "gemini exception: no content")
            }

            // 플랫폼별 이미지 추출
            val brunch = feedService.findBrunch(link)
            if (brunch == Source.BRUNCH){
                thumbnailImage = thumbnailImage.removePrefix("//")
            }

            logger().info(
                """
            Attempting to update summarized feed with the following arguments:
            subject: ${content.subject}
            summary: ${content.summary}
            category: ${content.category}
            keyword: ${content.keyword}
            brunch: $brunch
            feedId: $feedId
            userId: $userId
            thumbnailImage: $thumbnailImage
            """.trimIndent()
            )

            feedService.updateSummarizedFeed(
                content.subject,
                content.summary,
                content.category,
                content.keyword,
                brunch,
                feedId,
                userId,
                thumbnailImage,
            )
        } catch (e: Exception){
            val feed = feedService.findFeedOrElseThrow(feedId)
            feed.updateStatus(Status.FAILED)
            feedRepository.save(feed)
            logger().error(e.message)
            logger().info("gemini exception: failed to summarize ${payload.originUrl} by userName ${payload.userName}")
        }
        return null
    }

    override fun refillToken(): Unit {
        this.summarizeRequestLimiter.refillToken()
        logger().info("refill token")
    }
}
