# LogAnomaly Detection Platform

## 🎯 Overview

This project is a platform for monitoring and detecting anomalies in logs in real-time. The system is built on an Event-Driven Microservices architecture using Spring Boot and utilizes Apache Kafka as the backbone for data streaming.

## ⚙️ Key Technologies

- **Backend**: Java 17, Spring Boot 3, Gradle Multi-Module
- **Frontend**: React, TypeScript, Vite
- **Database**: PostgreSQL 14 (for storing users and alerts)
- **Messaging**: Apache Kafka 7.3.0 (for log streaming)
- **Infrastructure**: Docker, Docker Compose
- **Security**: Spring Security, JWT (Auth Service)

## 🏗️ Architectural Structure

The project consists of four main modules and infrastructure components:

| Module / Service | Role | Technology | Port |
|-----------------|------|------------|------|
| **auth-service** | Handles registration, login, and JWT issuance | Spring Security, JPA, PostgreSQL | 8080 |
| **ingestion-service** | Receives logs via REST API and pushes them to Kafka (Producer) | Spring Web, Spring Kafka | 8081 |
| **analysis-service** | Consumes logs from Kafka (Consumer), executes anomaly detection logic, and persists alerts | Spring Kafka, JPA, PostgreSQL | 8082 |
| **frontend-ui** | React-based web interface for interacting with the platform | React, TypeScript, Vite | 5173 |
| **common** | Java library containing shared data models (DTOs) | Java | - |

### Infrastructure Services

| Service | Purpose | Port |
|---------|---------|------|
| **postgres-db** | PostgreSQL database for data persistence | 5432 |
| **kafka** | Apache Kafka message broker | 9092 |
| **zookeeper** | ZooKeeper for Kafka coordination | 2181 |

## 🚀 Quick Start

### Prerequisites

- **Java 17 JDK** (for local development)
- **Docker & Docker Compose** (for running the entire stack)
- **Node.js 18+** (for frontend development, if running locally)
- **Gradle** (comes with the wrapper - `gradlew`)

### Option 1: Run Everything with Docker Compose (Recommended)

The easiest way to get started is to run the entire stack using Docker Compose:

```bash
# Build and start all services
docker-compose up --build -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

This will:
- Start PostgreSQL database
- Start ZooKeeper and Kafka
- Build and start all backend services (auth, ingestion, analysis)
- Start the frontend development server

### Option 2: Local Development Setup

#### 1. Start Infrastructure Services

Start only the infrastructure services (database and Kafka):

```bash
docker-compose up -d postgres-db zookeeper kafka
```

#### 2. Build Backend Services

Since the project is a Multi-Module build, build all modules using the Gradle wrapper:

```bash
# Build all modules (skip tests for faster build)
./gradlew clean build -x test

# Or build with tests
./gradlew clean build

# Build specific service
./gradlew :auth-service:build
./gradlew :ingestion-service:build
./gradlew :analysis-service:build
```

#### 3. Run Backend Services Locally

Each service can be run individually using Gradle:

```bash
# Run auth-service
cd auth-service
./gradlew bootRun

# Run ingestion-service (in another terminal)
cd ingestion-service
./gradlew bootRun

# Run analysis-service (in another terminal)
cd analysis-service
./gradlew bootRun
```

#### 4. Run Frontend Locally

```bash
cd frontend-ui
npm install
npm run dev
```

## 🔧 Configuration

### Database Configuration

Both `auth-service` and `analysis-service` are configured to use PostgreSQL:

- **Host**: `postgres-db` (in Docker) or `localhost` (local)
- **Port**: `5432`
- **Database**: `log_db`
- **Username**: `user`
- **Password**: `password`

### Kafka Configuration

- **Bootstrap Servers**: `kafka:29092` (in Docker) or `localhost:9092` (local)
- **Topic**: `log-events`
- **Consumer Group**: `analysis-service-group`

### Service Configuration Files

- `auth-service/src/main/resources/application.properties` - Auth service config with DB settings
- `analysis-service/src/main/resources/application.properties` - Analysis service config with DB and Kafka settings
- `ingestion-service/src/main/resources/application.properties` - Ingestion service config

## 🔑 API Usage

### Step 1: User Registration/Login (Auth Service)

Register a new user or login to receive a JWT token:

```bash
# Register (POST /register)
curl -X POST http://localhost:8080/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user1",
    "password": "password123",
    "email": "user1@example.com"
  }'

