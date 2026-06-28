# EcoTrack — Project Overview

## What is EcoTrack?

EcoTrack is a **local community sharing platform** that allows neighbors to share and lend tools, equipment, and other items. The backend is a Spring Boot microservices system demonstrating service discovery, API routing, inter-service communication with resilience patterns, Redis caching, AOP-based observability, image management, and real-time WebSocket messaging — all designed to run on a single local machine without cloud dependencies.

---

## Technology Stack

### Backend (Java / Spring Boot)

| Technology | Version | Purpose |
|---|---|---|
| Java | 17 | Runtime |
| Spring Boot | 3.2.0 | Application framework |
| Spring Cloud Gateway | 2023.0.0 (BOM) | Reactive API gateway |
| Netflix Eureka Server/Client | 2023.0.0 (BOM) | Service registry & discovery |
| Spring Data JPA | 3.2.0 | ORM / database access |
| MySQL Connector/J | (managed) | Production database driver |
| H2 Database | (test scope) | In-memory DB for unit/integration tests |
| Spring Security | 3.2.0 | Password hashing (BCrypt), endpoint protection |
| Spring Cache + Redis | 3.2.0 | Distributed response caching |
| Spring OpenFeign | 2023.0.0 (BOM) | Declarative HTTP client for inter-service calls |
| Resilience4j | 2023.0.0 (BOM) | Circuit breaker + retry for Feign calls |
| Spring WebSocket (STOMP) | 3.2.0 | Real-time bidirectional messaging |
| Spring AOP | 3.2.0 | Cross-cutting logging & performance aspects |
| Springdoc OpenAPI | 2.3.0 | Swagger UI / API documentation (item-service, user-service) |
| Lombok | 1.18.42 | Boilerplate reduction |
| Maven | 3.8+ | Build tool |
| JUnit 5 + Mockito | (managed) | Unit & integration testing |

### Frontend (React)
- **React 18** with Vite build tooling
- **Tailwind CSS** for responsive styling
- **Axios** for API communication
- **React Router** for navigation

---

## Architecture

The platform consists of **5 microservices**:

```
React Frontend (Port 5173)
        │ HTTP/REST + WebSocket (all traffic through gateway)
        ▼
┌────────────────────────────────────────────┐
│         API Gateway  (Port 8080)           │
│         Spring Cloud Gateway               │
│  • CorsConfig (reactive CorsWebFilter)     │
│  • Routes: /api/items/**                   │
│            /api/users/**                   │
│            /api/chat/**                    │
│            /ws-chat/** (WebSocket)         │
│  • Load balancing (lb://)                  │
│  • Eureka-based dynamic discovery          │
└──────────┬─────────────────────────────────┘
           │ Eureka-resolved routing
   ┌───────┼──────────┬──────────────────────┐
   ▼       ▼          ▼                      ▼
┌──────┐ ┌─────────┐ ┌──────────┐  ┌─────────────────────┐
│Disc. │ │  Item   │ │  User    │  │  Communication      │
│Server│ │Service  │ │ Service  │  │  Service            │
│:8761 │ │ :8088   │ │ :8089    │  │  :8087              │
│      │ │         │ │          │  │                     │
│Eureka│ │• CRUD   │ │• Register│  │• WebSocket (STOMP)  │
│Server│ │• Search │ │• Login   │  │• Real-time chat     │
│(no   │ │• Paging │ │• Profile │  │• Chat history (REST)│
│self- │ │• Images │ │• BCrypt  │  │• Item-context msgs  │
│reg.) │ │• Redis  │ │• Redis   │  │• AOP logging        │
│      │ │• Feign→ │ │• AOP log │  │                     │
│      │ │ user-svc│ │          │  │                     │
└──────┘ └────┬────┘ └────┬─────┘  └──────────┬──────────┘
              │           │                    │
              ▼           ▼                    ▼
         ┌────────┐ ┌──────────┐    ┌──────────────────┐
         │db_items│ │ db_users │    │ db_communication │
         │ MySQL  │ │  MySQL   │    │     MySQL        │
         └────────┘ └──────────┘    └──────────────────┘
              ↑           ↑
           Redis :6379 (shared cache)
        (item-service + user-service)
```

