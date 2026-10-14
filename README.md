# No One Sleeps Hungry 🍲

A community-driven initiative to collect surplus food from hotels, restaurants, weddings, housewarmings and parties — and get it to people who need it.

> If good food is available and someone is hungry, we make sure that food reaches them.

See **[PLAN.md](./PLAN.md)** for how the system works.

## Status
**Re-planning against the full product spec.** The first prototype is working; the build plan is now in [PLAN.md](./PLAN.md) and the product spec is in [docs/PRODUCT_SPEC.md](./docs/PRODUCT_SPEC.md). Work proceeds step by step from Step 1.

Prototype notes (will change as steps land):
Phase 1 features done: public forms, admin login + dashboard, localities and coordinators (each coordinator only sees their own localities), deliveries (match food to hunger reports, track pickup and delivery) and an activity log.
Next: deploy and pilot in one locality; then notifications (Phase 2).

## Stack
- Frontend: React (Vite) — `frontend/`
- Backend: Java 17 + Spring Boot 3 — `backend/`
- Database: MySQL 8 (schema managed by Flyway)

## Run locally

### 1. Database
Either use your own MySQL (default login `root` / `root`, database is auto-created), or:
```
docker compose up -d
```
Override with env vars if needed: `DB_URL`, `DB_USER`, `DB_PASSWORD`.

### 2. Backend (port 8080)
```
cd backend
.\mvnw.cmd spring-boot:run      # Windows   (Mac/Linux: ./mvnw spring-boot:run)
```
Check: http://localhost:8080/api/health  → `{"status":"ok"}`

### 3. Frontend (port 5174)
```
cd frontend
npm install
npm run dev
```
Open http://localhost:5174 — the dev server proxies `/api` to the backend.

## Admin login
Open http://localhost:5174/admin. On first start the backend creates a Super Admin:

- email: `admin@nosleepshungry.local`
- password: `ChangeMe123!`

Change these *before* going live by setting `ADMIN_EMAIL`, `ADMIN_PASSWORD` and `JWT_SECRET` (32+ characters)
as environment variables **before the first start** (the account is only created when the users table is empty).

## Public API (so far)
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/public/food-offers` | Offer surplus food |
| POST | `/api/public/hunger-reports` | Report people who need food |
| POST | `/api/public/volunteer-applications` | Apply to volunteer |
| GET | `/api/public/stats` | Public counts only |
| GET | `/api/health` | Health check |

Admin (needs `Authorization: Bearer <token>` from `POST /api/auth/login`):

| Method | Path | Who |
|---|---|---|
| GET | `/api/admin/food-offers`, `/hunger-reports` (optional `?status=`) | admins |
| PATCH | `/api/admin/food-offers/{id}/status`, `/hunger-reports/{id}/status` | admins |
| GET / PATCH | `/api/admin/volunteer-applications` | Super Admin only |
| POST | `/api/admin/volunteer-applications/{id}/approve` | Super Admin: creates the coordinator account |
| GET | `/api/admin/localities` | admins |
| POST | `/api/admin/localities` | Super Admin |
| GET / POST / PATCH | `/api/admin/coordinators` | Super Admin |
| PATCH | `/api/admin/food-offers/{id}/locality`, `/hunger-reports/{id}/locality` | Super Admin |
| POST | `/api/admin/me/password` | any admin |
| GET / POST | `/api/admin/deliveries` | admins (own localities) |
| PATCH | `/api/admin/deliveries/{id}/status` | admins (own localities) |
| GET | `/api/admin/activity` | Super Admin |

## Roadmap
Phase 0 Foundation → **Phase 1 MVP (one locality)** → Phase 2 Notifications & polish → Phase 3 Multi-city.
