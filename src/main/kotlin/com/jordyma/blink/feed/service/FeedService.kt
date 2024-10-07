package com.jordyma.blink.feed.service

import com.google.firebase.messaging.Message
import com.jordyma.blink.auth.jwt.user_account.UserAccount
import com.jordyma.blink.fcm.client.FcmClient
import com.jordyma.blink.feed.entity.Feed
import com.jordyma.blink.feed.entity.Source
import com.jordyma.blink.feed.entity.Status
import com.jordyma.blink.feed.repository.FeedRepository
import com.jordyma.blink.folder.entity.Recommend
import com.jordyma.blink.folder.repository.RecommendRepository
import com.jordyma.blink.folder.service.FolderService
import com.jordyma.blink.global.exception.ApplicationException
import com.jordyma.blink.global.exception.ErrorCode
import com.jordyma.blink.global.gemini.response.PromptResponse
import com.jordyma.blink.keyword.repository.KeywordRepository
import com.jordyma.blink.keyword.service.KeywordService
import com.jordyma.blink.logger
import com.jordyma.blink.user.dto.UserInfoDto
import com.jordyma.blink.user.entity.SocialType
import com.jordyma.blink.user.entity.User
import com.jordyma.blink.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Service
class FeedService(
    private val feedRepository: FeedRepository,
    private val folderService: FolderService,
    private val keywordRepository: KeywordRepository,
    private val userRepository: UserRepository,
    private val recommendRepository: RecommendRepository,
    private val keywordService: KeywordService,
    private val fcmClient: FcmClient,
) {

    // 요약 실패 피드 생성
    @Transactional
    fun createFailed(userId: Long, link: String) {
        val user = findUserOrElseThrow(userId)
        val failedFolder = folderService.getFailed(user.id!!)
        val feed = Feed(
            folder = failedFolder,
            originUrl = link,
            summary = "",
            title = "",
            platform = "",
            status = Status.FAILED,
        )
        feedRepository.save(feed)
    }

    fun findUserOrElseThrow(userId: Long): User {
        return userRepository.findById(userId).orElseThrow {
            ApplicationException(ErrorCode.USER_NOT_FOUND, "유저를 찾을 수 없습니다.")
        }
    }

    // gemini 요약 결과 업데이트
    @Transactional
    fun updateSummarizedFeed(
        subject: String,
        summary: String,
        category: List<String>,
        keyword: List<String>,
        brunch: Source,
        feedId: Long,
        userId: Long
    ) {
        logger().info(">>>>> feed update start")

        val feed = findFeedOrElseThrow(feedId)
        val folder = folderService.getUnclassified(userId)
        val user = findUserOrElseThrow(userId)

        // 요약 결과 업데이트 (status: COMPLETE 포함)
        feed.updateSummarizedContent(summary, subject, brunch)
        feed.updateFolder(folder)
        feedRepository.save(feed)

        logger().info("요약 결과 업데이트 성공")

        createRecommendFolders(feed, category)
        keywordService.createKeywords(feed, keyword)

        val fcmToken = user.iosPushToken ?: user.aosPushToken ?: ""
        val message = fcmClient.createMessage(
            fcmToken,
            SUMMARY_COMPLETED,
            "# " + feed.title,
            emptyMap()
        )
        fcmClient.send(message)
    }

    @Transactional
    fun createRecommendFolders(feed: Feed, category: List<String>) {
        var cnt = 0
        val recommendFolders: MutableList<Recommend> = mutableListOf()
        for (folderName in category) {
            val recommend = Recommend(
                feed = feed,
                folderName = folderName,
                priority = cnt
            )
            recommendRepository.save(recommend)
            recommendFolders.add(recommend)
            cnt++
        }
        feed.recommendFolders = recommendFolders
    }

    private fun makeFeed(userId: Long, content: PromptResponse, brunch: Source, link: String): Feed {
        val user = findUserOrElseThrow(userId)
        val folder = folderService.getUnclassified(userId)

        // ai 요약 결과로 피드 생성 (유저 매칭을 위해 폴더는 미분류로 지정)
        val feed = Feed(
            folder = folder!!,
            originUrl = link,
            summary = content?.summary ?: "",
            title = content?.subject ?: "",
            platform = brunch.source,
            status = Status.COMPLETED,  // TODO: 워커 이식하면서 수정하기
            isChecked = false,
        )
        return feedRepository.save(feed)
    }


    fun findBrunch(link: String = ""): Source {
        return if(link.contains("blog.naver.com")){
            Source.NAVER_BLOG
        } else if (link.contains("velog.io")){
            Source.VELOG
        } else if (link.contains("brunch.co.kr")){
            Source.BRUNCH
        } else if (link.contains("yozm.wishket")){
            Source.YOZM_IT
        } else if (link.contains("tistory.com")){
            Source.TISTORY
        } else if (link.contains("eopla.net")){
            Source.EO
        } else if (link.contains("youtube.com")) {
            Source.YOUTUBE
        } else if (link.contains("naver.com")) {
            Source.NAVER
        } else if (link.contains("google.com")) {
            Source.GOOGLE
        } else {
            Source.DEFAULT
        }
    }

    fun findFeedOrElseThrow(feedId: Long): Feed{
        return feedRepository.findById(feedId).orElseThrow {
            ApplicationException(ErrorCode.FEED_NOT_FOUND, "피드를 찾을 수 없습니다.")
        }
    }

    companion object{
        const val SUMMARY_COMPLETED = "링크 요약이 완료되었어요."
    }
}