---

## Service Details

### 1. Discovery Server — Port 8761

**Package:** `com.ecotrack.discovery`

- Netflix Eureka Server (`@EnableEurekaServer`)
- Self-preservation mode disabled (`enable-self-preservation=false`)
- Does **not** register itself with Eureka (`register-with-eureka=false`)
- Exposes `/health` and `/info` Actuator endpoints
- CORS configured for frontend at `http://localhost:5173`

---

### 2. API Gateway — Port 8080

**Package:** `com.ecotrack.gateway`

- Spring Cloud Gateway (reactive, WebFlux-based)
- Registered as Eureka client; fetches registry to resolve `lb://` URIs
- **Configured Routes:**

| Route ID | URI | Path Predicate | Strip Prefix |
|---|---|---|---|
| `item-service` | `lb://item-service` | `/api/items/**` | 0 |
| `user-service` | `lb://user-service` | `/api/users/**` | 0 |
| `communication-service` | `lb://communication-service` | `/api/chat/**`, `/ws-chat/**` | 0 |

- **Dynamic discovery** also enabled (`spring.cloud.gateway.discovery.locator.enabled=true`, `lower-case-service-id=true`)
- **WebSocket** support enabled (`spring.cloud.gateway.websocket.enabled=true`)
- **CORS** via `CorsConfig` bean (`CorsWebFilter`): origin `http://localhost:5173`, methods GET/POST/PUT/DELETE/PATCH/OPTIONS, all headers, credentials allowed, 1-hour max-age
- Actuator endpoints: `gateway`, `health`, `info`

---

### 3. Item Service — Port 8088

**Package:** `com.ecotrack.item`

#### Data Model (`items` table)

| Field | Type | Constraints |
|---|---|---|
| `id` | `BIGINT` | PK, auto-increment |
| `name` | `VARCHAR(100)` | NOT NULL, 2–100 chars |
| `description` | `VARCHAR(1000)` | optional, max 1000 chars |
| `owner_id` | `BIGINT` | NOT NULL (FK to user-service, soft reference) |
| `category` | `VARCHAR` | NOT NULL |
| `available` | `BOOLEAN` | NOT NULL, default `true` |
| `image_data` | `LONGBLOB` | Binary image storage, excluded from JSON |
| `image_type` | `VARCHAR` | MIME type of uploaded image |
| `image_name` | `VARCHAR` | Original filename |
| `created_at` | `DATETIME` | Set on `@PrePersist`, not updatable |
| `updated_at` | `DATETIME` | Updated on `@PreUpdate` |

**Database indexes:** `idx_items_owner_id`, `idx_items_category`, `idx_items_available`, `idx_items_name`

#### REST API

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/items` | Get all items |
| `GET` | `/api/items/{id}` | Get item by ID |
| `GET` | `/api/items/owner/{ownerId}` | Get all items by owner |
| `GET` | `/api/items/available` | Get all available items (Redis cached, TTL 2 min) |
| `GET` | `/api/items/available/page` | Paginated available items (params: `page`, `size`, `sortBy`, `direction`) |
| `GET` | `/api/items/category/{category}` | Get items by category (Redis cached, TTL 5 min) |
| `GET` | `/api/items/search?name=` | Case-insensitive name search |
| `GET` | `/api/items/{id}/image` | Serve item image as raw bytes with correct Content-Type |
| `POST` | `/api/items` | Create item (multipart/form-data: `item` part + optional `image` part) |
| `PUT` | `/api/items/{id}` | Update item (multipart/form-data; ownership NOT transferable) |
| `PATCH` | `/api/items/{id}/toggle-availability` | Toggle `available` flag |
| `DELETE` | `/api/items/{id}` | Delete item |

**File upload limits:** 5 MB max per file and per request.

#### Key Implementation Details

- **Multipart uploads:** Item create/update accepts `multipart/form-data` with a JSON `item` part and an optional `image` part; image stored as `LONGBLOB` in MySQL.
- **Ownership immutability:** `ItemMapper.updateEntity()` intentionally does NOT update `ownerId` — ownership transfer is unsupported.
- **Caching (Redis):**
  - `availableItems` → TTL 2 minutes
  - `itemsByCategory` → TTL 5 minutes
  - `items` (general) → TTL 5 minutes (default 10 min)
  - Cache evicted on create, update, delete, and toggle operations
- **Inter-service call:** Feign client `UserServiceClient` calls `GET /api/users/{id}` on `user-service` to verify owner existence before item creation.
- **Circuit Breaker (Resilience4j):** `userService` instance — sliding window 10, failure threshold 50%, open-state wait 10s, half-open 3 calls, auto transition enabled.
- **Retry (Resilience4j):** `userService` — max 3 attempts, 1s wait.
- **Fallback:** `UserServiceClientFallback` returns a stub `UserDto` when user-service is unavailable; `verifyOwnerFallback` also logs a warning but allows item creation to proceed.
- **AOP — `LoggingAspect`:** `@Around` on all service methods (logs entry, args, duration, exceptions); `@Before` on all controller methods (logs API request name).
- **AOP — `PerformanceAspect`:** `@Around` on methods annotated with `@TrackExecutionTime`; logs duration, emits `WARN` if > 1000 ms.
- **Swagger UI:** Available at `http://localhost:8088/swagger-ui.html` via Springdoc OpenAPI 2.3.0.

