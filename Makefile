.PHONY: build up down restart logs test clean

# Build and start everything
up:
	docker compose up --build -d

# Stop everything
down:
	docker compose down

# Stop and remove all data
clean:
	docker compose down -v

# Restart the app (rebuild)
restart:
	docker compose up --build -d app

# View logs (follow)
logs:
	docker compose logs -f app

# View all logs
logs-all:
	docker compose logs -f

# Run tests locally (uses H2, no Docker needed)
test:
	./mvnw test

# Build JAR locally
build:
	./mvnw clean package -DskipTests
