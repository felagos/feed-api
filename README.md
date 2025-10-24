# Feed API - Kafka-Based Social Feed System

A high-performance social media feed API built with Spring Boot, featuring Kafka-driven fan-out architecture, Redis caching, and PostgreSQL persistence. This system demonstrates both **push (fan-out)** and **pull** feed models for comparison.

## Overview

This project implements a scalable social feed system similar to Twitter/X, where users can create posts, follow other users, and view personalized timelines. The system uses Apache Kafka for asynchronous event processing and implements the fan-out-on-write pattern for optimal read performance.

### Key Features

- **Kafka-Based Fan-Out**: Asynchronous feed distribution using Kafka events
- **Dual Feed Models**: Both push (fan-out) and pull models for performance comparison
- **Redis Caching**: Multi-layer caching strategy for posts and feeds
- **PostgreSQL**: Robust relational data storage with optimized indexes
- **Active User Filtering**: Only distributes to users active within last 10 days
- **Docker Compose**: Complete containerized development environment

## Architecture

```
┌─────────────┐    POST    ┌──────────────┐    Kafka     ┌─────────────────┐
│   Client    │───────────>│  FeedService │────────────>│ PostCreatedEvent │
└─────────────┘            └──────────────┘   Producer   └─────────────────┘
                                                                   │
                                                                   ▼
┌─────────────┐           ┌───────────────────┐          ┌──────────────┐
│  Feed Items │<──────────│ Kafka Consumer    │<─────────│ Kafka Topic  │
│  (Fanout)   │   Write   │ (Future: Consumer)│ Consumer └──────────────┘
└─────────────┘           └───────────────────┘
```

## Tech Stack

- **Framework**: Spring Boot 3.5.6
- **Language**: Java 21
- **Build Tool**: Gradle 8.14.3
- **Database**: PostgreSQL 15
- **Cache**: Redis 7
- **Message Broker**: Apache Kafka 7.5.0 (with Zookeeper)
- **Containerization**: Docker & Docker Compose

## Prerequisites

- Docker
- Docker Compose
- Make (optional, for simplified commands)
- Java 21 (for local development)

## Quick Start

### Using Makefile (Recommended)

```bash
# Show all available commands
make help

# Start development environment (build + up + logs)
make dev

# Quick start without logs
make up

# Check status
make status

# Test API endpoints
make test-api

# Stop everything
make down
```

### Using Docker Compose Directly

1. **Clone the repository and navigate to the project directory**

2. **Start the services**
   ```bash
   docker-compose up -d
   ```

3. **Check the status**
   ```bash
   docker-compose ps
   ```

4. **View logs**
   ```bash
   # View all logs
   docker-compose logs -f
   
   # View specific service logs
   docker-compose logs -f feed-api
   docker-compose logs -f postgres
   ```

## Services

The application runs as a multi-container Docker environment:

### 1. Feed API Application
- **Container**: `feed-api`
- **Port**: `8080`
- **Context Path**: `/api`
- **Health Check**: `http://localhost:8080/api/actuator/health`
- **Description**: Main Spring Boot application with REST API

### 2. PostgreSQL Database
- **Container**: `feed-postgres`
- **Port**: `5432`
- **Database**: `feed_db`
- **User**: `feed_user`
- **Password**: `feed_password`
- **Volume**: `postgres_data` (persistent storage)
- **Initialization**: Automated via `schema.sql`

### 3. Redis Cache
- **Container**: `feed-redis`
- **Port**: `6379`
- **Description**: In-memory cache for posts and user feeds
- **Cache TTL**: 
  - Posts: 6 hours
  - User Feeds: 15 minutes
  - Feed Items: 1 hour

### 4. Apache Kafka
- **Container**: `feed-kafka`
- **Port**: `9092` (external), `29092` (internal)
- **Topics**: 
  - `post-created-events` (3 partitions)
  - `user-followed-events` (3 partitions)
- **Consumer Group**: `feed-fanout-group`

### 5. Zookeeper
- **Container**: `feed-zookeeper`
- **Port**: `2181`
- **Description**: Kafka coordination service

## API Endpoints

### Push Model (Fan-Out) - **Recommended for Production**
Base URL: `http://localhost:8080/api/feed`

