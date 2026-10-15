> **Superseded (Oct 2026).** MealUp was simplified to vendors, coworkers and requests; see [PLAN.md](../PLAN.md) and [README.md](../README.md). This original spec is kept for history and no longer describes the product.

# MealUp --- Community Food Rescue & Redistribution Platform

## Product Concept, Roles, Workflows, and Implementation Brief for an LLM

> **Purpose of this document:** Give an AI coding assistant the complete
> product concept and clear instructions for designing and building the
> first version. Treat this as the source of truth. You may suggest
> improvements, but explain the reason and do not silently remove core
> requirements.

------------------------------------------------------------------------

## 1. The Idea

Build a simple, mobile-friendly website that helps a community collect
safe surplus food from hotels, restaurants, wedding halls,
housewarmings, parties, and other events, then coordinate its
distribution to people who need food.

The website is a coordination tool for a real-world initiative. It is
**not** a food-delivery marketplace, payment platform, or automatic
food-matching system.

The founder will personally visit or call hotels, restaurants, caterers,
wedding/event organisers, and other food providers. The founder or an
authorised admin must be able to add each provider to the database,
including its name, address, contact person, phone number, locality, and
notes. Later, the admin should be able to open the provider record and
quickly see whether that hotel or event currently has surplus food
available.

The system must answer practical questions: - Which hotels/vendors are
registered? - Which vendors currently have extra food? - What food is
available, how much is available, and until what time can it be
collected? - Who reported people needing food, and approximately how
many people need it? - Which admin or volunteer is responsible for a
locality or delivery? - Which food offer was allocated to which
report? - Was the food collected and delivered? How much? When? By
whom? - What is still pending, expired, cancelled, or completed?

**Core operating principle:** Admins review the information and make the
allocation decision. The system records and supports that decision; it
must not automatically promise food or assign food to people without
admin approval.

## 2. Product Goals

1.  Make it easy for the founder to build and maintain a reliable
    database of local food providers.
2.  Let providers notify the team when surplus food is available.
3.  Let the public report people or communities who need food.
4.  Give the founder a complete view of providers, available food,
    hunger reports, assignments, deliveries, and admin activity.
5.  Allow trusted locality admins to help within their assigned areas
    without giving them founder-level control.
6.  Track the full process from report/offer to actual collection and
    distribution.
7.  Keep the first version small enough to launch in one locality and
    expand later.

## 3. User Types and Permissions

### 3.1 Founder / Super Admin

There is one top-level Super Admin: the founder.

The Super Admin must be able to: - View and manage all localities,
users, admins, vendors, food offers, hunger reports, allocations,
delivery records, and audit logs. - Add, edit, deactivate, and view
hotels, restaurants, caterers, wedding halls, event organisers, and
other food providers. - Store and view provider contact phone numbers
and contact-person details. - Record a provider's normal details even
when it has no surplus food today. - Record a surplus-food offer on
behalf of a provider after a phone call or in-person conversation. -
View which providers currently have food available, with quantity, food
details, location, contact phone, and pickup deadline. - Review and
verify public hunger reports and surplus-food submissions. - Decide
which food offer should be used for which hunger report. - Create
allocations, assign collection/delivery tasks, and choose the
responsible admin/volunteer. - View and update the status of every
collection and delivery. - Add admins, approve invitations, deactivate
admins, and assign admins to specific localities. - View admin phone
numbers and contact details so the team can coordinate by phone. - See
an audit history of important actions, including who changed a record
and when. - Correct inaccurate records while preserving an audit
trail. - Export or download a basic report later, if practical.

The Super Admin alone controls admin access and locality assignments.
Locality admins cannot promote themselves, add other admins, or change
the Super Admin.

### 3.2 Locality Admin

A locality admin is a trusted person responsible for one or more
assigned localities.

They can, within their assigned localities: - View registered vendors
and their contact details. - Add or update vendor records, subject to
the system's permissions. - Record food offers reported by vendors or
events. - Review hunger reports and contact the reporter if necessary. -
Coordinate collection and delivery tasks. - Update task statuses and
enter confirmed quantities and outcomes. - View the phone numbers of
team members relevant to their work.

