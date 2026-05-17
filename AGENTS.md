# AGENTS.md

> **Learning Reference Repository**
> This repo is a hands-on learning reference for Spring Cloud micro-services concepts.
> Each sub-module demonstrates one Spring Cloud feature in isolation (Config Server,
> Config Client, Load Balancer, etc.).  It is intentionally kept simple so that
> concepts are easy to follow.  Do **not** treat it as a production template.

---

## Scope and Current State
- Runnable modules: `ConfigServer/`, `ConfigClient/`, `LoadBalancer/` (with sub-services `LoadBalancerService1/` and `LoadBalancerService2/`).
- Root `README.md` describes the full intended multi-module Spring Cloud sample.
- Intended flow: Config Clients fetch externalized config from Config Server over HTTP (`localhost:8888`).
- External config backend is expected to be Git (`spring.cloud.config.server.git.uri` in README examples), but that property is not committed in `application.properties` yet.

---

## Big-Picture Architecture
- Runtime entry point is `ConfigServer/src/main/java/com/keroles/configserver/ConfigServerApplication.java`.
- Build/dependency boundary is module-local (each module has its own `build.gradle`), not a root multi-project Gradle build.
- Intended flow (from `README.md`): Config Clients fetch externalized config from Config Server over HTTP (`localhost:8888`).

---

## How to Work in This Repo
- Use Java 17 toolchain (declared in each module's `build.gradle`).
- Start a service from its module directory:
  - `cd ConfigServer && ./gradlew bootRun`
  - `cd ConfigClient && ./gradlew bootRun`
  - `cd LoadBalancer && ./gradlew bootRun`
  - `cd LoadBalancer/LoadBalancerService1 && ./gradlew bootRun`
  - `cd LoadBalancer/LoadBalancerService2 && ./gradlew bootRun`
- Run tests from a module directory:
  - `cd ConfigServer && ./gradlew test`
- The only committed test is a Spring context smoke test in `ConfigServer/src/test/java/com/keroles/configserver/ConfigServerApplicationTests.java`.

---

## Project-Specific Conventions
- Keep Java code under package `com.keroles.<modulename>` (mirroring existing packages).
- Keep runtime config in each module's `src/main/resources/application.properties` (properties format, not YAML).
- Spring Cloud dependency versions are centralized via BOM import:
  - `springCloudVersion` ext property + `dependencyManagement` block in each module's `build.gradle`.
- Preserve wrapper-based workflows (`./gradlew`), since wrapper files are committed per module.

---

## Used Ports

> **⚠️ Port Allocation Rules for Agents**
> - **Never reuse** a port that is already listed in the table below.
> - **Always assign a new, unique port** for every new service/module you create.
> - After assigning a port, add it to the table below and set `server.port=<port>` in the
>   corresponding `application.properties` file before committing.
> - Ports are first-come-first-served; if two PRs race, the one merged last must re-assign.

| Port | Module / Service         | `application.properties` path (from repo root)                                              |
|------|--------------------------|----------------------------------------------------------------------------------------------|
| 8888 | ConfigServer             | [`ConfigServer/src/main/resources/application.properties`](ConfigServer/src/main/resources/application.properties) |
| 8090 | ConfigClient             | [`ConfigClient/src/main/resources/application.properties`](ConfigClient/src/main/resources/application.properties) |
| 8080 | LoadBalancer (gateway)   | [`LoadBalancer/src/main/resources/application.properties`](LoadBalancer/src/main/resources/application.properties) |
| 8001 | LoadBalancerService1     | [`LoadBalancer/LoadBalancerService1/src/main/resources/application.properties`](LoadBalancer/LoadBalancerService1/src/main/resources/application.properties) |
| 8002 | LoadBalancerService2     | [`LoadBalancer/LoadBalancerService2/src/main/resources/application.properties`](LoadBalancer/LoadBalancerService2/src/main/resources/application.properties) |

> **Next available port suggestion:** `8003` (for additional back-end instances) or `8091+` (for additional client-facing services).

---

## Integration Points and Gotchas
- Config Server capability depends on `org.springframework.cloud:spring-cloud-config-server` in `ConfigServer/build.gradle`.
- If you add a Config Client module, follow README patterns (`spring-cloud-starter-config`, `spring.config.import=optional:configserver:http://localhost:8888`) and register a new port in the **Used Ports** table above.
- README mentions `/actuator/health`, but `build.gradle` currently does not include an actuator starter; verify endpoint availability before relying on it.
- Do not hardcode private Git config repo URLs or credentials in committed properties; prefer env/local overrides.

---

## When Editing
- Reference `README.md` as the source of intended cross-module direction.
- Treat unimplemented modules/features in README as planned, not already present behaviour.
- If adding modules, update this file with: new module boundaries, run/test commands, inter-module contracts, **and** a new row in the Used Ports table.
