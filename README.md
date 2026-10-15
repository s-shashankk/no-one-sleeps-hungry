# MealUp 🍲

A small record-keeping website for a community food-rescue initiative. **Coordination happens by phone and WhatsApp; this site keeps the records.**

## How it works
1. You add **vendors** (hotels, caterers, event hosts) with name, phone and location.
2. You add **coworkers** (the people who collect and distribute food) with name, phone and location.
3. When someone reports that people need food, it appears under **Requests → Pending**. (Anyone can use the public form at `/report-hunger`; you can also type one in.)
4. Open the request: **call vendors** to find surplus food, then **message a coworker on WhatsApp** (the message is already written; you can edit it).
5. When the food has been delivered, enter the **number of meals** and press **Mark completed**. Or press **Unable to serve** and the request goes off the pending list.
6. **Surplus food** offered by a vendor (public form at `/surplus-food`) is also a request: press **Accept** if you will send a team, or **Unable to collect** to reject it.
7. A mistake? Open the request and press **Reopen**.

The admin home shows four numbers: **total meals delivered**, **active vendors**, **active coworkers**, **pending requests**. Each opens the list behind it.

Vendors and coworkers are never deleted; **Switch off** hides them from the active lists and the counts, and **Switch on** brings them back.

## Stack
React + Vite + TypeScript · Java 21 + Spring Boot 3.5 · MySQL 8 + Flyway · Springdoc OpenAPI · JUnit, Mockito, Testcontainers

## Database (4 tables)
| Table | What it holds |
|---|---|
| `users` | the admin login (email, password hash, phone) |
| `vendors` | name, contact person (optional), phone, location, notes, active |
| `coworkers` | name, phone, location, notes, active |
| `requests` | kind (`NEED_FOOD` / `SURPLUS_FOOD`), status (`PENDING`, `ACCEPTED`, `COMPLETED`, `REJECTED`), contact, location, quantity (people or meals), details, meals delivered, note |

The schema is owned by Flyway (`backend/src/main/resources/db/migration`). `V3__simplify.sql` turned the earlier, larger design into these four tables: your admin login and vendors are kept, and old team members become coworkers.

## Run locally
You need Java 21, Node 20+ and MySQL 8 on port 3306. Docker is only needed for the integration tests.

**Backend** (port 8080)
```
cd backend
.\mvnw.cmd spring-boot:run        # Windows      (Mac/Linux: ./mvnw spring-boot:run)
```
Dev profile: database `mealup` is created on your local MySQL, and the admin account is created on first start. Set `DB_PASSWORD` if your MySQL password differs. Health: http://localhost:8080/api/v1/health · API docs: http://localhost:8080/swagger-ui.html

**Frontend** (port 5174)
```
cd frontend
npm install
npm run dev
```
Open http://localhost:5174. Admin login: http://localhost:5174/login (dev defaults `founder@mealup.local` / `DevOnly#Pass2026`).

**Tests**: `cd backend && .\mvnw.cmd test` (needs Docker running; without it the integration tests are skipped) and `cd frontend && npm run typecheck`.

## Configuration (environment variables)
| Variable | Used in | Meaning |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | production | `prod` |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | prod (optional in dev) | MySQL connection |
| `MEALUP_ADMIN_NAME`, `_PHONE`, `_EMAIL`, `_PASSWORD` | first start | the admin account (password at least 12 characters). Created only if no account exists yet. |
| `MEALUP_RATE_PUBLIC_PER_HOUR`, `MEALUP_RATE_LOGIN_PER_10_MIN` | prod (optional) | abuse limits per address (defaults 20 and 10) |
| `VITE_CONTACT_EMAIL` | frontend build (optional) | shown on the privacy page |
| `VITE_DEFAULT_COUNTRY_CODE` | frontend build (optional) | added to 10-digit numbers for WhatsApp links (default `91`) |

More: [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) · [docs/PERMISSIONS.md](docs/PERMISSIONS.md) · [docs/PILOT.md](docs/PILOT.md) · `ops/backup.sh`, `ops/restore-check.sh`.

## Project layout
```
backend/    Spring Boot API   (com.mealup: auth, users, vendors, coworkers, requests, dashboard, security, common)
frontend/   React + TypeScript app
docs/       DEPLOYMENT, PERMISSIONS, PILOT; PRODUCT_SPEC.md is the original, larger spec (kept for history)
ops/        backup and restore-check scripts
```
