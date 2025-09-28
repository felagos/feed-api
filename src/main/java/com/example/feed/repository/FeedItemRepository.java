package com.example.feed.repository;

import com.example.feed.entity.FeedItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedItemRepository extends JpaRepository<FeedItem, Long> {
    @Query("SELECT f FROM FeedItem f WHERE f.userId = :userId ORDER BY f.createdAt DESC")
    Page<FeedItem> findFeedByUserId(@Param("userId") Long userId, Pageable pageable);
    
    void deleteByPostId(Long postId);
}