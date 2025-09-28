# Feed API Makefile
# Simplifies Docker Compose and development operations

.PHONY: help build up down restart logs clean status test db-connect db-logs app-logs health rebuild


build: ## Build the application Docker image
	docker-compose build --no-cache

up: ## Start all services in detached mode
	docker-compose up -d

down: ## Stop and remove all containers
	docker-compose down

restart: ## Restart all services
	docker-compose restart

stop: ## Stop all services without removing containers
	docker-compose stop

start: ## Start stopped services
	docker-compose start

# Logs and Monitoring
logs: ## Show logs for all services
	docker-compose logs -f

app-logs: ## Show logs for the application only
	docker-compose logs -f feed-api

db-logs: ## Show logs for PostgreSQL only
	docker-compose logs -f postgres

status: ## Show status of all services
	docker-compose ps

health: ## Check application health endpoint
	@echo "Checking application health..."
	@curl -f http://localhost:8080/api/actuator/health || echo "Application not responding"

# Development Operations
dev: ## Start development environment (build + up + logs)
	$(MAKE) build
	$(MAKE) up
	@echo "Waiting for services to start..."
	@sleep 10
	$(MAKE) health
	$(MAKE) logs

rebuild: ## Rebuild and restart everything
	$(MAKE) down
	$(MAKE) build
	$(MAKE) up
	@echo "Waiting for services to start..."
	@sleep 10
	$(MAKE) health

# Cleanup Operations
clean: ## Stop services and remove containers, networks, and images
	docker-compose down --rmi all --volumes --remove-orphans

clean-volumes: ## Remove all volumes (WARNING: This will delete database data)
	docker-compose down -v
	docker volume prune -f

clean-all: ## Complete cleanup - remove everything including unused Docker resources
	$(MAKE) clean
	docker system prune -af --volumes

# Application Operations
gradle-build: ## Build application using Gradle (without Docker)
	./gradlew build -x test

gradle-clean: ## Clean Gradle build
	./gradlew clean