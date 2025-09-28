# Feed API Makefile
# Simplifies Docker Compose and development operations
# Cross-platform compatible for Windows and Unix-based systems

# OS Detection
ifeq ($(OS),Windows_NT)
	# Windows commands
	GRADLE_CMD = gradlew.bat
	SLEEP_CMD = timeout /t
	CURL_CMD = powershell -Command "try { Invoke-RestMethod -Uri 'http://localhost:8080/api/actuator/health' -Method Get -TimeoutSec 5 | Out-Null; Write-Host 'Application is healthy' } catch { Write-Host 'Application not responding'; exit 1 }"
	ECHO_CMD = echo
	RM_CMD = rmdir /s /q
else
	# Unix/Linux/macOS commands
	GRADLE_CMD = ./gradlew
	SLEEP_CMD = sleep
	CURL_CMD = curl -f http://localhost:8080/api/actuator/health || echo "Application not responding"
	ECHO_CMD = echo
	RM_CMD = rm -rf
endif

.PHONY: help build up down restart logs clean status test db-connect db-logs app-logs health rebuild

# Default target
help: ## Show this help message
	@$(ECHO_CMD) "Feed API Makefile Commands:"
	@$(ECHO_CMD) ""
	@$(ECHO_CMD) "Service Management:"
	@$(ECHO_CMD) "  build                Build the application Docker image"
	@$(ECHO_CMD) "  up                   Start all services in detached mode"
	@$(ECHO_CMD) "  down                 Stop and remove all containers"
	@$(ECHO_CMD) "  restart              Restart all services"
	@$(ECHO_CMD) "  stop                 Stop all services without removing containers"
	@$(ECHO_CMD) "  start                Start stopped services"
	@$(ECHO_CMD) ""
	@$(ECHO_CMD) "Logs and Monitoring:"
	@$(ECHO_CMD) "  logs                 Show logs for all services"
	@$(ECHO_CMD) "  app-logs             Show logs for the application only"
	@$(ECHO_CMD) "  db-logs              Show logs for PostgreSQL only"
	@$(ECHO_CMD) "  status               Show status of all services"
	@$(ECHO_CMD) "  health               Check application health endpoint"
	@$(ECHO_CMD) ""
	@$(ECHO_CMD) "Development Operations:"
	@$(ECHO_CMD) "  dev                  Start development environment (build + up + logs)"
	@$(ECHO_CMD) "  rebuild              Rebuild and restart everything"
	@$(ECHO_CMD) ""
	@$(ECHO_CMD) "Cleanup Operations:"
	@$(ECHO_CMD) "  clean                Stop services and remove containers, networks, and images"
	@$(ECHO_CMD) "  clean-volumes        Remove all volumes (WARNING: This will delete database data)"
	@$(ECHO_CMD) "  clean-all            Complete cleanup - remove everything including unused Docker resources"
	@$(ECHO_CMD) ""
	@$(ECHO_CMD) "Application Operations:"
	@$(ECHO_CMD) "  gradle-build         Build application using Gradle (without Docker)"
	@$(ECHO_CMD) "  gradle-clean         Clean Gradle build"
	@$(ECHO_CMD) ""

# Service Management
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
	@$(ECHO_CMD) "Checking application health..."
	@$(CURL_CMD)

# Development Operations
dev: ## Start development environment (build + up + logs)
	$(MAKE) build
	$(MAKE) up
	@$(ECHO_CMD) "Waiting for services to start..."
	@$(SLEEP_CMD) 10
	$(MAKE) health
	$(MAKE) logs

rebuild: ## Rebuild and restart everything
	$(MAKE) down
	$(MAKE) build
	$(MAKE) up
	@$(ECHO_CMD) "Waiting for services to start..."
	@$(SLEEP_CMD) 10
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
	$(GRADLE_CMD) build -x test

gradle-clean: ## Clean Gradle build
	$(GRADLE_CMD) clean