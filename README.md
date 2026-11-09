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

### Hotel pickups, areas, joining (added Oct 2026)
- **Areas** (admin → Areas): Indiranagar is open; Vijayanagar and Domlur start as "coming soon". Open an area when you are ready, add more any time. Give each **hotel** an area (admin → Hotels).
- **Deliver food** (public `/deliver`, also offered after someone reports people in need): a volunteer picks an open area, picks a partner hotel, and enters name and phone. Visitors see the hotel's name and address only, never its phone number.
- **You get the message**: the pledge appears in admin → **Pickups** (badge on the menu, number in the browser tab title, refreshed every 30 seconds). Press **Message hotel on WhatsApp** (text already written), then **I informed the hotel**. Message the volunteer too.
- **Plates served**: after delivery press **Record plates served**. The number is added to the public **meals served** total. (Do not also enter the same meals on a food request, or they are counted twice.)
- **Join the community** (public `/join`): arrives in admin → **Joining**. Call them, then **Mark as contacted**.
- **Public dashboard** (home page and `/impact`): meals served, cities, areas open, hotel partners, and a table by area. Counts only.

- **Sample data** (dev only): on a local run the app loads sample hotels, volunteers, one waiting pickup and one finished pickup (48 plates), all marked "(sample)", so you can try the whole flow. Edit them, switch them off, or start the backend with `MEALUP_DEMO_DATA=false` to skip. Never turn it on for the live site.
- **Areas** can be added, renamed, opened or set to "coming soon" any time in admin → Areas.
- **Hotel logos** (admin → Hotels → Upload logo; PNG, JPG or WebP under 500 KB): logos appear in the moving "Hotels standing with us" slider on the home page. A hotel without a logo shows its name instead.

The admin home shows what is waiting (new pickups, pending requests, new join requests) and the totals. Each opens the list behind it.

Vendors and coworkers are never deleted; **Switch off** hides them from the active lists and the counts, and **Switch on** brings them back.

## Stack
React + Vite + TypeScript · Java 21 + Spring Boot 3.5 · MySQL 8 + Flyway · Springdoc OpenAPI · JUnit, Mockito, Testcontainers

## Database (7 tables)
| Table | What it holds |
|---|---|
| `users` | accounts: the admin and volunteers (`role`), email, password hash, phone |
| `vendors` | name, contact person (optional), phone, location, notes, active |
| `coworkers` | name, phone, location, notes, active |
| `areas` | city, name, open or coming soon |
| `pickups` | volunteer, hotel, area, status (`PLEDGED`, `CONFIRMED`, `COMPLETED`, `CANCELLED`), plates served |
| `join_requests` | name, phone, email, location, message, status (`NEW`, `CONTACTED`) |
| `requests` | kind (`NEED_FOOD` / `SURPLUS_FOOD`), status (`PENDING`, `ACCEPTED`, `COMPLETED`, `REJECTED`), contact, location, quantity (people or meals), details, meals delivered, note |

The schema is owned by Flyway (`backend/src/main/resources/db/migration`). `V4__areas_pickups_joins.sql` added areas, pickups and join requests (and an area on each vendor). `V3__simplify.sql` turned the earlier, larger design into these four tables: your admin login and vendors are kept, and old team members become coworkers.

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

### Volunteer accounts
Volunteers create an account at `/register` (email + password) and sign in at `/login`. To collect food they choose area, hotel and **how many people they will feed**; this is stored with the pickup (`pickups.people_count`, `pickups.volunteer_user_id`). Volunteers see their own totals at `/me`. The admin sees everyone at `/admin/volunteers` (pickups asked, deliveries done, plates served, people reported, last sign-in), can download a CSV, switch an account off, or make a temporary password (there is no email reset). A volunteer can have at most 3 pickups open at once. In dev, three sample volunteers (`ravi.sample@mealup.local`, `ananya.sample@mealup.local`, `imran.sample@mealup.local`, password `Sample#Pass123`) are created.

**Tests**: `cd backend && .\mvnw.cmd test` (needs Docker running; without it the integration tests are skipped) and `cd frontend && npm run typecheck`.

### Hotel staff, daily limits, feedback
- **Hotel staff:** the hotel's person signs up like any volunteer (`/register`). In **Admin → Volunteers** press **Make hotel staff** and choose their hotel. From then on they sign in with the same email and password and land on `/hotel`, where they see only their own hotel's bookings as cards. "Make ordinary volunteer" takes it away again.
- **Accept = food served:** when a volunteer books, the hotel sees the booking. When they give the food they press **Accept**, and the meal counts as served (one plate per person the volunteer said they would feed). There is no code to type, so a busy kitchen only presses one button. The hotel does not see running plate totals.
- **You can accept for them:** in **Pickups** press **Accept for the hotel**. It shows as done on the hotel's screen ("accepted for you by the MealUp team") and as "accepted by you" on yours. Whoever accepts first wins.
- **Daily limit:** each hotel gives a number of plates per day (default 5; set it in **Hotels**). A volunteer's booking takes one plate for every person they will feed, so a booking for 3 leaves 2 of 5. A booking larger than the plates left is refused. Cancelled bookings give their plates back. The day runs on India time. Volunteers see "N plates left today", and a hotel with none left is not offered.
- **Feedback and messages:** volunteers and hotel staff use **Feedback** in their menu to send feedback (with 1-5 stars) or a message. You read them in **Admin → Messages** (a badge shows new ones), call or WhatsApp the sender, and mark them done.
- The hotel's screen refreshes every 15 seconds and the tab title shows a count. Real SMS or WhatsApp push messages would need a paid provider and can be added later.
- **Dummy data (dev):** sign in as `hotel.sample@mealup.local` (password `Sample#Pass123`, staff of Sunrise Residency) to see two waiting bookings, one accepted by the hotel and one accepted by the admin, plus five sample feedback notes in Admin → Messages. Edit or delete them as you like.

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

## Look and images
The home page tells people who the project is for with three drawn scenes (a child, an elder, a family). To use real photos instead, put `child.jpg`, `elder.jpg` and `family.jpg` in `frontend/public/img/photos/` and set `VITE_STORY_PHOTOS=true` when building. Use only photos of people who agreed to be photographed, and keep faces and places private if they ask.

Colours and fonts are at the top of `frontend/src/styles.css`. The banana-leaf illustration is `frontend/public/img/hero-meal.svg` and the logo is `frontend/public/img/logo.svg`. To use a real photo, put it at `frontend/public/img/hero.jpg` and change the `src` in `frontend/src/pages/Home.tsx` (use photos you own or that are licensed for this use, and compress them below 300 KB).

## Deploying
See `docs/DEPLOYMENT.md`. Fastest: a VPS with Docker, then `docker compose -f docker-compose.prod.yml --env-file deploy/.env up -d --build` (HTTPS is automatic).

## Bookings that nobody collects

Every 15 minutes the API closes any booking still open after 12 hours (`mealup.pickups.expire-after-hours`, set `0` to switch off).
A closed booking frees the hotel's daily place and the volunteer's open-pickup slot. See `PickupExpiryJob`.

## Editable website text

The admin can change the wording of the home page, About page and footer at `/admin/texts` (menu: Site text). Only changed texts are stored (table `site_texts`, migration V9); the original wording lives in `frontend/src/textDefaults.ts`, so clearing a text brings it back. Public read: `GET /api/v1/public/texts`; admin save: `PUT /api/v1/texts`. To make another sentence editable, add it to `textDefaults.ts` and use `t('your.key')` from `useTexts()`.
