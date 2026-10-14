# MealUp — Build Plan

**Source of truth:** [`docs/PRODUCT_SPEC.md`](docs/PRODUCT_SPEC.md). This plan only decides *how and in what order* we build it.
Nothing outside the spec gets built. (The first prototype plan is kept in `docs/OLD_PLAN_v0.md`.)

Rule for every step: build small → test → run it → report *what works, what was tested, what remains, how to run it* → only then start the next step.

---

## 1. Architecture (modular monolith)

One React app (public site + protected admin area) → one Spring Boot API → one MySQL database.

```
frontend/  React + Vite + TypeScript         (public pages + /admin routes)
backend/   Java 21, Spring Boot, Spring Security, JPA/Hibernate, Bean Validation,
           Flyway, Springdoc OpenAPI, JUnit + Mockito
           packages by module:  auth · users · localities · vendors · foodoffers ·
                                hungerreports · allocations · delivery · audit · dashboard · common
MySQL 8    schema owned by Flyway migrations
```

Conventions: API under `/api/v1`; DTOs only (never JPA entities in responses); pagination + filtering on lists;
one consistent error format; permissions and **locality scope enforced in the backend service layer**; status transitions validated in the backend.
No microservices, Kafka, Kubernetes, AI matching, payments or native app.

## 2. Where we are today vs. the spec

The prototype proved the basics (public forms, login, dashboard, localities, deliveries, activity log). It does **not** match the spec in important ways, so Step 1 rebuilds the foundation instead of patching it.

| Area | Prototype | Spec | Decision |
|---|---|---|---|
| Vendors | none (free-text name on an offer) | full vendor directory, separate from offers | **build new** (Step 3) |
| Food offers | form only, no quantity/unit, no vendor link | vendor-linked, units, confirmed quantity, review lifecycle | **rebuild** (Step 4) |
| Hunger reports | basic | urgency, consent, optional image, verification workflow | **rebuild** (Step 5) |
| Allocation | 1 delivery = 1 offer → reports | many-to-many with quantities, no over-allocation | **rebuild** (Step 6) |
| Roles | Super Admin, Coordinator | Super Admin, Locality Admin, Volunteer | **rebuild** (Step 2) |
| Admin onboarding | Super Admin types a password | invitation flow, never public signup | **rebuild** (Step 2) |
| Public volunteer sign-up | exists | **not in the spec** | **remove** (can come back later) |
| Audit | simple log | actor, before/after values, role/locality changes | **rebuild** (Step 8) |
| Java / frontend | Java 17, JavaScript | Java 21, TypeScript | **upgrade** (Step 1) |
| Tests, OpenAPI | none | JUnit/Mockito, Springdoc | **add** (Step 1 onward) |

Reusable from the prototype: project layout, Vite proxy setup, password hashing, locality-scoping idea, expiry job idea, form components.
There is only test data, so the database is **reset** at Step 1 (fresh Flyway V1).

## 3. Roles and permission matrix

| Capability | Super Admin | Locality Admin | Volunteer | Public |
|---|---|---|---|---|
| Submit hunger report / surplus-food report | ✔ | ✔ | ✔ | ✔ (lands in *Pending Review*) |
| Manage localities | ✔ | – | – | – |
| Invite / edit / deactivate admins, assign localities | ✔ | – | – | – |
| View team phone numbers | all | people in their localities | coordinator on their task | – |
| Vendors: view (incl. phone) | all | own localities | only the vendor on an assigned task | – |
| Vendors: add / edit / activate / deactivate | all | own localities | – | – |
| Food offers: record, review, confirm, reject unsafe, cancel | all | own localities | – | – |
| Available Food board | all | own localities | – | – |
| Hunger reports: view (incl. phone/location), verify, notes | all | own localities | only recipient details inside an assigned task | – |
| Allocations: create / cancel | all | own localities (offer **and** report in scope) | – | – |
| Assign coordinator / volunteer | all | own localities | – | – |
| Delivery tasks: update status & quantities | all | own localities | only tasks assigned to them | – |
| Dashboard | all | own localities | – | – |
| Audit history | ✔ | – | – | – |

