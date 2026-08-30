# CLAUDE.md — wso2-mi (learning project)

Curriculum lives in `TODO.md`, phase by phase, in order. Concepts live in `WSO2-GUIDE.md`. Don't duplicate either here — read them first.

Learning mode: teach and verify, don't pre-write what the TODO asks the user to do themselves in VS Code (WSO2 Integrator: MI extension). Tick a TODO box only after the user demonstrates the step, not just reads about it.

Verified environment (2026-08-12):
- MI runtime at `/home/kero/.wso2-mi/micro-integrator/wso2mi-4.6.0`; Java 21 Temurin via `.sdkman/candidates/java/current`.
- Backend is `../wso2-server`. Real port is `8093`, real login path is `/auth/login`. `BackendServiceConn.xml` (`localhost:8009`) and `DataAPI.xml` (`relativePath>/login`) are both stale — fixing these two is the first hands-on exercise, not a skippable pre-req.
- Seeded login: `mbank` / `123456` (SQLite, `DatabaseInitializer.java`). `/data` requires the JWT; `/not-auth-data` is public, no token needed.
- `MI.LEGACY_EXPRESSION_ENABLED: false` in `.vscode/settings.json` → this project uses `${}` Synapse expressions, not old XPath/`get-property()`. Company project (`ni-vcn-wso2-integration`) runs MI 4.5.0; this one is 4.6.0 — syntax can differ.
- `target/kero_1.0.0.car` is a stale build from an earlier `NotAuthDataAPI` exercise, not the current `DataAPI.xml`. Fine for Phase 5 `.car`-structure study, not for tracing current flow.
