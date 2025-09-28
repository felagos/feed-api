package com.example.feed.controller;

import com.example.feed.dto.CreatePostRequest;
import com.example.feed.dto.FeedItemDTO;
import com.example.feed.entity.Post;
import com.example.feed.service.FeedService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/feed")
@Validated
public class FeedController {
    
    private final FeedService feedService;
    
    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }
    
    @PostMapping("/posts")
    public ResponseEntity<Post> createPost(
            @RequestHeader("User-Id") Long userId,
            @Valid @RequestBody CreatePostRequest request) {
        
        Post post = feedService.createPost(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(post);
    }
    
    @GetMapping("/timeline")
    public ResponseEntity<Page<FeedItemDTO>> getUserFeed(
            @RequestHeader("User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<FeedItemDTO> feed = feedService.getUserFeed(userId, page, size);
        return ResponseEntity.ok(feed);
    }
    
    @PostMapping("/follow/{followeeId}")
    public ResponseEntity<Void> followUser(
            @RequestHeader("User-Id") Long followerId,
            @PathVariable Long followeeId) {
        
        feedService.followUser(followerId, followeeId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/follow/{followeeId}")
    public ResponseEntity<Void> unfollowUser(
            @RequestHeader("User-Id") Long followerId,
            @PathVariable Long followeeId) {
        
        feedService.unfollowUser(followerId, followeeId);
        return ResponseEntity.ok().build();
    }
}