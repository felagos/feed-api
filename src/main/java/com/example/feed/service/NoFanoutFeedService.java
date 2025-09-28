package com.example.feed.service;

import com.example.feed.dto.FeedItemDTO;
import com.example.feed.entity.Post;
import com.example.feed.repository.FollowRepository;
import com.example.feed.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class NoFanoutFeedService {

    private static final Logger log = LoggerFactory.getLogger(NoFanoutFeedService.class);

    private final PostRepository postRepository;
    private final FollowRepository followRepository;

    public NoFanoutFeedService(PostRepository postRepository, FollowRepository followRepository) {
        this.postRepository = postRepository;
        this.followRepository = followRepository;
    }

    public Page<FeedItemDTO> getUserFeedPullModel(Long userId, int page, int size) {
        long startTime = System.currentTimeMillis();
        log.info("Iniciando consulta de feed sin fan-out para usuario {}", userId);

        Pageable pageable = PageRequest.of(page, size);

        long followingCount = followRepository.countByFollowerId(userId);
        log.info("Usuario {} sigue a {} personas", userId, followingCount);

        List<Post> posts = postRepository.findPostsFromFollowedUsers(userId, pageable);

        long queryTime = System.currentTimeMillis() - startTime;
        log.info("Consulta pull completada en {} ms para {} posts", queryTime, posts.size());

        List<FeedItemDTO> feedItems = posts.stream()
                .map(post -> new FeedItemDTO(
                        post.getId(),
                        post.getUserId(),
                        post.getContent(),
                        post.getCreatedAt(),
                        false))
                .toList();

        long totalElements = Math.max(feedItems.size(), (long) page * size + feedItems.size());

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Feed pull model completado en {} ms total", totalTime);

        return new PageImpl<>(feedItems, pageable, totalElements);
    }

    public FeedComplexityStats getComplexityStats(Long userId) {
        long followingCount = followRepository.countByFollowerId(userId);

        long avgPostsPerUser = 50;
        long estimatedPostsToScan = followingCount * avgPostsPerUser;

        return new FeedComplexityStats(
                followingCount,
                estimatedPostsToScan,
                "O(n*m) donde n=usuarios seguidos, m=posts promedio por usuario");
    }

    public record FeedComplexityStats(
            long usersFollowing,
            long estimatedPostsToScan,
            String timeComplexity) {
    }
}