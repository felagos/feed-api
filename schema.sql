
CREATE TABLE users (
    user_id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_active TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
);

CREATE TABLE user_follows (
    follower_id VARCHAR(255) NOT NULL,
    followed_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_id, followed_id),
    FOREIGN KEY (follower_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (followed_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT chk_no_self_follow CHECK (follower_id != followed_id),
);

CREATE TABLE posts (
    post_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    content_type ENUM('text', 'image', 'video', 'link') NOT NULL,
    content TEXT,
    media_urls VARCHAR(2048)[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
);

CREATE TABLE engagements (
    engagement_id VARCHAR(255) PRIMARY KEY,
    post_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    type ENUM('like', 'comment', 'share') NOT NULL,
    content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY unique_like_share (post_id, user_id, type),
);

CREATE TABLE user_feeds (
    user_id VARCHAR(255) NOT NULL,
    post_id VARCHAR(255) NOT NULL,
    score FLOAT NOT NULL DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, post_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE,
);

INSERT INTO users (user_id, username, email) VALUES 
('user-001', 'john_doe', 'john.doe@example.com'),
('user-002', 'jane_smith', 'jane.smith@example.com'),
('user-003', 'mike_wilson', 'mike.wilson@example.com'),
('user-004', 'sarah_jones', 'sarah.jones@example.com');

INSERT INTO user_follows (follower_id, followed_id) VALUES 
('user-001', 'user-002'),
('user-001', 'user-003'),
('user-002', 'user-001'),
('user-002', 'user-004'),
('user-003', 'user-001'),
('user-004', 'user-002');

INSERT INTO posts (post_id, user_id, content_type, content, media_urls) VALUES 
('post-001', 'user-001', 'text', 'Hello everyone! This is my first post on the platform.', NULL),
('post-002', 'user-002', 'image', 'Check out this amazing sunset!', ARRAY['https://example.com/sunset1.jpg']),
('post-003', 'user-002', 'text', 'Having a great day at the beach!', NULL),
('post-004', 'user-003', 'video', 'Here is my latest cooking tutorial', ARRAY['https://example.com/cooking-video.mp4']),
('post-005', 'user-001', 'link', 'Found this interesting article about technology trends', NULL),
('post-006', 'user-004', 'image', 'My photography portfolio', ARRAY['https://example.com/photo1.jpg', 'https://example.com/photo2.jpg', 'https://example.com/photo3.jpg']);

INSERT INTO engagements (engagement_id, post_id, user_id, type, content) VALUES 
('eng-001', 'post-001', 'user-002', 'like', NULL),
('eng-002', 'post-001', 'user-003', 'comment', 'Welcome to the platform!'),
('eng-003', 'post-002', 'user-001', 'like', NULL),
('eng-004', 'post-002', 'user-003', 'comment', 'Beautiful shot!'),
('eng-005', 'post-003', 'user-001', 'like', NULL),
('eng-006', 'post-004', 'user-002', 'like', NULL),
('eng-007', 'post-004', 'user-004', 'comment', 'Great tutorial, thanks for sharing!'),
('eng-008', 'post-005', 'user-002', 'share', NULL),
('eng-009', 'post-006', 'user-001', 'like', NULL),
('eng-010', 'post-006', 'user-003', 'comment', 'Amazing work!');

INSERT INTO user_feeds (user_id, post_id, score) VALUES 
('user-001', 'post-002', 0.95),
('user-001', 'post-003', 0.87),
('user-001', 'post-004', 0.76),
('user-001', 'post-006', 0.82),
('user-002', 'post-001', 0.91),
('user-002', 'post-004', 0.79),
('user-002', 'post-005', 0.68),
('user-003', 'post-001', 0.88),
('user-003', 'post-002', 0.93),
('user-003', 'post-006', 0.85),
('user-004', 'post-001', 0.72),
('user-004', 'post-002', 0.84),
('user-004', 'post-003', 0.77),
('user-004', 'post-004', 0.89);
