-- Database will be created automatically by PostgreSQL init process
-- Using the database specified in POSTGRES_DB environment variable

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    bio TEXT,
    profile_image_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_last_login ON users(last_login_at);

CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL CHECK (LENGTH(content) <= 500),
    image_url VARCHAR(500),
    likes_count INTEGER DEFAULT 0,
    comments_count INTEGER DEFAULT 0,
    shares_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_posts_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);
CREATE INDEX idx_posts_is_active ON posts(is_active);
CREATE INDEX idx_posts_user_created ON posts(user_id, created_at DESC);

CREATE TABLE follows (
    id BIGSERIAL PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    followee_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_follows_follower_id 
        FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_follows_followee_id 
        FOREIGN KEY (followee_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_follow_relationship 
        UNIQUE (follower_id, followee_id),
    CONSTRAINT check_no_self_follow 
        CHECK (follower_id != followee_id)
);

CREATE INDEX idx_follows_follower_id ON follows(follower_id);
CREATE INDEX idx_follows_followee_id ON follows(followee_id);
CREATE INDEX idx_follows_created_at ON follows(created_at);

CREATE TABLE feed_items (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_feed_items_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_feed_items_post_id 
        FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_feed_items_author_id 
        FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_feed_item 
        UNIQUE (user_id, post_id)
);

CREATE INDEX idx_feed_items_user_created ON feed_items(user_id, created_at DESC);
CREATE INDEX idx_feed_items_post_id ON feed_items(post_id);
CREATE INDEX idx_feed_items_author_id ON feed_items(author_id);
CREATE INDEX idx_feed_items_is_read ON feed_items(user_id, is_read);

CREATE TABLE likes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_likes_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_likes_post_id 
        FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT unique_like 
        UNIQUE (user_id, post_id)
);

CREATE INDEX idx_likes_user_id ON likes(user_id);
CREATE INDEX idx_likes_post_id ON likes(post_id);
CREATE INDEX idx_likes_created_at ON likes(created_at);

CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    parent_comment_id BIGINT,
    content TEXT NOT NULL CHECK (LENGTH(content) <= 300),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_comments_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_post_id 
        FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_parent_id 
        FOREIGN KEY (parent_comment_id) REFERENCES comments(id) ON DELETE CASCADE
);

CREATE INDEX idx_comments_post_id ON comments(post_id, created_at);
CREATE INDEX idx_comments_user_id ON comments(user_id);
CREATE INDEX idx_comments_parent_id ON comments(parent_comment_id);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    actor_id BIGINT,
    type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_notifications_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_actor_id 
        FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_is_read ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_type ON notifications(type);

CREATE VIEW user_stats AS
SELECT 
    u.id,
    u.username,
    u.full_name,
    COUNT(DISTINCT p.id) as posts_count,
    COUNT(DISTINCT f1.id) as followers_count,
    COUNT(DISTINCT f2.id) as following_count,
    u.created_at
FROM users u
LEFT JOIN posts p ON u.id = p.user_id AND p.is_active = true
LEFT JOIN follows f1 ON u.id = f1.followee_id
LEFT JOIN follows f2 ON u.id = f2.follower_id
WHERE u.is_active = true
GROUP BY u.id, u.username, u.full_name, u.created_at;

CREATE VIEW posts_with_stats AS
SELECT 
    p.*,
    u.username as author_username,
    u.full_name as author_name,
    u.profile_image_url as author_avatar,
    COALESCE(l.likes_count, 0) as actual_likes_count,
    COALESCE(c.comments_count, 0) as actual_comments_count
FROM posts p
JOIN users u ON p.user_id = u.id
LEFT JOIN (
    SELECT post_id, COUNT(*) as likes_count 
    FROM likes 
    GROUP BY post_id
) l ON p.id = l.post_id
LEFT JOIN (
    SELECT post_id, COUNT(*) as comments_count 
    FROM comments 
    WHERE is_active = true 
    GROUP BY post_id
) c ON p.id = c.post_id
WHERE p.is_active = true;

