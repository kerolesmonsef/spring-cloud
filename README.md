# Spring Cloud Sample

> **Bookmark** — jump to section:
> - [Config Server](#spring-cloud-config-server)
> - [Load Balancer](#spring-cloud-load-balancer)
> - [OpenFeign](#spring-cloud-openfeign)

---

# Spring Cloud Config Server

## What is Config Server?

A centralized service that stores and serves **externalized configuration** (`.properties`/`.yml`) to all microservices in a system. Instead of each service having its own config file, they all fetch config from one place.

```
┌─────────────┐     HTTP     ┌──────────────┐     ┌──────────┐
│  Config     │◄────────────►│  Config      │────►│ Git      │
│  Client A   │              │  Server      │     │ Backend  │
├─────────────┤              │  :8888       │     └──────────┘
│  Config     │              └──────────────┘
│  Client B   │
├─────────────┤
│  Config     │
│  Client C   │
└─────────────┘
```

## Problem It Solves

**Without Config Server** — config is scattered everywhere:



**Issues:**
- Restart every service to change a property
- Can't rotate secrets without redeploy
- No audit trail — who changed what, where
- Each service needs its own env-specific build

**With Config Server** — single source of truth:

```
     ┌─────────────────────────────────────┐
     │         Config Server :8888         │
     │  (reads from Git backend)           │
     └──────────┬──────────────────────────┘
                │
    ┌───────────┼───────────┬───────────┐
    ▼           ▼           ▼           ▼
┌───────┐  ┌───────┐  ┌───────┐  ┌───────┐
│  App  │  │  App  │  │  App  │  │  App  │
│   A   │  │   B   │  │   C   │  │   D   │
└───────┘  └───────┘  └───────┘  └───────┘
```

**Fixes:**
- Change config **once** → all services pick it up on restart (or with `/actuator/refresh`)
- Rotate secrets without touching/deploying code
- Config is versioned (Git history = audit trail)
- Same artifact across all environments — just point at different Git branches/profiles

## How to Enable in Spring Boot

```properties
spring.config.import=optional:configserver:http://localhost:8888
```


## When to Use

| Scenario | Use Config Server? |
|----------|-------------------|
| 1–2 microservices | Overkill — local files are fine |
| 3+ microservices | ✅ Yes — centralize management |
| Need secret rotation | ✅ Yes — change in Git, redeploy not needed |
| Kubernetes/Cloud | ✅ Yes — pairs with native cloud config |

---

# Spring Cloud Load Balancer

## What is Load Balancer?

A client-side load balancer that routes requests across multiple service instances. Uses **Spring Cloud LoadBalancer** (not Ribbon —Ribbon is in maintenance mode) to distribute traffic.

```
┌─────────────┐          ┌─────────────────────┐          ┌──────────────────┐
│   Client    │─────────►│  Load Balancer      │─────────►│ Service Instance │
│  (Gateway)  │          │  (Gateway :8080)    │          │     :8001        │
│   :8080     │          └─────────────────────┘          ├──────────────────┤
└─────────────┘                    │                       │ Service Instance │
                                    └──────────────────────►│     :8002        │
                                                          └──────────────────┘
```

## Problem It Solves

**Without Load Balancer** — client must know all service URLs:

```
App A ──► http://server1:8001/info
App A ──► http://server2:8002/info
```

**Issues:**
- Hardcode URLs in client — tight coupling
- No traffic distribution — can't scale horizontally
- No fault tolerance — if one server dies, client fails
- Manual service discovery — add/remove servers requires code changes

**With Load Balancer** — client uses logical service name:

```
         ┌─────────────────────────────────────┐
         │       Gateway (Load Balancer)       │
         │   Uses @LoadBalanced RestTemplate   │
         └──────────────┬──────────────────────┘
                        │ "myservice" (logical name)
        ┌───────────────┼───────────┐
        ▼               ▼           ▼
   ┌─────────┐    ┌─────────┐  ┌─────────┐
   │ Service1│    │ Service1│  │ Service2│
   │  :8001  │    │  :8001  │  │  :8002  │
   │(Instance1)   │(Instance2)  │(Instance3)
   └─────────┘    └─────────┘  └─────────┘
```

**Fixes:**
- Client references **logical service name**, not IP/port
- Traffic automatically distributed across instances
- Automatic failover — dead instances are skipped
- Dynamic — add/remove instances without code changes

## How to Enable in Spring Boot

**Gateway (Load Balancer client):**

```java
@Configuration
public class LoadBalancerConfig {
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@RestController
public class GatewayController {
    @GetMapping("/call")
    public String callService() {
        // Uses logical name "myservice" - LoadBalancer resolves to actual URL
        return restTemplate.getForObject("http://myservice/info", String.class);
    }
}
```

**application.properties:**

```properties
spring.cloud.discovery.client.simple.instances.myservice[0].uri=http://localhost:8001
spring.cloud.discovery.client.simple.instances.myservice[1].uri=http://localhost:8002
```

### Alternative: Reactive WebClient

`RestTemplate` is **deprecated** as of Spring Boot 3.4 / Spring Cloud 2024.0. Use **WebClient** instead for reactive, non-blocking HTTP calls.

**Add dependency (if not already present):**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

**Configure WebClient with LoadBalancer:**

```java
@Configuration
public class LoadBalancerConfig {

    @Bean
    public WebClient.Builder webClientBuilder(LoadBalancerExchangeFilterFunction filter) {
        return WebClient.builder()
                .filter(filter);
    }
}

@RestController
public class GatewayController {

    private final WebClient webClient;

    public GatewayController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @GetMapping("/call")
    public Mono<String> callService() {
        return webClient.get()
                .uri("http://myservice/info")
                .retrieve()
                .bodyToMono(String.class);
    }
}
```

> **Note:** Either RestTemplate or WebClient can be used — both work with `@LoadBalanced`. RestTemplate is simpler for sync workflows; WebClient is non-blocking and recommended for modern reactive applications.

## When to Use

| Scenario | Use Load Balancer? |
|----------|-------------------|
| Single service, no scaling | Overkill — direct URLs are fine |
| Multiple instances, need traffic distribution | ✅ Yes — distributes requests |
| Fault tolerance required | ✅ Yes — skips dead instances |
| Microservices communication | ✅ Yes — decouples client from server URLs |

---

# Spring Cloud OpenFeign

## What is OpenFeign?

A **declarative HTTP client** that generates client-side stubs from interfaces. Instead of writing HTTP calls manually, you define a Java interface — Feign generates the implementation at runtime.

```
┌─────────────┐          ┌──────────────────┐          ┌─────────────┐
│   Client    │─────────►│   OpenFeign      │─────────►│   Remote    │
│             │          │   Client Proxy   │          │   Service   │
│ (Interface)│          │ (generated impl)  │          │             │
└─────────────┘          └──────────────────┘          └─────────────┘
```

## Problem It Solves

**Without OpenFeign** — manual HTTP calls:

```java
@RestController
public class PostController {
    private final RestTemplate restTemplate;

    @GetMapping("/posts")
    public List<Post> getPosts() {
        // URL, headers, error handling — all manual
        return restTemplate.exchange(
            "https://api.example.com/posts",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Post>>() {}
        ).getBody();
    }
}
```

**Issues:**
- Verbose — every HTTP call requires manual boilerplate
- No type safety — URL is a string, easy to typo
- Error-prone — must handle serialization/deserialization manually
- Scattered — no central place to see all HTTP clients

**With OpenFeign** — declarative interface:

```java
@FeignClient(name = "posts-client", url = "https://api.example.com")
public interface PostsClient {
    @GetMapping("/posts")
    List<Post> getPosts();
}

@RestController
public class PostController {
    private final PostsClient postsClient;

    @GetMapping("/posts")
    public List<Post> getPosts() {
        return postsClient.getPosts(); // Just call the interface!
    }
}
```

**Fixes:**
- **Declarative** — define interface, not implementation
- **Type-safe** — compile-time checks on endpoints
- **Automatic** — serialization/deserialization handled
- **Centralized** — all HTTP clients are interfaces

## How to Enable in Spring Boot

**1. Add dependency:**

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

**2. Enable Feign on main class:**

```java
@SpringBootApplication
@EnableFeignClients
public class OpenFeignApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpenFeignApplication.class, args);
    }
}
```

**3. Define Feign client interface:**

```java
@FeignClient(name = "posts-client", url = "${external.api.url}")
public interface PostsClient {

    @GetMapping("/posts")
    PostResponse getPosts();

    @GetMapping("/posts/{id}")
    Post getPostById(@PathVariable("id") Long id);
}
```

**4. Use it in your controller:**

```java
@RestController
@RequestMapping("/posts")
public class PostsController {

    private final PostsClient postsClient;

    public PostsController(PostsClient postsClient) {
        this.postsClient = postsClient;
    }

    @GetMapping
    public PostResponse listPosts() {
        return postsClient.getPosts();
    }
}
```

**application.properties:**

```properties
spring.application.name=OpenFeign
server.port=8091
external.api.url=https://api.example.com
```

## OpenFeign with LoadBalancer

OpenFeign integrates with **Spring Cloud LoadBalancer** to enable load-balanced calls to microservices:

```java
@FeignClient(name = "myservice")
public interface MyServiceClient {
    @GetMapping("/info")
    String getInfo();
}
```

This automatically uses the LoadBalancer to resolve `myservice` to available instances — same as `@LoadBalanced` RestTemplate, but with declarative syntax.

## OpenFeign Advanced Features

OpenFeign can be extended with additional capabilities:

### Load Balancing with Eureka

When using **Eureka** as a service registry, OpenFeign can discover services dynamically:

```java
@FeignClient(name = "user-service")  // name = Eureka service ID
public interface UserClient {
    @GetMapping("/users/{id}")
    User getUser(@PathVariable("id") Long id);
}
```

```properties
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

### Circuit Breaker with Resilience4j

Add circuit breaker to prevent cascading failures:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker</artifactId>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
```

```java
@FeignClient(name = "myservice", fallback = MyServiceFallback.class)
public interface MyServiceClient {
    @GetMapping("/info")
    String getInfo();
}

@Component
public class MyServiceFallback implements MyServiceClient {
    @Override
    public String getInfo() {
        return "Fallback: Service unavailable";
    }
}
```

```properties
resilience4j.circuitbreaker.configs.default.sliding-window-size=10
resilience4j.circuitbreaker.configs.default.failure-rate-threshold=50
resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state=5s
```

### Retry

Automatically retry failed requests:

```properties
spring.cloud.openfeign.client.config.default.connect-timeout=2000
spring.cloud.openfeign.client.config.default.read-timeout=5000
spring.cloud.openfeign.client.config.default.retry-on-error=true
spring.cloud.openfeign.client.config.default.retry-max=3
```

Or with Resilience4j:

```properties
resilience4j.retry.configs.default.max-attempts=3
resilience4j.retry.configs.default.wait-duration=1s
```

### Interceptors

Add custom interceptors for authentication, logging, etc.:

```java
@Configuration
public class FeignInterceptorConfig {

    @Bean
    public RequestInterceptor authInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("Authorization", "Bearer token123");
            requestTemplate.header("X-Custom-Header", "value");
        };
    }

    @Bean
    public RequestInterceptor loggingInterceptor() {
        return requestTemplate -> {
            System.out.println("Feign Request: " + requestTemplate.url());
        };
    }
}
```

```java
@FeignClient(name = "myservice", configuration = FeignInterceptorConfig.class)
public interface MyServiceClient {
    @GetMapping("/info")
    String getInfo();
}
```

## When to Use

| Scenario | Use OpenFeign? |
|----------|-------------------|
| Need simple HTTP calls to external services | ✅ Yes — clean declarative API |
| Want type-safe API contracts | ✅ Yes — compile-time checks |
| Need to integrate with LoadBalancer | ✅ Yes — native LoadBalancer support |
| Migrating from RestTemplate | ✅ Yes — less boilerplate |
| Complex async/reactive needs | Consider WebClient instead |
