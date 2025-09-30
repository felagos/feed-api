package com.example.feed.service;

import com.example.feed.dto.CreatePostRequest;
import com.example.feed.dto.FeedItemDTO;
import com.example.feed.entity.Follow;
import com.example.feed.entity.Post;
import com.example.feed.event.PostCreatedEvent;
import com.example.feed.event.UserFollowedEvent;
import com.example.feed.model.FeedItemWithPost;
import com.example.feed.repository.FeedCacheRepository;
import com.example.feed.repository.FollowRepository;
import com.example.feed.repository.PostCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class FeedService {

    private static final Logger log = LoggerFactory.getLogger(FeedService.class);

    private final PostCacheRepository postCacheRepository;
    private final FeedCacheRepository feedCacheRepository;
    private final FollowRepository followRepository;
    private final ApplicationEventPublisher eventPublisher;

    public FeedService(PostCacheRepository postCacheRepository,
            FeedCacheRepository feedCacheRepository,
            FollowRepository followRepository,
            ApplicationEventPublisher eventPublisher) {
        this.postCacheRepository = postCacheRepository;
        this.feedCacheRepository = feedCacheRepository;
        this.followRepository = followRepository;
        this.eventPublisher = eventPublisher;
    }

    @CacheEvict(value = {"userFeeds", "feedItems"}, allEntries = true)
    public Post createPost(Long userId, CreatePostRequest request) {
        Post post = new Post();
        post.setUserId(userId);
        post.setContent(request.getContent());

        Post savedPost = postCacheRepository.save(post);

        PostCreatedEvent event = new PostCreatedEvent(
                savedPost.getId(),
                savedPost.getUserId(),
                savedPost.getContent(),
                savedPost.getCreatedAt());

        eventPublisher.publishEvent(event);

        log.info("Post creado con ID: {} por usuario: {}", savedPost.getId(), userId);
        return savedPost;
    }

    public Post getPostById(Long postId) {
        return postCacheRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post no encontrado con ID: " + postId));
    }

    @Cacheable(value = "userFeeds", key = "#userId + '_' + #page + '_' + #size")
    public Page<FeedItemDTO> getUserFeed(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<FeedItemWithPost> feedItemsWithPosts = feedCacheRepository.findFeedWithPostsByUserId(userId, pageable);
        
        log.info("Recuperando feed para usuario: {} desde caché o base de datos", userId);
        
        return feedItemsWithPosts.map(item -> new FeedItemDTO(
                item.getPostId(),
                item.getAuthorId(),
                item.getPostContent(),
                item.getCreatedAt(),
                item.getIsRead()));
    }

    @CacheEvict(value = {"userFeeds", "feedItems"}, allEntries = true)
    public void followUser(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new IllegalArgumentException("No puedes seguirte a ti mismo");
        }

        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw new IllegalArgumentException("Ya sigues a este usuario");
        }

        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFolloweeId(followeeId);
        followRepository.save(follow);

        UserFollowedEvent userFollowedEvent = new UserFollowedEvent(
                followerId,
                followeeId,
                LocalDateTime.now());
        eventPublisher.publishEvent(userFollowedEvent);

        log.info("Usuario {} ahora sigue a usuario {}", followerId, followeeId);
    }

    @CacheEvict(value = {"userFeeds", "feedItems"}, allEntries = true)
    public void unfollowUser(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new IllegalArgumentException("No puedes dejar de seguirte a ti mismo");
        }

        if (!followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw new IllegalArgumentException("No sigues a este usuario");
        }

        followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);

        feedCacheRepository.deleteByUserIdAndAuthorId(followerId, followeeId);

        log.info("Usuario {} ya no sigue a usuario {}", followerId, followeeId);
    }

}