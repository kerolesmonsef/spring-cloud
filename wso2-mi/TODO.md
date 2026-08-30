# WSO2 Learning TODO

Learning target: understand enough WSO2 Micro Integrator to read, change, build, and deploy a basic integration safely.

## Phase 1: Vocabulary

- [ ] Understand Micro Integrator versus API Manager
- [ ] Understand API, resource, sequence, endpoint, local entry, mediator, and CAR
- [ ] Read `WSO2-GUIDE.md`
- [ ] Identify the client, MI runtime, and backend in a request flow

## Phase 2: Small Project

- [ ] Read `pom.xml`
- [ ] Read `.vscode/settings.json`
- [ ] Read `deployment/deployment.toml`
- [ ] Read `src/main/wso2mi/artifacts/apis/DataAPI.xml`
- [ ] Read `src/main/wso2mi/artifacts/local-entries/BackendServiceConn.xml`
- [ ] Draw the `DataAPI` request flow in plain English
- [ ] Identify every input, backend call, variable, response, and error path

## Phase 3: XML And Mediation

- [ ] Understand API contexts and resource paths
- [ ] Understand `inSequence` and `faultSequence`
- [ ] Understand logs
- [ ] Understand properties and variables
- [ ] Understand JSON expressions
- [ ] Understand request and response headers
- [ ] Understand HTTP calls
- [ ] Create an MI API that returns a static JSON response without calling a backend
- [ ] Understand payload transformation
- [ ] Understand HTTP status handling
- [ ] Understand reusable sequences

## Phase 4: Contracts And Configuration

- [ ] Compare `DataAPI.xml` with `resources/api-definitions/DataAPI.yaml`
- [ ] Understand why OpenAPI and runtime XML are separate
- [ ] Understand local entries and named backend connections
- [ ] Understand project configuration versus server configuration
- [ ] Understand why `.env` and secrets must not be committed

## Phase 5: Build And Deployment

- [ ] Understand Maven packaging
- [ ] Understand what a `.car` file contains
- [ ] Understand local MI deployment
- [ ] Understand Docker deployment
- [ ] Understand environment-specific configuration
- [ ] Understand server truststores and keystores
- [ ] Understand why certificate changes may require a restart

## Phase 6: Company Project

- [ ] Choose one simple API from `ni-vcn-wso2-integration`
- [ ] Trace its API XML from request to response
- [ ] Read each referenced shared sequence
- [ ] Compare its API XML with its OpenAPI YAML
- [ ] Understand request validation
- [ ] Understand backend header construction
- [ ] Understand OAuth or login token flow
- [ ] Understand response and error mapping
- [ ] Understand dynamic environment values
- [ ] Read the company Dockerfile and deployment configuration

## Phase 7: Production Basics

- [ ] Define how secrets are supplied in each environment
- [ ] Configure TLS correctly
- [ ] Import required backend CA certificates
- [ ] Set connection and socket timeouts
- [ ] Decide retry behavior without duplicating unsafe requests
- [ ] Add correlation IDs
- [ ] Mask credentials and personal data in logs
- [ ] Add health checks and monitoring
- [ ] Build a CI/CD path for the `.car` or Docker image
- [ ] Document rollback

## Ignore For Now

- [ ] WSO2 Identity Server
- [ ] WSO2 Streaming Integrator
- [ ] Kafka and message brokers
- [ ] Advanced API Manager policies
- [ ] Clustering
- [ ] Custom Java mediators
- [ ] Distributed transactions
- [ ] API monetization

## Completion Criteria

You are ready for basic MI work when you can:

- Explain an API XML file without guessing
- Trace a request through its sequences and backend call
- Change a backend URL without changing business logic
- Explain the difference between `.car` contents and server configuration
- Explain how the same integration moves from local to test to production
- Identify where credentials, certificates, timeouts, and logs are configured
