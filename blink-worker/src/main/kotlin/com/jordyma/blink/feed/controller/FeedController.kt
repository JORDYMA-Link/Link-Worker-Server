package com.jordyma.blink.feed.controller

import com.jordyma.blink.feed.service.FeedService
import com.jordyma.blink.user.service.UserService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/feeds")
class FeedController(
    private val feedService: FeedService,
    private val userService: UserService
) {
//    @Operation(summary = "캘린더 피드 검색 api", description = "년도와 월(yyyy-MM)을 param으로 넣어주면, 해당 월의 피드들을 반환해줍니다.")
//    @GetMapping("")
//    fun getFeedsByDate(
//        @RequestParam("yearMonth") yearMonth: String,
//        @RequestUserId userId: Long
//    ): ResponseEntity<FeedCalendarResponseDto> {
//        val userDto: UserInfoDto = userService.find(userId)
//        val response = feedService.getFeedsByMonth(user = userDto, yrMonth = yearMonth)
//        return ResponseEntity.ok(response)
//    }

}
