# AGENTS.md

## Scope and Current State
- This repository currently has one runnable module: `ConfigServer/`.
- Root `README.md` describes a multi-module Spring Cloud sample, but only Config Server code is implemented now.

## Big-Picture Architecture
- Runtime entry point is `ConfigServer/src/main/java/com/keroles/configserver/ConfigServerApplication.java`.
- Build/dependency boundary is module-local (`ConfigServer/build.gradle`), not a root multi-project Gradle build.
- Intended flow (from `README.md`): Config Clients fetch externalized config from Config Server over HTTP (`localhost:8888`).
- External config backend is expected to be Git (`spring.cloud.config.server.git.uri` in README examples), but that property is not committed in `application.properties` yet.

## How to Work in This Repo
- Use Java 17 toolchain (declared in `ConfigServer/build.gradle`).
- Start server from module directory:
  - `cd ConfigServer && ./gradlew bootRun`
- Run tests from module directory:
  - `cd ConfigServer && ./gradlew test`
- The only committed test is a Spring context smoke test in `ConfigServer/src/test/java/com/keroles/configserver/ConfigServerApplicationTests.java`.

## Project-Specific Conventions
- Keep Java code under package `com.keroles.configserver` (see main + test classes).
- Keep runtime config in `ConfigServer/src/main/resources/application.properties` (properties format, not YAML in current code).
- Spring Cloud dependency versions are centralized via BOM import:
  - `springCloudVersion` ext property + `dependencyManagement` block in `ConfigServer/build.gradle`.
- Preserve wrapper-based workflows (`./gradlew`), since wrapper files are committed in `ConfigServer/gradle/wrapper/`.

## Integration Points and Gotchas
- Config Server capability depends on `org.springframework.cloud:spring-cloud-config-server` in `ConfigServer/build.gradle`.
- If you add a Config Client module, follow README patterns (`spring-cloud-starter-config`, `spring.config.import=optional:configserver:http://localhost:8888`).
- README mentions `/actuator/health`, but `build.gradle` currently does not include an actuator starter; verify endpoint availability before relying on it.
- Do not hardcode private Git config repo URLs or credentials in committed properties; prefer env/local overrides.

## When Editing
- Reference `README.md` as the source of intended cross-module direction.
- Treat unimplemented modules/features in README as planned, not already present behavior.
- If adding modules, update this file with new module boundaries, commands, and inter-module contracts.

