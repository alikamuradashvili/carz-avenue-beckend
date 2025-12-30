# Carz Avenue Backend (Java/Spring Boot)

Spring Boot backend for a car marketplace with JWT auth (access + refresh), car CRUD, VIP ads, uploads, user profile management, admin tools, and Swagger docs.

## Tech
- Java 17, Spring Boot 3.3
- Spring Web, Security, Data JPA, Validation
- PostgreSQL + Flyway
- JWT (jjwt), BCrypt
- springdoc-openapi (`/swagger-ui.html`, `/v3/api-docs`)

## Getting Started
1) Install Java 17 and Maven.
2) Copy `.env.example` values into your environment or set directly.
3) Run DB (PostgreSQL) and update `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`.
4) Start API:
```bash
mvn spring-boot:run
```
Swagger UI: `http://localhost:8080/swagger-ui.html`

## Environment
- `DATABASE_URL` (e.g. `jdbc:postgresql://localhost:5432/carz_avenue`)
- `DATABASE_USER`, `DATABASE_PASSWORD`
- `JWT_SECRET` (base64-encoded, 256-bit) for access tokens
- `REFRESH_SECRET` (base64-encoded, 256-bit) for refresh tokens
- `IMAGE_STORAGE_PATH` (default `uploads`) local folder for images
- `IMAGE_BASE_URL` (default `http://localhost:8080/uploads`)
- `PORT` (optional, default 8080)

## Migrations
Flyway migrations live in `src/main/resources/db/migration`. They create users, car listings (with photos, VIP fields), and refresh tokens.

## Key Endpoints
- Auth: `/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/change-password`, `/auth/change-email`
- Users: `/users/me`, `/users/me/listings`, `/users/me` (PUT)
- Cars: `/cars` (filters + paging), `/cars/{id}`, `/cars` (POST), `/cars/{id}` (PUT/DELETE), `/cars/{id}/vip` (POST)
- Upload: `/upload/image` (multipart `files[]`, auth required)
- Admin (ROLE_ADMIN): `/admin/listings`, `/admin/listings/{id}` (DELETE), `/admin/users`, `/admin/users/{id}/block?blocked=true|false`

## Notes
- Access token: 15m default. Refresh token: 30d default.
- Refresh tokens are stored in DB; logout revokes them.
- VIP ads store `isVip` and `vipExpiresAt`; listing query hides expired VIPs.
- Static file serving maps `/uploads/**` to `IMAGE_STORAGE_PATH`. Use Cloudinary/S3 by swapping `UploadService`.

## Photos / images for cars
- Public reads: `GET /cars`, `/cars/all`, `/cars/{id}` return `photos` and `images` arrays; frontend can render either.
- Upload flow (real data):
  1. `POST /upload/image` (multipart/form-data, field `files`, auth) → returns image URLs (e.g. `http://localhost:8080/uploads/<uuid>.jpg`).
  2. `POST /cars` or `PUT /cars/{id}` (auth) with `photos` (or `images`) containing those URLs.
- Fallback (dev only): if a car has no stored photos, `CarMapper.toResponse` returns the local placeholder at `http://localhost:8080/images/default-car.jpg`. Rebuild/restart the backend to pick up this code. The frontend must point at the same base (e.g. `http://localhost:8080`).
