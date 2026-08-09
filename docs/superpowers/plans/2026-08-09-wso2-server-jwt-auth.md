# WSO2 Server JWT Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add SQLite-backed login, JWT issuance, and one protected static endpoint to `wso2-server`.

**Architecture:** `AuthController` calls `UserRepository` through `JdbcTemplate`, then `JWTService` signs a token. A stateless Spring Security filter validates Bearer tokens before `DataController` is reached. A startup initializer creates and seeds the SQLite table.

**Tech Stack:** Spring Boot 4.1.0, Spring MVC, Spring Security, Spring JDBC, SQLite JDBC, JJWT 0.12.6, JUnit/MockMvc.

## Global Constraints

- Use Java 17.
- Keep code under `com.keroles.wso2server`.
- Keep runtime configuration in `wso2-server/src/main/resources/application.properties`.
- Use test-only plaintext credentials: `(1, 'mbank', '123456')`.
- Run the service on port `8093`.
- Do not add registration, refresh tokens, roles, password hashing, or production secret management.

---

### Task 1: Add dependencies and SQLite configuration

**Files:**
- Modify: `wso2-server/build.gradle`
- Modify: `wso2-server/src/main/resources/application.properties`

**Interfaces:**
- Produces a `DataSource` for `jdbc:sqlite:src/main/resources/database.db`.
- Produces JWT properties `jwt.secret` and `jwt.expiration-ms`.

- [ ] **Step 1: Add failing dependency/config smoke coverage**

Update the existing context test to assert that the application context starts with the configured SQLite datasource.

- [ ] **Step 2: Run the smoke test and verify it fails**

Run from `wso2-server`:

```bash
./gradlew test --tests com.keroles.wso2server.Wso2ServerApplicationTests
```

Expected: FAIL because the SQLite/JDBC configuration and later beans do not yet exist.

- [ ] **Step 3: Add the minimal dependencies and properties**

Add:

```gradle
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'org.springframework.boot:spring-boot-starter-jdbc'
implementation 'org.xerial:sqlite-jdbc:3.46.1.0'
implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'
```

Add:

```properties
server.port=8093
spring.datasource.url=jdbc:sqlite:src/main/resources/database.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.sql.init.mode=never
jwt.secret=learning-only-secret-key-with-at-least-32-bytes
jwt.expiration-ms=3600000
```

- [ ] **Step 4: Run the smoke test and verify it passes**

Run the same Gradle test command and expect `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add wso2-server/build.gradle wso2-server/src/main/resources/application.properties wso2-server/src/test/java/com/keroles/wso2server/Wso2ServerApplicationTests.java
git commit -m "feat: configure sqlite and jwt dependencies"
```

### Task 2: Add the SQLite user store

**Files:**
- Create: `wso2-server/src/main/java/com/keroles/wso2server/UserRepository.java`
- Create: `wso2-server/src/main/java/com/keroles/wso2server/DatabaseInitializer.java`
- Create: `wso2-server/src/main/java/com/keroles/wso2server/User.java`
- Test: `wso2-server/src/test/java/com/keroles/wso2server/UserRepositoryTest.java`

**Interfaces:**
- `UserRepository.findByUsername(String username): Optional<User>`.
- `User` contains `long id`, `String username`, and `String password`.
- `DatabaseInitializer` creates `users(id INTEGER PRIMARY KEY, username TEXT UNIQUE NOT NULL, pass TEXT NOT NULL)` and inserts the seed user only if absent.

- [ ] **Step 1: Write the failing repository test**

Use `@SpringBootTest` and `JdbcTemplate` to verify `findByUsername("mbank")` returns password `123456`.

- [ ] **Step 2: Run it and verify it fails**

```bash
./gradlew test --tests com.keroles.wso2server.UserRepositoryTest
```

Expected: FAIL because the repository and initializer do not exist.

- [ ] **Step 3: Implement the record, repository, and initializer**

Use one `JdbcTemplate.query(...)` query and `CREATE TABLE IF NOT EXISTS` plus `INSERT OR IGNORE`; do not add JPA.

- [ ] **Step 4: Run the repository test**

Run the command above and expect `PASS`.

- [ ] **Step 5: Commit**

```bash
git add wso2-server/src/main/java/com/keroles/wso2server wso2-server/src/test/java/com/keroles/wso2server/UserRepositoryTest.java
git commit -m "feat: add sqlite user store"
```

### Task 3: Add JWT service and stateless security

