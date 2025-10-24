package com.example.feed.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    
    public static final String POST_CREATED_TOPIC = "post-created-events";
    public static final String USER_FOLLOWED_TOPIC = "user-followed-events";
    
    @Bean
    public NewTopic postCreatedTopic() {
        return TopicBuilder.name(POST_CREATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic userFollowedTopic() {
        return TopicBuilder.name(USER_FOLLOWED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
