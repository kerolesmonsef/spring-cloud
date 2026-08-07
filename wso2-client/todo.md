# WSO2 API Manager — Learning Path

Install: `/mnt/2427cce8-dbbb-44b3-bd3b-87c263acaf9e/programs/wso2am-4.4.0` (v4.4.0, Java 17 OK — GraalVM 17.0.12 confirmed on this box).

Default ports (no offset set): Publisher/DevPortal/Carbon console `9443` (https), Gateway `8243` (https) / `8280` (http). No clash w/ springCload's used ports (all ≤ 8888 except this).

Start server: `cd /mnt/2427cce8-dbbb-44b3-bd3b-87c263acaf9e/programs/wso2am-4.4.0/bin && ./api-manager.sh` (first boot slow, several min — DB init + OSGi bundle resolve).
Console: `https://localhost:9443/carbon` (admin/admin default creds).
Publisher: `https://localhost:9443/publisher`. DevPortal: `https://localhost:9443/devportal`.

Goal of this module (`springCload/wso2/`): a plain Spring Boot backend service that WSO2 AM fronts as a managed API — so you exercise gateway/proxy features against a real Spring backend, not a mock.

## 0. What / Why (concepts first, no hands yet)
- [ ] What is an API Gateway, and what problem does it solve vs. calling backend services directly?
- [ ] What is WSO2 API Manager specifically — read the 4-plane architecture: Traffic Manager (gateway, rate limiting), Publisher (API lifecycle, design), DevPortal (subscriber-facing catalog, app/key mgmt), Key Manager (OAuth2 tokens).
- [ ] Where it fits vs. what you already know from this repo: compare to `LoadBalancer/` (client-side LB) and `ServiceDiscovery/` (Eureka) — WSO2 AM is a full API management layer, not just routing/discovery.
- [ ] When to reach for WSO2 AM vs. Spring Cloud Gateway vs. just Nginx — tradeoffs (governance/monetization/analytics vs. lightweight routing).

## 1. First run
- [x] Start server, log into Carbon console, Publisher, DevPortal — get the 3 UIs straight in your head.
- [x] Change default admin password (never leave admin/admin). — SKIPPED, learning box only, admin/admin kept.

## 2. Build the target backend (this module)
- [x] Flesh out `Wso2Application` with 1-2 simple REST endpoints (`/hello`, `/items/{id}`) — `BackendController.java`.
- [x] Assign it a port (`8092`), add row to root `AGENTS.md` Used Ports table.
- [x] Run it standalone, confirm with curl before touching WSO2.

## 3. Publish your first API
- [x] In Publisher: create API from the backend above (manual REST API, `WSO2LearningAPI`, context `/wso2learn`, v1.0.0, endpoint `http://localhost:8092`).
- [x] Define resources/methods matching your endpoints.
- [x] Publish the API (lifecycle: Created → Published).
- [x] In DevPortal: subscribe via `DefaultApplication`, generate sandbox keys, get access token.
- [x] Invoke through the gateway (`https://localhost:8243/wso2learn/1.0.0/hello`) with the token — confirmed response matches direct backend call.

## 4. Security
- [ ] Call gateway with NO token — confirm 401 (proves gateway enforces auth, not just proxying).
- [ ] OAuth2 client-credentials flow (what you just did in #3) — understand token endpoint, scopes.
- [ ] API key vs OAuth2 vs mutual TLS — when each applies.
- [ ] Add a scope to one resource, require it, verify 403 without it.

## 5. Traffic control
- [ ] Rate limiting / throttling tiers (Gold/Silver/Bronze or custom) — apply to your API, hit the limit, see 429.
- [ ] Request/response size limits, basic quota policy.

## 6. Mediation (request/response transformation)
- [ ] Write a simple mediation policy/sequence (e.g. add a header, log request) using the Policy editor.
- [ ] Understand Synapse config underneath (what the UI generates).

## 7. Versioning & lifecycle
- [ ] Create v2 of your API with a breaking change, run v1/v2 side by side.
- [ ] Deprecate/retire v1, see effect on DevPortal visibility.

## 8. Observability
- [ ] Explore built-in Analytics/Insights (or note if this 4.4.0 distro needs Choreo/Analytics add-on — verify before assuming available).
- [ ] Check gateway access logs under `repository/logs/`.

## 9. Bigger picture (only after 1-8 hands-on)
- [ ] Microgateway / API Gateway-only deployment mode (decoupled from Publisher/DevPortal) — when you'd split these planes.
- [ ] Docker/Kubernetes deployment of WSO2 AM (vs. this local unpacked distro).
- [ ] CI/CD for API definitions (OpenAPI import/export, `apictl` CLI) instead of clicking through Publisher UI.

## Notes / decisions log
- (fill in as you go — surprises, gotchas, config you changed from default)
