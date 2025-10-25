# Copilot Instructions for Feed API

## Project Overview
This is a **Kafka-driven social feed system** implementing the **fan-out-on-write pattern** for high-performance timeline generation. The system demonstrates both push (fan-out) and pull (real-time) feed models for comparison.

## Architecture Patterns

### Core Event-Driven Flow
- **Posts**: `FeedService.createPost()` → Save to PostgreSQL → Send `PostCreatedEvent` to Kafka → Future: Async fan-out to followers
- **Follows**: `FeedService.followUser()` → Save relationship → Send `UserFollowedEvent` to Kafka → Future: Backfill user feeds
- **Feeds**: Pre-calculated `feed_items` table provides O(1) timeline retrieval vs O(n*m) JOINs in pull model

### Multi-Layer Caching Strategy
The system uses **three Redis cache layers** with different TTLs:
- `userFeeds` (15 min): Paginated timeline results - `@Cacheable` on `FeedCacheRepository.findFeedWithPostsByUserId()`
- `posts` (6 hours): Individual posts - `PostCacheRepository` wraps `PostRepository`
- `feedItems` (1 hour): Raw feed items - Cache eviction on `@CacheEvict(value = {"userFeeds", "feedItems"})`

### Repository Pattern with Caching
- **Cached Repos**: `FeedCacheRepository`, `PostCacheRepository` - Add caching layer over JPA repos
- **Direct Repos**: `FeedItemRepository`, `PostRepository`, etc. - Standard JPA repositories
- **Pattern**: Service → Cached Repo → Direct Repo → Database

## Development Workflows

### Essential Commands (Use Makefile)
```bash
make dev           # Full development setup: build + up + logs
make rebuild       # After code changes: down + build + up
make health        # Check if app is responding on :8080/api/actuator/health
make kafka-topics  # List Kafka topics (post-created-events, user-followed-events)
make app-logs      # Application logs only (filters noise)
```

### Testing API Endpoints
**Push Model (Main)**: `/api/feed/*` - Uses pre-calculated feeds
**Pull Model (Comparison)**: `/api/pull-feed/*` - Real-time JOINs

All endpoints require `User-Id` header. Example:
```bash
curl -X POST http://localhost:8080/api/feed/posts \
  -H "User-Id: 1" -H "Content-Type: application/json" \
  -d '{"content": "Test post"}'
```

### Database Access
```bash
make down && make up  # Reset database with schema.sql sample data
docker-compose exec postgres psql -U feed_user -d feed_db
```

## Project-Specific Conventions

### Service Layer Patterns
- **Transactional Services**: All `@Service` classes use `@Transactional` for consistency
- **Cache Eviction**: Use `@CacheEvict(value = {"userFeeds", "feedItems"}, allEntries = true)` on write operations
- **Event Publishing**: Services publish to Kafka, don't handle consumption (future: separate consumer services)

### Configuration Management
- **Environment-based**: `application.properties` for defaults, Docker Compose overrides via env vars
- **Kafka Topics**: Defined as `@Bean` in `KafkaConfig` - 3 partitions, 1 replica for local dev
- **Connection Pools**: HikariCP configured for PostgreSQL, Lettuce for Redis

### Entity Design
- **Primary Keys**: Auto-generated `@Id` Long values
- **Timestamps**: `@CreationTimestamp` for audit fields
- **Relationships**: Minimal JPA associations - prefer explicit queries over lazy loading
- **Feed Storage**: `FeedItem` entities store denormalized feed data for fast retrieval

### Controller Patterns
- **Header-based Auth**: `@RequestHeader("User-Id")` simulates authentication
- **Validation**: `@Valid` on request bodies, `@Validated` on controllers
- **Error Handling**: Return `ResponseEntity` with appropriate HTTP status codes
- **Base Path**: All APIs under `/api` context path

### Integration Points
- **Docker Compose**: 5 services - app, postgres, redis, kafka, zookeeper
- **Health Checks**: All containers have health checks, app depends on healthy dependencies
- **Kafka Topics**: Auto-created on first message, consumer group `feed-fanout-group`
- **Database Init**: `schema.sql` runs on container startup with sample data

## Key Files for Understanding
- `FeedService.java` - Main business logic and Kafka event publishing
- `FeedCacheRepository.java` - Caching patterns and cache key strategies  
- `docker-compose.yml` - Multi-service architecture and environment setup
- `KafkaConfig.java` - Topic definitions and partitioning strategy
- `application.properties` - Database pools, cache TTLs, Kafka configuration

## Future Implementation Notes
The system currently **publishes** Kafka events but lacks **consumer implementation**. When adding consumers:
- Create `@KafkaListener` methods for `PostCreatedEvent` and `UserFollowedEvent`
- Implement bulk `FeedItem` creation for active followers (last 10 days filter)
- Handle consumer failures and dead letter queues
- Consider consumer scaling with multiple partitions

## Common Patterns to Follow
- **Service Methods**: Return entities, throw `IllegalArgumentException` for business rule violations
- **Repository Queries**: Use `@Query` for complex joins, method naming for simple queries
- **Caching**: Always evict caches on write operations, use composite cache keys for pagination
- **Events**: Include all necessary data in event objects to avoid lookups in consumers
- **Docker**: Use health checks and dependency conditions for reliable startup ordering