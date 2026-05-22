# OpenFeign Posts Demo

This module demonstrates a simple Spring Cloud OpenFeign client that calls JSONPlaceholder and lists posts.

## What it does

- Reads the remote API base URL from `src/main/resources/application.properties`:

```properties
posts.api.url=https://jsonplaceholder.typicode.com
```

- Uses `@FeignClient(name = "posts-clienti", url = "${posts.api.url}")` in `PostsClient`.
- Maps each remote post into `PostResponse`.
- Exposes a local endpoint that delegates to the Feign client:

```http
GET http://localhost:8091/posts
```
