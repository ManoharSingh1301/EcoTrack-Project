> ## 🔔 Latest release notes
>
> This build removes Redis entirely and re‑implements chat on Spring's in‑memory STOMP broker (MySQL‑persisted). It adds a **Borrow Request workflow** (request → accept/reject → return with automatic late‑fee calculation), **Favorites/wishlist**, a **personal Dashboard**, richer item metadata (condition, max borrow days, late fee, security deposit, borrow count), sorting, stronger client + server validation, and a fully protected, session‑aware auth flow (Login/Register hidden once authenticated).
>
> See **[ENHANCEMENTS.md](ENHANCEMENTS.md)** for the full status, known limitations, roadmap, and the free deployment guide. Stack stays limited to **Java · Spring Boot · MySQL · React** — no Docker/Redis/Kubernetes/cloud services.

# 🌱 EcoTrack — Local Community Sharing Platform

A production-grade microservices platform where neighbors can share and lend tools and items within a local community. Built with **Java 17**, **Spring Boot 3.2**, **Spring Cloud 2023.0**, **MySQL**, **WebSocket/STOMP**, **Netflix Eureka**, and a **React + Tailwind** frontend — designed to run entirely on a local machine with only a JDK, Maven, Node, and MySQL (no Redis or other infrastructure required).

---

## ✨ What's Actually Built

> This section reflects the **verified, deployed implementation** — not aspirational roadmap items.

### Backend Capabilities
- ✅ **Service Registry** — Netflix Eureka for automatic service discovery
- ✅ **API Gateway** — Reactive Spring Cloud Gateway with CORS, load balancing, WebSocket support
- ✅ **User Management** — Registration, login (BCrypt), profile CRUD, password re-hashing on update
- ✅ **Item Management** — CRUD, image upload/serve, availability toggle, category filtering, name search, pagination
- ✅ **Real-Time Chat** — WebSocket/STOMP messaging between users, item-context chat, persistent history (communication-service)
- ✅ **Inter-Service Communication** — Feign client (item→user verification) with Resilience4j circuit breaker + retry + fallback
- ✅ **Caching** — Spring Cache with an in-memory `ConcurrentMapCacheManager` (available items, items by category, user by ID); evicted on writes. No external cache server needed.
- ✅ **AOP Observability** — Logging aspect (service/controller entry, exit, duration, exceptions) across all 3 domain services; Performance aspect (`@TrackExecutionTime` in item-service)
- ✅ **Global Exception Handling** — Structured `ErrorResponse` JSON for all error types across all services
- ✅ **API Documentation** — Swagger UI via Springdoc OpenAPI on item-service and user-service
- ✅ **Automated Tests** — Unit tests (Mockito), repository integration tests (H2/DataJpaTest), controller integration tests (MockMvc/WebMvcTest)

---

## 🏗️ System Architecture

```
React Frontend (Port 5173)
        │ HTTP/REST + WebSocket (all traffic through gateway)
        ▼
┌─────────────────────────────────────────────────────┐
│              API Gateway  :8080                     │
│         Spring Cloud Gateway (WebFlux)              │
│  • Reactive CorsWebFilter                           │
│  • Routes: /api/items/**  → lb://item-service       │
│            /api/users/**  → lb://user-service       │
│            /api/chat/**   → lb://communication-service │
│            /ws-chat/**    → lb://communication-service │
│  • WebSocket support enabled                        │
│  • Dynamic discovery locator                        │
└──────────────┬──────────────────────────────────────┘
               │ Eureka-resolved lb:// URIs
   ┌───────────┼──────────────┬────────────────────────┐
   ▼           ▼              ▼                        ▼
┌──────────┐ ┌─────────────┐ ┌──────────────────┐ ┌───────────────────────┐
│Discovery │ │ Item Service│ │  User Service    │ │ Communication Service │
│  Server  │ │  :8088      │ │  :8089           │ │  :8087                │
│  :8761   │ │             │ │                  │ │                       │
│          │ │MySQL:db_items│ │MySQL:db_users    │ │MySQL:db_communication │
│Netflix   │ │In-mem cache │ │In-mem cache      │ │                       │
│Eureka    │ │Feign client │ │BCrypt + Security │ │WebSocket (STOMP)      │
│(no self- │ │Circuit break│ │AOP logging       │ │Real-time chat         │
│register) │ │AOP logging  │ │Swagger UI        │ │Chat history REST API  │
└──────────┘ └─────────────┘ └──────────────────┘ │AOP logging            │
                  │ Feign: GET /api/users/{id}      └───────────────────────┘
                  └───────────────────────────────▶ User Service
```

