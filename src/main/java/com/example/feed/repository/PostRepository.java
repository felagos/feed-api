package com.example.feed.repository;

import com.example.feed.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    List<Post> findByUserIdAndIsActiveTrue(Long userId);
    
    @Query("""
        SELECT p FROM Post p 
        JOIN Follow f ON p.userId = f.followeeId 
        WHERE f.followerId = :userId 
        AND p.isActive = true 
        ORDER BY p.createdAt DESC
    """)
    List<Post> findPostsFromFollowedUsers(@Param("userId") Long userId, Pageable pageable);
}