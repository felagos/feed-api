package com.example.feed.repository;

import com.example.feed.entity.FeedItem;
import com.example.feed.model.FeedItemWithPost;
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
    
    @Query("""
           SELECT new FeedItemWithPost(
               f.id, f.userId, f.postId, f.authorId, f.createdAt, f.isRead, 
               p.content, p.createdAt
           ) 
           FROM FeedItem f 
           JOIN Post p ON f.postId = p.id 
           WHERE f.userId = :userId 
           ORDER BY f.createdAt DESC
           """)
    Page<FeedItemWithPost> findFeedWithPostsByUserId(@Param("userId") Long userId, Pageable pageable);
    
    void deleteByPostId(Long postId);
    void deleteByUserIdAndAuthorId(Long userId, Long authorId);
}