#### Posts
- **POST** `/posts` - Create a new post (triggers Kafka event)
  - Headers: `User-Id: {userId}`
  - Body: `{"content": "Post content"}`
  - Response: Post object with ID

#### Timeline
- **GET** `/timeline` - Get user's pre-calculated feed (O(1) complexity)
  - Headers: `User-Id: {userId}`
  - Query: `page={page}&size={size}`
  - Response: Paginated feed items

#### Social
- **POST** `/follow/{followeeId}` - Follow a user (triggers Kafka event)
  - Headers: `User-Id: {followerId}`
  - Response: 200 OK

- **DELETE** `/follow/{followeeId}` - Unfollow a user
  - Headers: `User-Id: {followerId}`
  - Response: 200 OK

### Pull Model (No Fan-Out) - **For Comparison Only**
Base URL: `http://localhost:8080/api/pull-feed`

- **GET** `/timeline/{userId}` - Real-time feed with JOINs (O(n*m) complexity)
- **GET** `/stats/{userId}` - Get feed complexity statistics
- **GET** `/comparison` - Compare push vs pull models

### Example Usage

```bash
# Create a post (triggers fan-out via Kafka)
curl -X POST http://localhost:8080/api/feed/posts \
  -H "Content-Type: application/json" \
  -H "User-Id: 1" \
  -d '{"content": "Hello from Kafka-powered feed!"}'

# Get timeline (fast, O(1))
curl -X GET "http://localhost:8080/api/feed/timeline?page=0&size=20" \
  -H "User-Id: 1"

# Follow a user (triggers backfill via Kafka)
curl -X POST http://localhost:8080/api/feed/follow/2 \
  -H "User-Id: 1"

# Unfollow a user
curl -X DELETE http://localhost:8080/api/feed/follow/2 \
  -H "User-Id: 1"

# Compare pull model (slower, real-time)
curl -X GET "http://localhost:8080/api/pull-feed/timeline/1?page=0&size=20"

# Get performance stats
curl -X GET "http://localhost:8080/api/pull-feed/stats/1"

# Compare models
curl -X GET "http://localhost:8080/api/pull-feed/comparison"
```

## Development Commands

### Makefile Commands (Recommended)

```bash
# Service Management
make build             # Build the application Docker image
make up                # Start all services in detached mode
make down              # Stop and remove all containers
make restart           # Restart all services
make status            # Show service status

# Logs and Monitoring
make logs              # Show all logs
make app-logs          # Show application logs only
make db-logs           # Show database logs only
make kafka-logs        # Show Kafka logs
make health            # Check application health

# Kafka Operations
make kafka-topics      # List all Kafka topics
make kafka-consumers   # Show consumer group details
make kafka-read-posts  # Read messages from post-created-events topic
make kafka-read-follows # Read messages from user-followed-events topic

# Development Workflow
make dev               # Start development environment (build + up + logs)
make rebuild           # Rebuild and restart everything

# Cleanup
make clean             # Remove containers, networks, and images
make clean-volumes     # Remove volumes (WARNING: deletes database data)
make clean-all         # Complete cleanup including unused Docker resources

# Application
make gradle-build      # Build application using Gradle (without Docker)
make gradle-clean      # Clean Gradle build

# Information
make help              # Show all available commands
```

### Docker Compose Commands

```bash
# Start services
docker-compose up -d

# Rebuild and start (after code changes)
docker-compose up -d --build

# Stop services
docker-compose down

# Stop services and remove volumes (WARNING: This will delete your database data)
docker-compose down -v

# View container logs
docker-compose logs -f feed-api

# Execute commands in containers
docker-compose exec postgres psql -U feed_user -d feed_db
docker-compose exec feed-api bash
```

## Database Connection

To connect to the PostgreSQL database directly:

```bash
# Using docker-compose exec
docker-compose exec postgres psql -U feed_user -d feed_db

# Using external client (while containers are running)
psql -h localhost -p 5432 -U feed_user -d feed_db
```

## Project Structure

