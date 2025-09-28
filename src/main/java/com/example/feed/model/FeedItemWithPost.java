package com.example.feed.model;

import com.example.feed.entity.FeedItem;
import com.example.feed.entity.Post;

import java.time.LocalDateTime;

/**
 * Projection class representing a FeedItem with its associated Post data
 */
public class FeedItemWithPost {
    private Long id;
    private Long userId;
    private Long postId;
    private Long authorId;
    private LocalDateTime createdAt;
    private Boolean isRead;
    private String postContent;
    private LocalDateTime postCreatedAt;

    public FeedItemWithPost() {}

    public FeedItemWithPost(FeedItem feedItem, Post post) {
        this.id = feedItem.getId();
        this.userId = feedItem.getUserId();
        this.postId = feedItem.getPostId();
        this.authorId = feedItem.getAuthorId();
        this.createdAt = feedItem.getCreatedAt();
        this.isRead = feedItem.getIsRead();
        this.postContent = post.getContent();
        this.postCreatedAt = post.getCreatedAt();
    }

    public FeedItemWithPost(Long id, Long userId, Long postId, Long authorId, 
                           LocalDateTime createdAt, Boolean isRead, 
                           String postContent, LocalDateTime postCreatedAt) {
        this.id = id;
        this.userId = userId;
        this.postId = postId;
        this.authorId = authorId;
        this.createdAt = createdAt;
        this.isRead = isRead;
        this.postContent = postContent;
        this.postCreatedAt = postCreatedAt;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public String getPostContent() {
        return postContent;
    }

    public void setPostContent(String postContent) {
        this.postContent = postContent;
    }

    public LocalDateTime getPostCreatedAt() {
        return postCreatedAt;
    }

    public void setPostCreatedAt(LocalDateTime postCreatedAt) {
        this.postCreatedAt = postCreatedAt;
    }
}