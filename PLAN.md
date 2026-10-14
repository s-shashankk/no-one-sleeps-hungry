# No One Sleeps Hungry — Project Plan

> If good food is available and someone is hungry, we make sure that food reaches them.

This document describes **how the system actually works** — people, flows, data, pages, and phases — so we can build against it. Nothing here is code yet.

---

## 1. What we are building

A simple, mobile-friendly website that connects three groups:

| Group | What they do on the site |
|---|---|
| **Donors / Vendors** (hotels, restaurants, event hosts) | Tell us surplus food is available |
| **Reporters** (anyone) | Tell us where hungry people are |
| **Coordinators** (volunteers) | Match food to need, arrange pickup & delivery, record completion |

The Super Admin (founder) oversees everything.

**Principle:** this is a *community network* first, software second. The site only has to make sure nothing is forgotten. Keep it small and reliable.

---

## 2. Roles & permissions

| Role | Who | Can do |
|---|---|---|
| **Public visitor** | Anyone, no login | Submit a *Hunger Report*; submit a *Food Offer*; see public impact numbers |
| **Vendor** (registered contact) | Regular donors added by an admin | Same as visitor, but offers are pre-linked to their vendor profile (Phase 2: simple login / magic link) |
| **Coordinator (Admin)** | Approved volunteer, assigned to one or more localities | See & manage reports/offers in *their* localities; add vendors; create and run Deliveries |
| **Super Admin** | Founder only | Everything, across all localities. Sole power to **approve / revoke admins** and **assign localities** |

Rules:
- Only the Super Admin can create or remove Coordinators.
- A Coordinator can only see/act on data in their assigned localities.
- Every state change is logged (who, what, when).

---

## 3. Core flows

### 3.1 Food Offer flow (surplus food → delivered)

```
Vendor/anyone submits Food Offer
        │  (what, how much, pickup address, ready-by / expires-at, contact)
        ▼
  Status: OPEN  ──► Coordinator of that locality is notified
        │
        ▼
Coordinator reviews, checks food is safe & still available
        │
        ├─ Not suitable / expired ──► Status: REJECTED / EXPIRED (reason saved)
        ▼
Coordinator picks a Hunger Report (or several) to serve
        │   → creates a DELIVERY linking Offer ↔ Report(s)
        ▼
Assigns a volunteer / self for pickup + drop
        │   Status: ASSIGNED
        ▼
Pickup done ► IN_TRANSIT ► Delivered  → coordinator records
        │   (people served, photo optional, notes)
        ▼
Status: COMPLETED   (counts toward impact numbers)
```

### 3.2 Hunger Report flow (someone needs food → served)

```
Anyone submits Hunger Report
   (location / landmark, optional map pin, approx. number of people, notes, optional contact)
        ▼
Status: NEW  → routed to the locality's coordinator
        ▼
Coordinator verifies (call reporter / visit / trusted volunteer)
        ▼
Status: VERIFIED  → waiting for food
        ▼
Matched to a Delivery  → Status: SCHEDULED → SERVED
        ▼
Closed.  If it recurs (same place daily), mark as RECURRING.
```

### 3.3 Volunteer / Coordinator onboarding

1. Person applies via a simple form ("Join as volunteer").
2. Super Admin reviews (and ideally has a phone call).
3. Super Admin approves → account created, localities assigned.
4. Super Admin can suspend/revoke at any time.

### 3.4 Vendor onboarding

1. Coordinator or Super Admin visits/calls the vendor.
2. Adds vendor profile: name, type, address, contact, locality, usual surplus times.
3. Vendor gets a short link / QR code for quick "surplus available" submission.

---

## 4. Food safety rules (built into the process)

Food safety is the biggest real-world risk. The site should *encourage* and *record* it:

- Offer form asks: **cooked time**, **how it's stored**, **ready-by / safe-until time**.
- Coordinator must tick a **"checked: looks/smells fine, within time limit"** box before assigning.
- Prefer delivery within a short window (e.g. 2–3 hours of cooking). Admin sets the limit.
- Offers past `expires_at` auto-flip to **EXPIRED** and cannot be assigned.
- Pickup is *vendor-agrees-to-hand-over*; we never take food a vendor didn't offer.
- Clear public disclaimer: food is donated surplus, handled on best-effort basis.