They cannot: - View or change records belonging to unassigned
localities. - Add, approve, promote, or deactivate admins. - change
Super Admin permissions or locality assignments. - Access system-wide
audit logs or sensitive data beyond their permissions. - Allocate food
outside their permitted scope unless the Super Admin explicitly grants
that permission.

A locality admin must have a name, phone number, account status, role,
and assigned locality list.

### 3.3 Volunteer / Coordinator

Volunteers help collect and distribute food. Keep their access limited.

Depending on permissions, they may: - View only tasks assigned to
them. - See the collection location, provider contact phone, pickup
deadline, delivery destination instructions, and coordinator contact
details required to complete the task. - Update task progress, enter
collected/delivered quantities, and add completion notes. - Report a
failed pickup, unsafe food, recipient unavailable, or another issue.

Volunteers must not see the entire database or private recipient details
unrelated to their tasks.

### 3.4 Vendor / Hotel / Event Organiser

Vendors are food sources. Examples include: - Hotels and restaurants -
Caterers and catering companies - Wedding halls and wedding organisers -
Housewarming hosts - Birthday and private-party organisers - Corporate
events, community events, and other gatherings

A vendor may be added by the founder/admin even if the vendor never uses
the website. This is essential because the founder will personally speak
to providers and enter their details into the database.

A vendor may later submit a surplus-food offer through a simple public
form or an authenticated vendor account. Do not require every vendor to
create an account in the first version.

Vendor information should include: - Business/venue name - Provider
type - Contact person's name - Phone number - Alternate phone number
(optional) - Full address and locality - Map coordinates or map link
(optional) - Preferred contact method/time (optional) - Notes, operating
hours, and pickup instructions - Whether the provider is
verified/approved - Active/inactive status - Created by and last updated
timestamps

Vendor phone numbers must be visible to authorised admins. They should
not be displayed publicly by default.

### 3.5 Public User / Reporter

The public website should offer two clear actions: 1. **Report People
Who Need Food** 2. **Inform Us About Surplus Food**

A person reporting hunger should be able to submit a report without
creating an account. Collect only the information needed to verify and
coordinate help.

Suggested hunger-report fields: - Reporter name (optional, if anonymous
reporting is allowed) - Reporter phone number (recommended, with clear
privacy notice) - Location/address or landmark - Locality - Approximate
number of people who need food - Situation/context (optional) - Image
(optional; do not make it mandatory) - Urgency - Best time to contact -
Submission timestamp - Consent/privacy acknowledgement

Do not publicly expose exact hunger-report locations, phone numbers,
names, or uploaded images. These details are visible only to authorised
team members who need them.

A person reporting surplus food should be able to provide: -
Hotel/business/event name - Provider contact person's name - Contact
phone number - Food location - Type of event/provider - Food items or
food category - Estimated quantity and unit - Preparation time, if
known - Storage/handling details, if known - Pickup deadline - Whether
the food is packed and ready - Additional notes

Public submissions must enter a **Pending Review** queue. They must not
automatically become confirmed food offers or confirmed hunger cases.

## 4. Main Admin Experience (Highest Priority)

The admin dashboard is the most important part of this product.

### 4.1 Dashboard Home

Show a useful overview with counts such as: - Active registered
vendors - Vendors currently reporting available surplus food - Food
offers awaiting review - Confirmed food available for pickup - Hunger
reports awaiting review - Verified hunger reports still needing food -
Collections/deliveries in progress - Completed deliveries today/this
week - Offers nearing pickup deadline

These counts should link to filtered lists so the admin can act quickly.

### 4.2 Vendor Directory

Create a searchable vendor directory. Admins should be able to: - Add a
hotel or event provider manually after speaking with them. - Search by
vendor name, locality, phone number, or provider type. - Open a vendor
profile and view all contact details. - Edit details when a phone
number, contact person, or address changes. - Mark a vendor active or
inactive without deleting historical records. - View the provider's past
food offers and delivery history. - See a clear **Current Food Status**
such as: - No current offer - Offer awaiting review - Food available -
Partially allocated - Fully allocated - Collected - Expired - Cancelled

