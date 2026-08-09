# WSO2 Server JWT Authentication Design

## Goal

Add a small testing-only authentication example to `wso2-server`: users are
stored in SQLite, a valid login returns a JWT, and a protected endpoint returns
static data.

## API

- `POST /auth/login`
  - Request: `{"username":"mbank","password":"123456"}`
  - Success: `{"token":"<jwt>"}`
  - Invalid credentials: HTTP 401
- `GET /data`
  - Requires `Authorization: Bearer <jwt>`
  - Success: `{"message":"hello you are logged in and authorized"}`
  - Missing or invalid token: HTTP 401

## Components

- `AuthController` accepts login requests and looks up the user in SQLite.
- `JWTService` creates and validates signed tokens.
- `SecurityConfig` permits `/auth/login`, protects `/data`, disables CSRF and
  server-side sessions, and installs the JWT filter.
- `DataController` exposes the protected static response.
- SQLite stores `users(id, username, pass)` and initializes the seed user
  `(1, 'mbank', '123456')` on startup.

## Dependencies and Configuration

Use Spring Security, JDBC, the SQLite JDBC driver, and a small JWT library.
Keep the database file under `src/main/resources/database.db` as requested for
this learning example. Run the service on port `8093`.

## Testing

Add tests for successful login, invalid login, protected access without a token,
and protected access with the returned token. The existing context smoke test
must continue to pass.

## Explicit Scope

This example does not add registration, refresh tokens, roles, password hashing,
or production-grade secret management. The plaintext password and test JWT
secret are intentional limitations of this demonstration.