---

## 📋 Technology Stack

### Backend Microservices

| Technology | Version | Purpose |
|---|---|---|
| Java | 17 | Runtime |
| Spring Boot | 3.2.0 | Framework |
| Spring Cloud Gateway | 2023.0.0 BOM | API gateway (reactive) |
| Spring Cloud Eureka Server | 2023.0.0 BOM | Service registry |
| Spring Cloud Eureka Client | 2023.0.0 BOM | Service registration |
| Spring Cloud OpenFeign | 2023.0.0 BOM | Declarative HTTP client |
| Spring Cloud Circuit Breaker (Resilience4j) | 2023.0.0 BOM | Fault tolerance |
| Spring Data JPA | 3.2.0 | ORM |
| Spring Security | 3.2.0 | Password encoding, endpoint security |
| Spring Cache (ConcurrentMapCacheManager) | 3.2.0 | In-memory caching (no external server) |
| **Spring WebSocket (STOMP)** | **3.2.0** | **Real-time bidirectional messaging** |
| Spring AOP | 3.2.0 | Cross-cutting concerns |
| Spring Boot Actuator | 3.2.0 | Health/info endpoints |
| Spring Boot Validation | 3.2.0 | Bean validation (JSR-380) |
| MySQL Connector/J | (managed) | Production DB driver |
| H2 | (test scope) | In-memory DB for tests |
| Springdoc OpenAPI | 2.3.0 | Swagger UI (item-service & user-service) |
| Lombok | 1.18.42 | Code generation |
| Maven | 3.8+ | Build & dependency management |
| JUnit 5 + Mockito | (managed) | Testing |
| AssertJ | (managed) | Fluent test assertions |

---

## 🚀 Complete Setup Guide

### 📦 Prerequisites

| Tool | Version | Verify |
|---|---|---|
| JDK | 17+ | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| MySQL | 8.0+ | `mysql --version` |
| Node.js | 18+ | `node -v` |

> **No Redis / message broker / Docker required.** Caching is in-memory and chat is delivered through Spring's in-memory STOMP broker. The only external service you run is MySQL.

---

### 🗄️ Phase 1: Database Setup

#### Step 1.1 — Start MySQL

```powershell
# Windows PowerShell
Start-Service MySQL80
```

#### Step 1.2 — Create Databases

```sql
-- Connect: mysql -u root -p
CREATE DATABASE IF NOT EXISTS db_items
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS db_users
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

SHOW DATABASES;
EXIT;
```

> Both databases are also auto-created by JPA on first startup thanks to `createDatabaseIfNotExist=true` in the JDBC URL.

#### Step 1.3 — Set Credentials

Edit all three service `application.properties` files if your MySQL password differs from `admin`:

**`item-service/src/main/resources/application.properties`**
```properties
spring.datasource.password=admin   # ← change to your MySQL root password
```

**`user-service/src/main/resources/application.properties`**
```properties
spring.datasource.password=admin   # ← change to your MySQL root password
```

**`communication/src/main/resources/application.properties`**
```properties
spring.datasource.password=admin   # ← change to your MySQL root password
```

---

### ⚡ Phase 2: Environment Variables

The services read secrets from the environment (no hardcoded credentials):

```bash
# Linux/macOS
export DB_USERNAME=root
export DB_PASSWORD=your_mysql_password
export JWT_SECRET=change-me-to-a-random-string-at-least-32-chars

# Windows PowerShell (persist, then reopen the terminal)
setx DB_USERNAME "root"
setx DB_PASSWORD "your_mysql_password"
setx JWT_SECRET "change-me-to-a-random-string-at-least-32-chars"
```

