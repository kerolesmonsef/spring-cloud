# WSO2 Micro Integrator (MI) — Beginner Guide

## What WSO2 MI is
Integration runtime for connecting APIs, services, files, queues. Config-driven (mostly XML synapse configs), not a Java app you write from scratch. Think: ESB (Enterprise Service Bus) successor, lightweight, built for microservice-style integration flows.

Two ways to build:
- **WSO2 Integrator: MI extension for VSCode** ([marketplace link](https://marketplace.visualstudio.com/items?itemName=WSO2.micro-integrator)) — this repo uses this. GUI + XML underneath, replaces the old Eclipse-based Integration Studio.
- **Hand-write XML** artifacts directly, deploy as a `.car` (Composite Application Archive)

## Core concepts (learn in this order)

1. **API** — HTTP-facing artifact. Defines a context path + resources (GET/POST/etc), like a REST controller. This is your entry point.
2. **Sequence** — reusable block of mediation logic (steps to process a message). APIs call sequences. Think: middleware chain / pipeline.
3. **Mediator** — single step inside a sequence. Building blocks: `log`, `payloadFactory`, `filter`, `switch`, `call`, `send`, `respond`, `property`, `header`.
4. **Endpoint** — where a message goes (backend URL). Types: HTTP endpoint, address endpoint, load-balance, failover.
5. **Message flow model** — every request has an **In sequence** (request path) and **Out sequence** (response path), plus a **Fault sequence** (error handling). This trips up everyone at first — internalize it early.
6. **Proxy Service** — older/alt entry point (non-REST, SOAP-friendly). APIs are the modern preferred way; know proxies exist but don't start there.
7. **Connectors** — pre-built integrations (Salesforce, Gmail, DB, etc) you plug in instead of writing raw HTTP calls.
8. **Data Services / Data Mapper** — for DB-backed APIs and transforming payload shape (JSON↔JSON, JSON↔XML).
9. **Registry** — shared storage built into MI, for stuff your integrations need that isn't mediation logic itself: WSDL/XSD schemas, properties/config values, reusable XML fragments, certs. Similar idea to a `.env`, but more than that — it can hold actual files (not just key=value), organized in folder-like paths (`gov:/config/myfile.xml`), and a sequence can read/re-read it at request-time, not just app startup. Two scopes: `gov:` (shared server-wide) and `conf:` (local to that one MI instance). Skip it for your first API — reach for it once you have a shared schema or a config value you don't want hardcoded in every sequence.
10. **CApp (Carbon Application) / `.car` file** — the deployable unit. **Not** the same model as a Spring Boot `.jar`: a jar is your whole compiled app (run it directly). MI itself is already a running server; a `.car` is just your packaged integration config (APIs/sequences/endpoints XML), which you deploy *into* that running server — same model as dropping a WAR into Tomcat. The VSCode extension exports your project to a `.car` file; copy it into `<MI_HOME>/repository/deployment/server/carbonapps/` and MI hot-deploys it automatically (no restart). Check `<MI_HOME>/repository/logs/wso2-carbon.log` to confirm it deployed. Same `.car` gets promoted across dev/staging/prod — just copy to each instance's carbonapps folder.

## Key terminology cheat sheet
| Term | Meaning |
|---|---|
| Synapse | the underlying mediation engine/config language (XML) |
| Mediation | the act of processing a message as it flows through |
| ESB | Enterprise Service Bus — the older product family MI evolved from |
| Payload | the message body being processed |
| `synapse.env` / message context | the object carrying the message + properties through mediators |
| Passthrough transport | MI's default non-blocking HTTP transport |
| Inbound Endpoint | alt way to trigger flows: polling (file, JMS) or listening (HTTP) sources instead of REST API |

## Practical starting path
1. Install **WSO2 Integrator: MI** VSCode extension + the **Micro Integrator runtime** separately (extension = design-time, MI = runtime).
2. Build one REST API in VSCode: single resource, `log` mediator, `respond` mediator. Run it, hit with curl.
3. Add a real backend call: `call` mediator to an HTTP endpoint (e.g. your springCloud `LoadBalancerService1` on :8001), see request/response passthrough.
4. Add `payloadFactory` to reshape JSON before forwarding.
5. Add a `filter`/`switch` for conditional routing (e.g. route by header or path param).
6. Add a `fault` sequence, force an error, confirm it's caught not just 500'd raw.
7. Package as `.car`, deploy into MI's `repository/deployment/server/carbonapps/`, confirm hot-deploy picks it up.
8. Only then: look at connectors / Data Mapper / inbound endpoints.

## Where this fits your repo
Your `wso2-server` project (Spring Boot backend) is a plain REST target — MI would sit in front of it as the integration/gateway layer, similar role to your `LoadBalancer` gateway module but with mediation/transformation instead of just routing. Good first exercise: point an MI API at `wso2-server`'s existing endpoints.

## Docs
Official docs: https://mi.docs.wso2.com/en/latest/ — "Quick Start Guide" + "Key Concepts" pages match this list almost 1:1.
