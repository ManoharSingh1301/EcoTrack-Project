# 🌱 EcoTrack — Local Community Sharing Platform

A simple, full-stack app where neighbours share and lend items. Built as a clean
**React → Spring Boot → MySQL** stack, designed to run entirely on a local
machine — no Docker, Redis, service registry, or cloud services.

> **Looking to run it?** Follow **[LOCAL_SETUP_GUIDE.md](LOCAL_SETUP_GUIDE.md)** —
> it covers prerequisites, database, backend, frontend, testing, and
> troubleshooting step by step.

---

## Architecture

```
React (Vite, :5173)  ──HTTP/REST + WebSocket──►  Spring Boot backend (:8080)  ──►  MySQL "ecotrack" (:3306)
```

A single Spring Boot application serves every domain:

- **users** — registration, login (JWT + BCrypt), profile CRUD
- **items** — CRUD, image upload/serve, categories, search, availability
- **borrow** — request → accept/reject → return, with automatic late-fee calc
- **favorites** — per-user wishlist
- **chat** — real-time messaging over STOMP/WebSocket, history persisted to MySQL

Authentication is enforced in-process by a servlet JWT filter
(`config/JwtAuthenticationFilter`) that validates the bearer token and injects a
trusted `X-User-Id` header for controllers.

## Tech stack

| Layer    | Technology |
|----------|------------|
| Frontend | React 18, Vite 5, Tailwind CSS 3, React Router 6, Axios, STOMP/SockJS |
| Backend  | Java 17, Spring Boot 3.2 (Web, Data JPA, Security, Validation, WebSocket, AOP, Actuator), JJWT |
| Database | MySQL 8 (single schema) |

## Quick start

```bash
# 1) Database
mysql -u root -p < database/setup.sql

# 2) Backend  (terminal 1)
cd backend
cp .env.example .env        # set DB_PASSWORD and a real JWT_SECRET
mvn spring-boot:run         # → http://localhost:8080

# 3) Frontend (terminal 2)
cd frontend
npm install
npm run dev                 # → http://localhost:5173
```

Full details, environment variables, and testing steps are in
**[LOCAL_SETUP_GUIDE.md](LOCAL_SETUP_GUIDE.md)**.

## Repository layout

```
backend/    Single Spring Boot application (com.ecotrack.*)
frontend/   React + Vite app
database/   setup.sql + notes for the single `ecotrack` schema
```

## API surface (all under http://localhost:8080)

| Area     | Endpoints |
|----------|-----------|
| Users    | `POST /api/users/register`, `POST /api/users/login`, `GET/PUT/DELETE /api/users/**` |
| Items    | `GET/POST/PUT/PATCH/DELETE /api/items/**`, `GET /api/items/{id}/image` |
| Borrow   | `POST /api/borrow-requests`, `GET .../incoming|outgoing`, `PATCH .../{id}/accept|reject|cancel|return` |
| Favorites| `GET /api/favorites`, `GET /api/favorites/ids`, `POST/DELETE /api/favorites/{itemId}` |
| Chat     | `GET /api/chat/history/{u1}/{u2}`, WebSocket `ws://localhost:8080/ws-chat` |

`register`, `login`, chat history, the WebSocket handshake, and `/actuator/**` are
public; everything else requires `Authorization: Bearer <token>`.
