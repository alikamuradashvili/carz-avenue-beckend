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
2) Copy `.env.example` to `.env` or set the values directly in your environment.
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
- `SERVER_PORT` (optional, default 8080)
- `GOOGLE_CLIENT_ID` (OAuth client ID)
- `GOOGLE_CLIENT_SECRET` (OAuth client secret)
- `GOOGLE_REDIRECT_URI`
- `FRONTEND_CALLBACK_URL`

## Google OAuth2 (OpenID Connect)
Steps to enable Google login in dev:
1) Go to Google Cloud Console -> APIs & Services -> Credentials.
2) Create OAuth client ID (Web application).
3) Add authorized redirect URI:
   - `http://localhost:8080/auth/google/callback`
4) Copy the client ID + secret into environment variables:
   - `GOOGLE_CLIENT_ID`
   - `GOOGLE_CLIENT_SECRET`
5) Set redirect + frontend callback URLs:
   - `GOOGLE_REDIRECT_URI=http://localhost:8080/auth/google/callback`
   - `FRONTEND_CALLBACK_URL=http://localhost:5174/auth/callback`
6) Restart the backend after setting env vars (required).
Note: if any Google OAuth env var is missing, the backend will fail startup with a clear error.

### Example application.yml snippet
```yaml
google:
  oauth:
    client-id: ${GOOGLE_CLIENT_ID:}
    client-secret: ${GOOGLE_CLIENT_SECRET:}
    redirect-uri: ${GOOGLE_REDIRECT_URI:}
    frontend-callback-url: ${FRONTEND_CALLBACK_URL:}
```

### Windows (PowerShell)
```powershell
$env:GOOGLE_CLIENT_ID="your-client-id"
$env:GOOGLE_CLIENT_SECRET="your-client-secret"
$env:GOOGLE_REDIRECT_URI="http://localhost:8080/auth/google/callback"
$env:FRONTEND_CALLBACK_URL="http://localhost:5174/auth/callback"
mvn spring-boot:run
```
Note: use the same terminal window and restart the backend after changes.
Note: `.env` is supported via Spring config import. Restart the backend after edits.

### macOS / Linux (bash)
```bash
export GOOGLE_CLIENT_ID="your-client-id"
export GOOGLE_CLIENT_SECRET="your-client-secret"
export GOOGLE_REDIRECT_URI="http://localhost:8080/auth/google/callback"
export FRONTEND_CALLBACK_URL="http://localhost:5174/auth/callback"
mvn spring-boot:run
```

### IntelliJ Run Configuration
Set Environment variables:
```
GOOGLE_CLIENT_ID=your-client-id;GOOGLE_CLIENT_SECRET=your-client-secret;GOOGLE_REDIRECT_URI=http://localhost:8080/auth/google/callback;FRONTEND_CALLBACK_URL=http://localhost:5174/auth/callback
```

Note: restart the server after changing env vars.

### Docker Compose example
```yaml
environment:
  GOOGLE_CLIENT_ID: your-client-id
  GOOGLE_CLIENT_SECRET: your-client-secret
  GOOGLE_REDIRECT_URI: http://localhost:8080/auth/google/callback
  FRONTEND_CALLBACK_URL: http://localhost:5174/auth/callback
```

### Verification
1) Start backend and confirm log line:
   `Google OAuth configured: clientId=..., redirectUri=..., frontendCallbackUrl=...`
2) Hit `GET /auth/google/url` and verify it returns a Google URL with your client_id.

## Migrations
Flyway migrations live in `src/main/resources/db/migration`. They create users, car listings (with photos, VIP fields), and refresh tokens.

## Car make/model master list
The backend maintains a normalized master list of manufacturers (`car_make`) and models (`car_model`) for dropdowns and filtering. Listings are stored separately in `car_listing`.

Data flow:
`Seeder` -> `car_make` / `car_model` -> API -> frontend dropdowns

