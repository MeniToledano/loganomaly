# LogAnomaly Detection Platform

Real-time log monitoring and anomaly detection platform built with Event-Driven Microservices.

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 17, Spring Boot 3, Gradle |
| Frontend | React, TypeScript, Vite |
| Database | PostgreSQL 14 |
| Messaging | Apache Kafka |
| Infrastructure | Docker, Docker Compose |

## Architecture

```mermaid
flowchart TB
    subgraph clients [External Clients]
        User[Dashboard User]
        App1[Client Application]
        App2[Log Agent]
    end

    subgraph frontend [Frontend Layer]
        UI[React Dashboard - Port 5173]
    end

    subgraph backend [Backend Microservices]
        Auth[Auth Service - Port 8080]
        Ingestion[Ingestion Service - Port 8081]
        Analysis[Analysis Service - Port 8082]
    end

    subgraph messaging [Message Broker]
        Kafka[(Apache Kafka - log-events topic)]
    end

    subgraph storage [Data Layer]
        Postgres[(PostgreSQL)]
    end

    User -->|JWT Auth| UI
    UI -->|Login/Register| Auth
    UI -->|View Events| Analysis
    
    App1 -->|API Key + Logs| Ingestion
    App2 -->|API Key + Logs| Ingestion
    
    Auth -->|Store Users| Postgres
    Ingestion -->|Produce Events| Kafka
    Kafka -->|Consume Events| Analysis
    Analysis -->|Store Events| Postgres
```

## Data Flow

```mermaid
sequenceDiagram
    participant Client as Client App
    participant Ingestion as Ingestion Service
    participant Kafka as Kafka
    participant Analysis as Analysis Service
    participant DB as PostgreSQL

    Client->>Ingestion: POST /api/logs (X-API-Key)
    Ingestion->>Ingestion: Validate & Sanitize Input
    Ingestion->>Kafka: Produce log event
    Ingestion-->>Client: 202 Accepted (event ID)
    
    Kafka->>Analysis: Consume log event
    Analysis->>DB: Store event
    Analysis->>Analysis: Check for anomalies
    Note over Analysis,DB: If >5 errors/min → Create Alert
```

## Services

| Service | Purpose | Port |
|---------|---------|------|
| auth-service | User registration, login, JWT | 8080 |
| ingestion-service | Receives logs, validates, sanitizes, publishes to Kafka | 8081 |
| analysis-service | Consumes from Kafka, stores in DB, anomaly detection | 8082 |
| frontend-ui | React dashboard | 5173 |
| postgres-db | PostgreSQL database | 5432 |
| kafka | Message broker | 9092 |

## Quick Start

```bash
# Start everything
docker-compose up --build -d

# View logs
docker-compose logs -f

# Stop and clean
docker-compose down -v
```

## API Usage

### Send Logs (Ingestion Service)

```bash
# Single log
curl -X POST http://localhost:8081/api/logs \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key-here-change-in-production" \
  -d '{
    "level": "INFO",
    "message": "User login successful",
    "service": "auth-service",
    "metadata": {"userId": "123"}
  }'

# Batch logs
curl -X POST http://localhost:8081/api/logs/batch \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key-here-change-in-production" \
  -d '{
    "logs": [
      {"level": "INFO", "message": "Request processed", "service": "api-gateway"},
      {"level": "ERROR", "message": "Connection failed", "service": "db-service"}
    ]
  }'
```

### Query Events (Analysis Service)

```bash
# Get recent events
curl http://localhost:8082/api/events

# Get event count
curl http://localhost:8082/api/events/count

# Filter by service
curl http://localhost:8082/api/events/service/auth-service

# Filter by level
curl http://localhost:8082/api/events/level/ERROR
```

### Alerts API (Anomaly Detection)

```bash
# Get all alerts
curl http://localhost:8082/api/alerts

# Get unacknowledged alerts
curl http://localhost:8082/api/alerts/unacknowledged

# Get alert statistics
curl http://localhost:8082/api/alerts/stats

# Acknowledge an alert
curl -X PATCH "http://localhost:8082/api/alerts/{id}/acknowledge?acknowledgedBy=admin"
```

