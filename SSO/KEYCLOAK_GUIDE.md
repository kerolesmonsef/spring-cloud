# Learning SSO with Keycloak and Spring Boot

This guide will walk you through setting up Single Sign-On (SSO) using Keycloak as the Identity Provider and a Spring Boot application as the Resource Server.

## 1. Prerequisites
- Docker installed on your machine.
- Java 17+ and Gradle.
- Postman or `curl` for testing.

---

## 2. Start Keycloak using Docker

The easiest way to run Keycloak locally is via Docker. Run the following command:

```bash
docker run -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:24.0.1 \
  start-dev
```

- **Admin Console**: [http://localhost:8080](http://localhost:8080)
- **Username**: `admin`
- **Password**: `admin`

---

## 3. Configure Keycloak

Once Keycloak is running:

1.  **Login** to the Admin Console.
2.  **Create a Realm**:
    - Click on the realm dropdown (top left, usually says "Master").
    - Click **Create Realm**.
    - Name it `my-sso-realm`.
3.  **Create a Client**:
    - Go to **Clients** > **Create client**.
    - **Client ID**: `spring-boot-app`
    - **Client Protocol**: `openid-connect`
    - Click **Next**.
    - Set **Client authentication** to `Off` (for this simple Resource Server demo).
    - Set **Authentication flow** to include `Standard flow` and `Direct access grants` (optional, for testing).
    - Click **Save**.
4.  **Create a User**:
    - Go to **Users** > **Add user**.
    - **Username**: `testuser`
    - Click **Create**.
    - Go to the **Credentials** tab and click **Set password**.
    - Enter a password (e.g., `password`) and turn **Temporary** `Off`.
5.  **Create a Role (Optional for Admin test)**:
    - Go to **Realm Roles** > **Create role**.
    - **Role Name**: `admin`.
    - Go to **Users** > `testuser` > **Role mapping** > **Assign role** and select `admin`.

---

## 4. Spring Boot Configuration

Your `application.properties` in the `SSO` module is already configured to point to this realm:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/my-sso-realm
```

Spring Boot will automatically fetch the public keys from Keycloak to verify the JWT tokens sent in the `Authorization` header.

---

## 5. Running the Application

Navigate to the `SSO` directory and run:

```bash
./gradlew bootRun
```

The application will start on port `8003`.

---

## 6. Testing the SSO Flow

### A. Get an Access Token
You can use `curl` to exchange your username/password for a JWT token:

```bash
curl -X POST http://localhost:8080/realms/my-sso-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=spring-boot-app" \
  -d "username=testuser" \
  -d "password=password"
```

Save the `access_token` from the response.

### B. Access Public Endpoint (No Token Needed)
```bash
curl http://localhost:8003/api/public
```

### C. Access Private Endpoint (With Token)
```bash
curl http://localhost:8003/api/private \
  -H "Authorization: Bearer <YOUR_ACCESS_TOKEN>"
```

### D. Access Admin Endpoint
If you mapped the `admin` role/scope correctly:
```bash
curl http://localhost:8003/api/admin \
  -H "Authorization: Bearer <YOUR_ACCESS_TOKEN>"
```

---

## Key Concepts Learned
1.  **Identity Provider (IdP)**: Keycloak manages users and issues tokens.
2.  **Resource Server**: Your Spring Boot app protects resources and validates tokens.
3.  **JWT (JSON Web Token)**: The "ticket" that carries user identity and permissions.
4.  **Issuer URI**: The endpoint where Spring Boot finds the IdP's metadata and public keys.