---

### 4. User Service — Port 8089

**Package:** `com.ecotrack.user`

#### Data Model (`users` table)

| Field | Type | Constraints |
|---|---|---|
| `id` | `BIGINT` | PK, auto-increment |
| `username` | `VARCHAR(50)` | NOT NULL, UNIQUE, 3–50 chars |
| `email` | `VARCHAR` | NOT NULL, UNIQUE, valid email |
| `password` | `VARCHAR` | NOT NULL, min 8 chars, stored as BCrypt hash, write-only in JSON |
| `full_name` | `VARCHAR` | NOT NULL |
| `address` | `VARCHAR` | optional |
| `phone` | `VARCHAR` | optional, regex: `^$|^[+]?[0-9]{10,15}$` |
| `bio` | `VARCHAR(500)` | optional, max 500 chars |
| `created_at` | `DATETIME` | Set on `@PrePersist`, not updatable |
| `updated_at` | `DATETIME` | Updated on `@PreUpdate` |

**Database indexes:** `idx_users_username` (unique), `idx_users_email` (unique)

#### REST API

| Method | Path | Auth Required | Description |
|---|---|---|---|
| `GET` | `/api/users` | Yes | Get all users |
| `GET` | `/api/users/{id}` | No (permitted for inter-service) | Get user by ID |
| `GET` | `/api/users/username/{username}` | No (permitted for inter-service) | Get user by username |
| `POST` | `/api/users/register` | No | Register new user |
| `POST` | `/api/users/login` | No | Login (returns user info + "Login successful" message) |
| `PUT` | `/api/users/{id}` | Yes | Update user profile |
| `DELETE` | `/api/users/{id}` | Yes | Delete user |

> **Note:** Login does **not** issue a JWT or session token. The `LoginResponse` returns `{userId, username, email, fullName, message}` only. There is no stateless token issued.

#### Key Implementation Details

- **Password security:** BCrypt via `PasswordEncoderConfig` (`BCryptPasswordEncoder`). Password field is `@JsonProperty(access = WRITE_ONLY)` — never included in responses.
- **Duplicate detection:** `existsByUsername` and `existsByEmail` checked before creation; `existsByEmail` re-checked on update if email changed.
- **Caching (Redis):** `users` cache keyed by `id`, TTL 10 minutes. Cache evicted on update and delete.
- **Security config (`SecurityConfig`):** CSRF disabled, HTTP Basic disabled. Public endpoints: Swagger UI, Actuator health/info, `/api/users/register`, `/api/users/login`, `/api/users/{id}`, `/api/users/username/**`. All other endpoints require authentication (but there is no token-based auth mechanism implemented).
- **AOP — `LoggingAspect`:** Identical pattern to item-service — `@Around` service layer, `@Before` controller layer.
- **Swagger UI:** Available at `http://localhost:8089/swagger-ui.html`.
- **`UserResponse` DTO:** Excludes password; uses `@JsonInclude(NON_NULL)` to omit null optional fields.