> `JWT_SECRET` must be **≥ 32 characters** (HS256 requirement) and identical for user-service and api-gateway. No Redis step is needed.

---

### 🏃 Phase 3: Start Microservices

#### Option A — Automated (Windows PowerShell)

```powershell
# Run from project root
.\start-all-services.ps1
```

This script starts services in the correct order with delays: Discovery Server (30s wait) → API Gateway (20s wait) → Item Service (15s wait) → User Service (15s wait) → Communication Service (15s wait) → Frontend.

#### Option B — Manual (each in a separate terminal)

```bash
# Terminal 1: Discovery Server (start first, wait ~30s)
cd discovery-server
mvn spring-boot:run

# Terminal 2: API Gateway (start after Eureka is up)
cd api-gateway
mvn spring-boot:run

# Terminal 3: Item Service
cd item-service
mvn spring-boot:run

# Terminal 4: User Service
cd user-service
mvn spring-boot:run

# Terminal 5: Communication Service
cd communication
mvn spring-boot:run

# Terminal 6: Frontend
cd frontend
npm install
npm run dev
```

#### Option C — Pre-built JARs

```bash
# Build each service
cd item-service && mvn clean package -DskipTests
cd user-service && mvn clean package -DskipTests

# Run
java -jar item-service/target/item-service-1.0.0.jar
java -jar user-service/target/user-service-1.0.0.jar
```

---

### 🌐 Service URLs

| Service | URL | Notes |
|---|---|---|
| Eureka Dashboard | http://localhost:8761 | View registered services |
| API Gateway | http://localhost:8080 | All frontend traffic goes here |
| Item Service (direct) | http://localhost:8088 | Direct access (bypass gateway) |
| Item Service Swagger | http://localhost:8088/swagger-ui.html | Interactive API docs |
| User Service (direct) | http://localhost:8089 | Direct access (bypass gateway) |
| User Service Swagger | http://localhost:8089/swagger-ui.html | Interactive API docs |
| Communication Service | http://localhost:8087 | REST chat history endpoint |
| WebSocket Endpoint | ws://localhost:8087/ws-chat?userId={id} | STOMP over WebSocket |
| React Frontend | http://localhost:5173 | Vite dev server |

---

## 📡 REST API Reference

### User Service `/api/users`

| Method | Path | Public | Request Body | Response |
|---|---|---|---|---|
| `POST` | `/register` | ✅ | `User` JSON (username, email, password, fullName, address?, phone?, bio?) | `201 UserResponse` |
| `POST` | `/login` | ✅ | `{username, password}` | `200 LoginResponse` (userId, username, email, fullName, token, message) |
| `GET` | `/` | 🔐 | — | `200 List<UserResponse>` |
| `GET` | `/{id}` | ✅* | — | `200 UserResponse` |
| `GET` | `/username/{username}` | ✅* | — | `200 UserResponse` |
| `PUT` | `/{id}` | 🔐 | `User` JSON | `200 UserResponse` |
| `DELETE` | `/{id}` | 🔐 | — | `204 No Content` |

> ✅* = public to allow inter-service Feign calls from item-service. 🔐 = requires JWT token in the `Authorization: Bearer <token>` header, validated at the API Gateway.

**Login Note:** Login issues a signed JWT token on success. The client must store this token and pass it in the `Authorization` header for subsequent requests to protected endpoints.

**Validation Rules:**
- `username`: 3–50 characters, unique
- `email`: valid email format, unique
- `password`: minimum 8 characters (stored as BCrypt hash, never returned)
- `fullName`: required
- `phone`: optional, regex `^$|^[+]?[0-9]{10,15}$`
- `bio`: max 500 characters

---

### Item Service `/api/items`