A registered vendor does not necessarily have food available right now.
Keep the vendor record separate from individual food offers.

### 4.3 "Who Has Extra Food Right Now?" View

This is a key requirement.

Provide a dedicated page called **Available Food** or **Surplus Food
Board**. It should show all currently valid, confirmed food offers that
may still be collected.

Each row/card should show: - Vendor/hotel/event name - Provider type -
Contact person and phone number (admins only) - Collection address and
locality - Food items/category - Estimated and confirmed quantity with
unit - Preparation time, if known - Storage/handling notes - Time
reported - Pickup deadline / expiry time - Current status -
Admin/coordinator responsible - Actions: view details, contact vendor,
allocate food, assign pickup, mark collected, cancel/expire

Filters should include locality, provider type, status, food category,
and pickup deadline. Add a search box for vendor name and phone number.

Use clear visual status text and colour sparingly. The list must
distinguish **confirmed available food** from unverified submissions and
expired offers.

### 4.4 Hunger Reports Queue

Admins should see incoming reports with status: - Pending Review -
Contact/Verification Needed - Verified - Allocation in Progress -
Partially Served - Served/Completed - Unable to Serve - Rejected /
Duplicate / Invalid

Admin actions: - Review details and location - Call or contact the
reporter when appropriate - Verify the approximate number of people -
Add internal notes - Mark as verified, duplicate, invalid, or unable to
verify - Link the report to one or more food allocations - Assign a
coordinator - Record the outcome and reason if help could not be
provided

### 4.5 Food Offers Queue

Food offers should have a separate lifecycle: - Pending Review -
Confirmed Available - Partially Allocated - Fully Allocated - Pickup
Assigned - Collected - Expired - Cancelled - Rejected / Unsafe

Admins must be able to record a food offer manually after a phone call.
The offer must be linked to a vendor record, and the admin should be
able to create a new vendor from the same workflow if the provider is
not yet registered.

Do not treat an old offer as available forever. Availability depends on
its pickup deadline and current status.

### 4.6 Allocation and Delivery Tracking

The founder/admin decides how food is allocated. The system should
support: - Selecting a confirmed food offer - Selecting one or more
verified hunger reports - Entering the quantity allocated to each
report - Assigning a locality admin or volunteer - Recording pickup and
delivery deadlines - Confirming quantity actually collected - Confirming
quantity actually delivered - Recording remaining/wasted/unusable
quantity and reason - Marking the allocation/delivery as completed or
failed - Adding proof/notes if appropriate

Relationship rules: - One food offer can help multiple hunger reports. -
One hunger report can receive food from multiple food offers. -
Therefore, do not store a single `hunger_report_id` directly on a food
offer as the only allocation mechanism. Use a separate allocation record
connecting offers and reports. - Keep the offer, allocation, and
delivery/task records separate enough to preserve history.

Quantities must not be over-allocated. For example, if 100 meal portions
are confirmed, the sum of active allocations must not exceed 100.
Recheck this on the backend within a database transaction, not only in
the frontend.

### 4.7 Admin and Team Management

Super Admin-only area: - Invite/create an admin - Record admin name and
phone number - Assign role and one or more localities -
Activate/deactivate account - Reset/revoke access using a secure
process - View last login and relevant activity, if implemented - Review
changes to roles and locality assignments

Never allow public signup to grant an admin role. The first Super Admin
should be created through a secure setup/seed process, not by an open
public form.

### 4.8 Audit History

Record important changes, including: - Who created or edited a vendor -
Who recorded or changed a food offer - Who reviewed a hunger report -
Who allocated food and in what quantity - Who assigned a
volunteer/coordinator - Who marked a collection or delivery complete -
Who changed an admin's role, account status, or locality - Timestamp and
relevant before/after values where practical

Audit records should not be editable by ordinary admins.

## 5. End-to-End Workflows

