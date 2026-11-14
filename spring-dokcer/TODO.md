# Docker Learning TODO — `spring-dokcer`

Goal: go from "it runs in Docker" to **small, fast, secure, reproducible** images and
containers, using this Spring Boot 4.1 / Java 17 module as the guinea pig.

Work top to bottom. Each phase ends with a **✅ Checkpoint** — don't move on until it passes.
Record numbers (image size, build time, startup time, RAM) in the [Results log](#results-log) at the bottom so you can *see* each optimization pay off.

---

## Phase 0 — Prep the app (it's a bare scaffold right now)

- [ ] Add `spring-boot-starter-web` (an HTTP endpoint to hit) and `spring-boot-starter-actuator` (health checks) to `build.gradle`
- [ ] Add a `HelloController` → `GET /hello` returns hostname + timestamp (hostname proves *which* container answered)
- [ ] Pick a port: **8095** (next free). Set `server.port=8095` in `application.properties` **and add the row to the Used Ports table in `../AGENTS.md`**
- [ ] Expose health: `management.endpoints.web.exposure.include=health,info`
- [ ] Note: `spring-boot-docker-compose` is already a `developmentOnly` dep — it auto-starts `compose.yaml` on `bootRun`. Keep in mind for Phase 11
- [ ] Run locally: `./gradlew bootRun` → `curl localhost:8095/hello` and `curl localhost:8095/actuator/health`
- [ ] Build the jar: `./gradlew bootJar` → `build/libs/spring-dokcer-0.0.1-SNAPSHOT.jar`

✅ **Checkpoint:** `java -jar build/libs/*.jar` works without Gradle.

---

## Phase 1 — Docker fundamentals (no Dockerfile yet)

- [ ] `docker version`, `docker info` — understand client vs daemon
- [ ] Image vs container vs layer vs registry — write a one-line definition of each here
- [ ] `docker pull eclipse-temurin:17-jre` → `docker image ls` → `docker image history eclipse-temurin:17-jre`
- [ ] `docker run -it --rm eclipse-temurin:17-jre java -version`
- [ ] Container lifecycle: `run`, `ps`, `ps -a`, `stop`, `start`, `restart`, `rm`, `logs -f`, `exec -it <c> sh`
- [ ] `docker inspect <container>` — find IP, mounts, env, restart policy
- [ ] `docker stats` — live CPU/RAM per container
- [ ] Cleanup habits: `docker system df`, `docker image prune`, `docker system prune` (know what each deletes before running!)

✅ **Checkpoint:** you can explain what happens on `docker run` (pull → create → start).

---

## Phase 2 — First Dockerfile (naive, on purpose)

- [ ] `Dockerfile.v1`: `FROM eclipse-temurin:17-jdk`, `COPY build/libs/*.jar app.jar`, `ENTRYPOINT ["java","-jar","/app.jar"]`
- [ ] `docker build -f Dockerfile.v1 -t spring-dokcer:v1 .`
- [ ] `docker run --rm -p 8095:8095 spring-dokcer:v1` → curl `/hello`
- [ ] Record image size in the Results log (expect big — full JDK)
- [ ] Learn: `ENTRYPOINT` vs `CMD`, exec form `["..."]` vs shell form (signal handling / PID 1!)
- [ ] Learn: `EXPOSE` is documentation only; `-p` actually publishes

✅ **Checkpoint:** container answers on 8095; `docker stop` shuts it down in < 10s (graceful, not killed).

---

## Phase 3 — `.dockerignore` & build context

- [ ] Run `docker build` and watch "transferring context" size
- [ ] Add `.dockerignore`: `.gradle/`, `build/` (except jar if needed), `.git/`, `.idea/`, `*.iml`, `out/`, `HELP.md`
- [ ] Rebuild and compare context size
- [ ] Learn: why context size matters (speed + accidentally leaking secrets like `.env`)

✅ **Checkpoint:** context transfer is a few KB/MB, not hundreds.

---

## Phase 4 — Multi-stage build (build inside Docker)

- [ ] `Dockerfile.v2`: stage `build` (`FROM eclipse-temurin:17-jdk AS build`) runs `./gradlew bootJar`; stage `runtime` (`FROM eclipse-temurin:17-jre`) copies only the jar
- [ ] Order layers for cache: copy `gradlew`, `gradle/`, `build.gradle`, `settings.gradle` **first** → download deps → **then** copy `src/`
- [ ] Use BuildKit cache mount: `RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar --no-daemon`
- [ ] Change one line in a controller → rebuild → confirm deps layer is `CACHED`
- [ ] Record: size (JRE vs JDK) and rebuild time after a code-only change

✅ **Checkpoint:** code-only rebuild is seconds, not minutes; no JDK/Gradle in the final image.

---

## Phase 5 — Spring Boot layered jar (better caching of the runtime image)

- [ ] Learn why: a fat jar = 1 layer; every code change re-ships ~20 MB of unchanged deps
- [ ] Extract layers: `java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted`
      (Boot 3.3+/4.x uses `jarmode=tools`; the old `layertools` is gone)
- [ ] Inspect layers: `dependencies`, `spring-boot-loader`, `snapshot-dependencies`, `application`
- [ ] `Dockerfile.v3`: `COPY` each layer folder separately, least-changing first; `ENTRYPOINT ["java","org.springframework.boot.loader.launch.JarLauncher"]`
- [ ] `docker image history spring-dokcer:v3` — confirm `application` layer is tiny
- [ ] Optional: try `./gradlew bootBuildImage` (Cloud Native Buildpacks, no Dockerfile) and compare size/startup

✅ **Checkpoint:** after a code change, only the small `application` layer changes.

---

## Phase 6 — Smallest / safest base image

Try each, record size + startup time:

- [ ] `eclipse-temurin:17-jre` (Ubuntu) — baseline
- [ ] `eclipse-temurin:17-jre-alpine` — smaller (musl libc; know the trade-offs)
- [ ] `gcr.io/distroless/java17-debian12` — no shell, no package manager (debug with `:debug` tag)
- [ ] Custom JRE with `jdeps` + `jlink` (`--strip-debug --no-man-pages --no-header-files --compress`) on top of `debian:bookworm-slim` / `alpine`
- [ ] **Pin versions**: never `latest`; use specific tags, bonus: pin by digest `@sha256:...`
- [ ] Scan each: `docker scout cves spring-dokcer:vX` (or `trivy image spring-dokcer:vX`) — compare CVE counts

✅ **Checkpoint:** pick a winner and write *why* (size vs debuggability vs CVEs).

---

## Phase 7 — Security hardening

- [ ] Run as non-root: create user (`useradd`/`adduser`) + `USER app` (distroless: `USER nonroot`)
- [ ] Verify: `docker exec <c> id` → not `uid=0`
- [ ] `--read-only` root filesystem + `--tmpfs /tmp`
- [ ] `--cap-drop=ALL`, `--security-opt no-new-privileges`
- [ ] No secrets in image: no `ENV PASSWORD=...`, no `COPY .env`; check with `docker image history --no-trunc`
- [ ] Use BuildKit secrets for build-time secrets: `RUN --mount=type=secret,id=...`
- [ ] Add `LABEL` metadata (OCI: `org.opencontainers.image.source`, `version`, `revision`)
- [ ] Lint Dockerfile: `hadolint Dockerfile` (via `docker run --rm -i hadolint/hadolint < Dockerfile`)

✅ **Checkpoint:** hadolint clean, non-root, 0 critical CVEs.

---

## Phase 8 — JVM-in-container performance

- [ ] Confirm container awareness: `java -XX:+PrintFlagsFinal -version | grep -E "MaxHeapSize|UseContainerSupport"` inside container with `-m 512m`
- [ ] Set heap by percentage, not fixed: `-XX:MaxRAMPercentage=75.0` (via `JAVA_TOOL_OPTIONS` env, not hardcoded)
- [ ] Limit resources: `docker run -m 512m --cpus=1.0` — watch `docker stats`
- [ ] Trigger OOM on purpose with a tiny `-m 128m` → see `OOMKilled: true` in `docker inspect`
- [ ] Graceful shutdown: `server.shutdown=graceful` + `spring.lifecycle.timeout-per-shutdown-phase=20s`; test with `docker stop -t 30`
- [ ] Faster startup: Class Data Sharing (CDS) — Boot supports `-Dspring.context.exit=onRefresh` training run + `-XX:SharedArchiveFile=app.jsa`; measure before/after
- [ ] Optional: try `spring.main.lazy-initialization=true` and compare startup
- [ ] Optional advanced: GraalVM native image (`org.graalvm.buildtools.native` plugin) — compare size, startup, RAM

✅ **Checkpoint:** startup time & RSS recorded for at least 2 configurations.

---

## Phase 9 — Healthchecks, restart policies, logging

- [ ] `HEALTHCHECK` in Dockerfile hitting `/actuator/health` (needs `wget`/`curl` in image — not available in distroless → do it in compose or use a tiny Java check)
- [ ] `docker ps` shows `(healthy)`
- [ ] Restart policies: `--restart no | on-failure:3 | unless-stopped | always` — kill the process and observe
- [ ] Logs to stdout/stderr only (no log files inside container); `docker logs --since 5m -f`
- [ ] Log driver & rotation: `--log-opt max-size=10m --log-opt max-file=3`

✅ **Checkpoint:** container self-reports health and restarts on crash.

---

## Phase 10 — Volumes & networking

- [ ] Bind mount vs named volume vs tmpfs — when to use each
- [ ] `docker volume create`, `ls`, `inspect`, `rm`
- [ ] Persist data from a DB container across `rm` / re-`run`
- [ ] User-defined bridge network: `docker network create app-net`; run 2 containers and reach each other **by name** (DNS)
- [ ] Why the default `bridge` network has no DNS by name
- [ ] `-p 127.0.0.1:8095:8095` vs `-p 8095:8095` (bind only to localhost)
- [ ] Inside container, `localhost` = the container itself → use service name (or `host.docker.internal` for the host)

✅ **Checkpoint:** app container talks to a DB container by hostname.

---

## Phase 11 — Docker Compose

- [ ] Fill `compose.yaml`: `app` (build from Dockerfile) + `postgres` (pinned tag) + named volume
- [ ] Add `spring-boot-starter-data-jpa` + postgres driver; add a tiny entity/endpoint that writes & reads
- [ ] Config via env: `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/app` (relaxed binding)
- [ ] `.env` file for values (git-ignore it; commit `.env.example`)
- [ ] `depends_on` with `condition: service_healthy` + postgres `healthcheck` (`pg_isready`)
- [ ] App `healthcheck`, `restart: unless-stopped`, `deploy.resources.limits` (memory/cpus)
- [ ] Commands: `docker compose up -d --build`, `ps`, `logs -f app`, `exec app sh`, `down`, `down -v` (deletes volumes!)
- [ ] Scale: `docker compose up -d --scale app=3` (remove fixed host port first) + see different hostnames from `/hello`
- [ ] Profiles: put a tool like `adminer`/`pgadmin` under `profiles: [tools]` → `docker compose --profile tools up`
- [ ] Override files: `compose.override.yaml` for dev (debug port 5005, bind mounts) vs `compose.prod.yaml`
- [ ] `docker compose config` — see the merged final config
- [ ] Revisit `spring-boot-docker-compose`: `./gradlew bootRun` auto-starts postgres from compose (service connection, no URL needed)
- [ ] Optional: `docker compose watch` (`develop.watch`) for auto-rebuild on change

✅ **Checkpoint:** `docker compose up -d` from clean state → app healthy → data survives `down`/`up` (but not `down -v`).

---

## Phase 12 — Registry, tagging, multi-arch

- [ ] Tagging strategy: `spring-dokcer:0.0.1`, `:git-<sha>`, avoid relying on `:latest`
- [ ] Push to Docker Hub or GHCR: `docker login`, `docker tag`, `docker push`
- [ ] Run a local registry: `docker run -d -p 5000:5000 registry:2` → push/pull to `localhost:5000`
- [ ] Multi-arch with buildx: `docker buildx build --platform linux/amd64,linux/arm64 -t ... --push .`
- [ ] Learn `docker save` / `docker load` (offline transfer)

✅ **Checkpoint:** image pulled and run on a clean machine/context.

---

## Phase 13 — Debugging & introspection

- [ ] Remote debug: `JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005` + `-p 5005:5005`, attach from IntelliJ
- [ ] `dive spring-dokcer:vX` — browse layers, find wasted space
- [ ] `docker cp`, `docker diff <c>` (what changed in container FS)
- [ ] `docker events` while starting/stopping containers
- [ ] Debug distroless: `docker run --rm -it --pid=container:<c> --network=container:<c> busybox sh` (sidecar debugging)

---

## Phase 14 — Final "perfect" Dockerfile & wrap-up

- [ ] One `Dockerfile` combining: multi-stage + cache mounts + layered jar + chosen slim base + non-root + pinned tags + OCI labels + JVM flags via env + healthcheck strategy
- [ ] Final `compose.yaml` with app + db + healthchecks + limits + profiles
- [ ] `README.md` in this module: how to build, run, compose, and what each optimization bought (link the Results log)
- [ ] Optional: CI pipeline (GitHub Actions) building & scanning the image on push

---

## Results log

| Version | Base image | Image size | Rebuild (code-only) | Startup time | RSS @ idle | CVEs (crit/high) | Notes |
|---------|------------|-----------:|--------------------:|-------------:|-----------:|------------------|-------|
| v1 | temurin:17-jdk | | | | | | naive |
| v2 | temurin:17-jre | | | | | | multi-stage |
| v3 | temurin:17-jre | | | | | | layered jar |
| v4 | alpine / distroless / jlink | | | | | | |
| v5 | + CDS | | | | | | |

Measure with: `docker image ls`, `time docker build ...`, the `Started ... in X seconds` log line, `docker stats --no-stream`, `docker scout cves`.

---

## Command cheat sheet

```bash
# build / run
docker build -t spring-dokcer:dev .
docker run -d --name app -p 8095:8095 -m 512m --cpus=1 spring-dokcer:dev
docker logs -f app
docker exec -it app sh
docker stop app && docker rm app

# inspect
docker image history spring-dokcer:dev
docker inspect app
docker stats

# compose
docker compose up -d --build
docker compose logs -f app
docker compose down        # keep volumes
docker compose down -v     # DELETE volumes

# cleanup (read what it removes first!)
docker system df
docker image prune
docker builder prune
```