### Database schema overview
Core tables and relationships:
- `car_make`: stores all manufacturers (`id`, `name`).
- `car_model`: stores models (`id`, `name`) with `make_id` FK to `car_make(id)`.
- `car_listing`: stores actual listings (`make`, `model`, and other listing fields). This is not FK-linked to `car_make` or `car_model` to keep listings flexible.

Schema snippet (from V13):
```sql
CREATE TABLE car_make (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE car_model (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    make_id BIGINT NOT NULL REFERENCES car_make(id),
    UNIQUE (make_id, name)
);
```

### Flyway migrations for master data
- `src/main/resources/db/migration/V13__create_car_make_model_tables.sql` creates `car_make` and `car_model`.
- `src/main/resources/db/migration/V14__seed_car_makes.sql` seeds the master make list.

### How to seed data
1) Flyway on app startup (default)
   - Migrations run automatically on `mvn spring-boot:run`.
   - This will create tables and insert the base make list.

2) Optional NHTSA-based seeder (profile-based)
   - `MakeModelSeeder` can fetch makes/models from NHTSA when the `seed-makes` profile is enabled.
   - It normalizes names and upserts into `car_make` and `car_model`.
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=seed-makes
```
Note: the NHTSA seeder calls `https://vpic.nhtsa.dot.gov/api/vehicles` and requires network access.

### API endpoints
- `GET /api/makes`
  - Returns all makes (for dropdowns).
  - Example:
```bash
curl http://localhost:8080/api/makes
```
- `GET /api/models?makeId=`
  - Returns models for a given make.
  - Example:
```bash
curl "http://localhost:8080/api/models?makeId=1"
```

### Testing notes
- Tests use PostgreSQL Testcontainers (`src/test/java/.../PostgresTestContainer.java`).
- Docker is required to run `mvn test`.

## Key Endpoints
- Auth: `/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/change-password`, `/auth/change-email`, `/auth/google/url`, `/auth/google/callback`, `/auth/me`
- Users: `/users/me`, `/users/me/listings`, `/users/me` (PUT)
- Cars: `/cars` (filters + paging), `/cars/{id}`, `/cars` (POST), `/cars/{id}` (PUT/DELETE), `/cars/{id}/vip` (POST)
- Upload: `/upload/image` (multipart `files[]`, auth required)
- Admin (ROLE_ADMIN): `/admin/listings`, `/admin/listings/{id}` (DELETE), `/admin/users`, `/admin/users/{id}/block?blocked=true|false`

### Make / Model Master List Endpoints
- `GET /api/makes` - Returns all car manufacturers from `car_make`.
- `GET /api/models?makeId={id}` - Returns models for the selected manufacturer from `car_model`.
- These endpoints power frontend Make/Model dropdowns.
- Data comes from the database (not from car listings).
- Master data is seeded via Flyway (`V14__seed_car_makes.sql`) or the NHTSA seeder.

## Database Relationships (ER Overview)
- `car_make` (parent): stores all manufacturers.
- `car_model` (child): stores models, FK -> `car_make.id`.
- `car_listing`: stores actual car listings and references make/model by name.

Notes:
- `car_make` -> `car_model` is a one-to-many relationship.
- `car_listing` represents real cars for sale.
- Make/Model master data is independent from listings.

Example create car listing request:
```json
{
  "title": "Toyota Camry",
  "make": "Toyota",
  "model": "Camry",
  "year": 2020,
  "price": 15000
}
```

## Database & Flyway Verification (Troubleshooting)
- If `car_make` / `car_model` tables are not visible, the most common cause is connecting to a different database than the one used by the application at runtime.
- Verify the active database on application startup by checking logs for:
  - Datasource URL
  - Database name and port
  - `car_make` table existence and row count
- Flyway migrations run on the same datasource defined by `spring.datasource.*` and `spring.flyway.*`.
- Startup diagnostics: `DatabaseStartupLogger` logs the active database and `car_make` status to help debug mismatches.

Example expected startup log:
```
Datasource URL: jdbc:postgresql://localhost:5432/<db_name>
Database name: <db_name>, port: 5432
car_make table exists=true, row count=XXX
```

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
