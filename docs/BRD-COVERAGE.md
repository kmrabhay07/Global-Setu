# BRD implementation coverage

Baseline: Global_Setu_BRD_Developer_V1.docx, version 1.0. This is an implementation audit, not a completion claim. The repository now contains working foundations for the listed modules; no requirement is fully accepted until security, UAT and platform checks are completed.

| ID | Module | Status | Required behavior |
|---|---|---|---|
| FR-01 | Account and access | Partial implementation; acceptance pending | Support mobile-number-based registration/login and secure session handling. OTP/SMS provider and any alternate login method to be confirmed. |
| FR-02 | Customer profile | Partial implementation; acceptance pending | Store name, mobile number, service address/location, saved addresses and consent preferences. |
| FR-03 | Service catalogue | Partial implementation; acceptance pending | Display category, subcategory, service description, service area, photos, indicative pricing if available and enquiry/booking action. |
| FR-04 | Product/material catalogue | Partial implementation; acceptance pending | Support item name, brand, type, dimensions/unit, photos, quality details, price per unit, last-updated timestamp, stock availability, transport/delivery window, warranty, applicable taxes and billing details. |
| FR-05 | Search and filters | Partial implementation; acceptance pending | Search by keyword/category and filter by location/service area, price range, availability and other configured attributes. |
| FR-06 | Request form | Partial implementation; acceptance pending | Capture work type, location, approximate area/quantity, expected date, budget, priorities (price/quality/time/proximity), photos/measurements and privacy/contact consent. |
| FR-07 | Request management | Partial implementation; acceptance pending | Generate unique ID; save draft if feasible; show request history and status; allow admin clarification and cancellation according to policy. |
| FR-08 | Provider onboarding | Partial implementation; acceptance pending | Capture identity/contact, skills/categories, experience, service area, availability, rate card, work samples/references and verification state. |
| FR-09 | Vendor onboarding | Partial implementation; acceptance pending | Capture shop name/address, contact/hours, brands/material types, legal/business information where required, stock/prices, transport and billing information. |
| FR-10 | Verification controls | Partial implementation; acceptance pending | Allow admin to review submitted documents/details, approve/reject/request changes, add internal notes and maintain review history. Unverified providers must not be assignable by default. |
| FR-11 | Quotation management | Partial implementation; acceptance pending | Allow providers/admin to submit quotations with line items, quantity/unit, labour/material charges, taxes/fees, delivery charges, timeline, validity and exclusions. |
| FR-12 | Quotation comparison | Partial implementation; acceptance pending | Show customer quotations in a consistent layout and clearly identify differences in scope, charges, timing and validity. |
| FR-13 | Customer approval | Partial implementation; acceptance pending | Record accept/reject/clarification actions and timestamp. A booking is confirmed only after required approval and operational checks. |
| FR-14 | Booking/order management | Partial implementation; acceptance pending | Maintain unique booking/order ID, selected provider/vendor, approved scope, agreed price, scheduled date, address and status history. |
| FR-15 | Delivery management | Partial implementation; acceptance pending | Create delivery task linked to a product/order or standalone delivery request; capture pickup/drop-off, package details, charges, assigned delivery partner and proof of delivery. Exact dispatch/tracking method to be confirmed. |
| FR-16 | Payment and receipts | Partial implementation; acceptance pending | Record payment method, amount, transaction reference, status, receipt/invoice reference, refunds and failed-payment state. Integrate a payment gateway only after business and provider approval. |
| FR-17 | Notifications | Partial implementation; acceptance pending | Send in-app and, where configured, SMS/WhatsApp/email notifications for request receipt, clarification, quotation, approval, booking, schedule changes, payment, delivery, complaint and closure. |
| FR-18 | Work progress | Partial implementation; acceptance pending | Support configured status updates such as Confirmed, Scheduled, In Progress, On Hold, Completed, Cancelled and Disputed. |
| FR-19 | Complaints and support | Partial implementation; acceptance pending | Customer can raise complaint with category, description and attachments; admin assigns owner, records action and resolution; customer can view progress. |
| FR-20 | Feedback and ratings | Partial implementation; acceptance pending | Collect customer feedback after completion; allow admin moderation of abusive/fraudulent content; show only approved feedback. |
| FR-21 | Admin dashboard | Partial implementation; acceptance pending | Show requests by status, pending verification, quotation turnaround, upcoming jobs, overdue items, complaints, payments and key operational counts. |
| FR-22 | Reports and export | Partial implementation; acceptance pending | Provide date/category/city/provider/status filters and export to CSV/XLSX for requests, bookings, quotations, providers, vendors, payments and complaints. |
| FR-23 | Configuration | Partial implementation; acceptance pending | Admin can manage service categories, locations, service areas, units, status labels, banners/content, provider visibility and notification templates. |
| FR-24 | Audit trail | Partial implementation; acceptance pending | Record critical changes to quotations, approvals, bookings, payments, verification and status, including actor and timestamp. |
| FR-25 | Role-based access | Partial implementation; acceptance pending | Restrict screens and data according to role; provider/vendor must not access another provider’s private records. |
| FR-26 | Data and content management | Partial implementation; acceptance pending | Support image upload, file size/type validation, secure storage, and admin controls to hide/remove inappropriate or obsolete content. |