### Authentication (Auth Service)

```bash
# Register
curl -X POST http://localhost:8080/register \
  -H "Content-Type: application/json" \
  -d '{"username": "user1", "email": "user1@example.com", "password": "password123"}'

# Login
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user1", "password": "password123"}'
```

## Log Event Schema

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| level | String | Yes | TRACE, DEBUG, INFO, WARN, ERROR, FATAL |
| message | String | Yes | Log message (max 64KB) |
| service | String | Yes | Source service name (max 100 chars) |
| timestamp | ISO-8601 | No | Auto-generated if not provided |
| metadata | Object | No | Additional key-value pairs |

## Security Features

- **Input Validation**: All log fields are validated with Jakarta Bean Validation
- **Input Sanitization**: Log messages are sanitized to remove:
  - `<script>` tags (XSS prevention)
  - SQL injection patterns (`DROP`, `DELETE`, etc.)
  - Control characters
- **API Key Authentication**: Machine-to-machine auth for log ingestion
- **JWT Authentication**: User authentication for dashboard access
- **Secrets Externalization**: All credentials stored in environment variables

## Local Development

```bash
# Start infrastructure only
docker-compose up -d postgres-db zookeeper kafka

# Build all services
./gradlew build

# Run individual services
./gradlew :auth-service:bootRun
./gradlew :ingestion-service:bootRun
./gradlew :analysis-service:bootRun

# Run frontend
cd frontend-ui && npm install && npm run dev
```

## Testing

```bash
# Run all tests
./gradlew test

# Test specific service
./gradlew :ingestion-service:test
./gradlew :analysis-service:test
./gradlew :auth-service:test
```

## Project Structure

```
loganomaly/
├── auth-service/          # JWT authentication
├── ingestion-service/     # Log ingestion (Kafka producer)
├── analysis-service/      # Event processing (Kafka consumer)
├── frontend-ui/           # React dashboard
├── common/                # Shared DTOs
├── docker-compose.yml
└── test-pipeline.http     # API test file
```

## Configuration

### Environment Variables (Secrets)

All secrets are externalized to environment variables with fallback defaults:

| Variable | Service | Default |
|----------|---------|---------|
| `INGESTION_API_KEY` | ingestion-service | `your-api-key-here-change-in-production` |
| `JWT_SECRET` | auth-service | 32-char secret key |
| `DB_USER` | auth/analysis | `user` |
| `DB_PASSWORD` | auth/analysis | `password` |

**Production**: Set these in your environment before running:

```bash
export INGESTION_API_KEY="my-secure-api-key"
export JWT_SECRET="my-256-bit-secret-key-at-least-32-chars"
export DB_USER="prod_user"
export DB_PASSWORD="strong_password"
docker-compose up --build -d
```

### Anomaly Detection Settings

| Variable | Default | Description |
|----------|---------|-------------|
| `ANOMALY_ERROR_THRESHOLD` | 5 | Errors to trigger alert |
| `ANOMALY_TIME_WINDOW_MINUTES` | 1 | Time window for counting |
| `ANOMALY_COOLDOWN_MINUTES` | 5 | Cooldown between alerts |

### Service Configuration

| Config | Location | Key Settings |
|--------|----------|--------------|
| API Key | ingestion-service/application.properties | `ingestion.api-key` |
| JWT Secret | auth-service/application.properties | `jwt.secret` |
| Database | */application.properties | `spring.datasource.*` |
| Kafka | */application.properties | `spring.kafka.bootstrap-servers` |

## Troubleshooting

```bash
# Check running containers
docker ps

# View service logs
docker-compose logs -f ingestion-service
docker-compose logs -f analysis-service

# Restart a service
docker-compose restart analysis-service

# Reset database
docker-compose down -v
docker-compose up --build -d
```