---

## 5. Data model (MySQL)

Core tables (simplified):

**users** — id, name, email, phone, password_hash, role (`SUPER_ADMIN` / `COORDINATOR`), status (`ACTIVE`/`SUSPENDED`), created_at
**localities** — id, name, city, notes
**coordinator_localities** — user_id, locality_id
**volunteer_applications** — id, name, phone, email, college/occupation, locality_pref, message, status (`PENDING`/`APPROVED`/`REJECTED`), reviewed_by
**vendors** — id, name, type (hotel/restaurant/event/other), address, locality_id, contact_name, contact_phone, notes, created_by, active
**food_offers** — id, vendor_id (nullable), submitter_name, submitter_phone, locality_id, description, quantity_estimate (servings), food_type (veg/non-veg/mixed), cooked_at, expires_at, pickup_address, status, rejection_reason, safety_checked, created_at
**hunger_reports** — id, locality_id, location_text, latitude, longitude, people_count, notes, reporter_name (optional), reporter_phone (optional), status, verified_by, created_at
**deliveries** — id, food_offer_id, assigned_coordinator_id, assigned_volunteer_name, status, picked_up_at, delivered_at, people_served, notes
**delivery_reports** — delivery_id, hunger_report_id (a delivery can serve several reports)
**activity_log** — id, user_id, entity, entity_id, action, details, created_at

Status enums:
- **Offer:** `OPEN → ASSIGNED → IN_TRANSIT → COMPLETED` (or `REJECTED`, `EXPIRED`, `CANCELLED`)
- **Report:** `NEW → VERIFIED → SCHEDULED → SERVED` (or `INVALID`, `RECURRING`)
- **Delivery:** `ASSIGNED → PICKED_UP → DELIVERED` (or `FAILED`, `CANCELLED`)

---

## 6. Website — pages

### 6.1 Public site
| Page | Purpose |
|---|---|
| `/` Home | Mission, two big buttons: **"I have surplus food"** and **"Someone needs food"**, live impact counter |
| `/offer-food` | Food Offer form |
| `/report-hunger` | Hunger Report form (with "use my location" button) |
| `/join` | Volunteer application |
| `/about` | Story, how it works, safety note |
| `/thank-you` | Confirmation page after any form |

Design: single column, big buttons, works on low-end phones and slow networks. English first; Telugu/Hindi/Tamil etc. can be added later.

### 6.2 Admin dashboard (`/admin`)
| Page | Who | Purpose |
|---|---|---|
| Login | all admins | Email/phone + password |
| Overview | all | Open offers, new reports, active deliveries for my localities |
| Food Offers | coordinator+ | List, filter, review, reject, assign |
| Hunger Reports | coordinator+ | List, verify, mark served |
| Deliveries | coordinator+ | Create/track pickups & drops, record people served |
| Vendors | coordinator+ | Add/edit vendors |
| Volunteer Applications | **Super Admin** | Approve / reject |
| Admins & Localities | **Super Admin** | Create/suspend admins, assign localities |
| Activity Log & Stats | **Super Admin** | Everything, exports (CSV) |

---

## 7. API sketch (Spring Boot, REST + JSON)

Public (rate-limited, CAPTCHA):
- `POST /api/public/food-offers`
- `POST /api/public/hunger-reports`
- `POST /api/public/volunteer-applications`
- `GET  /api/public/stats`

Admin (JWT auth):
- `POST /api/auth/login`
- `GET/PATCH /api/admin/food-offers`, `/hunger-reports`, `/deliveries`, `/vendors`
- `POST /api/admin/deliveries` (match offer ↔ reports)
- Super Admin only: `/api/admin/users`, `/api/admin/localities`, `/api/admin/volunteer-applications`, `/api/admin/activity-log`

Security: BCrypt passwords, JWT, role checks at controller level, **locality-scoping enforced in the service layer**, input validation, rate-limiting & CAPTCHA on public forms, HTTPS only.

---

## 8. Tech stack & proposed structure

