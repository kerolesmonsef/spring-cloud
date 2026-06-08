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

---

## 7. Who Manages the Keycloak Dashboard?

**Both sys-admins and backend developers interact with Keycloak, but they own different parts:**

| Responsibility | Role | Why |
|---|---|---|
| Installing, upgrading, backups, infrastructure (Docker, DB, TLS certs) | **Sys-admin / DevOps** | This is pure infrastructure work. Keeping Keycloak alive, patched, and available. |
| Creating Realms, Clients, Client Scopes | **Backend Developer** | These are application-level concerns. The developer knows which apps need which protocols and claims. |
| Managing Users, Groups, and Roles | **Mixed** — small teams: backend developer. Large orgs: a dedicated IdP/identity team or sys-admin. | Day-to-day user management is often delegated to support or HR staff via Keycloak's "Realm Admin" role, but the initial setup and role model design is done by the backend developer. |
| Monitoring Sessions & Events | **Backend Developer / Security team** | Developers debug auth issues; security teams audit suspicious logins. |
| Writing the application code that validates tokens | **Backend Developer** | Only the developer knows what endpoints to protect and which roles/scopes to check. |

**In short:** You (the backend developer) will spend most of your time in the Keycloak dashboard during development. In production, the sys-admin keeps the server running, and you or a dedicated identity team manage the realm configuration.

---

## 8. Keycloak Core Concepts — Explained with Examples

### 8.1 Realm

A **Realm** is an isolated workspace inside Keycloak. Think of it as a "tenant" — everything (users, roles, clients, sessions) lives inside a realm and is invisible to other realms.

- Keycloak always starts with a `master` realm (for super-admin management).
- You should **create a separate realm** for each project or environment.

**Example:**  
You work at a company called `Acme`. You create a realm called `acme-prod` for production and `acme-dev` for development. Users in `acme-dev` cannot log in to apps in `acme-prod` unless you explicitly configure it.

**In the dashboard:** Top-left dropdown → "Create Realm" → enter name → Create.

---

### 8.2 Clients

A **Client** in Keycloak represents an application or service that wants to use Keycloak for authentication. It can be:

- A **web app** (frontend that redirects users to Keycloak to log in)
- A **REST API / backend service** (validates tokens)
- A **mobile app** or **SPA** (requests tokens directly)

Each client has a unique `client_id` and defines how that app is allowed to interact with Keycloak.

**Key settings per client:**

| Setting | What it means |
|---|---|
| Client ID | Unique identifier, e.g., `spring-boot-app` |
| Client Authentication | `On` = confidential client (has a secret). `Off` = public client (no secret, used by SPAs/mobile). |
| Valid Redirect URIs | Where Keycloak is allowed to send the user after login (prevents open redirect attacks). |
| Valid Post Logout Redirect URIs | Where to send the user after logout. |
| Web Origins | Allowed CORS origins for token requests. |

**Example:**  
You have two apps:

1. `acme-web` — a React SPA (Client Authentication: Off, because the browser can't keep a secret)
2. `spring-boot-app` — your Spring Boot API (Client Authentication: On, server-to-server with a client secret)

Each is registered as a separate Client. They both live in the same realm `acme-prod`, so a user who logs in once (SSO) is recognized by both apps.

**In the dashboard:** Clients → Create client → fill in Client ID, protocol → Next → configure flows → Save.

---

### 8.3 Client Scopes

A **Client Scope** is a reusable bundle of **claims** (pieces of information) that get included in tokens. Instead of hardcoding "give this token the email and roles", you define a client scope once and assign it to multiple clients.

**Types of client scopes:**

- **Default** — automatically added to every token for that client.
- **Optional** — only included if the client explicitly requests it (via `scope` parameter in the token request).

**Built-in scopes Keycloak provides out of the box:**

| Scope name | What it adds to the token |
|---|---|
| `openid` | Standard OpenID Connect scope — adds `sub` (subject) claim. |
| `profile` | Adds `name`, `family_name`, `given_name`, etc. |
| `email` | Adds `email` and `email_verified`. |
| `roles` | Adds `realm_access.roles` and `resource_access.*.roles` — the user's roles. |
| `address` | Adds address-related claims. |
| `phone` | Adds `phone_number`, `phone_number_verified`. |

**Example:**  
Your `spring-boot-app` client needs the user's email and roles in the JWT. You assign:

- `openid` (default) — always included
- `email` (default) — always included
- `roles` (default) — always included
- `phone` (optional) — only included if the client requests `scope=openid phone`

Now the JWT token will contain:

```json
{
  "sub": "a3f2b...",
  "email": "testuser@example.com",
  "realm_access": {
    "roles": ["admin"]
  }
}
```

But it will NOT contain `phone_number` unless explicitly requested.

**In the dashboard:** Client Scopes → Create client scope → define name and type → then go to your Client → Client scopes tab → Add scope.

---

### 8.4 Realm Roles

A **Realm Role** is a global role that exists at the realm level (not tied to any specific client). It represents a permission or identity that applies across all clients in that realm.

**Example:**  
In realm `acme-prod`, you create these realm roles:

| Role | Purpose |
|---|---|
| `admin` | Full access to everything |
| `editor` | Can create and edit content |
| `viewer` | Read-only access |

You assign roles to users:

- `testuser` → `admin`
- `alice` → `editor`
- `bob` → `viewer`

In your Spring Boot app, you check:

```java
@GetMapping("/admin")
@PreAuthorize("hasRole('admin')")
public String admin() { return "Admin area"; }
```

**Composite roles:** A realm role can contain other roles. For example, create a role `super-admin` that includes `admin` + `editor` + `viewer`. Assigning `super-admin` to a user gives them all three.

**Important distinction:**

- **Realm Roles** — global across the realm (used in `realm_access.roles` in the JWT).
- **Client Roles** — specific to one client (used in `resource_access.<client-id>.roles`). Use client roles when different apps need different role names (e.g., `admin` means something different in `app-a` vs `app-b`).

**In the dashboard:** Realm roles → Create role → enter name. Then Users → select user → Role mapping → Assign role.

---

### 8.5 Users

A **User** is exactly what it sounds like — an identity for a real person (or service account). Each user has:

| Property | Example |
|---|---|
| Username | `testuser` |
| Email | `testuser@example.com` |
| First / Last name | `Test` / `User` |
| Enabled | `true` (disabled users cannot log in) |
| Email Verified | `true` / `false` |
| Required Actions | e.g., "Change password on next login", "Verify email" |

**User lifecycle example:**

1. Admin creates user `alice` with a temporary password.
2. Keycloak sets required action: `UPDATE_PASSWORD`.
3. Alice logs in → Keycloak forces her to change her password.
4. Required action is cleared. Alice can now log in normally.

**Service accounts:** If you create a **confidential client** with "Service accounts enabled", Keycloak can issue tokens for the client itself (no human user). This is used for server-to-server communication.

```bash
# Server-to-server token request (no user, just client credentials)
curl -X POST http://localhost:8080/realms/my-sso-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=spring-boot-app" \
  -d "client_secret=<YOUR_CLIENT_SECRET>"
```

**In the dashboard:** Users → Add user → fill details → Credentials tab → Set password.

---

### 8.6 Groups

**Groups** are collections of users. They are primarily used to **organize users hierarchically** and **assign roles in bulk**.

**Why use groups instead of just roles?**

- Groups = *who the user is* (e.g., "Engineering", "HR", "Management")
- Roles = *what the user can do* (e.g., "admin", "editor", "viewer")

You can assign roles to a group, and every user in that group inherits those roles. This saves you from assigning roles one-by-one to every user.

**Example — Company structure:**

```
Acme (top-level group)
├── Engineering
│   ├── Backend
│   └── Frontend
├── HR
└── Management
```

- Assign `editor` role to the `Engineering` group → all users in `Engineering`, `Backend`, and `Frontend` get the `editor` role.
- Assign `admin` role to the `Management` group → all managers become admins.
- Assign `viewer` role to the `HR` group → HR staff can only view.

**In the dashboard:** Groups → Create group → add sub-groups → add members → assign roles to the group.

**How it appears in the JWT:**

```json
{
  "realm_access": {
    "roles": ["editor"]
  },
  "groups": ["/Engineering/Backend"]
}
```

---

### 8.7 Sessions

A **Session** represents an active login. When a user authenticates, Keycloak creates a session. The session tracks:

- Which user is logged in
- Which client(s) they logged in to
- When the session expires
- The IP address and user agent of the login

**Why sessions matter:**

- **SSO (Single Sign-On):** When a user logs in through Client A, they get a session. When they visit Client B, Keycloak sees the existing session and logs them in automatically — no password prompt again.
- **Single Logout (SLO):** When you revoke a session in Keycloak, the user is logged out from **all** clients at once.
- **Security:** If you see a suspicious login, you can kill that specific session without affecting the user's other devices.

**Example — Checking sessions:**

1. User `testuser` logs in from their laptop → Session S1 created.
2. Same user logs in from their phone → Session S2 created.
3. Admin goes to Keycloak → Users → `testuser` → Sessions → sees both S1 and S2.
4. Admin clicks "Logout" on S1 → the laptop session is destroyed, but the phone (S2) stays active.

**In the dashboard:** Users → select user → Sessions tab. Or: Realm → Sessions (see all active sessions across all users).

**Session configuration (per realm):**

| Setting | Default | What it controls |
|---|---|---|
| SSO Session Idle | 30 min | How long before an idle session expires |
| SSO Session Max | 10 hours | Maximum lifetime of a session |
| Access Token Lifespan | 5 min | How long an individual access token is valid |

**In the dashboard:** Realm settings → Tokens tab → adjust lifespans.

---

### 8.8 Events

**Events** are Keycloak's audit log. They record every authentication and administrative action.

**Two types of events:**

| Type | Examples | Purpose |
|---|---|---|
| **Login Events** | `LOGIN`, `LOGIN_ERROR`, `LOGOUT`, `CODE_TO_TOKEN`, `REFRESH_TOKEN` | Track who logged in, from where, and whether it succeeded. |
| **Admin Events** | `CREATE_USER`, `UPDATE_ROLE`, `DELETE_CLIENT`, `ASSIGN_ROLE` | Track what admins changed in the Keycloak configuration. |

**Login event example (what you see in the Events tab):**

```json
{
  "type": "LOGIN",
  "userId": "a3f2b...",
  "ipAddress": "192.168.1.42",
  "client": "spring-boot-app",
  "details": {
    "auth_method": "password",
    "username": "testuser"
  }
}
```

**Why you should care about events:**

- **Debugging:** A user says "I can't log in" → you check Login Events → find `LOGIN_ERROR` with reason `INVALID_CREDENTIALS`.
- **Security auditing:** You see `LOGIN` events from an IP in a foreign country → possible account compromise.
- **Compliance:** Many regulations (GDPR, SOC2, etc.) require you to track who accessed what and when.
- **Admin accountability:** Admin Events tell you who deleted a role, changed a user's permissions, or created a new client.

**Enabling events (disabled by default):**

1. Go to Realm settings → Events tab.
2. Under **Event Config**, toggle **Save events** to ON.
3. Set **Expiration** (e.g., 30 days) so old events are cleaned up.
4. Click **Save**.
5. Repeat for **Admin events** — toggle **Save events** to ON.

**Viewing events:** Realm settings → Events tab → Event log or Admin log.

**Optional — Event listeners (for production):**
Keycloak can push events to an external system (e.g., send login events to Slack, write admin events to a database). This is done via **Event Listeners** (SPI extensions). Common setups: send events to an ELK stack, Splunk, or a webhook.

---

## 9. Putting It All Together — A Real-World Scenario

Imagine you're building a SaaS product called `TaskMaster` (a project management tool) with three apps:

| App | Type | Keycloak Client ID |
|---|---|---|
| React web frontend | SPA | `taskmaster-web` |
| Spring Boot API | Backend service | `taskmaster-api` |
| Mobile app (React Native) | Mobile | `taskmaster-mobile` |

### Step-by-step Keycloak setup:

1. **Create a realm** called `taskmaster`.
2. **Create three Clients**: `taskmaster-web` (public), `taskmaster-api` (confidential), `taskmaster-mobile` (public).
3. **Create Client Scopes**: `taskmaster-profile` (default — name, email), `taskmaster-roles` (default — roles), `taskmaster-admin` (optional — admin-specific claims).
4. **Create Realm Roles**: `owner`, `manager`, `member`. Assign these to clients as needed.
5. **Create Groups**: `Owners`, `Managers`, `Members`. Assign roles to groups:
   - `Owners` group → `owner` role
   - `Managers` group → `manager` role
   - `Members` group → `member` role
6. **Create Users** and add them to groups:
   - `john@example.com` → `Owners` group → inherits `owner` role.
   - `sara@example.com` → `Managers` group → inherits `manager` role.
7. **Enable Events** for auditing logins and admin changes.
8. **Configure your Spring Boot API** (`taskmaster-api`) to:
   - Validate JWT tokens issued by Keycloak.
   - Check `realm_access.roles` to enforce authorization (`owner` can delete projects, `member` can only view).

### What happens when John logs in?

1. John enters credentials on the React web app (`taskmaster-web`).
2. `taskmaster-web` redirects to Keycloak → Keycloak authenticates John.
3. Keycloak creates a **session** for John.
4. Keycloak issues a JWT containing: `roles: ["owner"]`, `groups: ["/Owners"]`, `email: "john@example.com"`.
5. The React app sends this JWT to `taskmaster-api` on every request.
6. Spring Boot validates the JWT signature using Keycloak's public keys.
7. The `owner` role in the token authorizes John to access admin endpoints.
8. If Sara later logs in from her phone, the same session applies (SSO) — no re-login needed if the session is still active.

---

## 10. Quick Reference — Keycloak Dashboard Menu

| Menu Item | What it's for |
|---|---|
| **Realm Settings** | General realm config: tokens, themes, login settings, events |
| **Clients** | Register and configure applications that use this realm for auth |
| **Client Scopes** | Define reusable bundles of claims (profile, email, roles, custom) |
| **Realm Roles** | Define global roles (admin, editor, viewer, etc.) |
| **Users** | Create and manage user accounts, credentials, sessions, role mappings |
| **Groups** | Organize users hierarchically, assign roles in bulk |
| **Sessions** | View and manage active user sessions (SSO, logout) |
| **Events** | Audit log for logins and admin actions |
| **Identity Providers** | Configure external logins (Google, Facebook, GitHub, SAML, etc.) |
| **Authentication** | Configure login flows, MFA, password policies, OTP |
| **User Federation** | Connect Keycloak to LDAP/Active Directory for existing corp directories |
