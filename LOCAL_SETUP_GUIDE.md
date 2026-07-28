# EcoTrack — Local Setup Guide

This guide takes a developer from a **fresh machine** to a fully running EcoTrack
app. Follow it top to bottom.

---

## Project Overview

**Detected stack**

| Layer     | Technology                                                            |
|-----------|-----------------------------------------------------------------------|
| Frontend  | React 18, Vite 5, Tailwind CSS 3, React Router 6, Axios, STOMP/SockJS |
| Backend   | Java 17, Spring Boot 3.2 (Web, Data JPA, Security, Validation, WebSocket, AOP, Actuator) |
| Auth      | JWT (JJWT 0.12) + BCrypt password hashing                             |
| Realtime  | Spring STOMP over WebSocket, in-memory broker (no Redis)              |
| Database  | MySQL 8 (single schema `ecotrack`)                                    |
| Build     | Maven (backend), npm + Vite (frontend)                                |

**Architecture** (simple, single backend):

```
React (Vite dev server, :5173)
        │  HTTP/REST + WebSocket
        ▼
Spring Boot backend (:8080)   ──►   MySQL database "ecotrack" (:3306)
  users · items · borrow · favorites · chat
```

The backend is a single Spring Boot application. There is **no** service registry,
API gateway, Redis, Docker, or cloud dependency.

---

## Required Software

Install these on the machine that will run the app:

