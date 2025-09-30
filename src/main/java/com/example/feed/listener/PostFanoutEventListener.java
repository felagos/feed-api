package com.example.feed.listener;

import com.example.feed.entity.FeedItem;
import com.example.feed.entity.Follow;
import com.example.feed.entity.Post;
import com.example.feed.event.PostCreatedEvent;
import com.example.feed.event.UserFollowedEvent;
import com.example.feed.repository.FeedItemRepository;
import com.example.feed.repository.FollowRepository;
import com.example.feed.repository.PostRepository;
import com.example.feed.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PostFanoutEventListener {
    
    private static final Logger log = LoggerFactory.getLogger(PostFanoutEventListener.class);
    private static final int ACTIVE_USER_DAYS = 10;
    
    private final FeedItemRepository feedItemRepository;
    private final FollowRepository followRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    
    public PostFanoutEventListener(FeedItemRepository feedItemRepository,
                                  FollowRepository followRepository,
                                  PostRepository postRepository,
                                  UserRepository userRepository) {
        this.feedItemRepository = feedItemRepository;
        this.followRepository = followRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }
    
    @EventListener
    @Async("fanoutTaskExecutor")
    public void handlePostCreated(PostCreatedEvent event) {
        log.info("Iniciando fanout para post ID: {}", event.getPostId());
        
        List<Follow> followers = followRepository.findByFolloweeId(event.getAuthorId());
        
        if (followers.isEmpty()) {
            log.info("No hay seguidores para fanout del post ID: {}", event.getPostId());
            return;
        }
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(ACTIVE_USER_DAYS);
        List<Long> followerIds = followers.stream()
            .map(Follow::getFollowerId)
            .collect(Collectors.toList());
        
        Set<Long> activeUserIds = userRepository.findActiveUsersInList(followerIds, cutoffDate)
            .stream()
            .map(user -> user.getId())
            .collect(Collectors.toSet());
        
        List<FeedItem> feedItems = followers.stream()
            .filter(follow -> activeUserIds.contains(follow.getFollowerId()))
            .map(follow -> new FeedItem(
                null,
                follow.getFollowerId(),
                event.getPostId(),
                event.getAuthorId(),
                event.getCreatedAt(),
                false
            ))
            .collect(Collectors.toList());
        
        feedItemRepository.saveAll(feedItems);
        
        log.info("Fanout completado para post ID: {} - {} seguidores activos notificados de {} seguidores totales", 
                event.getPostId(), feedItems.size(), followers.size());
    }
    
    @EventListener
    @Async("fanoutTaskExecutor")
    public void handleUserFollowed(UserFollowedEvent event) {
        log.info("Iniciando fanout de posts existentes para nuevo seguidor: {} del usuario: {}", 
                event.getFollowerId(), event.getFolloweeId());
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(ACTIVE_USER_DAYS);
        boolean isFollowerActive = userRepository.findActiveUsersInList(
            List.of(event.getFollowerId()), cutoffDate).size() > 0;
        
        if (!isFollowerActive) {
            log.info("Usuario {} no ha tenido actividad en los últimos {} días, omitiendo fanout", 
                    event.getFollowerId(), ACTIVE_USER_DAYS);
            return;
        }
        
        List<Post> existingPosts = postRepository.findByUserIdAndIsActiveTrue(event.getFolloweeId());
        
        if (existingPosts.isEmpty()) {
            log.info("No hay posts existentes para fanout del usuario: {}", event.getFolloweeId());
            return;
        }
        
        List<FeedItem> feedItems = existingPosts.stream()
            .map(post -> new FeedItem(
                null,
                event.getFollowerId(),
                post.getId(),
                post.getUserId(),
                post.getCreatedAt(),
                false
            ))
            .collect(Collectors.toList());
        
        feedItemRepository.saveAll(feedItems);
        
        log.info("Fanout de posts existentes completado para usuario activo {} - {} posts agregados al feed", 
                event.getFollowerId(), feedItems.size());
    }
}