```
feed-api/
├── src/main/java/com/example/feed/
│   ├── config/
│   │   ├── AsyncConfig.java          # Disabled: Replaced by Kafka
│   │   ├── CacheConfig.java          # Redis cache configuration
│   │   └── KafkaConfig.java          # Kafka topics configuration
│   ├── controller/
│   │   ├── FeedController.java       # Push model endpoints
│   │   └── NoFanoutFeedController.java # Pull model endpoints
│   ├── dto/
│   │   ├── CreatePostRequest.java
│   │   └── FeedItemDTO.java
│   ├── entity/
│   │   ├── FeedItem.java             # Pre-calculated feed storage
│   │   ├── Follow.java
│   │   ├── Post.java
│   │   └── User.java
│   ├── event/
│   │   ├── PostCreatedEvent.java     # Kafka event for new posts
│   │   └── UserFollowedEvent.java    # Kafka event for new follows
│   ├── listener/
│   │   └── PostFanoutEventListener.java # Disabled: Async replaced by Kafka
│   ├── model/
│   │   └── FeedItemWithPost.java
│   ├── repository/
│   │   ├── FeedCacheRepository.java  # Feed with Redis caching
│   │   ├── FeedItemRepository.java
│   │   ├── FollowRepository.java
│   │   ├── PostCacheRepository.java  # Posts with Redis caching
│   │   ├── PostRepository.java
│   │   └── UserRepository.java
│   ├── service/
│   │   ├── CacheManagementService.java
│   │   ├── FeedService.java          # Main feed logic with Kafka
│   │   ├── NoFanoutFeedService.java  # Pull model for comparison
│   │   └── UserService.java
│   └── FeedApplication.java
├── src/main/resources/
│   ├── application.properties        # Main configuration
│   └── application-kafka.properties  # Kafka-specific config
├── docker-compose.yml                # Multi-container setup
├── Dockerfile                        # Spring Boot container
├── schema.sql                        # Database initialization
├── Makefile                          # Development automation
└── build.gradle                      # Dependencies & build
```

## Future Improvements

### High Priority
- [ ] **Implement Kafka Consumer** for actual async fan-out processing
- [ ] **Add authentication** (JWT/OAuth2)
- [ ] **Rate limiting** on post creation and follows
- [ ] **Monitoring** with Prometheus/Grafana

### Medium Priority
- [ ] **Pagination improvements** (cursor-based)
- [ ] **Feed ranking algorithm** (not just chronological)
- [ ] **Image/media support** for posts
- [ ] **Notification system** via Kafka

### Low Priority
- [ ] **GraphQL API** alongside REST
- [ ] **WebSocket** for real-time updates
- [ ] **Multi-region deployment** support
- [ ] **A/B testing framework**

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is for educational and demonstration purposes.

## References