---

---

### 5. Communication Service — Port 8087

**Package:** `com.ecotrack.communication`

- Eureka client (`@EnableDiscoveryClient`); registered as `communication-service`
- Provides **real-time bidirectional chat** between users over STOMP/WebSocket
- Persists all messages to MySQL for history retrieval
- CSRF disabled; all endpoints publicly accessible (no auth enforced)

#### Data Model (`chat_messages` table)

| Field | Type | Constraints |
|---|---|---|
| `id` | `BIGINT` | PK, auto-increment |
| `sender_id` | `BIGINT` | NOT NULL |
| `recipient_id` | `BIGINT` | NOT NULL |
| `item_id` | `BIGINT` | optional — links chat to a specific shared item |
| `content` | `TEXT` | NOT NULL |
| `timestamp` | `DATETIME` | NOT NULL, set via `@PrePersist` if null |

**Database indexes:** `idx_chat_sender_recipient` (sender_id, recipient_id), `idx_chat_item_id`, `idx_chat_timestamp`

#### WebSocket Configuration

| Config | Value |
|---|---|
| STOMP endpoint | `/ws-chat` |
| SockJS fallback | ✅ enabled (second `addEndpoint` without SockJS for native WS) |
| Allowed origins | `*` (all) |
| App destination prefix | `/app` |
| User destination prefix | `/user` |
| Broker destinations | `/user`, `/topic`, `/queue` |
| In-memory broker | Simple broker (no external message broker) |

#### User Identity (CustomHandshakeHandler)

UserId is extracted from the WebSocket upgrade request query string:
```
ws://localhost:8087/ws-chat?userId=42
```
- If `userId` is present in the query string, it becomes the STOMP `Principal` name
- If absent, a random UUID is assigned (anonymous connection)

#### Message Flow

```
Client A ──STOMP /app/chat.send──▶ ChatController.sendMessage()
                                         │ saves to db_communication
                                         ├──▶ /user/{recipientId}/queue/messages  (Recipient's queue)
                                         └──▶ /user/{senderId}/queue/messages     (Sender's echo)
```

- **Sender spoofing protection:** If the authenticated `Principal.getName()` differs from `chatMessage.getSenderId()`, the server overrides the senderId with the authenticated identity and logs a warning.