### Workflow A --- Founder registers a hotel

1.  Founder visits/calls a hotel or restaurant.
2.  Founder logs into the admin dashboard.
3.  Opens **Vendors → Add Vendor**.
4.  Enters hotel name, provider type, contact person's name, phone
    number, address, locality, and notes.
5.  Saves the record.
6.  The hotel appears in the searchable vendor directory even if it has
    no extra food today.
7.  Later, the founder opens the same hotel profile and records a new
    food offer when the hotel has surplus food.

### Workflow B --- Hotel calls to report surplus food

1.  Hotel calls the founder or locality admin.
2.  Admin opens the hotel's vendor profile and selects **Record Food
    Offer**.
3.  Admin enters food items, quantity, preparation time, storage
    details, pickup deadline, and notes.
4.  The offer is saved as **Confirmed Available** only after the admin
    verifies it with the provider; otherwise it remains **Pending
    Review**.
5.  The offer appears on the Available Food board with its locality,
    pickup deadline, and admin-only contact phone.
6.  The admin reviews verified hunger reports and decides whether and
    where to allocate the food.

### Workflow C --- Wedding or party has extra food

1.  A wedding organiser or host contacts the team, or submits the public
    surplus-food form.
2.  If the provider is new, the admin creates a vendor/event-provider
    record.
3.  Admin records the offer with event address, contact phone,
    approximate portions, food type, preparation time, storage details,
    and latest safe pickup time.
4.  Admin verifies that the food is suitable and safe to distribute.
5.  The offer is confirmed and shown on the Available Food board.
6.  Admin chooses the allocation and assigns a collection/delivery
    coordinator.

### Workflow D --- Someone reports people who need food

1.  Public user opens **Report People Who Need Food**.
2.  User submits the location, approximate number of people, optional
    context/image, and contact information if they are willing to
    provide it.
3.  The report enters **Pending Review**.
4.  An authorised admin checks the report, verifies details when
    possible, and records notes.
5.  If valid, the report becomes **Verified** and appears in the admin's
    work queue.
6.  The founder/admin chooses an available food offer and creates one or
    more allocations.
7.  A coordinator is assigned.
8.  The team records collection and delivery results.

### Workflow E --- Collection and delivery

1.  Coordinator receives the assigned task and necessary
    contact/location details.
2.  Coordinator contacts the vendor and confirms the pickup.
3.  Coordinator collects the food and records actual quantity, pickup
    time, and any issue.
4.  Coordinator delivers food and records actual delivered quantity,
    delivery time, and notes.
5.  Admin reviews the result and marks the task/allocation complete,
    partially complete, failed, or cancelled.
6.  The dashboard and vendor/report history update.
7.  The audit log records the status changes.

### Workflow F --- Food expires or is unsafe

1.  If the pickup deadline passes, the offer should be flagged as
    expired and must no longer appear as currently available.
2.  If food is found unsafe or its handling history is uncertain, stop
    allocation/distribution and mark it **Rejected / Unsafe**.
3.  Record the reason and who made the decision.
4.  Notify the relevant admin/coordinator if notifications are
    implemented.
5.  Never encourage distribution of food that may be unsafe.

## 6. Data Model (Suggested Starting Point)

Use relational tables with clear foreign keys. Refine the design during
implementation, but preserve the relationships and permission rules
below.

### `users`

-   `id`
-   `full_name`
-   `phone_number`
-   `email` (optional depending on login approach)
-   `password_hash` or secure identity-provider reference
-   `role` or role mapping
-   `account_status` (INVITED, ACTIVE, DISABLED)
-   `created_at`, `updated_at`, `last_login_at`

Never store plaintext passwords.

### `localities`

-   `id`
-   `name`
-   `city`
-   `state`
-   `postal_code` (optional)
-   `is_active`

### `admin_localities`

-   `user_id`
-   `locality_id`
-   Unique constraint on the pair

Use this mapping to restrict locality-admin access.

### `vendors`