**Files:**
- Create: `wso2-server/src/main/java/com/keroles/wso2server/JWTService.java`
- Create: `wso2-server/src/main/java/com/keroles/wso2server/JwtAuthenticationFilter.java`
- Create: `wso2-server/src/main/java/com/keroles/wso2server/SecurityConfig.java`
- Test: `wso2-server/src/test/java/com/keroles/wso2server/JWTServiceTest.java`

**Interfaces:**
- `JWTService.generateToken(String username): String`.
- `JWTService.extractUsername(String token): String`.
- `JWTService.isTokenValid(String token, String username): boolean`.
- The filter sets an authenticated `UsernamePasswordAuthenticationToken` only for a valid Bearer JWT.

- [ ] **Step 1: Write the failing JWT test**

Generate a token for `mbank`, assert the username can be extracted, and assert it validates for `mbank` but not another username.

- [ ] **Step 2: Run it and verify it fails**

```bash
./gradlew test --tests com.keroles.wso2server.JWTServiceTest
```

Expected: FAIL because `JWTService` does not exist.

- [ ] **Step 3: Implement JWTService**

Use JJWT `Keys.hmacShaKeyFor`, `Jwts.builder()`, and the configured expiration. Catch malformed/expired JWTs in the filter and leave the request unauthenticated.

- [ ] **Step 4: Implement SecurityConfig and filter**

Permit `/auth/login` and `/error`, require authentication for every other request, disable CSRF, use `SessionCreationPolicy.STATELESS`, and insert the filter before `UsernamePasswordAuthenticationFilter`.

- [ ] **Step 5: Run JWT and context tests**

```bash
./gradlew test --tests com.keroles.wso2server.JWTServiceTest --tests com.keroles.wso2server.Wso2ServerApplicationTests
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add wso2-server/src/main/java/com/keroles/wso2server wso2-server/src/test/java/com/keroles/wso2server/JWTServiceTest.java
git commit -m "feat: secure wso2 server with jwt"
```

### Task 4: Add login and protected data APIs

**Files:**
- Create: `wso2-server/src/main/java/com/keroles/wso2server/AuthController.java`
- Create: `wso2-server/src/main/java/com/keroles/wso2server/DataController.java`
- Create: `wso2-server/src/main/java/com/keroles/wso2server/LoginRequest.java`
- Create: `wso2-server/src/main/java/com/keroles/wso2server/LoginResponse.java`
- Test: `wso2-server/src/test/java/com/keroles/wso2server/AuthApiTest.java`

**Interfaces:**
- `POST /auth/login` consumes `LoginRequest(username, password)` and returns `LoginResponse(token)`.
- `GET /data` returns `{"message":"hello you are logged in and authorized"}`.

- [ ] **Step 1: Write failing MockMvc tests**

Cover successful login, invalid login returning 401, `/data` without a token returning 401, and `/data` with the login token returning 200 and the exact message.

- [ ] **Step 2: Run the API tests and verify they fail**

```bash
./gradlew test --tests com.keroles.wso2server.AuthApiTest
```

Expected: FAIL because the controllers do not exist.

- [ ] **Step 3: Implement the DTOs and controllers**

Compare the submitted password directly with the SQLite `pass` column. Return `ResponseEntity.status(HttpStatus.UNAUTHORIZED)` for unknown users or mismatched passwords. Return the JWT in a `token` JSON property.

- [ ] **Step 4: Run the API tests**

Run the command above and expect all API tests to pass.

- [ ] **Step 5: Run the complete module test suite**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add wso2-server/src/main/java/com/keroles/wso2server wso2-server/src/test/java/com/keroles/wso2server/AuthApiTest.java
git commit -m "feat: add jwt login and protected data api"
```

### Task 5: Verify manually with curl

**Files:** None.

- [ ] **Step 1: Start the service**

```bash
./gradlew bootRun
```

- [ ] **Step 2: Login**

```bash
curl -s -X POST http://localhost:8093/auth/login -H 'Content-Type: application/json' -d '{"username":"mbank","password":"123456"}'
```

Expected: JSON containing a non-empty `token`.

- [ ] **Step 3: Call the protected endpoint**

```bash
curl -i http://localhost:8093/data -H 'Authorization: Bearer <token-from-login>'
```

Expected: HTTP 200 and `hello you are logged in and authorized`.

- [ ] **Step 4: Verify rejection without a token**

```bash
curl -i http://localhost:8093/data
```

Expected: HTTP 401.