## Implementation sequence

1. Identity, secure sessions, six roles, record ownership, consent versions and audit events. Acceptance: cross-user reads and mutations are denied; elevated roles cannot be self-assigned.
2. Configurable cities/categories, provider/vendor onboarding and document review. Acceptance: unverified or suspended providers cannot receive assignments.
3. Requests with private uploads, internal notes and matching. Quotations with immutable versions, itemized costs and customer decisions. Acceptance: scope changes invalidate earlier approval.
4. Transactional booking with verification and approval guards, scheduling, progress and completion. Acceptance: retries cannot create duplicate bookings; customer closure is distinct from provider completion.
5. Delivery assignment and proof of delivery; payment/receipt/refund records. Acceptance: failed/pending payments never appear successful; refunds cannot exceed captured amounts.
6. Complaints, moderation, notifications, management reports, CSV/XLSX exports and configuration. Acceptance: internal notes remain private and CSV exports neutralize spreadsheet formula injection.
7. Android/iOS role-based clients, responsive customer website and operations panel with accessible forms, loading/error states and localization support.
8. Integration tests, mobile device testing, UAT, backup/restore exercise, deployment, app signing and account handover.

## Architecture proposal

Java 21 Spring Boot modular backend, a responsive HTML/CSS/JavaScript web client, and an Expo React Native client. The API uses bearer tokens, stores local uploads, and includes workflow transitions and a PostgreSQL migration. H2 is the local development store. External object storage, durable notification delivery and production migration validation remain to be completed.

## Owner decisions and external dependencies

The BRD leaves launch languages, geographic boundaries, OTP provider, verification documents, collection/settlement rules, revenue model, taxes, cancellation/refund policy, quote validity policy, delivery model and support escalation undecided. Keep commercial settings unset until agreed. Do not invent commission percentages or mark simulated transactions paid.

Real OTP, gateway webhooks, maps, messaging, cloud deployment and app-store publication require selected providers and owner-controlled accounts. No paid services have been activated. Store credentials in deployment secrets, never in Git. Legal terms, privacy/retention rules, commercial estimates, support commitments and app-store submissions are outstanding deliverables.

## Verification status

A prior Java 21 integration run passed seven tests covering access isolation, approval, booking, offline payment, refund and OTP behavior. The mobile TypeScript check passes. Recent follow-up changes require a fresh Java 21 verification and browser/device QA. PostgreSQL migration, native mobile builds, and production deployment have not been verified. No native release binaries or deployment exist.
