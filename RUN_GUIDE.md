# 🚀 EcoTrack — Run Guide (Clone → Running App)

This is the only guide you need to go from a fresh `git clone` to a fully working app.
EcoTrack runs entirely on your machine with **no Redis, no Docker, no message broker** —
the only external service is **MySQL**.

---

## 1. Install the toolchain (one time)

| Tool | Version | Verify command | Notes |
|------|---------|----------------|-------|
| **JDK** | 17 | `java -version` | Temurin 17 recommended |
| **Maven** | 3.8+ | `mvn -version` | Builds/runs the backend |
| **Node.js** | 18+ | `node -v` | Runs the React frontend |
| **MySQL** | 8.0+ | `mysql --version` | The only external service |

> You do **not** install Redis, Docker, Kafka, or anything else.
> The frontend’s libraries (React, axios, **@stomp/stompjs**, **sockjs-client**, …) are
> downloaded automatically by `npm install` — they are not separate programs.

### macOS (Homebrew)
```bash
brew install --cask temurin17
brew install maven node mysql
brew services start mysql
```

### Windows
- JDK 17: install the Temurin `.msi` (tick “Set JAVA_HOME” + “Add to PATH”).
- Maven: unzip to `C:\maven`, add `C:\maven\bin` to PATH.
- Node.js: install the LTS `.msi`.
- MySQL: use the MySQL Installer, set a **root password you remember**.

Close and reopen your terminal, then confirm all four `verify` commands work.

---

## 2. Create the databases (empty — no tables to write)

```bash
mysql -u root -p < database/setup.sql
```
This creates 3 empty databases: `db_users`, `db_items`, `db_communication`.
**You never create tables by hand** — Hibernate builds them automatically on first boot
(`spring.jpa.hibernate.ddl-auto=update`).

---

## 3. Set environment variables

The services read secrets from the environment (nothing sensitive is hardcoded).

### macOS / Linux
```bash
export DB_USERNAME=root
export DB_PASSWORD=your_mysql_password
export JWT_SECRET=change-me-to-a-random-string-at-least-32-chars
export JWT_EXPIRATION_MS=86400000
```
Run these in **every** terminal that starts a backend service (or add them to `~/.zshrc`).

### Windows (PowerShell)
```powershell
setx DB_USERNAME "root"
setx DB_PASSWORD "your_mysql_password"
setx JWT_SECRET "change-me-to-a-random-string-at-least-32-chars"
setx JWT_EXPIRATION_MS "86400000"
```
`setx` is permanent but only affects **new** windows — **close and reopen PowerShell**, then
verify with `echo $env:JWT_SECRET`.

> ⚠️ **`JWT_SECRET` must be at least 32 characters** (HS256 requirement) and **identical**
> for `user-service` and `api-gateway` (one signs tokens, the other verifies them).

---

## 4. Install frontend dependencies (one time)

```bash
cd frontend
npm install      # pulls React, axios, @stomp/stompjs, sockjs-client, tailwind, etc.
cd ..
```

---

## 5. Start everything (order matters)

Eureka is the service registry — it must be up first, then the services register, then the
gateway can route to them. Give it 1–2 minutes to settle.

### Option A — Windows helper script
```powershell
.\start-all-services.ps1
```
(If blocked: `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.)

### Option B — manual (any OS): open 6 terminals
```bash
# 1) Service registry — wait until http://localhost:8761 loads
cd discovery-server && mvn spring-boot:run

# 2) API gateway
cd api-gateway     && mvn spring-boot:run

# 3) User service
cd user-service    && mvn spring-boot:run

# 4) Item service
cd item-service    && mvn spring-boot:run

# 5) Communication (chat) service
cd communication   && mvn spring-boot:run

# 6) Frontend
cd frontend        && npm run dev
```

### Ports
| Component | URL |
|-----------|-----|
| Eureka dashboard | http://localhost:8761 |
| API Gateway (all API + WebSocket traffic) | http://localhost:8080 |
| User service | 8089 |
| Item service | 8088 |
| Communication service | 8087 |
| **Frontend (open this)** | **http://localhost:5173** |

---

## 6. Verify it’s healthy

1. Open **http://localhost:8761** → you should see **API-GATEWAY, USER-SERVICE,
   ITEM-SERVICE, COMMUNICATION-SERVICE** registered.
2. Health checks return `{"status":"UP"}`:
   ```bash
   curl http://localhost:8089/actuator/health
   curl http://localhost:8088/actuator/health
   curl http://localhost:8087/actuator/health
   ```
3. Open **http://localhost:5173** → the status dot (bottom-right) turns green.

---

## 7. Full functionality checklist

Walk through this to confirm end-to-end:

- [ ] **Register** a user → you’re auto-logged-in and land on Browse
- [ ] **Log out**, then **log in** → session persists on refresh
- [ ] **Browse Items** → filter by **category**, toggle **Available only**, **search** + clear
- [ ] **My Items → Add new item** → fill the form, **upload a photo** → the card shows the image
- [ ] **Edit** an item (no new photo) → old photo is kept; **toggle** availability; **delete** → styled confirm + toast
- [ ] **Messages** → register a *second* account in another browser/incognito, then chat live between the two (messages appear instantly on both sides; history persists after refresh)
- [ ] **Profile** → address/phone/bio load; edit + save → toast; refresh keeps the values
- [ ] **Dark mode** toggle (navbar) → whole UI themes correctly
- [ ] **Security**: logged in as user A, calling `PUT /api/users/{B}` returns **403**

---

## 8. Build & test (optional but recommended)

```bash
cd user-service   && mvn test
cd ../item-service && mvn test
cd ../communication && mvn test
cd ../frontend     && npm run build
```

---

## 9. Troubleshooting

| Symptom | Fix |
|---------|-----|
| `mvn` / `java` not found | Reopen terminal; reinstall with PATH option |
| Service crashes with a JWT / `WeakKey` error | `JWT_SECRET` too short or unset — use 32+ chars, re-`export`/`setx`, restart |
| `Access denied for user 'root'` | Wrong `DB_PASSWORD` — fix env var, restart the service |
| Gateway returns 503 / “no instances available” | Service not registered yet — wait 60s, check :8761 |
| `echo $env:JWT_SECRET` blank (Windows) | You didn’t reopen PowerShell after `setx` |
| Chat shows “Connecting…” forever | Ensure the **communication service** and **gateway** are both up; refresh |
| Frontend can’t reach backend | The **API Gateway (:8080)** must be running |

---

## 10. Tech stack

Java 17 · Spring Boot 3.2 · Spring Cloud 2023 (Eureka + Gateway + OpenFeign + Resilience4j) ·
Spring Security + JWT · Spring Data JPA · **in-memory Spring Cache** · WebSocket/STOMP ·
Spring AOP · MySQL · React 18 + Vite + Tailwind CSS.
