# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Read AGENTS.md first

`AGENTS.md` is the primary contract for this repo and is kept current: it covers the per-module run/test commands, the Java 17 toolchain, the properties-not-YAML convention, the BOM-based Spring Cloud versioning, and — importantly — the **Used Ports table you must update before adding any module/service**. Don't duplicate that here; follow it.

This file records only what AGENTS.md omits or gets wrong.

## What this repo is

A learning reference for Spring Cloud, **not** a production template. Each top-level directory is an independent Spring Boot project demonstrating one feature in isolation. There is **no root Gradle build** — each module has its own `build.gradle`, `settings.gradle`, and committed `./gradlew` wrapper. Build/run/test always happens from inside a module directory.

## Corrections to AGENTS.md

- The Eureka module directory is `ServiceDiscovery/` (AGENTS.md and run commands spell it `ServiceDescovery` — wrong). Use `cd ServiceDiscovery && ./gradlew bootRun`.

## Module states (some are scaffolds, not implemented)

- **ConfigServer** (:8888), **ConfigClient** (:8090), **LoadBalancer** (:8080 gateway + **LoadBalancerService1** :8001 / **LoadBalancerService2** :8002), **OpenFeign** (:8091) — implemented per README.
- **SSO** (:8003) — OAuth2 resource server validating JWTs against a Keycloak realm at `http://localhost:8080/realms/my-sso-realm` (`SecurityConfig.java`, `DemoController.java`). Needs Keycloak running on :8080.
- **ServiceDiscovery** (:8761) — intended Eureka server, but `build.gradle` has **no eureka dependency yet**; it's a bare Spring Boot scaffold. Add `spring-cloud-starter-netflix-eureka-server` before relying on it.
- **ddd** — JPA + webmvc scaffold (`DddApplication` only), no port assigned, not in README.

When a module is a scaffold, treat README/AGENTS descriptions of it as the *intended* design, not current behavior — verify dependencies in `build.gradle` before assuming a feature exists.