#### REST API

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/chat/history/{user1Id}/{user2Id}` | Fetch full chat history between two users, ordered by timestamp ASC |
| `GET` | `/api/chat/history/{user1Id}/{user2Id}?itemId={id}` | Fetch chat history scoped to a specific item |

History queries are bidirectional — returns messages where either user is sender or recipient.

#### Key Implementation Details

- **No Redis caching** — chat history is always fetched live from MySQL
- **No Swagger/OpenAPI** — not included in this service's dependencies
- **CORS:** `@CrossOrigin(origins = "http://localhost:5173")` on `ChatController`; WebSocket endpoint allows `*`
- **Security:** All endpoints (`/api/chat/**`, `/ws-chat/**`, `/actuator/**`) are fully public — `anyRequest().permitAll()`
- **AOP — `LoggingAspect`:** Same pattern as item/user services — `@Around` service layer, `@Before` controller layer
- **Global Exception Handling:** `GlobalExceptionHandler` covers `ResourceNotFoundException` (404) and generic `Exception` (500)
- **Database:** `db_communication` (MySQL), auto-created via `createDatabaseIfNotExist=true`; DDL mode `update`

---

## Inter-Service Communication

```
item-service  ──(Feign: GET /api/users/{id})──▶  user-service
               ──(Eureka discovery: lb://user-service)
               ──(Resilience4j: circuit breaker + retry)
               ──(Fallback: allow creation with stub user if unavailable)
```

- **Protocol:** Synchronous HTTP via Spring Cloud OpenFeign
- **Discovery:** Eureka service registry (no hardcoded URLs)
- **Resilience:** Resilience4j circuit breaker (`userService` instance) + retry configured in `item-service/application.properties`
- **Feign circuit breaker integration:** `spring.cloud.openfeign.circuitbreaker.enabled=true`

---

## Redis Caching

Both item-service and user-service connect to Redis on `localhost:6379`.

| Service | Cache Name | TTL | Eviction Triggers |
|---|---|---|---|
| item-service | `availableItems` | 2 min | create, update, delete, toggle |
| item-service | `itemsByCategory` | 5 min | create, update, delete |
| item-service | `items` | 5 min | (general config) |
| user-service | `users` | 10 min | update, delete |
| communication-service | *(none)* | — | No caching — history always fetched live |

Serialization: `GenericJackson2JsonRedisSerializer` (JSON format). Null values are not cached.

---

## Security

| Layer | Mechanism | Details |
|---|---|---|
| Password storage | BCrypt | `BCryptPasswordEncoder` in user-service |
| API authentication | Spring Security | All non-public user endpoints require authentication; **no JWT or session issued** |
| CORS | CorsWebFilter (gateway) | Allows `http://localhost:5173`, all standard methods/headers, credentials |
| CSRF | Disabled | In user-service and communication-service `SecurityConfig` |
| Sensitive fields | `@JsonProperty(WRITE_ONLY)` | Password excluded from all responses |
| Inter-service | Open (permitted) | `/api/users/{id}` and `/api/users/username/**` are publicly accessible |
| WebSocket auth | Query-param userId | `CustomHandshakeHandler` extracts `userId` from WS upgrade URL; no token validation |

> ⚠️ **Security Gap:** There is no JWT/session token issued after login. After a successful `POST /api/users/login`, the client receives user info but no bearer token. Subsequent calls to protected endpoints (e.g., `GET /api/users` or `DELETE /api/users/{id}`) would require Spring Security credentials that are never provided. In the current configuration, these endpoints effectively block all external callers without a valid credential mechanism.

---

## Testing

| Service | Test Type | File | Tests |
|---|---|---|---|
| item-service | Unit (Mockito) | `ItemServiceTest` | getAllItems, getById, create (with/without image, IO error), delete, toggle, search — 9 tests |
| item-service | Integration (DataJpaTest + H2) | `ItemRepositoryTest` | findByOwnerId, findByAvailable, findByCategory, search (case-insensitive, partial), pagination, timestamps — 8 tests |
| user-service | Unit (Mockito) | `UserServiceTest` | getAllUsers, getById, createUser (success, duplicate username/email), updateUser (success, not found, re-hash password), deleteUser, login (success, bad username, bad password) — 12 tests |
| user-service | Integration (WebMvcTest + MockMvc) | `UserControllerTest` | GET all, GET by id (found/404), POST register (success/409), POST login (success/401) — 6 tests |
| user-service | Integration (DataJpaTest + H2) | `UserRepositoryTest` | findByUsername, findByEmail, existsByUsername, existsByEmail, save-retrieve with full fields — 5 tests |
| communication-service | Smoke test only | `CommunicationApplicationTests` | Spring context loads — 1 test (no domain-logic tests) |

**Test infrastructure:** H2 in-memory database (`application.properties` under `src/test/resources`) with Eureka, Redis, and Feign circuit breaker all disabled for test profiles (`@ActiveProfiles("test")`).

---

## Project Structure

```
EcoTrack/
├── discovery-server/        # Netflix Eureka Server (Port 8761)
├── api-gateway/             # Spring Cloud Gateway (Port 8080)
├── item-service/            # Item management (Port 8088)
│   └── src/
│       ├── main/java/com/ecotrack/item/
│       │   ├── aspect/      # LoggingAspect, PerformanceAspect, @TrackExecutionTime
│       │   ├── client/      # UserServiceClient (Feign), UserServiceClientFallback, UserDto
│       │   ├── config/      # RedisConfig (@EnableCaching)
│       │   ├── controller/  # ItemController
│       │   ├── dto/         # ItemRequest, ItemResponse, ItemMapper
│       │   ├── exception/   # GlobalExceptionHandler, custom exceptions, ErrorResponse
│       │   ├── model/       # Item (@Entity)
│       │   ├── repository/  # ItemRepository (JpaRepository)
│       │   └── service/     # ItemService
│       └── test/            # ItemServiceTest, ItemRepositoryTest
├── user-service/            # User management (Port 8089)
│   └── src/
│       ├── main/java/com/ecotrack/user/
│       │   ├── aspect/      # LoggingAspect
│       │   ├── config/      # SecurityConfig, PasswordEncoderConfig, RedisConfig
│       │   ├── controller/  # UserController
│       │   ├── dto/         # LoginRequest, LoginResponse, UserResponse
│       │   ├── exception/   # GlobalExceptionHandler, custom exceptions, ErrorResponse
│       │   ├── model/       # User (@Entity)
│       │   ├── repository/  # UserRepository (JpaRepository)
│       │   └── service/     # UserService
│       └── test/            # UserServiceTest, UserControllerTest, UserRepositoryTest
├── communication/           # Real-time chat service (Port 8087)
│   └── src/
│       └── main/java/com/ecotrack/communication/
│           ├── aspect/      # LoggingAspect
│           ├── config/      # WebSocketConfig (STOMP), SecurityConfig, CustomHandshakeHandler
│           ├── controller/  # ChatController (WebSocket + REST)
│           ├── exception/   # GlobalExceptionHandler, ResourceNotFoundException, ErrorResponse
│           ├── model/       # ChatMessage (@Entity)
│           ├── repository/  # ChatMessageRepository (custom JPQL queries)
│           └── service/     # ChatMessageService
├── frontend/                # React/Vite frontend
├── database/                # DB scripts/config
├── logs/                    # Per-service log files
├── start-all-services.ps1   # PowerShell script to start all services sequentially
└── check-services.ps1       # Service health check script
```

---

## Infrastructure & Configuration

| Concern | Value |
|---|---|
| MySQL URL (items) | `jdbc:mysql://localhost:3306/db_items?createDatabaseIfNotExist=true` |
| MySQL URL (users) | `jdbc:mysql://localhost:3306/db_users?createDatabaseIfNotExist=true` |
| MySQL URL (chat) | `jdbc:mysql://localhost:3306/db_communication?createDatabaseIfNotExist=true` |
| MySQL credentials | `root` / `admin` (default; change for your environment) |
| JPA DDL mode | `update` (schema auto-updated on start) |
| Redis host/port | `localhost:6379` (item-service and user-service only) |
| Eureka URL | `http://localhost:8761/eureka/` |
| Frontend origin | `http://localhost:5173` |
| WebSocket endpoint | `ws://localhost:8087/ws-chat?userId={id}` |

---

## How to Start

### Prerequisites
- JDK 17+
- Maven 3.8+
- MySQL 8.0+ (running; `db_items`, `db_users`, `db_communication` created or auto-created on startup)
- Redis (running on default port 6379)
- Node.js 18+ (for frontend)

### Automated (Windows)
```powershell
# From the project root:
.\start-all-services.ps1
```
This script opens a new PowerShell window for each service in startup order: Discovery Server (wait 30s) → API Gateway (wait 20s) → Item Service (wait 15s) → User Service (wait 15s) → Communication Service (wait 15s) → Frontend.

### Manual Order
```bash
# 1. Discovery Server
cd discovery-server && mvn spring-boot:run

# 2. API Gateway (after Eureka is up)
cd api-gateway && mvn spring-boot:run

# 3. Item Service
cd item-service && mvn spring-boot:run

# 4. User Service
cd user-service && mvn spring-boot:run

# 5. Communication Service
cd communication && mvn spring-boot:run
```

### Service URLs
| Service | URL |
|---|---|
| Eureka Dashboard | http://localhost:8761 |
| API Gateway | http://localhost:8080 |
| Item Service Swagger | http://localhost:8088/swagger-ui.html |
| User Service Swagger | http://localhost:8089/swagger-ui.html |
| Communication Service | http://localhost:8087 |
| WebSocket Endpoint | ws://localhost:8087/ws-chat?userId={id} |
| React Frontend | http://localhost:5173 |

---

## Future Improvements

See the full prioritized list in the [Future Improvements section of README.md](README.md#future-improvements).

---

**EcoTrack Backend — Audited against source code on 2026-06-29 🌱**
