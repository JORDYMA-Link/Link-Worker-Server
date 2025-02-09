package com.jordyma.blink.folder.repository

import com.jordyma.blink.folder.entity.Recommend
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RecommendRepository : JpaRepository<Recommend, Long> {
    @Query("SELECT r FROM Recommend r WHERE r.feed.id = :feedId")
    fun findRecommendationsByFeedId(feedId: Long): List<Recommend>

    @Query("select r from Recommend r where r.feed.id =: feedId and r.priority =: priority")
    fun findRecommendFirst(feedId: Long, priority: Int): Recommend
}