# Feed API - AI Coding Assistant Instructions

## Project Overview
This is a social media feed API built with Spring Boot 3.5 and Java 21, demonstrating two different architectural approaches for feed generation: **push (fanout)** and **pull** models. The project compares performance trade-offs between pre-computed feeds vs. real-time aggregation.

## Architecture & Core Concepts

### Dual Feed Strategy
- **Push Model** (`/api/feed/*`): Pre-computed feeds using async fanout with Redis caching
- **Pull Model** (`/api/pull-feed/*`): Real-time feed generation with JOIN queries
- Both models coexist for performance comparison and demonstration

### Event-Driven Fanout System
- `PostCreatedEvent` triggers async fanout to followers via `PostFanoutEventListener`
- `UserFollowedEvent` backfills existing posts to new followers
- Async processing uses dedicated thread pool (`fanoutTaskExecutor`: 5-10 threads)
- Only processes active users (activity within 10 days)

### Caching Strategy (Redis)
```java
// Cache TTL by type
posts: 6 hours
userFeeds: 15 minutes  
feedItems: 1 hour
```
- Cache keys: `userId_page_size` for feeds, complexity stats cached separately
- Manual cache eviction on write operations (`@CacheEvict`)

## Development Workflow

### Essential Commands
```bash
# Primary development workflow
make dev              # build + up + logs (recommended)
make rebuild          # full rebuild cycle
make health           # check /api/actuator/health

# Service management
make up/down          # start/stop services
make app-logs         # application logs only
make db-logs          # PostgreSQL logs only
```

### Database Setup
- PostgreSQL with Redis for caching
- Auto-initialization via `schema.sql` in docker-entrypoint-initdb.d
- Connection pools: max 10, min idle 5

## Code Patterns & Conventions

### API Authentication Pattern
All endpoints require `User-Id` header (no auth middleware):
```java
@RequestHeader("User-Id") Long userId
```

### Service Layer Pattern
- `FeedService`: Push model with caching and events
- `NoFanoutFeedService`: Pull model for comparison
- Cache repositories wrap data repositories for consistent caching

### Entity Relationships
```
User -> Post (1:N)
User -> Follow -> User (M:N via followerId/followeeId)
FeedItem: denormalized feed entries (userId, postId, authorId)
```

### Event Processing
- Events published via `ApplicationEventPublisher`
- `@Async` listeners with custom executor
- Active user filtering to optimize fanout performance

## Key Files & Responsibilities

### Controllers
- `FeedController`: Push model endpoints (`/api/feed/*`)
- `NoFanoutFeedController`: Pull model + complexity analysis (`/api/pull-feed/*`)

### Core Services  
- `FeedService`: Main business logic with caching
- `PostFanoutEventListener`: Async fanout processing
- `CacheManagementService`: Cache operations

### Configuration
- `AsyncConfig`: Thread pool for fanout (`fanoutTaskExecutor`)
- `CacheConfig`: Redis cache manager with TTL settings
- `application.properties`: DB pools, Redis config, debug logging

## Performance Considerations

### Cache Strategy
- Feed queries are expensive, aggressively cached
- Write operations evict relevant cache entries
- Pull model caches complexity stats separately

### Fanout Optimization
- Only fans out to active users (10-day window)
- Async processing prevents blocking API calls
- Batch processing for feed item creation

## Common Tasks

### Adding New Feed Features
1. Extend `FeedService` for push model
2. Add corresponding method in `NoFanoutFeedService` for comparison
3. Update cache eviction logic if needed
4. Consider event publishing for async processing

### Debugging Performance
- Use `/api/pull-feed/stats/{userId}` for complexity analysis
- Check Redis cache hit rates in logs
- Monitor fanout thread pool via application logs
- Use `make app-logs` to filter application-specific output

### Database Changes
- Update `schema.sql` for docker initialization
- Consider cache eviction strategy for new fields
- Test both push and pull models for new queries