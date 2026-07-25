# EcoTrack – Enhancements, Status & Roadmap

> Community item‑sharing platform. Stack is intentionally limited to **Java + Spring Boot + MySQL + React**. No Docker, Redis, Kubernetes, Azure, or paid cloud services.

---

## Architecture

| Component | Port | Responsibility |
|-----------|------|----------------|
| discovery-server (Eureka) | 8761 | Service registry |
| api-gateway (Spring Cloud Gateway) | 8080 | Routing + JWT validation → injects `X-User-Id` / `X-Username` |
| user-service | 8081 | Auth (JWT), users, profiles |
| item-service | 8082 | Items, images, **borrow workflow**, **favorites** |
| communication | 8083 | Real‑time chat (STOMP over WebSocket, in‑memory broker) |
| frontend (React + Vite + Tailwind) | 5173 | SPA |

Databases (MySQL, auto‑created via `ddl-auto=update`): `db_users`, `db_items`, `db_communication`.

---

## ✅ Implemented

### Authentication & session
- JWT issued on login; stored client‑side and attached via Axios request interceptor.
- Session restored from `localStorage` on refresh; 401 response interceptor clears session and redirects to login.
- Navbar/sidebar hide Login/Register when authenticated and show Dashboard, Profile, Logout.
- All app routes are protected (`<Navigate to="/login">`); gateway enforces JWT on every non‑public path.
- Ownership‑based authorization: users can only edit/delete their own items, account, and requests (enforced server‑side from the gateway‑verified `X-User-Id`, never from client input).

### Messaging (Redis removed)
- Chat re‑implemented on Spring's **in‑memory STOMP broker** — no Redis. Messages persist to MySQL and are delivered to both participants via `convertAndSendToUser`.
- WebSocket handshake validates `userId`; controller overrides spoofed `senderId` with the authenticated principal.
- Stable auto‑reconnect (SockJS + STOMP), live append, persistent history, connection indicator.

### Borrow Request workflow (item-service) — **new**
- Entity `BorrowRequest` with lifecycle `PENDING → ACCEPTED → RETURNED` plus `REJECTED` / `CANCELLED`.
- Borrower requests an item with a chosen duration (1/3/7/15/30 days, capped by the owner's max) and optional note.
- Owner accepts (item auto‑marked unavailable, due date set, other pending requests auto‑rejected) or rejects; borrower can cancel while pending.
- Return marks the item available again, increments its borrow count, and **auto‑computes late fees** (`lateFeePerDay × overdue days`). Security‑deposit amount is snapshotted per request.
- REST: `POST /api/borrow-requests`, `GET /incoming`, `GET /outgoing`, `PATCH /{id}/accept|reject|cancel|return`.

### Item enhancements
- New fields: `condition`, `maxBorrowDays`, `lateFeePerDay`, `securityDeposit`, `borrowCount` — surfaced on cards and in the create/edit form with validation.
- Browse page sorting (Newest / Most borrowed / Alphabetical) plus existing search & category/availability filters.

### Favorites / wishlist (item-service) — **new**
- `Favorite` entity with unique `(user, item)` constraint; heart toggle on cards; dedicated wishlist page.
- REST: `GET /api/favorites`, `GET /api/favorites/ids`, `POST /{itemId}`, `DELETE /{itemId}`.

### Personal Dashboard — **new**
- Stat tiles: my items, requests to review, currently borrowing/lending, pending sent, completed borrows, favorites, items lent.
- Eco‑impact estimate (successful shares, money saved, waste avoided). Default post‑login landing page.

### UI/UX, validation & error handling
- Modern responsive Tailwind design system (`surface`, `btn-*`, `chip`, `input`, `brand-text`), dark mode, spinners, skeleton‑free empty states, toasts, confirm dialogs, magnetic buttons, spotlight background.
- Client‑side validation on Register (username length, email, password strength, phone pattern) mirroring server‑side Bean Validation; inline per‑field errors.
- Server‑side Bean Validation on all DTOs/entities; global exception handlers return structured `{status,error,message,path,timestamp,validationErrors}`.
- Real backend health polling in the Service Status widget (not static icons).

---

## ⚠️ Known limitations
- **Single‑instance messaging**: the in‑memory STOMP broker is correct for one communication‑service instance (the intended scope). Horizontal scale‑out would need an external broker — deliberately out of scope to avoid Redis/RabbitMQ.
- **No user roles/admin**: authorization is ownership‑based; there is no admin role or moderation module yet.
- **Chat route is gateway‑public**: history/WS auth is handled at the WebSocket layer, not the gateway. Fine for this scope; a JWT filter on the communication service would harden it.
- Borrow/late‑fee/deposit amounts are recorded and computed but not settled through any payment integration (by design).
- Community points, badges, reviews/ratings, notifications, waiting list, and communities are **not yet implemented** (roadmap below).

---

## 🗺️ Remaining roadmap (not yet built)
- **Notifications** (in‑app, DB‑backed + polling): request received/accepted, due‑soon, returned, new message.
- **Ratings & reviews** after completed borrows; average rating on profiles.
- **Community points & badges**: award/deduct on lending, on‑time returns, reviews; member levels + achievement badges.
- **Reservation + waiting list** for unavailable items.
- **Communities** (apartment/society/college/office) scoping items, plus community feed & leaderboard.
- **Admin module**: manage users/items/categories, remove listings, suspend users (introduce a `role` field + gateway role checks).
- **Advanced search**: by owner/community, most‑borrowed/highest‑rated facets, recently viewed, suggestions, pagination on the browse grid.
- Multiple images per item; image preview already present for single upload.

---

## 🚀 Simplest free deployment (Spring Boot + React + MySQL only)
No subscriptions, no Docker/cloud lock‑in:

1. **MySQL** — free tier of a managed MySQL (e.g. Aiven/Railway free plan) **or** a MySQL instance on the same VM. Create the three schemas (auto‑created on first run).
2. **Backend** — build each service with `mvn -DskipTests package` and run the jars on a single free/low‑cost Linux VM (e.g. Oracle Cloud Always‑Free, AWS/GCP free tier). Start order: discovery‑server → user/item/communication → api‑gateway. Set env vars `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION_MS`. Use `systemd` units (or `nohup`) to keep them running.
3. **Frontend** — `npm run build` produces static files in `frontend/dist`; host free on GitHub Pages / Netlify / Vercel free tier. Set `VITE_API_URL` to the gateway's public URL at build time.
4. **CORS** — update allowed origins in the gateway and service `application.properties` to the deployed frontend origin.

All four services can also run on **one** modest VM behind the gateway; only port 8080 (gateway) and the static frontend host need to be public.
