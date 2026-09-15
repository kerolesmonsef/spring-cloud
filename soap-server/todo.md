# SOAP Learning Plan

## What is SOAP (vs REST you already know)

- SOAP = Simple Object Access Protocol. XML-based messaging protocol, not an architecture style like REST.
- Every request/response is an XML envelope (`<soap:Envelope><soap:Body>...</soap:Body></soap:Envelope>`).
- Contract-first: you define a **WSDL** (Web Services Description Language) file that describes operations, input/output messages, and types (via XSD schema). Client and server both generate code from this contract.
- Transport usually HTTP POST to ONE endpoint (e.g. `/ws`), not many URLs per resource like REST. The operation name lives inside the XML body, not the URL.
- Strongly typed, verbose, has built-in standards for security (WS-Security), transactions (WS-AT), reliable messaging (WS-RM) — this is why enterprise/banking/legacy systems still use it.
- Spring support: **Spring-WS** (`spring-ws-core`), different from Spring MVC REST. Contract-first by default (write XSD, generate WSDL).

## Core concepts to learn, in order

1. **XSD (XML Schema)** — define request/response data shapes (like your DTO classes, but as XML schema).
2. **WSDL** — generated from XSD + defined operations; describes the whole service contract.
3. **Endpoint** — Spring-WS `@Endpoint` + `@PayloadRoot` (namespace + localPart) routes incoming XML to a Java method, analogous to `@RestController` + `@GetMapping`.
4. **Marshalling** — JAXB converts XML <-> Java objects automatically (like Jackson does JSON <-> Java).
5. **SOAP client** — test with `curl`/Postman (raw XML POST) or generate a Java client stub from the WSDL.

## Step-by-step build plan

- [ ] Step 1: Add dependencies (`spring-boot-starter-web-services`, `wsdl4j`) to `build.gradle`.
- [ ] Step 2: Write an XSD schema for one simple domain (e.g. "Country lookup": request has a country name, response has capital/population/currency).
- [ ] Step 3: Generate JAXB Java classes from XSD (gradle plugin or `xjc` tool).
- [ ] Step 4: Write `@Endpoint` class handling the request, returning a response object.
- [ ] Step 5: Configure `MessageDispatcherServlet` + `DefaultWsdl11Definition` bean to auto-expose WSDL at `/ws/countries.wsdl`.
- [ ] Step 6: Run app, view generated WSDL in browser.
- [ ] Step 7: Test with raw SOAP XML request via `curl -H "Content-Type: text/xml"` or Postman.
- [ ] Step 8: (stretch) Add WS-Security (basic auth or UsernameToken) to compare with your JWT REST setups.

## Port

Assign a port when you add the Boot app (check/update Used Ports table in root `AGENTS.md` per repo convention).

## How the current `user` package fits together (read this before copying it)

- `user/wsdl/UserXml.java` — the wire-format POJO (JAXB-annotated), NOT the JPA entity. Endpoint converts entity → this before returning.
- `user/wsdl/package-info.java` — `@XmlSchema(namespace=..., elementFormDefault=QUALIFIED)` for the package. JAXB has no namespace without it. Needed in EVERY package that holds JAXB classes (annotation doesn't inherit across packages) — that's why `request/` and `response/` each have their own copy of this file with the same namespace.
- `user/request/*Request.java`, `user/response/*Response.java` — one class per SOAP operation, JAXB beans, matched to XSD elements by `@XmlType`/element name.
- `user/endpoint/UserEndpoint.java` — `@Endpoint` bean. Each method: `@PayloadRoot(namespace, localPart="opNameRequest")` + `@RequestPayload`/`@ResponsePayload` = routes one XML operation to one method, like `@PostMapping` + `@RequestBody`/`@ResponseBody`. Delegates to `@Service`, never touches JPA directly.
- `user/model/User.java` — JPA `@Entity`. `user/repository/UserRepository.java` — `JpaRepository`. `user/service/UserService.java` — business logic, only class that talks to repository.
- XSD file (`src/main/resources/*.xsd`) is the actual source of truth — request/response Java classes must match its element names/types.

## Checklist for a new SOAP CRUD (e.g. "Posts")

- [ ] Write `posts.xsd` — define request/response elements + `post` type (id, title, body, ...).
- [ ] Point `DefaultWsdl11Definition`/`SimpleXsdSchema` bean (in `WebServiceConfig`) at the new XSD, or add a second WSDL bean if keeping it separate from `users.xsd`.
- [ ] `post/model/Post.java` — `@Entity`, mutable class (NOT a record), no-arg protected ctor + `@GeneratedValue(strategy=IDENTITY)` id.
- [ ] `post/repository/PostRepository.java` — `extends JpaRepository<Post, Long>`. Nothing else needed for basic CRUD.
- [ ] `post/service/PostService.java` — findAll/findById/create/update/delete, mirrors `UserService`.
- [ ] `post/wsdl/PostXml.java` — JAXB wire type + `post/wsdl/package-info.java` with `@XmlSchema` (own namespace, e.g. `.../posts`).
- [ ] `post/request/*Request.java` + `post/request/package-info.java` (same namespace as above).
- [ ] `post/response/*Response.java` + `post/response/package-info.java` (same namespace).
- [ ] `post/endpoint/PostEndpoint.java` — `@Endpoint`, one `@PayloadRoot` method per operation, `toXml()` mapper entity→PostXml.
- [ ] `application.properties` — nothing new needed if reusing same datasource; `ddl-auto=update` auto-creates `posts` table from `@Entity`.
- [ ] No new dependency needed (JPA/JAXB/Spring-WS already in `build.gradle`) unless posts need a different DB/dialect.
- [ ] Test via curl SOAP POST to `/ws` with the new operation's XML body.

## Pagination

- [ ] Learn: how to add pagination to a SOAP list operation.

## Reference

- Official guide: https://spring.io/guides/gs/producing-web-service
