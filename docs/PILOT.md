# Pilot plan

## Before you start
- [ ] Hosting chosen and deployed (docs/DEPLOYMENT.md)
- [ ] Backup run and **restore-check passed** on: ____ (date)
- [ ] Admin password changed; `MEALUP_ADMIN_*` removed from the env file
- [ ] Privacy notice (`/privacy`) read and adapted; `VITE_CONTACT_EMAIL` set
- [ ] 3–5 real vendors and your coworkers added

## Dry run (20 minutes, on your phone)
1. Open `/report-hunger` on a phone and send a test report. It appears under **Requests → Pending**, and the dashboard's *Pending requests* goes up by one.
2. Open it, tap **Call** on a vendor, tap **WhatsApp** on a coworker (the message is pre-filled).
3. Enter meals delivered and press **Mark completed**. *Total meals delivered* goes up by that number, *Pending* goes down.
4. Send another report and press **Unable to serve**. It leaves the pending list; the meals total does not change.
5. Send a test from `/surplus-food`. Press **Accept**; send another and press **Unable to collect**.
6. Press **Reopen** on a closed request; the meals it had counted are removed until you complete it again.
7. Switch a vendor and a coworker off and on; the dashboard counts follow.

## Checks that are not automated
- Every screen above is comfortable to use on a phone (AC "works on mobile").
- WhatsApp links open the right chat with 10-digit numbers (set `VITE_DEFAULT_COUNTRY_CODE` if you are not in India).

## Keep a feedback log
Date · what was confusing or broken · fix needed · done?

Pilot ends after two weeks of real use with no open "broken" items.
