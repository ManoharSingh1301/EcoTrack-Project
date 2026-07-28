# EcoTrack Database

The backend uses a **single MySQL database** named `ecotrack`.

## Create the database

```bash
mysql -u root -p < setup.sql
```

That is all that is required — the Spring Boot backend creates every table and
index automatically on first startup (`spring.jpa.hibernate.ddl-auto=update`),
because the JPA entities declare their columns and `@Index` definitions in code.

## Tables (auto-created)

| Table             | Purpose                                              |
|-------------------|------------------------------------------------------|
| `users`           | Accounts; passwords stored as BCrypt hashes          |
| `items`           | Shareable items, image bytes, borrow metadata        |
| `borrow_requests` | Borrow workflow (pending → accepted → returned, etc.)|
| `favorites`       | Per-user wishlist                                    |
| `chat_messages`   | Persisted chat history                               |

## Connection settings

Configured in `backend/src/main/resources/application.properties` and overridable
via `backend/.env` (see `backend/.env.example`): `DB_HOST`, `DB_PORT`, `DB_NAME`,
`DB_USERNAME`, `DB_PASSWORD`.