| Method | Path | Request | Response |
|---|---|---|---|
| `GET` | `/` | — | `200 List<ItemResponse>` |
| `GET` | `/{id}` | — | `200 ItemResponse` |
| `GET` | `/owner/{ownerId}` | — | `200 List<ItemResponse>` |
| `GET` | `/available` | — | `200 List<ItemResponse>` *(cached)* |
| `GET` | `/available/page` | `?page=0&size=10&sortBy=createdAt&direction=desc` | `200 Page<ItemResponse>` |
| `GET` | `/category/{category}` | — | `200 List<ItemResponse>` *(cached)* |
| `GET` | `/search?name=` | query param | `200 List<ItemResponse>` |
| `GET` | `/{id}/image` | — | `200 byte[]` with correct Content-Type |
| `POST` | `/` | `multipart/form-data`: `item` (JSON) + optional `image` | `201 ItemResponse` |
| `POST` | `/` | `application/json`: `ItemRequest` JSON (no image) | `201 ItemResponse` |
| `PUT` | `/{id}` | `multipart/form-data`: `item` (JSON) + optional `image` | `200 ItemResponse` |
| `PATCH` | `/{id}/toggle-availability` | — | `200 ItemResponse` |
| `DELETE` | `/{id}` | — | `204 No Content` |

**Item JSON fields (in `item` part of multipart):**
```json
{
  "name": "Power Drill",
  "description": "Cordless 18V drill",
  "ownerId": 1,
  "category": "Tools",
  "available": true
}
```

**Image upload:** Max 5 MB. Supported types: any valid MIME type (e.g., `image/jpeg`, `image/png`). Stored as `LONGBLOB` in MySQL. Served via `GET /api/items/{id}/image`.

**Validation Rules:**
- `name`: 2–100 characters, required
- `description`: max 1000 characters, optional
- `ownerId`: required (verified via Feign call to user-service on creation)
- `category`: required

---

### Error Response Format

All errors return a consistent JSON body:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Item not found with id: 99",
  "path": "/api/items/99",
  "timestamp": "2026-06-29T01:00:00",
  "validationErrors": {
    "name": "Item name is required"
  }
}
```

`validationErrors` is only present for `400 Validation Failed` responses.

---

### Communication Service `/api/chat` & WebSocket `/ws-chat`

#### WebSocket (STOMP) — Real-Time Messaging

**Connect:** `ws://localhost:8087/ws-chat?userId={yourUserId}`  
SockJS fallback also available at the same path.

**Send a message** (publish to STOMP destination):
```
Destination : /app/chat.send
Payload     : { "senderId": 1, "recipientId": 2, "itemId": 5, "content": "Is this still available?" }
```
- `itemId` is optional — omit to send a general user-to-user message
- Server overrides `senderId` with the authenticated `userId` from the handshake query param (anti-spoofing)

**Receive messages** (subscribe to user-specific queue):
```
Subscribe: /user/queue/messages
```
Both sender and recipient receive the saved `ChatMessage` object (including `id` and `timestamp`) after each send.

#### REST — Chat History

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/chat/history/{user1Id}/{user2Id}` | Full bidirectional history between two users (ASC by timestamp) |
| `GET` | `/api/chat/history/{user1Id}/{user2Id}?itemId={id}` | History filtered to a specific shared item |

**Chat Message JSON:**
```json
{
  "id": 42,
  "senderId": 1,
  "recipientId": 2,
  "itemId": 5,
  "content": "Is this still available?",
  "timestamp": "2026-06-29T01:00:00"
}
```

> **Note:** The communication service has no Swagger UI. All endpoints are fully public (no authentication enforced).

---

## 🌐 Error Response Format

All errors return a consistent JSON body:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Item not found with id: 99",
  "path": "/api/items/99",
  "timestamp": "2026-06-29T01:00:00",
  "validationErrors": {
    "name": "Item name is required"
  }
}
```

`validationErrors` is only present for `400 Validation Failed` responses.

---

## 🔒 Security

### Current Implementation

| Aspect | Implementation |
|---|---|
| Password hashing | BCrypt (`BCryptPasswordEncoder`) |
| Password in API | `@JsonProperty(WRITE_ONLY)` — never returned |
| CORS | Centralized at API Gateway via `CorsWebFilter` |
| CSRF | Disabled in user-service and communication-service |
| Allowed CORS origin | `http://localhost:5173` (gateway & chat controller) |
| Inter-service endpoints | `/api/users/{id}` and `/api/users/username/**` permitted without auth |
| WebSocket identity | `userId` query param on WS upgrade; no token validation — users can claim any ID |

