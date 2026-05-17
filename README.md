# Spring Cloud Samples

This repository contains Spring Cloud pattern samples.

## Patterns

### 1. Config Server and Config Client

Config Server is a central configuration management solution that provides externalized configuration for distributed systems.

**Architecture:**
```
┌─────────────┐         ┌──────────────┐
│ Config      │ ──────▶ │ Config       │
│ Server      │         │ Client       │
│ (8888)      │         │ (8080)       │
└─────────────┘         └──────────────┘
```

#### Config Server

**Features:**
- Centralized configuration management
- Supports multiple backends (git, svn, vault, etc.)
- Provides RESTful API for configuration
- Supports configuration encryption

**Build:** `ConfigServer/build.gradle`
- Spring Boot 4.0.6
- Spring Cloud 2025.1.1

**Run:**
```bash
cd ConfigServer
./gradlew bootRun
```

**Default Port:** 8888

**Configuration:**
```properties
spring.application.name=ConfigServer
server.port=8888
spring.cloud.config.server.git.uri=https://github.com/your-repo/config-repo
```

#### Config Client

To be added - will demonstrate how to connect to Config Server and consume externalized configuration.

**Dependencies:**
```gradle
implementation 'org.springframework.cloud:spring-cloud-starter-config'
```

**Configuration:**
```properties
spring.config.import=optional:configserver:http://localhost:8888
spring.application.name=your-app-name
```

## Project Structure

```
springCload/
├── ConfigServer/          # Config Server module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/     # Java source
│   │   │   └── resources/
│   │   └── test/
│   ├── build.gradle
│   └── gradle/
└── (more modules to be added)
```

## Requirements

- Java 17+
- Gradle (wrapper included)

## Quick Start

1. Clone the repository
2. Start Config Server: `cd ConfigServer && ./gradlew bootRun`
3. Config Server runs on http://localhost:8888
4. Access health endpoint: http://localhost:8888/actuator/health

## Documentation

- [Spring Cloud Config Docs](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/)
- [Spring Boot Docs](https://docs.spring.io/spring-boot/docs/current/reference/)