Rules: disabling a user takes effect on the very next request. No public path ever creates an admin or sets a role. Phone numbers, reporter details, exact locations and images are never on public pages or public APIs.

## 4. Data model

Tables (see spec §6 for fields). Relationships:

```
users ──< admin_localities >── localities
users ──< invitations                          (secure invite token, expiry, used_at)
localities ──< vendors ──< food_offers
localities ──< hunger_reports
food_offers ──< food_allocations >── hunger_reports      (many-to-many, with allocated_quantity)
food_allocations ──< delivery_tasks                     (several trips per allocation possible)
users ──< audit_logs                                    (actor, entity, before/after JSON)
```

Key rules
- Vendor records are separate from offers; a vendor can exist with no offer.
- A public surplus report may come from an unknown vendor, so `food_offers.vendor_id` is nullable **until review**; an offer cannot become *Confirmed Available* without a vendor (the admin can create one from the same screen).
- **No over-allocation:** when allocating, lock the offer row and check `sum(active allocations) + new ≤ confirmed_quantity` inside one DB transaction (backend, not only the UI).
- *Partially / Fully Allocated* is derived from the allocations, never typed by hand.
- Availability = status is *Confirmed/Partially Allocated* **and** pickup deadline not passed. A scheduled job flips late offers to *Expired*; queries also check the deadline themselves.
- Audit rows cannot be edited or deleted through the API.

## 5. Status rules (enforced in the backend; invalid transitions are rejected)

- **Vendor:** UNVERIFIED → ACTIVE ⇄ INACTIVE (history is never deleted)
- **Food offer:** PENDING_REVIEW → CONFIRMED_AVAILABLE → PARTIALLY_ALLOCATED → FULLY_ALLOCATED → PICKUP_ASSIGNED → COLLECTED;
  any open state → EXPIRED (deadline), CANCELLED, or REJECTED_UNSAFE (reason + who required)
- **Hunger report:** PENDING_REVIEW → NEEDS_VERIFICATION → VERIFIED → ALLOCATION_IN_PROGRESS → PARTIALLY_SERVED → COMPLETED;
  or UNABLE_TO_SERVE (reason) / REJECTED / DUPLICATE
- **Allocation / task:** PENDING → ASSIGNED → PICKUP_IN_PROGRESS → COLLECTED → DELIVERY_IN_PROGRESS → COMPLETED / PARTIALLY_COMPLETED;
  or FAILED (reason) / CANCELLED

The exact transition tables are written as code and unit-tested in the relevant step.

## 6. Steps (one at a time)

Each step ends with a short report: what works · what was tested · what remains · how to run. "AC" = acceptance criteria numbers from spec §13.

### Step 1 — Foundation
- Backend on Java 21 + current Spring Boot 3.5.x; add Springdoc and the test setup (JUnit, Mockito, MySQL via Testcontainers).
- Frontend migrated to TypeScript.
- Fresh Flyway **V1**: full schema for every table above (localities, users, admin_localities, invitations, vendors, hunger_reports, food_offers, food_allocations, delivery_tasks, audit_logs).
- `/api/v1` conventions: DTOs, error format, validation, pagination helper.
- First Super Admin created by a secure seed (credentials from environment variables; no default password in production).
- Remove the prototype code that the spec replaces.
- **Done when:** app starts clean on an empty DB, `/api/v1/health` and Swagger UI work, migrations + seed are tested.

### Step 2 — Admin access and locality permissions
- Login / logout / `GET /me` with a server-side session in an HttpOnly cookie (CSRF protected). Same-origin in dev (Vite proxy) and production.
- Roles: Super Admin, Locality Admin, Volunteer. Localities CRUD (Super Admin). `admin_localities` assignment.
- Super Admin: invite admin (name, phone, role, localities), activate/deactivate. Invitation sets the password via a one-time link/token.
- A reusable permission + locality-scope layer used by every later module.
- **Tests:** a locality admin cannot read or change another locality; a disabled user is locked out immediately; public cannot create admins.
- **Done when:** AC 11, 12, 13 (storage side) hold.

