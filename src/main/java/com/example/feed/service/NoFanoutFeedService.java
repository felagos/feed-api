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

/**
 * Servicio que implementa el modelo PULL (sin fan-out) para obtener feeds.
 * 
 * PROPÓSITO EDUCATIVO:
 * Este servicio demuestra cómo funciona un sistema de feed tradicional
 * donde los posts se consultan en tiempo real cuando el usuario solicita su feed.
 * 
 * DIFERENCIAS CON FeedService:
 * - FeedService: Usa tabla pre-calculada (feed_items) - RÁPIDO
 * - NoFanoutFeedService: Consulta en tiempo real con JOINs - LENTO
 * 
 * CASOS DE USO:
 * - Demostración de diferencias de rendimiento
 * - Sistemas pequeños con pocos usuarios
 * - Aplicaciones donde la consistencia inmediata es crítica
 */
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
    
    /**
     * Obtiene el feed de un usuario usando el modelo PULL (sin fan-out).
     * 
     * FLUJO:
     * 1. Consulta quiénes sigue el usuario (tabla follows)
     * 2. Obtiene posts de esos usuarios (JOIN posts + follows)
     * 3. Ordena por fecha de creación
     * 4. Aplica paginación
     * 
     * PROBLEMAS DE RENDIMIENTO:
     * - JOIN costoso entre posts y follows
     * - Tiempo de consulta crece con número de seguidos
     * - Mayor carga en la base de datos
     * - No escalable para usuarios con muchos follows
     * 
     * @param userId ID del usuario que solicita su feed
     * @param page Número de página (0-based)
     * @param size Número de elementos por página
     * @return Página de posts del feed
     */
    public Page<FeedItemDTO> getUserFeedPullModel(Long userId, int page, int size) {
        long startTime = System.currentTimeMillis();
        log.info("Iniciando consulta de feed sin fan-out para usuario {}", userId);
        
        Pageable pageable = PageRequest.of(page, size);
        
        // Contar seguidores para logging de rendimiento
        long followingCount = followRepository.countByFollowerId(userId);
        log.info("Usuario {} sigue a {} personas", userId, followingCount);
        
        // Esta consulta hace JOIN y puede ser muy costosa
        List<Post> posts = postRepository.findPostsFromFollowedUsers(userId, pageable);
        
        long queryTime = System.currentTimeMillis() - startTime;
        log.info("Consulta pull completada en {} ms para {} posts", queryTime, posts.size());
        
        // Convertir a DTOs
        List<FeedItemDTO> feedItems = posts.stream()
                .map(post -> new FeedItemDTO(
                        post.getId(),
                        post.getUserId(), // authorId
                        post.getContent(),
                        post.getCreatedAt(),
                        false // No hay información de "leído" en modelo pull
                ))
                .toList();
        
        // Simular conteo total para paginación
        // En producción real, harías una consulta COUNT separada
        long totalElements = Math.max(feedItems.size(), (long) page * size + feedItems.size());
        
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Feed pull model completado en {} ms total", totalTime);
        
        return new PageImpl<>(feedItems, pageable, totalElements);
    }
    
    /**
     * Obtiene estadísticas de rendimiento para análisis comparativo.
     * 
     * @param userId ID del usuario
     * @return Información sobre complejidad de la consulta
     */
    public FeedComplexityStats getComplexityStats(Long userId) {
        long followingCount = followRepository.countByFollowerId(userId);
        
        // Estimar posts por usuario seguido (promedio)
        long avgPostsPerUser = 50; // Estimación
        long estimatedPostsToScan = followingCount * avgPostsPerUser;
        
        return new FeedComplexityStats(
                followingCount,
                estimatedPostsToScan,
                "O(n*m) donde n=usuarios seguidos, m=posts promedio por usuario"
        );
    }
    
    /**
     * Record para estadísticas de complejidad del feed
     */
    public record FeedComplexityStats(
            long usersFollowing,
            long estimatedPostsToScan,
            String timeComplexity
    ) {}
}