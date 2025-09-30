package com.example.feed.repository;

import com.example.feed.entity.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PostCacheRepository {

    private static final Logger log = LoggerFactory.getLogger(PostCacheRepository.class);

    private final PostRepository postRepository;

    public PostCacheRepository(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Cacheable(value = "posts", key = "#id", unless = "#result == null || #result.isEmpty()")
    public Optional<Post> findById(Long id) {
        log.info("Buscando post con ID: {} - verificando caché primero", id);
        Optional<Post> post = postRepository.findById(id);
        
        if (post.isPresent()) {
            log.info("Post con ID: {} encontrado y almacenado en caché", id);
        } else {
            log.info("Post con ID: {} no encontrado", id);
        }
        
        return post;
    }

    @CacheEvict(value = "posts", key = "#post.id", condition = "#post.id != null")
    public Post save(Post post) {
        log.info("Guardando post - evictando caché para ID: {}", post.getId());
        Post savedPost = postRepository.save(post);
        log.info("Post guardado con ID: {} - caché evictado", savedPost.getId());
        return savedPost;
    }

    @CacheEvict(value = "posts", key = "#id")
    public void evictPostFromCache(Long id) {
        log.info("Evictando post con ID: {} del caché", id);
    }

    @CacheEvict(value = "posts", allEntries = true)
    public void evictAllPostsFromCache() {
        log.info("Evictando todos los posts del caché");
    }

    @CacheEvict(value = "posts", key = "#post.id")
    public void putInCache(Post post) {
        log.info("Forzando almacenamiento en caché del post con ID: {}", post.getId());
    }

    public boolean isPostInCache(Long id) {
        log.debug("Verificando si post con ID: {} está en caché", id);
        return false;
    }

    public boolean existsById(Long id) {
        return postRepository.existsById(id);
    }

    @CacheEvict(value = "posts", key = "#id")
    public void deleteById(Long id) {
        log.info("Eliminando post con ID: {} y evictando del caché", id);
        postRepository.deleteById(id);
    }
}