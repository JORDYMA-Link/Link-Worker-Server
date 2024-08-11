package com.jordyma.blink.feed.service

import com.jordyma.blink.feed.dto.FeedCalendarListDto
import com.jordyma.blink.global.error.KEYWORDS_NOT_FOUND
import com.jordyma.blink.global.error.exception.BadRequestException
import com.jordyma.blink.global.util.rangeTo
import com.jordyma.blink.feed.dto.FeedCalendarResponseDto
import com.jordyma.blink.feed.dto.FeedItemDto
import com.jordyma.blink.feed.repository.FeedRepository
import com.jordyma.blink.keyword.repository.KeywordRepository
import com.jordyma.blink.user.dto.UserInfoDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Service
class FeedService(
    private val feedRepository: FeedRepository,
    private val keywordRepository: KeywordRepository,
) {

    @Transactional(readOnly = true)
    fun getFeedsByMonth(user: UserInfoDto, yrMonth: String): FeedCalendarResponseDto {
        val yearMonth = YearMonth.parse(yrMonth, DateTimeFormatter.ofPattern("yyyy-MM"))
        val startOfMonth = yearMonth.atDay(1).atStartOfDay()
        val endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59)

        val feeds = feedRepository.findFeedFolderDtoByUserIdAndBetweenDate(user.id, startOfMonth, endOfMonth)
        val feedsByDate = feeds.groupBy { it.feed.createdAt?.toLocalDate() }

        val response = mutableMapOf<String, FeedCalendarListDto>()

        for (date in startOfMonth.toLocalDate().rangeTo(endOfMonth.toLocalDate())) {
            val feedItems = feedsByDate[date]?.map { feedFolderDto ->
                FeedItemDto(
                    folderId = feedFolderDto.folderId,
                    folderName = feedFolderDto.folderName,
                    feedId = feedFolderDto.feed.id,
                    title = feedFolderDto.feed.title,
                    summary = feedFolderDto.feed.summary,
                    source = feedFolderDto.feed.source,
                    sourceUrl = feedFolderDto.feed.url,
                    isMarked = feedFolderDto.feed.isMarked,
                    keywords = getKeywordsByFeedId(feedFolderDto.feed.id) // 키워드 추출 함수
                )
            } ?: emptyList()

            val isArchived = feedItems.isNotEmpty()

            response[date.toString()] = FeedCalendarListDto(
                isArchived = isArchived,
                list = feedItems
            )
        }

        return FeedCalendarResponseDto(response)
    }

    private fun getKeywordsByFeedId(feedId: Long): List<String> {
        val keywords = keywordRepository.findByFeedId(feedId)
        if (keywords.isEmpty()) {
            throw BadRequestException(KEYWORDS_NOT_FOUND)
        }
        return keywords.map { it.keyword }
    }


}