CREATE OR REPLACE FUNCTION update_post_counters()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF TG_TABLE_NAME = 'likes' THEN
            UPDATE posts SET likes_count = likes_count + 1 
            WHERE id = NEW.post_id;
        ELSIF TG_TABLE_NAME = 'comments' THEN
            UPDATE posts SET comments_count = comments_count + 1 
            WHERE id = NEW.post_id;
        END IF;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        IF TG_TABLE_NAME = 'likes' THEN
            UPDATE posts SET likes_count = GREATEST(likes_count - 1, 0) 
            WHERE id = OLD.post_id;
        ELSIF TG_TABLE_NAME = 'comments' THEN
            UPDATE posts SET comments_count = GREATEST(comments_count - 1, 0) 
            WHERE id = OLD.post_id;
        END IF;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_likes_counter
    AFTER INSERT OR DELETE ON likes
    FOR EACH ROW EXECUTE FUNCTION update_post_counters();

CREATE TRIGGER trigger_comments_counter
    AFTER INSERT OR DELETE ON comments
    FOR EACH ROW EXECUTE FUNCTION update_post_counters();

CREATE OR REPLACE FUNCTION cleanup_old_feed_items(days_to_keep INTEGER DEFAULT 30)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM feed_items 
    WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '1 day' * days_to_keep;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_user_feed(
    p_user_id BIGINT,
    p_limit INTEGER DEFAULT 20,
    p_offset INTEGER DEFAULT 0
)
RETURNS TABLE (
    post_id BIGINT,
    author_id BIGINT,
    author_username VARCHAR,
    author_name VARCHAR,
    content TEXT,
    likes_count INTEGER,
    comments_count INTEGER,
    created_at TIMESTAMP,
    is_read BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        p.id as post_id,
        p.user_id as author_id,
        u.username as author_username,
        u.full_name as author_name,
        p.content,
        p.likes_count,
        p.comments_count,
        fi.created_at,
        fi.is_read
    FROM feed_items fi
    JOIN posts p ON fi.post_id = p.id
    JOIN users u ON p.user_id = u.id
    WHERE fi.user_id = p_user_id
      AND p.is_active = true
      AND u.is_active = true
    ORDER BY fi.created_at DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

INSERT INTO users (username, email, full_name, bio, last_login_at) VALUES
('john_doe', 'john@example.com', 'John Doe', 'Tech enthusiast and coffee lover', CURRENT_TIMESTAMP - INTERVAL '1 day'),
('jane_smith', 'jane@example.com', 'Jane Smith', 'Designer by day, photographer by night', CURRENT_TIMESTAMP - INTERVAL '2 days'),
('tech_guru', 'guru@example.com', 'Tech Guru', 'Sharing the latest in technology', CURRENT_TIMESTAMP - INTERVAL '5 days'),
('foodie_mike', 'mike@example.com', 'Mike Johnson', 'Food blogger and chef', CURRENT_TIMESTAMP - INTERVAL '15 days'),
('travel_sarah', 'sarah@example.com', 'Sarah Wilson', 'World traveler and storyteller', CURRENT_TIMESTAMP - INTERVAL '3 days');

INSERT INTO follows (follower_id, followee_id) VALUES
(1, 2), (1, 3), (1, 4),
(2, 1), (2, 3), (2, 5),
(3, 1), (3, 2), (3, 4), (3, 5),
(4, 1), (4, 2), (4, 3),
(5, 1), (5, 2), (5, 3), (5, 4);

INSERT INTO posts (user_id, content) VALUES
(1, 'Just finished a great book on system design. Highly recommended!'),
(2, 'Beautiful sunset today. Sometimes you need to pause and appreciate the moment.'),
(3, 'New JavaScript framework released. Here are my thoughts...'),
(4, 'Tried a new recipe today. The combination of flavors was amazing!'),
(5, 'Currently in Tokyo. The city never fails to amaze me.');

CREATE INDEX idx_feed_complex_query ON feed_items(user_id, created_at DESC, is_read);