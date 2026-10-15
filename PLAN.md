# MealUp — Plan (simplified)

The first plan and `docs/PRODUCT_SPEC.md` described a large system (localities, roles, invitations, food offers with quantities, allocations, delivery tasks, audit history). In Oct 2026 the scope was cut to what the admin actually does. The original spec is kept for history only.

## Scope now
- **Vendors** and **coworkers**: add, view, edit, switch on/off.
- **Requests** (people need food / surplus food offered): view, add, correct, close, reopen.
  - Need food: **Mark completed** (record meals delivered) or **Unable to serve**.
  - Surplus food: **Accept** (a team will be sent) or **Unable to collect**.
- **Dashboard**: total meals delivered, active vendors, active coworkers, pending requests.
- **Public forms**: report people who need food; offer surplus food. Both only create *pending* requests.
- Coordination (calling vendors, messaging coworkers) happens outside the site; the site offers call and WhatsApp buttons.
- One admin login. Everything an admin can do is allowed to every signed-in admin.

## Removed on purpose
Localities and locality-scoped access, roles (Super Admin / Locality Admin / Volunteer), invitations, volunteer task screens, food offers with quantities and deadlines, allocations, delivery tasks, hunger-report photos and urgency, audit history, the Available Food board and the expiry job.

## Architecture
React app → Spring Boot API (`/api/v1`) → MySQL (Flyway). Session cookie login with CSRF protection, rate-limited public forms, security headers.

## Status
Built; waiting for a test run on your machine (see README). Then: choose hosting, run the backup/restore check, and pilot with real vendors (docs/PILOT.md).

## Open decisions
1. **Hosting**: shared hosting cannot run Spring Boot. Needs a VPS or a Java-friendly host (docs/DEPLOYMENT.md).
2. **More than one admin?** Today there is one login, created on first start. Adding a screen to manage admin logins is small if needed.
