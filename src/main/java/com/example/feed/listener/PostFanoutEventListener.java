package com.example.feed.listener;

import com.example.feed.entity.FeedItem;
import com.example.feed.entity.Follow;
import com.example.feed.entity.Post;
import com.example.feed.event.PostCreatedEvent;
import com.example.feed.event.UserFollowedEvent;
import com.example.feed.repository.FeedItemRepository;
import com.example.feed.repository.FollowRepository;
import com.example.feed.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PostFanoutEventListener {
    
    private static final Logger log = LoggerFactory.getLogger(PostFanoutEventListener.class);
    
    private final FeedItemRepository feedItemRepository;
    private final FollowRepository followRepository;
    private final PostRepository postRepository;
    
    public PostFanoutEventListener(FeedItemRepository feedItemRepository,
                                  FollowRepository followRepository,
                                  PostRepository postRepository) {
        this.feedItemRepository = feedItemRepository;
        this.followRepository = followRepository;
        this.postRepository = postRepository;
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
        
        List<FeedItem> feedItems = followers.stream()
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
        
        log.info("Fanout completado para post ID: {} - {} seguidores notificados", 
                event.getPostId(), feedItems.size());
    }
    
    @EventListener
    @Async("fanoutTaskExecutor")
    public void handleUserFollowed(UserFollowedEvent event) {
        log.info("Iniciando fanout de posts existentes para nuevo seguidor: {} del usuario: {}", 
                event.getFollowerId(), event.getFolloweeId());
        
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
        
        log.info("Fanout de posts existentes completado para usuario {} - {} posts agregados al feed", 
                event.getFollowerId(), feedItems.size());
    }
}