### 🔒 Security Architecture

Users authenticate via `POST /api/users/login` to obtain a JWT. The API Gateway validates this token at the routing layer (`JwtAuthenticationFilter`) and forwards requests downstream with injected headers (`X-User-Id`, `X-Username`). Downstream microservices enforce authorization via these gateway-supplied headers. Note that the `.env` file's `JWT_SECRET` must be wrapped in double quotes to avoid parsing truncation from `#` characters.

### ⚠️ Known Security Gaps

1. **Hardcoded DB credentials:** `username=root`, `password=admin` in plain `application.properties`. Must be externalized for any non-local deployment.
2. **WebSocket Authentication:** The WebSocket upgrade request does not yet validate the JWT token.
3. **No rate limiting:** No rate limiting at gateway or service level.
4. **Image MIME type not validated:** Upload accepts any claimed content type; no server-side validation of actual file content.

---

## 🧩 AOP Cross-Cutting Concerns

### LoggingAspect (item-service, user-service & communication-service)

```
@Around       → service layer     : logs method name, args, execution time, exceptions
@Before       → controller layer  : logs incoming API request method name
@AfterThrowing → service layer    : logs exception class + message
```

### PerformanceAspect (item-service only)

Applied to methods annotated with `@TrackExecutionTime`:
- Logs execution duration in ms
- Emits a `WARN` log if duration exceeds **1000ms**

Usage:
```java
@TrackExecutionTime
public ItemResponse someExpensiveMethod() { ... }
```

---

## 💾 Database Schema

### `db_items.items`

```sql
CREATE TABLE items (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  description VARCHAR(1000),
  owner_id    BIGINT NOT NULL,
  category    VARCHAR(255) NOT NULL,
  available   TINYINT(1) NOT NULL DEFAULT 1,
  image_data  LONGBLOB,
  image_type  VARCHAR(255),
  image_name  VARCHAR(255),
  created_at  DATETIME NOT NULL,
  updated_at  DATETIME,
  INDEX idx_items_owner_id (owner_id),
  INDEX idx_items_category (category),
  INDEX idx_items_available (available),
  INDEX idx_items_name (name)
);
```

### `db_users.users`

```sql
CREATE TABLE users (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  username   VARCHAR(50)  NOT NULL,
  email      VARCHAR(255) NOT NULL,
  password   VARCHAR(255) NOT NULL,
  full_name  VARCHAR(255) NOT NULL,
  address    VARCHAR(255),
  phone      VARCHAR(255),
  bio        VARCHAR(500),
  created_at DATETIME NOT NULL,
  updated_at DATETIME,
  UNIQUE INDEX idx_users_username (username),
  UNIQUE INDEX idx_users_email (email)
);
```

> Schema is managed by Hibernate with `ddl-auto=update` — tables are created/altered automatically on startup.

### `db_communication.chat_messages`

```sql
CREATE TABLE chat_messages (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  sender_id    BIGINT NOT NULL,
  recipient_id BIGINT NOT NULL,
  item_id      BIGINT,
  content      TEXT NOT NULL,
  timestamp    DATETIME NOT NULL,
  INDEX idx_chat_sender_recipient (sender_id, recipient_id),
  INDEX idx_chat_item_id (item_id),
  INDEX idx_chat_timestamp (timestamp)
);
```

---

## 🧪 Running Tests

```bash
# Item Service — all tests
cd item-service
mvn test

# User Service — all tests
cd user-service
mvn test

# Run a specific test class
mvn test -Dtest=ItemServiceTest
mvn test -Dtest=UserControllerTest
```

**Test isolation:** All tests use H2 in-memory database, with Eureka, Redis, and Feign circuit breakers disabled via `src/test/resources/application.properties` and `@ActiveProfiles("test")`.

### Test Coverage Summary