- **Fan-Out Pattern**: [System Design Interview](https://www.hellointerview.com/learn/system-design/answer-keys/twitter)
- **Kafka with Spring**: [Spring Kafka Docs](https://spring.io/projects/spring-kafka)
- **Redis Caching**: [Spring Cache Abstraction](https://spring.io/guides/gs/caching)
- **Feed Architecture**: [Instagram Engineering Blog](https://instagram-engineering.com/)

---

**Built with ❤️ using Spring Boot, Kafka, Redis, and PostgreSQL**

## Troubleshooting

### Common Issues

1. **Port conflicts**
   ```bash
   # Check which ports are in use
   docker ps
   # Modify ports in docker-compose.yml if needed
   ```

2. **Database connection issues**
   ```bash
   # Check PostgreSQL health
   docker-compose ps postgres
   make db-logs
   ```

3. **Kafka not receiving events**
   ```bash
   # Verify Kafka is running
   docker-compose ps kafka
   
   # Check Kafka topics
   make kafka-topics
   
   # Read recent messages
   make kafka-read-posts
   ```

4. **Redis cache not working**
   ```bash
   # Check Redis connection
   docker exec -it feed-redis redis-cli PING
   # Should return "PONG"
   ```

5. **Application not starting**
   ```bash
   # Check app logs
   make app-logs
   
   # Verify all dependencies are healthy
   make status
   
   # Rebuild from scratch
   make clean
   make build
   make up
   ```

6. **Rebuild after code changes**
   ```bash
   make rebuild
   # Or manually:
   docker-compose down
   docker-compose up -d --build
   ```

### Monitoring Kafka

```bash
# List all topics
make kafka-topics

# Show consumer lag
make kafka-consumers

# Read post creation events
make kafka-read-posts

# Read follow events
make kafka-read-follows

# Full Kafka logs
make kafka-logs
```

## System Design & Architecture

### Feed Distribution Models

#### 1. Push Model (Fan-Out on Write) - **Current Implementation**
**Endpoint**: `/api/feed/timeline`

**How it works**:
1. User creates a post → `FeedService.createPost()`
2. Post saved to PostgreSQL
3. `PostCreatedEvent` sent to Kafka topic
4. Kafka consumer (to be implemented) reads event
5. Fan-out: Creates feed items for all active followers
6. User retrieves feed → Simple query from `feed_items` table

**Pros**:
- ⚡ **Fast reads** (O(1) - simple SELECT by user_id)
- 📈 **Highly scalable** for read-heavy workloads
- 🔄 **Async processing** via Kafka
- 🎯 **Smart filtering** (only active users in last 10 days)

**Cons**:
- 💾 **Higher storage** (duplicated feed items)
- ⏱️ **Write amplification** (fanout to all followers)
- 🕐 **Eventual consistency**

#### 2. Pull Model (Fan-Out on Read) - **For Comparison**
**Endpoint**: `/api/pull-feed/timeline/{userId}`

**How it works**:
1. User requests feed
2. JOIN query: `posts JOIN follows` on user's followed accounts
3. Aggregate and sort all posts in real-time
4. Return paginated results

**Pros**:
- 💾 **Lower storage** (no duplication)
- 🔄 **Immediate consistency**
- 📝 **Simpler write path**

**Cons**:
- 🐌 **Slow reads** (O(n*m) - complex JOINs)
- 📉 **Poor scalability** for users following many accounts
- 🔥 **Database strain** on every request

### Kafka Event Flow

```
POST /posts
    ↓
FeedService.createPost()
    ↓
Save to PostgreSQL
    ↓
KafkaProducerService.sendPostCreatedEvent()
    ↓
Kafka Topic: post-created-events
    ↓
[Kafka Consumer - To Be Implemented]
    ↓
Fan-out to followers (filtered by activity)
    ↓
Bulk insert into feed_items table
```

### Caching Strategy

**Three-layer caching with Redis**:

1. **Posts Cache** (TTL: 6 hours)
   - Cache key: `posts:{postId}`
   - Eviction: On post update/delete

2. **User Feeds Cache** (TTL: 15 minutes)
   - Cache key: `userFeeds:{userId}_{page}_{size}`
   - Eviction: On new post/follow event

3. **Feed Items Cache** (TTL: 1 hour)
   - Cache key: `feedItems:{userId}`
   - Eviction: On feed update

### Database Schema

**Core Tables**:
- `users` - User profiles and activity tracking
- `posts` - User posts (max 500 chars)
- `follows` - Follower relationships
- `feed_items` - **Pre-calculated feed** (fan-out storage)
- `likes`, `comments`, `notifications` - Additional features

**Optimized Indexes**:
```sql
-- Feed retrieval (primary use case)
CREATE INDEX idx_feed_items_user_created 
  ON feed_items(user_id, created_at DESC);

-- Active user filtering
CREATE INDEX idx_users_last_login 
  ON users(last_login_at);

-- Pull model JOINs
CREATE INDEX idx_posts_user_created 
  ON posts(user_id, created_at DESC);
```

### Performance Comparison

| Metric | Push Model (Fan-Out) | Pull Model (No Fan-Out) |
|--------|---------------------|------------------------|
| **Read Time** | ~1-5ms | ~50-500ms+ |
| **Write Time** | ~5-20ms + async | ~1-5ms |
| **Storage** | High (duplicated) | Low (no duplication) |
| **Scalability** | Excellent | Limited |
| **Consistency** | Eventual | Immediate |
| **Best For** | Social media, news feeds | Simple apps, < 50 follows |

### Configuration Files

- `docker-compose.yml` - Multi-container orchestration
- `Dockerfile` - Spring Boot multi-stage build
- `application.properties` - Main app configuration
- `application-kafka.properties` - Kafka-specific config
- `schema.sql` - Database initialization with sample data
- `Makefile` - Development automation scripts
- `build.gradle` - Project dependencies and build config