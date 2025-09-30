package com.example.feed.service;

import com.example.feed.repository.FeedCacheRepository;
import com.example.feed.repository.PostCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CacheManagementService {

    private static final Logger log = LoggerFactory.getLogger(CacheManagementService.class);

    private final PostCacheRepository postCacheRepository;
    private final FeedCacheRepository feedCacheRepository;

    public CacheManagementService(PostCacheRepository postCacheRepository, 
                                FeedCacheRepository feedCacheRepository) {
        this.postCacheRepository = postCacheRepository;
        this.feedCacheRepository = feedCacheRepository;
    }

    public void evictPost(Long postId) {
        log.info("Evictando post con ID: {} del caché", postId);
        postCacheRepository.evictPostFromCache(postId);
    }

    public void evictAllPosts() {
        log.info("Evictando todos los posts del caché");
        postCacheRepository.evictAllPostsFromCache();
    }

    public void evictUserFeed(Long userId) {
        log.info("Evictando feed del usuario: {} del caché", userId);
        feedCacheRepository.evictUserFeedFromCache(userId);
    }

    public void evictUserFeedPage(Long userId, int page, int size) {
        log.info("Evictando página específica del feed del usuario: {} (página: {}, tamaño: {}) del caché", 
                userId, page, size);
        feedCacheRepository.evictUserFeedPageFromCache(userId, page, size);
    }

    public void evictAllUserFeeds() {
        log.info("Evictando todos los feeds de usuarios del caché");
        feedCacheRepository.evictAllUserFeedsFromCache();
    }

    public void clearAllCaches() {
        log.info("Limpiando todos los cachés");
        evictAllPosts();
        evictAllUserFeeds();
    }

    public void warmUpUserFeedCache(Long userId, int page, int size) {
        log.info("Precalentando caché del feed para usuario: {}", userId);
        try {
            feedCacheRepository.findFeedWithPostsByUserId(userId, 
                org.springframework.data.domain.PageRequest.of(page, size));
            log.info("Caché precalentado exitosamente para usuario: {}", userId);
        } catch (Exception e) {
            log.error("Error al precalentar caché para usuario: {}", userId, e);
        }
    }

    public void warmUpPostCache(Long postId) {
        log.info("Precalentando caché del post con ID: {}", postId);
        try {
            postCacheRepository.findById(postId);
            log.info("Caché del post precalentado exitosamente para ID: {}", postId);
        } catch (Exception e) {
            log.error("Error al precalentar caché del post con ID: {}", postId, e);
        }
    }
}