# Login (POST /login)
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user1",
    "password": "password123"
  }'
```

### Step 2: Sending Logs (Ingestion Service)

Send logs as JSON to the ingestion service, including the JWT in the Authorization header:

```bash
curl -X POST http://localhost:8081/api/logs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "timestamp": "2025-12-05T10:00:00Z",
    "level": "INFO",
    "message": "User login successful for user_id: 101",
    "service": "auth-service"
  }'
```

**Note**: Anomaly examples might include:
- Logs containing "SECURITY_BREACH"
- High volume of ERROR or CRITICAL level logs
- Unusual patterns detected by the analysis service

### Step 3: Checking Alerts (Analysis Service)

Retrieve the list of detected anomalies:

```bash
# Get all alerts
curl -X GET http://localhost:8082/api/alerts \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 🏗️ Project Structure

```
loganomaly/
├── auth-service/          # Authentication & authorization service
│   ├── src/main/java/     # Java source code
│   ├── src/main/resources/application.properties
│   └── Dockerfile
├── ingestion-service/     # Log ingestion service (Kafka producer)
│   ├── src/main/java/
│   ├── src/main/resources/application.properties
│   └── Dockerfile
├── analysis-service/      # Anomaly detection service (Kafka consumer)
│   ├── src/main/java/
│   ├── src/main/resources/application.properties
│   └── Dockerfile
├── frontend-ui/           # React frontend application
│   ├── src/
│   └── package.json
├── common/                # Shared Java library
│   └── src/main/java/
├── docker-compose.yml     # Docker Compose configuration
└── build.gradle           # Root Gradle build file
```

## 🐳 Docker Services

All services are containerized and can be managed via Docker Compose:

### Backend Services

- **auth-service**: Multi-stage Dockerfile (JDK for build, JRE for runtime)
- **ingestion-service**: Multi-stage Dockerfile
- **analysis-service**: Multi-stage Dockerfile with Kafka listener

### Frontend Service

- **frontend-ui**: Uses Node.js 18 Alpine, runs Vite dev server with hot reload

### Infrastructure

- **postgres-db**: PostgreSQL 14 Alpine with persistent volume
- **kafka**: Confluent Kafka 7.3.0
- **zookeeper**: Confluent ZooKeeper 7.3.0

## 🧪 Testing

Run tests for all services:

```bash
# Run all tests
./gradlew test

# Run tests for specific service
./gradlew :analysis-service:test
./gradlew :auth-service:test
./gradlew :ingestion-service:test
```

**Note**: Tests are configured to exclude Kafka auto-configuration to avoid requiring a running Kafka instance.

## 🛡️ DevSecOps Aspects

The project is designed with security best practices:

- **Separation of Concerns**: Clear separation between authentication, ingestion, and analysis services
- **Secure Communication**: JWT-based authentication for API access
- **Container Security**: Multi-stage Docker builds for minimal attack surface
- **Dependency Management**: Gradle dependency management with version control
- **Least Privilege**: Separate database users per service (in production)

## 📝 Development Notes

- **Kafka Listener**: The `analysis-service` includes a Kafka listener (`LogEventListener`) that automatically consumes messages from the `log-events` topic
- **Database Migrations**: JPA Hibernate auto-update is enabled for development (`spring.jpa.hibernate.ddl-auto=update`)
- **Service Discovery**: Services communicate using Docker service names (e.g., `postgres-db`, `kafka`)

## 🔍 Troubleshooting

### Services won't start
- Check Docker is running: `docker ps`
- Check logs: `docker-compose logs [service-name]`
- Ensure ports aren't in use: `netstat -an | grep 8080`

### Kafka connection issues
- Verify Kafka is running: `docker-compose ps kafka`
- Check Kafka logs: `docker-compose logs kafka`
- Ensure ZooKeeper is running before Kafka

### Database connection issues
- Verify PostgreSQL is running: `docker-compose ps postgres-db`
- Check database logs: `docker-compose logs postgres-db`
- Verify connection string in `application.properties`




