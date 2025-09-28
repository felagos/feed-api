package com.example.feed.dto;

import java.time.LocalDateTime;

public class FeedItemDTO {
    private Long postId;
    private Long authorId;
    private String content;
    private LocalDateTime createdAt;
    private Boolean isRead;

    public FeedItemDTO() {}

    public FeedItemDTO(Long postId, Long authorId, String content, LocalDateTime createdAt, Boolean isRead) {
        this.postId = postId;
        this.authorId = authorId;
        this.content = content;
        this.createdAt = createdAt;
        this.isRead = isRead;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
}