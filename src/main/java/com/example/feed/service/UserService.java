package com.example.feed.service;

import com.example.feed.entity.User;
import com.example.feed.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void updateLastLogin(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("Usuario {} login actualizado", userId);
        } else {
            log.warn("Usuario {} no encontrado para actualizar login", userId);
        }
    }

    public User createUser(String username, String email, String fullName) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setLastLoginAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        log.info("Usuario creado con ID: {}", savedUser.getId());
        return savedUser;
    }

    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    public boolean isUserActiveWithinDays(Long userId, int days) {
        Optional<User> userOpt = userRepository.findById(userId);
        return userOpt.map(user -> user.hasLoggedInWithinDays(days)).orElse(false);
    }
}