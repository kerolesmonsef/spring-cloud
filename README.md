# Spring Cloud Config Server

## What is Config Server?

A centralized service that stores and serves **externalized configuration** (`.properties`/`.yml`) to all microservices in a system. Instead of each service having its own config file, they all fetch config from one place.

```
┌─────────────┐     HTTP     ┌──────────────┐     ┌──────────┐
│  Config     │◄────────────►│  Config      │────►│ Git      │
│  Client A   │              │  Server      │     │ Backend  │
├─────────────┤              │  :8888       │     └──────────┘
│  Config     │              └──────────────┘
│  Client B   │
├─────────────┤
│  Config     │
│  Client C   │
└─────────────┘
```

## Problem It Solves

**Without Config Server** — config is scattered everywhere:

```
┌──────────┐  app.properties     ┌──────────┐  app.properties
│ Service A│  (hardcoded per env)│ Service B│  (hardcoded per env)
├──────────┤                     ├──────────┤
│ Service C│  app.properties     │ Service D│  app.properties
└──────────┘  (hardcoded per env)└──────────┘  (hardcoded per env)
```

**Issues:**
- Restart every service to change a property
- Can't rotate secrets without redeploy
- No audit trail — who changed what, where
- Each service needs its own env-specific build

**With Config Server** — single source of truth:

```
     ┌─────────────────────────────────────┐
     │         Config Server :8888         │
     │  (reads from Git backend)           │
     └──────────┬──────────────────────────┘
                │
    ┌───────────┼───────────┬───────────┐
    ▼           ▼           ▼           ▼
┌───────┐  ┌───────┐  ┌───────┐  ┌───────┐
│  App  │  │  App  │  │  App  │  │  App  │
│   A   │  │   B   │  │   C   │  │   D   │
└───────┘  └───────┘  └───────┘  └───────┘
```

**Fixes:**
- Change config **once** → all services pick it up on restart (or with `/actuator/refresh`)
- Rotate secrets without touching/deploying code
- Config is versioned (Git history = audit trail)
- Same artifact across all environments — just point at different Git branches/profiles

## How to Enable in Spring Boot

### 1. Add dependency (`build.gradle`)
```gradle
implementation 'org.springframework.cloud:spring-cloud-starter-config'
```

### 2. Add bootstrap or application property
```properties
spring.config.import=optional:configserver:http://localhost:8888
spring.application.name=my-service
```


## When to Use

| Scenario | Use Config Server? |
|----------|-------------------|
| 1–2 microservices | Overkill — local files are fine |
| 3+ microservices | ✅ Yes — centralize management |
| Need secret rotation | ✅ Yes — change in Git, redeploy not needed |
| Kubernetes/Cloud | ✅ Yes — pairs with native cloud config |
