# LogAnomaly Detection Platform

## 🎯 Overview

This project is a platform for monitoring and detecting Anomalies in logs in real-time. The system is built on an Event-Driven Microservices architecture using Spring Boot and utilizes Apache Kafka as the backbone for data streaming.

## ⚙️ Key Technologies

- **Backend**: Java 17, Spring Boot 3, Gradle Multi-Module
- **Database**: PostgreSQL (for storing users and alerts)
- **Messaging**: Apache Kafka (for log streaming)
- **DevOps**: Docker, Docker Compose, GitHub Actions (CI/CD)
- **Security**: Spring Security, JWT (Auth Service)

## 🏗️ Architectural Structure

The project consists of four main modules and infrastructure components:

| Module / Service | Role | Technology |
|-----------------|------|------------|
| **auth-service** | Handles registration, login, and JWT issuance. | Spring Security, JPA |
| **ingestion-service** | Receives logs via REST API and pushes them to Kafka (Producer). | Spring Web, Spring Kafka |
| **analysis-service** | Consumes logs from Kafka (Consumer), executes anomaly detection logic, and persists alerts. | Spring Kafka, JPA |
| **common** | Java library containing shared data models (DTOs). | Java |

## 🚀 Getting Started

### 1. Prerequisites

- Java 17 JDK
- Docker & Docker Compose
- Gradle (comes with the wrapper - gradlew)

### 2. Infrastructure Setup (Kafka & DB)

To start the PostgreSQL database and Kafka (including ZooKeeper), use the `docker-compose.yml` file located in the root directory:

```bash
docker-compose up -d postgres-db zookeeper kafka
```

### 3. Building the Modules

Since the project is a Multi-Module build, perform a full build of all four modules using the Gradle wrapper:

```bash
./gradlew clean build -x test
```

**Note**: The `-x test` flag skips running unit and integration tests to speed up the initial build.

### 4. Building and Running Services

Once the build is successful, you can build the Docker images for the Java services and run the entire system:

```bash
docker-compose up --build -d
```

| Service | Port | Function |
|---------|------|----------|
| auth-service | 8080 | JWT token issuance. |
| ingestion-service | 8081 | Log reception. |
| analysis-service | 8082 | Endpoint to check alerts. |

## 🔑 API Usage

### Step 1: User Registration (Auth Service)

Send a POST request to port 8080 to register a new user and receive a JWT.

### Step 2: Sending Logs (Ingestion Service)

Send logs as JSON to port 8081, including the received JWT in the Authorization header:

```json
{
  "timestamp": "2025-12-05T10:00:00Z",
  "level": "INFO",
  "message": "User login successful for user_id: 101"
}
```

**Note**: An anomaly log might be one containing the phrase "SECURITY_BREACH" or a high volume of critical errors.

### Step 3: Checking Alerts (Analysis Service)

Send a GET request to port 8082 to view the list of alerts identified by the Analysis Service.

## 🛡️ DevSecOps Aspects

The project is designed with an emphasis on integral security (Secure by Design):

- **Separation of Concerns**: Clear separation between authentication services (auth) and data services (ingestion).
- **Dependency Scanning**: Use of GitHub Actions to perform SCA (Software Composition Analysis) scans on dependencies.
- **Least Privilege**: Defining separate user accounts for each Microservice for DB access (in the production version).

