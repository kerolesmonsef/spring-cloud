# WSO2 Micro Integrator Guide

This repository is a small learning project for WSO2 Micro Integrator (MI).
The company project is:

`/mnt/2427cce8-dbbb-44b3-bd3b-87c263acaf9e/java_projects/fooProjects/ni-vcn-wso2-integration`

## What WSO2 Means Here

WSO2 Micro Integrator is middleware. It receives a request, performs integration logic, calls backend services, transforms data, and returns a response.

```text
Client -> WSO2 Micro Integrator -> Backend service
```

WSO2 API Manager is a different product. It publishes, secures, subscribes to, and monitors APIs. The browser notes file mostly describes API Manager concepts; this repository contains Micro Integrator artifacts.

Learn Micro Integrator first. Learn API Manager when an integration must be published for external consumers.

## VS Code

Use the **WSO2 Integrator: MI** extension. It supports Micro Integrator project artifacts, local server configuration, validation, and deployment workflows.

This project points VS Code to a local MI installation in `.vscode/settings.json`:

```json
{
  "MI.JAVA_HOME": "/home/kero/.sdkman/candidates/java/current",
  "MI.SERVER_PATH": "/home/kero/.wso2-mi/micro-integrator/wso2mi-4.6.0"
}
```

The company project uses MI `4.5.0`; this learning project uses MI `4.6.0`.

## Repository Structure

| Path | Purpose |
|---|---|
| `pom.xml` | Maven build that creates the deployable Carbon Application (`.car`) |
| `src/main/wso2mi/artifacts/apis/` | HTTP APIs exposed by MI |
| `src/main/wso2mi/artifacts/sequences/` | Reusable mediation flows |
| `src/main/wso2mi/artifacts/endpoints/` | Reusable backend connection definitions |
| `src/main/wso2mi/artifacts/local-entries/` | Named reusable configuration entries |
| `src/main/wso2mi/resources/api-definitions/` | OpenAPI definitions |
| `src/main/wso2mi/resources/conf/` | Project resources and configuration |
| `deployment/deployment.toml` | MI server-level configuration |
| `deployment/docker/` | Docker packaging files |
| `.vscode/settings.json` | VS Code MI extension settings |
| `.env` | Local environment values; do not commit secrets |

## Start With `DataAPI.xml`

File:

`src/main/wso2mi/artifacts/apis/DataAPI.xml`

The API exposes `GET /data`. Its flow is:

1. Call the backend `/login` endpoint.
2. Extract the returned token.
3. Call the backend `/data` endpoint with a Bearer token.
4. Return the backend response to the original caller.
5. Return a JSON error with HTTP `500` when the fault sequence is used.

The backend base URL is defined in:

`src/main/wso2mi/artifacts/local-entries/BackendServiceConn.xml`

It currently points to `http://localhost:8009`.

## XML Basics

The XML is Synapse configuration interpreted by Micro Integrator. It is not a normal Java controller.

```xml
<api context="/data" name="DataAPI">
    <resource methods="GET" uri-template="/">
        <inSequence>
            <!-- mediation steps go here -->
        </inSequence>
        <faultSequence>
            <!-- error steps go here -->
        </faultSequence>
    </resource>
</api>
```

Important elements:

| Element | Meaning |
|---|---|
| `<api>` | Defines an exposed API |
| `<resource>` | Defines an HTTP method and path |
| `<inSequence>` | Main successful request flow |
| `<faultSequence>` | Error flow |
| `<log>` | Writes a log message |
| `<http.get>` / `<http.post>` | Calls a backend service |
| `<variable>` | Stores a value for later steps |
| `<property>` | Sets metadata or transport values |
| `<payloadFactory>` | Creates or transforms a payload |
| `<sequence>` | Calls a reusable sequence |
| `<respond>` | Sends the response to the client |

## Deployment Model

The normal deployment flow is:

```text
XML/YAML source
    -> Maven build
    -> Carbon Application (.car)
    -> Micro Integrator runtime
```

The `.car` contains integration artifacts such as APIs and sequences. The MI server separately owns server configuration such as:

- `deployment.toml`
- Keystores
- Truststores
- Database configuration
- Runtime environment variables
- Transport settings

For production, promote a tested `.car` or Docker image through CI/CD. Keep URLs, credentials, certificates, and environment-specific values outside business XML whenever possible.

## Company Project Reading Order

Read the company project in this order:

1. One API under `src/main/wso2mi/artifacts/apis/`.
2. The sequences referenced by that API.
3. Its OpenAPI file under `src/main/wso2mi/resources/api-definitions/`.
4. Its backend endpoint or dynamic endpoint logic.
5. Its authentication and error mapping.
6. Its Docker and server configuration.

Do not read all company APIs at once. They repeat the same integration patterns.

## Production Topics

Before deploying a real integration, understand:

- Secrets management
- HTTPS and TLS
- Keystore versus truststore
- Backend certificate chains
- Timeouts and retry behavior
- HTTP status mapping
- Correlation IDs and sensitive-data masking
- Health checks and monitoring
- CI/CD and rollback

## Deliberately Out Of Scope

Do not study these yet:

- WSO2 Identity Server
- Streaming Integrator
- Advanced API Manager policies
- Kafka and message brokers
- Clustering
- Custom Java mediators
- Distributed transactions
- API monetization
