package com.example.feed.event;

import java.time.LocalDateTime;

public class UserFollowedEvent {

    private final Long followerId;
    private final Long followeeId;
    private final LocalDateTime followedAt;

    public UserFollowedEvent(Long followerId, Long followeeId, LocalDateTime followedAt) {
        this.followerId = followerId;
        this.followeeId = followeeId;
        this.followedAt = followedAt;
    }

    public Long getFollowerId() {
        return followerId;
    }

    public Long getFolloweeId() {
        return followeeId;
    }

    public LocalDateTime getFollowedAt() {
        return followedAt;
    }

    @Override
    public String toString() {
        return "UserFollowedEvent{" +
                "followerId=" + followerId +
                ", followeeId=" + followeeId +
                ", followedAt=" + followedAt +
                '}';
    }
}