-   `id`
-   `name`
-   `provider_type` (HOTEL, RESTAURANT, CATERER, WEDDING, HOUSEWARMING,
    PARTY, CORPORATE_EVENT, COMMUNITY_EVENT, OTHER)
-   `contact_person`
-   `phone_number`
-   `alternate_phone_number` (optional)
-   `address`
-   `locality_id`
-   `latitude`, `longitude` or `map_link` (optional)
-   `pickup_instructions` (optional)
-   `notes` (internal)
-   `verification_status`
-   `is_active`
-   `created_by_user_id`
-   `created_at`, `updated_at`

### `hunger_reports`

-   `id`
-   `reporter_name` (optional)
-   `reporter_phone` (optional, protect it)
-   `location_description`
-   `locality_id`
-   `latitude`, `longitude` (optional and access-controlled)
-   `estimated_people_count`
-   `urgency`
-   `context_notes`
-   `image_reference` (optional; store file in protected media storage,
    not as a public URL)
-   `status`
-   `reviewed_by_user_id` (optional)
-   `reviewed_at` (optional)
-   `created_at`, `updated_at`

### `food_offers`

-   `id`
-   `vendor_id`
-   `reported_by_user_id` (nullable if submitted publicly)
-   `food_items_or_category`
-   `estimated_quantity`
-   `confirmed_quantity`
-   `unit` (MEALS, PORTIONS, KG, PACKETS, OTHER)
-   `preparation_time` (optional)
-   `storage_details` (optional)
-   `safe_pickup_deadline`
-   `pickup_address` (can differ from vendor's normal address)
-   `locality_id`
-   `status`
-   `internal_notes`
-   `created_at`, `updated_at`

### `food_allocations`

-   `id`
-   `food_offer_id`
-   `hunger_report_id`
-   `allocated_quantity`
-   `status`
-   `allocated_by_user_id`
-   `assigned_coordinator_user_id` (optional)
-   `created_at`, `updated_at`

### `delivery_tasks` (or `delivery_records`)

-   `id`
-   `food_allocation_id`
-   `assigned_to_user_id`
-   `pickup_deadline`
-   `actual_pickup_time` (optional)
-   `actual_collected_quantity` (optional)
-   `actual_delivery_time` (optional)
-   `actual_delivered_quantity` (optional)
-   `remaining_quantity` (optional)
-   `status`
-   `failure_reason` (optional)
-   `completion_notes`
-   `created_at`, `updated_at`

If the implementation needs multiple trips per allocation, use a
separate task/attempt table rather than overwriting earlier history.

### `audit_logs`

-   `id`
-   `actor_user_id`
-   `action_type`
-   `entity_type`
-   `entity_id`
-   `timestamp`
-   `summary`
-   `before_values` / `after_values` (JSON or another appropriate
    representation, avoiding unnecessary personal data)

Additional tables can be introduced when needed, but do not overengineer
the first version.

## 7. Suggested Status Rules

Use explicit status enums and validate transitions in the backend.

**Vendor:** ACTIVE, INACTIVE, UNVERIFIED.

**Hunger report:** PENDING_REVIEW, NEEDS_VERIFICATION, VERIFIED,
ALLOCATION_IN_PROGRESS, PARTIALLY_SERVED, COMPLETED, UNABLE_TO_SERVE,
REJECTED, DUPLICATE.

**Food offer:** PENDING_REVIEW, CONFIRMED_AVAILABLE,
PARTIALLY_ALLOCATED, FULLY_ALLOCATED, PICKUP_ASSIGNED, COLLECTED,
EXPIRED, CANCELLED, REJECTED_UNSAFE.

**Allocation/task:** PENDING, ASSIGNED, PICKUP_IN_PROGRESS, COLLECTED,
DELIVERY_IN_PROGRESS, COMPLETED, PARTIALLY_COMPLETED, FAILED, CANCELLED.

Do not rely on frontend buttons alone to enforce status rules. The
backend must reject invalid transitions and prevent unauthorised
changes.

## 8. Suggested Technology

Use a straightforward modular monolith:

### Frontend

-   React
-   Vite
-   TypeScript
-   Responsive/mobile-friendly layout
-   Public website and admin dashboard in the same frontend application,
    with protected admin routes

### Backend

-   Java 21
-   Spring Boot
-   Spring Security
-   Spring Data JPA / Hibernate
-   Jakarta Bean Validation
-   Flyway database migrations
-   Springdoc OpenAPI
-   JUnit and Mockito tests

### Database and files

-   MySQL 8
-   Protected storage for optional uploaded images
-   Do not store private images at guessable public URLs

### Architecture

-   One backend API and one relational database for the first version.
-   Do not use microservices, Kafka, Kubernetes, AI-based automatic
    matching, payments, or a separate native mobile app in version one.
-   Design the API so a mobile app could be added later without
    replacing the backend.
-   Check the actual hosting plan before deployment: a static React site
    can use static hosting, but Spring Boot requires a host that
    supports Java or a separate Java-compatible backend/VPS.

## 9. Example API Areas

Names can change, but cover these capabilities:

-   `POST /api/v1/auth/login`
-   `POST /api/v1/auth/logout`
-   `GET /api/v1/auth/me`
-   `GET /api/v1/dashboard/summary`
-   `GET /api/v1/vendors`
-   `POST /api/v1/vendors`
-   `GET /api/v1/vendors/{id}`
-   `PATCH /api/v1/vendors/{id}`
-   `POST /api/v1/vendors/{id}/food-offers`
-   `GET /api/v1/food-offers`
-   `POST /api/v1/food-offers`
-   `PATCH /api/v1/food-offers/{id}`
-   `POST /api/v1/food-offers/{id}/allocations`
-   `GET /api/v1/hunger-reports`
-   `POST /api/v1/public/hunger-reports`
-   `POST /api/v1/public/surplus-food-reports`
-   `PATCH /api/v1/hunger-reports/{id}/status`
-   `POST /api/v1/allocations/{id}/assign`
-   `PATCH /api/v1/delivery-tasks/{id}`
-   `GET /api/v1/admin/users`
-   `POST /api/v1/admin/invitations`
-   `PATCH /api/v1/admin/users/{id}`
-   `GET /api/v1/audit-logs`

Use DTOs, pagination, filtering, server-side validation, and consistent
error responses. Do not expose JPA entities directly.

## 10. Security, Privacy, and Food Safety

These are core requirements, not optional polish.

-   Enforce role-based permissions on the backend for every protected
    endpoint.
-   Enforce locality-level data access on the backend; hiding a button
    in React is not sufficient.
-   Revoke access immediately when an admin is disabled.
-   Do not allow public users to create admin accounts or set their own
    role.
-   Protect vendor, admin, reporter, and recipient phone numbers.
-   Do not expose exact recipient locations or images on public pages.
-   Use HTTPS in production and secure password/session/token handling.
-   Validate and limit uploaded image file types and sizes.
-   Add rate limiting or abuse controls to public forms where practical.
-   Keep audit history for sensitive operations.
-   Back up the database and test restoration.
-   Collect only the personal information needed for coordination and
    explain how it is used.
-   Verify food preparation time, storage/handling, and the safe pickup
    deadline before confirming an offer.
-   Never distribute food when its safety is doubtful. Record
    unsafe/expired food as such and stop the process.

## 11. First-Version Pages

### Public pages

1.  Home page explaining the initiative
2.  Report People Who Need Food form
3.  Inform Us About Surplus Food form
4.  Thank-you/submission confirmation page
5.  Basic contact/about page

### Admin pages

1.  Login
2.  Dashboard
3.  Vendor directory
4.  Add/edit vendor form
5.  Vendor profile with food-offer history
6.  Available Food / Surplus Food Board
7.  Food-offer review and detail
8.  Hunger-report queue and detail
9.  Allocation and delivery tracking
10. My assigned tasks (for coordinators)
11. Admin/team management (Super Admin only)
12. Locality management
13. Audit history (permission-controlled)

Keep navigation simple and mobile-friendly. Admins may need to use the
system while speaking on the phone, so vendor creation and food-offer
entry should be quick and work well on a phone.

## 12. Suggested Build Plan

### Phase 1 --- Foundation

-   Create the React/Vite/TypeScript frontend.
-   Create the Java 21/Spring Boot backend.
-   Configure MySQL and Flyway.
-   Implement initial schema, validation, API error handling, and tests.
-   Create the first Super Admin securely.

### Phase 2 --- Admin access and locality permissions

-   Implement login/logout and `/me`.
-   Implement Super Admin and locality-admin permissions.
-   Test that locality admins cannot access another locality's data.

### Phase 3 --- Vendor directory

-   Build add/edit/search vendor functionality.
-   Include phone number, contact person, address, locality, provider
    type, and notes.
-   Build vendor profile and history.

### Phase 4 --- Food offers

-   Allow admins to record an offer directly against a vendor.
-   Build pending-review and confirmed-available lists.
-   Build the Available Food board with pickup deadlines and phone
    details visible only to authorised admins.
-   Prevent expired or unsafe offers from appearing as available.

### Phase 5 --- Hunger reports

-   Build the public report form and private admin review queue.
-   Add verification and duplicate/invalid handling.

### Phase 6 --- Allocation and delivery

-   Build the many-to-many allocation model.
-   Add assignment, collection, delivery, failure reasons, and outcome
    tracking.
-   Prevent over-allocation on the backend.

### Phase 7 --- Team management, audit, and deployment

-   Add admin invitations, phone numbers, locality assignments, account
    deactivation, and audit logs.
-   Add privacy/security checks, backups, and deployment configuration.
-   Test with a small pilot in one locality and a few vendors before
    expanding.

## 13. Acceptance Criteria

The first version is not ready until these are true:

1.  The founder can register a hotel manually with its name, contact
    person's name, phone number, address, and locality.
2.  The founder can record surplus food for an existing hotel in a short
    workflow.
3.  The admin can see a single list of currently available food and
    immediately identify which hotel/event has it, its phone number,
    address, quantity, and pickup deadline.
4.  A vendor record can exist even when it has no current surplus food.
5.  A public surplus-food submission stays pending until reviewed.
6.  A public hunger report stays private and pending until reviewed.
7.  The founder can connect a confirmed food offer to one or more
    verified hunger reports.
8.  The system prevents allocating more food than the confirmed
    available quantity.
9.  A coordinator can record what was collected and delivered.
10. The founder can see pending, in-progress, completed, expired,
    failed, and cancelled work.
11. Locality admins are restricted to their assigned areas.
12. Only the Super Admin can manage admins and their locality access.
13. Admin phone numbers are stored and visible to authorised team
    members.
14. Important changes are recorded in audit history.
15. Expired or unsafe food cannot be treated as currently available.
16. The main flows work on a mobile browser.

## 14. Instructions to the LLM Building This Project

You are acting as a senior product engineer and architect. Use this
document as the product specification.

Before coding: 1. Summarise the architecture and proposed database
relationships. 2. Identify genuine ambiguities or risks. Ask only
essential questions; otherwise state reasonable assumptions. 3. Propose
a small implementation sequence that produces working software at each
stage.

While coding: - Build in small, testable steps. - Keep the app simple
and usable; do not add unrequested complexity. - Preserve the founder's
Super Admin control and locality-based permissions. - Keep vendor
records separate from food offers, hunger reports separate from
allocations, and delivery outcomes separately traceable. - Make phone
numbers available to authorised admins where operationally necessary,
never expose them publicly by default. - Validate permissions and
quantities in the backend. - Add database migrations and automated tests
for important workflows. - Use realistic sample data only in
development, never in production. - Do not invent that an SMS, WhatsApp,
email, map, or hosting integration works unless it has actually been
implemented and configured. - Explain any required environment variables
and deployment steps. - After each phase, list what works, what has been
tested, what remains, and how to run it.

**First task:** design the schema, role/permission matrix, and
first-phase implementation plan. Then implement the foundation and
vendor directory before moving on to the other modules.
