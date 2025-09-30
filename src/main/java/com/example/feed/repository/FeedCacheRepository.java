package com.example.feed.repository;

import com.example.feed.model.FeedItemWithPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class FeedCacheRepository {

    private static final Logger log = LoggerFactory.getLogger(FeedCacheRepository.class);

    private final FeedItemRepository feedItemRepository;

    public FeedCacheRepository(FeedItemRepository feedItemRepository) {
        this.feedItemRepository = feedItemRepository;
    }

    @Cacheable(value = "userFeeds", key = "#userId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<FeedItemWithPost> findFeedWithPostsByUserId(Long userId, Pageable pageable) {
        log.info("Recuperando feed para usuario: {} - página: {}, tamaño: {} - verificando caché primero", 
                userId, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<FeedItemWithPost> result = feedItemRepository.findFeedWithPostsByUserId(userId, pageable);
        
        log.info("Feed recuperado para usuario: {} - {} elementos encontrados", userId, result.getTotalElements());
        
        return result;
    }

    @CacheEvict(value = "userFeeds", key = "#userId + '_*'")
    public void evictUserFeedFromCache(Long userId) {
        log.info("Evictando feed del usuario: {} del caché", userId);
    }

    @CacheEvict(value = "userFeeds", allEntries = true)
    public void evictAllUserFeedsFromCache() {
        log.info("Evictando todos los feeds de usuarios del caché");
    }

    @CacheEvict(value = "userFeeds", key = "#userId + '_' + #page + '_' + #size")
    public void evictUserFeedPageFromCache(Long userId, int page, int size) {
        log.info("Evictando página específica del feed del usuario: {} (página: {}, tamaño: {}) del caché", 
                userId, page, size);
    }

    public Page<FeedItemWithPost> findFeedWithPostsByUserIdDirectly(Long userId, Pageable pageable) {
        log.info("Acceso directo a la base de datos para feed del usuario: {} (sin caché)", userId);
        return feedItemRepository.findFeedWithPostsByUserId(userId, pageable);
    }

    @CacheEvict(value = "userFeeds", allEntries = true)
    public void deleteByUserIdAndAuthorId(Long userId, Long authorId) {
        log.info("Eliminando elementos del feed para usuario: {} del autor: {} y evictando caché", userId, authorId);
        feedItemRepository.deleteByUserIdAndAuthorId(userId, authorId);
    }
}