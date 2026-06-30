# Communication Microservice

The Communication Microservice is a Spring Boot application responsible for handling real-time messaging and chat history between users in the EcoTrack ecosystem.

## High-Level Architecture

The service leverages the following technologies and architectural patterns:

- **Spring Boot 3.2.x**: The core framework for the microservice.
- **WebSocket & STOMP**: Provides bidirectional, real-time communication. Clients connect to the `/ws-chat` endpoint (SockJS supported). An in-memory Simple Message Broker routes messages to specific user queues (`/user/{userId}/queue/messages`).
- **Redis Pub/Sub**: Acts as a message relay between service instances. After a message is persisted, it is published to the `chat:messages` Redis channel. A `RedisMessageSubscriber` listener picks it up and fans it out to connected WebSocket clients via `SimpMessagingTemplate`. This decouples message delivery from the controller and enables horizontal scaling.
- **REST API**: Exposes HTTP endpoints (e.g., `/api/chat/history/{user1Id}/{user2Id}`) to retrieve historical chat data.
- **Data Persistence**: Uses Spring Data JPA and MySQL (`db_communication`) to store chat messages (`ChatMessage` entity).
- **Service Discovery**: Integrates with Netflix Eureka Client to register itself as `communication-service`, making it discoverable by other services and the API Gateway.
- **Security**: A custom STOMP Handshake Handler (`CustomHandshakeHandler`) authenticates WebSocket connections and associates them with a `Principal` (the `userId`). If no valid `userId` is supplied, the connection is treated as anonymous — preventing `NumberFormatException` crashes.
- **Global CORS**: Allowed origins are configured centrally via `app.cors.allowed-origins` in `application.properties`, not hardcoded per controller.

### Message Flow

```
Client
  │
  │  1. WS connect: ws://localhost:8087/ws-chat?userId={id}
  ▼
CustomHandshakeHandler  ──validates userId──►  Principal established
  │
  │  2. Client subscribes to /user/{id}/queue/messages
  │  3. Client publishes JSON payload to /app/chat.send
  ▼
ChatController
  │  – verifies senderId against authenticated Principal
  │  – calls ChatMessageService.saveMessage()
  ▼
ChatMessageService
  │  – persists ChatMessage to MySQL
  │  – publishes message JSON to Redis channel "chat:messages"
  ▼
Redis (chat:messages channel)
  │
  ▼
RedisMessageSubscriber  (runs on every service instance)
  │  – delivers to /user/{recipientId}/queue/messages
  └─ – delivers to /user/{senderId}/queue/messages
```

## How to Test Locally

### Prerequisites

1. **Java 17** installed.
2. **MySQL Server** running locally.
3. **Redis** running locally on the default port (`localhost:6379`).
4. (Optional but recommended) **Eureka Server** running on port `8761`.

### Setup and Execution

1. **Database Setup**:
   Create a database named `db_communication` in your local MySQL instance:
   ```sql
   CREATE DATABASE db_communication;
   ```

2. **Environment Variables**:
   The service reads database credentials from environment variables (or a `.env` file at the project root):
   ```
   DB_USERNAME=your_mysql_user
   DB_PASSWORD=your_mysql_password
   ```

3. **Redis**:
   Ensure Redis is running. The service connects to `localhost:6379` by default. Override via:
   ```properties
   spring.data.redis.host=<host>
   spring.data.redis.port=<port>
   ```

4. **Run the Application**:
   ```bash
   ./mvnw spring-boot:run
   ```
   The service will start on port `8087`.

### Testing the Endpoints

#### 1. REST API — Chat History

Use Postman or `curl` to fetch chat history between two users:

```
GET http://localhost:8087/api/chat/history/1/2
```

To filter by a specific item:
```
GET http://localhost:8087/api/chat/history/1/2?itemId=100
```

You should receive a JSON array of `ChatMessage` objects, ordered by timestamp ascending.

---

#### 2. REST API — Health Check

```
GET http://localhost:8087/actuator/health
```

---

#### 3. WebSocket — Real-time Chat

Use Postman (v10+) or a browser STOMP client (`@stomp/stompjs` + `sockjs-client`).

> **Important**: The `userId` query parameter **must** be a valid numeric Long. Connections without it will be treated as anonymous.

**Steps (Postman STOMP):**

1. Open a new request → select **WebSocket** → change type from **Raw** to **STOMP**.
2. Connect to: `ws://localhost:8087/ws-chat?userId=1`
3. **Subscribe** to your user's queue: `/user/1/queue/messages`
4. **Send** a message to `/app/chat.send` with the payload:
   ```json
   {
       "senderId": 1,
       "recipientId": 2,
       "content": "Hello there!",
       "itemId": 100
   }
   ```
5. The message is saved to MySQL, published to Redis, and delivered back to both the sender's and recipient's queues. You should see it echoed in the subscribed queue.

**Verify Redis delivery (optional):**
```bash
redis-cli SUBSCRIBE chat:messages
```
You will see the raw JSON payload appear on the channel each time a message is sent.

---

#### 4. WebSocket — Anti-spoof Behaviour

Connect as `userId=1` but send a payload with `"senderId": 2`. The controller will override `senderId` to `1` and log a warning. This prevents client-side sender spoofing.