- **Frontend:** React (Vite), React Router, a light UI kit (e.g. Tailwind), mobile-first
- **Backend:** Java 17+, Spring Boot 3 (Web, Security, Data JPA, Validation), Flyway for DB migrations
- **Database:** MySQL 8
- **Notifications (Phase 2):** WhatsApp link / SMS / email to coordinators when a new offer arrives

```
MealUp/            (repo: no-one-sleeps-hungry)
├── README.md
├── PLAN.md
├── backend/       Spring Boot app
├── frontend/      React app
└── docs/          decisions, screenshots, runbook
```

---

## 9. Hosting — needs a decision ⚠️

Your plan says "existing Hostinger plan, subject to Java support". Reality check:

- **Hostinger shared/web hosting generally cannot run a Spring Boot (Java) app.** It's built for PHP/static sites + MySQL.
- **Hostinger VPS** can run Java (a small VPS is enough), and you control everything.

Options, simplest first:
1. **Hostinger VPS** — Java + MySQL + Nginx on one box. Needs a plan upgrade.
2. **Split hosting:** React static site on your Hostinger plan; Spring Boot on a free/cheap platform (Render, Railway, Fly.io); MySQL via Hostinger remote MySQL or a managed DB.
3. **Rethink backend** for shared hosting (e.g. PHP) — only if cost is the blocker.

**Recommendation:** build and test locally first; decide hosting before the launch phase. Check your Hostinger plan type before spending anything.

---

## 10. Privacy & trust

- Hunger reports may contain phone numbers and locations: **only admins see them**, never public.
- Public stats show **counts only** (meals delivered, people served, localities).
- Don't store photos of recipients' faces; ask consent for any photo.
- Provide a clear privacy note and a way to request data deletion.
- Volunteers handle only their locality's data.

---

## 11. Phases

### Phase 0 — Foundation *(now)*
- [x] Idea + plan
- [ ] Repo on GitHub, plan reviewed & agreed
- [ ] Decide hosting, domain name, first locality

### Phase 1 — MVP (the "usable by one locality" version)
1. Backend skeleton + MySQL + Flyway schema
2. Auth (Super Admin login), seed Super Admin
3. Public forms: Food Offer, Hunger Report, Volunteer Application
4. Admin: list/view offers & reports, change status
5. Admin: create Delivery, mark completed
6. Super Admin: create Coordinators, assign localities
7. Public home page with impact counter
8. Deploy + test with 2–3 real vendors in one locality

### Phase 2 — Make it comfortable
- Coordinator notifications (WhatsApp/SMS/email)
- Vendor quick-link/QR, vendor login
- Map view, CSV exports, search/filter
- Multi-language

### Phase 3 — Grow
- Multiple localities/cities, admin hierarchy
- Reports for donors ("you fed X people")
- Optional mobile app / PWA

---

## 12. Success measures (first 3 months)

- Number of vendors onboarded
- Number of active coordinators
- Offers received → % successfully delivered
- Average time from offer to delivery
- Total people served / meals delivered

---

## 13. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Food spoils / safety issue | Time limits, checklist, coordinator verification, disclaimer |
| Fake hunger reports / spam | CAPTCHA, rate limiting, coordinator verification before action |
| Vendors don't inform us | Personal relationships, QR/WhatsApp shortcut, thank-you recognition |
| Coordinators drop off | Small localities, backup coordinator per locality, Super Admin visibility |
| Misuse of personal data | Role-based access, minimal data, log all access |
| Hosting can't run Java | Decide early (see §9) |

---

## 14. Open questions for you

1. **Name/domain** — `nosleepshungry.org`? Already have a domain?
2. **First locality** — which area/city to start in?
3. **Hosting** — which Hostinger plan do you have (shared / Cloud / VPS)?
4. **Notification channel** — are volunteers mostly on WhatsApp?
5. **Languages** — English only to start, or also a local language?
6. **Legal** — consider registering as a trust/society or partnering with an existing NGO later (also helps with vendor trust and food licensing norms such as FSSAI guidelines).

---

## 15. Next step

Review this plan, answer §14, and mark anything to add/remove. Then we start **Phase 1, step 1**: project skeleton (Spring Boot + React + MySQL schema).