| Service | Test File | Tests | Type |
|---|---|---|---|
| item-service | `ItemServiceTest` | 9 | Unit (Mockito) |
| item-service | `ItemRepositoryTest` | 8 | Integration (DataJpaTest + H2) |
| user-service | `UserServiceTest` | 12 | Unit (Mockito) |
| user-service | `UserControllerTest` | 6 | Integration (WebMvcTest + MockMvc) |
| user-service | `UserRepositoryTest` | 5 | Integration (DataJpaTest + H2) |
| communication-service | `CommunicationApplicationTests` | 1 | Smoke (context loads only) |

---

## 🔧 Configuration Reference

### Item Service (`item-service/application.properties`)

```properties
server.port=8088
spring.application.name=item-service

# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/db_items?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=admin

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# File upload limits
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB

# Eureka
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.instance.instance-id=${spring.application.name}:${random.value}

# Resilience4j Circuit Breaker (userService)
resilience4j.circuitbreaker.instances.userService.slidingWindowSize=10
resilience4j.circuitbreaker.instances.userService.failureRateThreshold=50
resilience4j.circuitbreaker.instances.userService.waitDurationInOpenState=10s
resilience4j.circuitbreaker.instances.userService.permittedNumberOfCallsInHalfOpenState=3
resilience4j.circuitbreaker.instances.userService.automaticTransitionFromOpenToHalfOpenEnabled=true

# Resilience4j Retry (userService)
resilience4j.retry.instances.userService.maxAttempts=3
resilience4j.retry.instances.userService.waitDuration=1s

# Feign
spring.cloud.openfeign.circuitbreaker.enabled=true
```

### User Service (`user-service/application.properties`)

```properties
server.port=8089
spring.application.name=user-service

# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/db_users?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=admin

# JPA
spring.jpa.hibernate.ddl-auto=update

# Eureka
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.instance.instance-id=${spring.application.name}:${random.value}
```

---

## 🗺️ Future Improvements

> Items are tagged **[High]**, **[Medium]**, or **[Low]** by priority.

### 🔐 Security

| Priority | Improvement |
|---|---|
| **[High]** | Externalize secrets — move DB passwords, Redis credentials to environment variables or Spring Cloud Config / Vault |
| **[High]** | Validate image MIME types server-side (Apache Tika or file magic bytes), prevent content-type spoofing |
| **[Medium]** | Add rate limiting at the API Gateway (Spring Cloud Gateway `RequestRateLimiter` filter with Redis) |
| **[Medium]** | Add `X-Content-Type-Options`, `X-Frame-Options`, `Content-Security-Policy` headers via Gateway filter |
| **[Low]** | Restrict internal endpoints (`/api/users/{id}`) to internal network only (IP filter or mutual TLS) |

### 🧪 Testing

| Priority | Improvement |
|---|---|
| **[High]** | Add integration tests for item-service controller (currently no `ItemControllerTest`) |
| **[High]** | Add JaCoCo code coverage reporting; enforce minimum thresholds in CI |
| **[Medium]** | Add contract tests (Spring Cloud Contract or Pact) between item-service (consumer) and user-service (provider) |
| **[Medium]** | Add end-to-end tests against a Testcontainers-based MySQL + Redis stack |
| **[Low]** | Add load/stress tests (Gatling or JMeter) for item search and image serving |

### 📊 Logging & Observability

| Priority | Improvement |
|---|---|
| **[High]** | Implement distributed tracing (Spring Cloud Sleuth + Zipkin or OpenTelemetry) — add correlation/trace IDs across Feign calls |
| **[High]** | Replace SLF4J console logging with structured JSON logging (Logstash encoder) for log aggregation |
| **[Medium]** | Expose Micrometer metrics (Prometheus endpoint) for Redis cache hit/miss ratio, circuit breaker state, HTTP latency |
| **[Medium]** | Configure Grafana + Prometheus dashboards for real-time monitoring |
| **[Low]** | Add request-scoped MDC (Mapped Diagnostic Context) with userId, requestId for all log entries |

### ⚡ Performance & Scalability

