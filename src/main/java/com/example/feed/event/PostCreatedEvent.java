package com.example.feed.event;

import java.time.LocalDateTime;

public class PostCreatedEvent {
    private Long postId;
    private Long authorId;
    private String content;
    private LocalDateTime createdAt;

    public PostCreatedEvent() {}

    public PostCreatedEvent(Long postId, Long authorId, String content, LocalDateTime createdAt) {
        this.postId = postId;
        this.authorId = authorId;
        this.content = content;
        this.createdAt = createdAt;
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
}