| Tool     | Version           | Notes                                       |
|----------|-------------------|---------------------------------------------|
| Git      | any recent        | to clone the repo                           |
| JDK      | **17** (17.x)     | `java -version` must report 17              |
| Maven    | **3.8+**          | `mvn -v`. (Or use an IDE's bundled Maven.)  |
| Node.js  | **18 LTS or 20**  | `node -v`                                   |
| npm      | 9+ (ships w/ Node)| `npm -v`                                     |
| MySQL    | **8.0+**          | server running locally on port 3306         |

> This repository was prepared on a machine that cannot install packages or run
> builds. Run the `mvn`/`npm install` commands below on your own machine.

---

## Project Structure

```
EcoTrack-Project/
├── backend/                     # Single Spring Boot application
│   ├── pom.xml
│   ├── .env.example             # copy to .env and edit
│   └── src/main/
│       ├── java/com/ecotrack/
│       │   ├── EcoTrackApplication.java     # entry point
│       │   ├── config/          # security, JWT filter, CORS, cache, WebSocket
│       │   ├── common/exception/# shared exceptions + global handler
│       │   ├── aspect/          # logging & performance AOP
│       │   ├── user/            # accounts + auth (model/repo/service/controller/dto)
│       │   ├── item/            # items, images, borrow/, favorite/
│       │   └── communication/   # chat (REST history + WebSocket)
│       └── resources/
│           ├── application.properties
│           └── logback-spring.xml
├── frontend/                    # React + Vite app
│   ├── package.json
│   ├── .env.example
│   └── src/ (pages, components, contexts, api)
├── database/
│   ├── setup.sql                # creates the single `ecotrack` DB
│   └── README.md
├── LOCAL_SETUP_GUIDE.md         # this file
└── README.md
```

---

## Environment Variables

### Backend (`backend/.env`, copied from `backend/.env.example`)

| Variable               | Sample value                          | Meaning                                        |
|------------------------|---------------------------------------|------------------------------------------------|
| `DB_HOST`              | `localhost`                           | MySQL host                                     |
| `DB_PORT`              | `3306`                                | MySQL port                                     |
| `DB_NAME`              | `ecotrack`                            | Database name                                  |
| `DB_USERNAME`          | `root`                                | MySQL user                                     |
| `DB_PASSWORD`          | `your_mysql_password`                 | MySQL password                                 |
| `JWT_SECRET`           | a random string **≥ 32 chars**        | HS256 signing key — change this                |
| `JWT_EXPIRATION_MS`    | `86400000`                            | Token lifetime (ms), default 24h               |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173`               | Comma-separated allowed frontend origins       |

> Every backend variable has a safe local default in `application.properties`, so
> the app will start even without a `.env`. You **must** set `DB_PASSWORD` if your
> MySQL root user has a password, and you **should** set a real `JWT_SECRET`.

### Frontend (`frontend/.env`, optional)

| Variable       | Sample value            | Meaning                              |
|----------------|-------------------------|--------------------------------------|
| `VITE_API_URL` | `http://localhost:8080` | Backend base URL (defaults to this)  |

---

## Database Setup

1. **Install & start MySQL 8** (via your OS package manager or the official
   installer). Ensure it listens on `localhost:3306`.

2. **Create the database** (Hibernate creates the tables/indexes automatically on
   first backend startup, so this one step is enough):

   ```bash
   mysql -u root -p < database/setup.sql
   ```

   This creates a database named `ecotrack`.

3. *(Optional)* Create a dedicated MySQL user instead of `root` — see the
   commented block in `database/setup.sql`, then set `DB_USERNAME`/`DB_PASSWORD`
   in `backend/.env` accordingly.

---

## Backend Setup

```bash
# 1. Open the backend folder
cd backend

# 2. Configure environment (once)
cp .env.example .env
#    then edit .env — set DB_PASSWORD and a real JWT_SECRET

# 3. Run the Spring Boot app (installs dependencies on first run)
mvn spring-boot:run
```

The backend starts on **http://localhost:8080**.

- Health check: <http://localhost:8080/actuator/health> → `{"status":"UP"}`
- API docs (Swagger UI): <http://localhost:8080/swagger-ui.html>

To build a runnable jar instead:

```bash
mvn clean package
java -jar target/ecotrack-backend-1.0.0.jar
```

---

## Frontend Setup

```bash
# 1. Open the frontend folder (in a second terminal)
cd frontend

# 2. Install dependencies (first time only)
npm install

# 3. Start the Vite dev server
npm run dev
```

The app opens at **http://localhost:5173**.

---

## First-Time Startup Guide

Start things in this order:

1. **MySQL** — make sure the server is running and the `ecotrack` DB exists.
2. **Backend** — `cd backend && mvn spring-boot:run`. Wait for
   `Started EcoTrackApplication`.
3. **Frontend** — `cd frontend && npm run dev`. The browser opens
   `http://localhost:5173`.
4. In the app, click **Register**, create an account, and you are logged in.

The floating status dot (bottom-right) turns **green** when the frontend can reach
the backend.

---

## Functional Testing

Verify the full stack end-to-end:

| Feature              | How to test                                                                 | Expected                                                       |
|----------------------|------------------------------------------------------------------------------|----------------------------------------------------------------|
| **Registration**     | `/register`, fill the form, submit                                          | Auto-login, redirect to Dashboard                              |
| **Login**            | Log out, then `/login` with the same credentials                            | Redirect to Dashboard; token stored                            |
| **Dashboard**        | Open `/dashboard`                                                            | Shows your items and borrow stats                              |
| **Create item (CRUD)** | `/my-items` → add an item (optionally an image)                           | Item appears; row persists in `items` table                    |
| **Browse / search**  | `/items`, search & filter by category                                       | Matching items render                                          |
| **Favorites**        | Heart an item on `/items`, open `/favorites`                                | Item listed; row in `favorites` table                          |
| **Borrow workflow**  | As user B request user A's item; as A accept; either party marks returned   | Status flows PENDING→ACCEPTED→RETURNED; late fee computed       |
| **Chat**             | Open `/chat` in two browsers (two accounts) and message                     | Messages appear in real time; rows in `chat_messages`          |
| **API directly**     | `curl http://localhost:8080/actuator/health`                                | `{"status":"UP"}`                                              |
| **DB persistence**   | `mysql> USE ecotrack; SELECT * FROM users;`                                 | Your account is present                                        |

Quick API smoke test:

```bash
# Register
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"password123","fullName":"Alice"}'

# Login (copy the "token" from the response)
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}'

# Authenticated call
curl http://localhost:8080/api/items -H "Authorization: Bearer <TOKEN>"
```

---

## Common Errors

| Symptom | Cause | Fix |
|---------|-------|-----|
| Backend exits: `Access denied for user 'root'@'localhost'` | Wrong DB credentials | Set correct `DB_PASSWORD` (and `DB_USERNAME`) in `backend/.env` |
| Backend exits: `Communications link failure` | MySQL not running / wrong host/port | Start MySQL; check `DB_HOST`/`DB_PORT` |
| Backend exits: `Unknown database 'ecotrack'` | DB not created | Run `mysql -u root -p < database/setup.sql` (or keep `createDatabaseIfNotExist=true`, which is the default) |
| Startup fails mentioning the JWT key is too short | `JWT_SECRET` under 32 chars | Use a longer secret (≥ 32 characters) |
| Frontend shows red status dot / network errors | Backend not running or wrong URL | Start backend; check `VITE_API_URL` |
| `401 Unauthorized` after some time | JWT expired | Log in again (raise `JWT_EXPIRATION_MS` if needed) |
| CORS error in browser console | Frontend origin not allowed | Add the origin to `CORS_ALLOWED_ORIGINS` in `backend/.env` |
| `Port 8080 already in use` | Another process on 8080 | Stop it, or set `server.port` in `application.properties` and `VITE_API_URL` to match |
| `mvn: command not found` | Maven not installed / not on PATH | Install Maven 3.8+ or run from an IDE |

---

## Troubleshooting Guide

1. **Confirm prerequisites:** `java -version` (17), `mvn -v`, `node -v` (18/20),
   and that MySQL is running.
2. **Backend won't start:** read the first stack trace in the console — it is
   almost always the datasource (credentials/host/db). Verify `backend/.env`.
3. **Frontend can't reach backend:** open <http://localhost:8080/actuator/health>
   directly. If that fails, the problem is the backend, not the frontend.
4. **Auth issues:** clear the browser's `localStorage` (`user` key) and log in
   again; a stale/expired token forces a redirect to `/login`.
5. **Chat not connecting:** ensure the backend is up (WebSocket endpoint is
   `ws://localhost:8080/ws-chat`) and that you are logged in.
6. **Reset the database:** `DROP DATABASE ecotrack; CREATE DATABASE ecotrack;`
   then restart the backend — tables are recreated automatically.

---

## Maintenance Guide

- **Add a REST endpoint:** add a method to the relevant controller under
  `com.ecotrack.<domain>.controller` (or `item.borrow` / `item.favorite`), plus
  service/repository logic. Protected paths automatically receive the
  authenticated user via the `X-User-Id` request header (injected by
  `JwtAuthenticationFilter`).
- **Add a public (no-auth) path:** add its prefix to `PUBLIC_PATHS` in
  `config/JwtAuthenticationFilter.java`.
- **Change the schema:** edit the JPA `@Entity`; `ddl-auto=update` applies additive
  changes on restart. For destructive changes, adjust the table manually or reset
  the DB.
- **Rotate the JWT secret:** change `JWT_SECRET` in `.env` (invalidates existing
  tokens — users simply log in again).
- **Configuration:** all tunables live in
  `backend/src/main/resources/application.properties`, overridable by env vars.
- **Logs:** written to `backend/logs/` (see `logback-spring.xml`).
- **Dependencies:** backend in `backend/pom.xml`; frontend in
  `frontend/package.json`. Keep them minimal.