| Priority | Improvement |
|---|---|
| **[High]** | Move image storage out of MySQL (`LONGBLOB`) to object storage (MinIO or S3-compatible) — current approach degrades DB performance at scale |
| **[High]** | Add Redis TTL-based cache warming for frequently accessed categories |
| **[Medium]** | Replace synchronous Feign owner verification with an event-driven approach (Kafka/RabbitMQ) to decouple item and user services |
| **[Medium]** | Add database connection pooling configuration (HikariCP settings: pool size, timeout, keepalive) |
| **[Medium]** | Implement Spring Cache `@CachePut` for `updateItem` to avoid full cache eviction |
| **[Low]** | Enable HTTP/2 at the gateway level for multiplexed frontend connections |
| **[Low]** | Add database read replicas with Spring's `AbstractRoutingDataSource` for read-heavy item queries |

### 🧹 Code Quality

| Priority | Improvement |
|---|---|
| **[High]** | Introduce a dedicated `RegisterRequest` DTO for user creation — the `POST /register` endpoint currently accepts the raw `User` entity, exposing internal model fields |
| **[Medium]** | Standardize `LoginRequest`/`LoginResponse` to use Lombok `@Data`/`@Builder` (currently manual getters/setters) for consistency with the rest of the codebase |
| **[Medium]** | Extract `ItemService.verifyOwnerExists()` circuit breaker into a dedicated `UserVerificationService` class |
| **[Medium]** | Add OpenAPI annotations (`@Operation`, `@ApiResponse`, `@Schema`) to controllers for richer Swagger documentation |
| **[Low]** | Remove duplicate `LoggingAspect` code between item-service and user-service — extract to a shared library or common module |
| **[Low]** | Apply `@TrackExecutionTime` to slow operations (image fetching, search queries) in item-service |

### 🛠️ Developer Experience

| Priority | Improvement |
|---|---|
| **[High]** | Add Docker Compose file to spin up MySQL, Redis, and all microservices with a single command |
| **[Medium]** | Add GitHub Actions CI pipeline: build, test, code coverage, static analysis (SpotBugs/Checkstyle) |
| **[Medium]** | Add Spring Boot DevTools for hot reload during development |
| **[Medium]** | Add a `communication-service` readme and include it in the architecture documentation |
| **[Low]** | Add a Makefile with common targets: `make start`, `make test`, `make build`, `make clean` |
| **[Low]** | Add `.env.example` template for environment-specific configuration |

---

## 📝 Documentation Audit Summary

### What Was Updated (vs. Previous Docs)
- **Removed:** Claims about JWT authentication, dynamic ports for item/user services (both have fixed ports 8088/8089)
- **Removed:** "Item image uploads" from future enhancements — it is already implemented
- **Added:** Redis caching details (TTLs, cache names, eviction triggers)
- **Added:** Resilience4j circuit breaker and retry configuration
- **Added:** AOP aspects (LoggingAspect, PerformanceAspect, @TrackExecutionTime)
- **Added:** Accurate port numbers for all services
- **Added:** Communication service acknowledgment (5th service, not in scope of this audit)
- **Added:** Exact test coverage inventory
- **Added:** Security gap documentation (no JWT token issued post-login)
- **Added:** `@TrackExecutionTime` custom annotation documentation
- **Added:** Multipart image upload details and LONGBLOB storage

### Documentation Gaps Found
- No dedicated `communication-service` documentation in the project
- No API versioning strategy documented
- No runbook for common failure scenarios (Redis down, MySQL down, Eureka down)
- No documented strategy for password reset or account recovery

### Key Architectural Observations
1. **Soft foreign key:** `item.owner_id` references users by ID without a database-level foreign key — relies on application-level verification via Feign with circuit breaker fallback that permits creation even when user-service is down.
2. **Image storage in DB:** Binary images stored as `LONGBLOB` in MySQL — acceptable for a local demo but a significant concern at production scale.
3. **Gateway JWT validation:** The gateway-level `JwtAuthenticationFilter` handles stateless JWT security, transferring user identity downstream via trusted headers.
4. **Test profile isolation is clean:** H2, Eureka disabled, Redis disabled, and Feign circuit breakers all properly configured for test profiles — tests are hermetic.

---

**EcoTrack Backend — Source-Verified Documentation | June 2026 🌱**