### Step 3 — Vendor directory
- Add / edit / search (name, locality, phone, provider type) / activate–deactivate vendors; vendor profile page; phone visible to authorised admins only.
- Vendor profile shows "Current Food Status" and past offers (history fills in once Step 4 lands).
- Quick, phone-friendly add-vendor form.
- **Done when:** AC 1, 4; locality scoping tested for vendors.

### Step 4 — Food offers and the Available Food board
- Record an offer from a vendor's profile (or create the vendor in the same flow); items, quantity + unit, prep time, storage, pickup deadline, pickup address.
- Public "Inform Us About Surplus Food" form → *Pending Review* queue → admin verifies → *Confirmed Available* (with confirmed quantity).
- Available Food board with filters (locality, provider type, status, category, deadline) and search; contact phone for admins only.
- Expiry job; *Rejected / Unsafe* with reason; expired/unsafe never shown as available.
- **Done when:** AC 2, 3, 5, 15.

### Step 5 — Hunger reports
- Public "Report People Who Need Food" form (urgency, best contact time, consent, optional image in protected storage with type/size limits).
- Private admin queue: verify, needs-verification, duplicate/invalid, internal notes, contact reporter, outcome/reason when unable to serve.
- **Done when:** AC 6; images are not reachable at public URLs.

### Step 6 — Allocation and delivery
- Allocate quantity from a confirmed offer to one or more verified reports (many-to-many), with the transactional over-allocation check.
- Assign locality admin / volunteer; pickup and delivery deadlines.
- Delivery tasks: collected / delivered quantities, remaining or wasted + reason, failed/cancelled; multiple trips allowed.
- Volunteer "My assigned tasks" view that shows only what that task needs.
- **Tests:** concurrent allocations cannot exceed the confirmed quantity; invalid status transitions are rejected.
- **Done when:** AC 7, 8, 9, 10.

### Step 7 — Dashboard
- Counts from spec §4.1 (active vendors, vendors with food now, offers awaiting review, confirmed food available, reports awaiting review, verified reports needing food, deliveries in progress, completed today/this week, offers near deadline), each linking to a filtered list, scoped by locality.
- **Done when:** counts match the lists they link to.

### Step 8 — Audit history and team management polish
- Audit entries for every sensitive action listed in spec §4.8, with before/after values (no unnecessary personal data); Super Admin audit screen with filters.
- Team page: phone numbers, last login, role/locality change history.
- **Done when:** AC 14 and AC 12 verified end-to-end.

### Step 9 — Hardening, deployment, pilot
- Rate limiting on public forms, security headers, upload checks, privacy notice text, review of every endpoint's permissions.
- Backups and a tested restore; production configuration and environment variables documented.
- Deployment on a host that can run Java (decision needed — see §8).
- Pilot: one locality, a few real vendors. Fix what the pilot shows.
- **Done when:** all 16 acceptance criteria in spec §13 pass, including AC 16 (mobile).

## 7. Assumptions (change any you disagree with)

1. Sessions in an HttpOnly cookie rather than tokens in browser storage (safer, supports real logout). Frontend and API are served from the same domain.
2. Admin invitations produce a one-time link that the Super Admin shares personally (WhatsApp/phone). No email/SMS integration in v1 — we will not claim one works until it does.
3. The public volunteer sign-up form from the prototype is dropped (not in the spec).
4. Optional hunger-report image upload is built in Step 5, stored outside the web root, served only to authorised users.
5. Units: MEALS, PORTIONS, KG, PACKETS, OTHER. Provider types as in the spec.
6. Locality admins can allocate food only when both the offer and the report are in their localities.
7. Timestamps stored in UTC, shown in the user's local time (India Standard Time for the pilot).
8. English first; other languages later.

## 8. Decisions needed from you

1. **Hosting** — Hostinger shared hosting cannot run Spring Boot. Options: a Hostinger VPS, or React on your current plan plus the API on a Java-friendly host. Needed before Step 9, not before.
2. **First locality and the first few vendors** for the pilot (Step 9).

## 9. How to run (current instructions live in README.md)
