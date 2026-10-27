# BRD implementation coverage

Baseline: Global_Setu_BRD_Developer_V1.docx, version 1.0. This is an implementation audit, not a completion claim. No requirement is fully accepted yet.

| ID | Module | Status | Required behavior |
|---|---|---|---|
| FR-01 | Account and access | Not implemented | Support mobile-number-based registration/login and secure session handling. OTP/SMS provider and any alternate login method to be confirmed. |
| FR-02 | Customer profile | Not implemented | Store name, mobile number, service address/location, saved addresses and consent preferences. |
| FR-03 | Service catalogue | Partial prototype | Display category, subcategory, service description, service area, photos, indicative pricing if available and enquiry/booking action. |
| FR-04 | Product/material catalogue | Not implemented | Support item name, brand, type, dimensions/unit, photos, quality details, price per unit, last-updated timestamp, stock availability, transport/delivery window, warranty, applicable taxes and billing details. |
| FR-05 | Search and filters | Not implemented | Search by keyword/category and filter by location/service area, price range, availability and other configured attributes. |
| FR-06 | Request form | Partial prototype | Capture work type, location, approximate area/quantity, expected date, budget, priorities (price/quality/time/proximity), photos/measurements and privacy/contact consent. |
| FR-07 | Request management | Partial prototype | Generate unique ID; save draft if feasible; show request history and status; allow admin clarification and cancellation according to policy. |
| FR-08 | Provider onboarding | Not implemented | Capture identity/contact, skills/categories, experience, service area, availability, rate card, work samples/references and verification state. |
| FR-09 | Vendor onboarding | Not implemented | Capture shop name/address, contact/hours, brands/material types, legal/business information where required, stock/prices, transport and billing information. |
| FR-10 | Verification controls | Not implemented | Allow admin to review submitted documents/details, approve/reject/request changes, add internal notes and maintain review history. Unverified providers must not be assignable by default. |
| FR-11 | Quotation management | Partial prototype | Allow providers/admin to submit quotations with line items, quantity/unit, labour/material charges, taxes/fees, delivery charges, timeline, validity and exclusions. |
| FR-12 | Quotation comparison | Partial prototype | Show customer quotations in a consistent layout and clearly identify differences in scope, charges, timing and validity. |
| FR-13 | Customer approval | Partial prototype | Record accept/reject/clarification actions and timestamp. A booking is confirmed only after required approval and operational checks. |
| FR-14 | Booking/order management | Not implemented | Maintain unique booking/order ID, selected provider/vendor, approved scope, agreed price, scheduled date, address and status history. |
| FR-15 | Delivery management | Not implemented | Create delivery task linked to a product/order or standalone delivery request; capture pickup/drop-off, package details, charges, assigned delivery partner and proof of delivery. Exact dispatch/tracking method to be confirmed. |
| FR-16 | Payment and receipts | Not implemented | Record payment method, amount, transaction reference, status, receipt/invoice reference, refunds and failed-payment state. Integrate a payment gateway only after business and provider approval. |
| FR-17 | Notifications | Not implemented | Send in-app and, where configured, SMS/WhatsApp/email notifications for request receipt, clarification, quotation, approval, booking, schedule changes, payment, delivery, complaint and closure. |
| FR-18 | Work progress | Not implemented | Support configured status updates such as Confirmed, Scheduled, In Progress, On Hold, Completed, Cancelled and Disputed. |
| FR-19 | Complaints and support | Not implemented | Customer can raise complaint with category, description and attachments; admin assigns owner, records action and resolution; customer can view progress. |
| FR-20 | Feedback and ratings | Not implemented | Collect customer feedback after completion; allow admin moderation of abusive/fraudulent content; show only approved feedback. |
| FR-21 | Admin dashboard | Partial prototype | Show requests by status, pending verification, quotation turnaround, upcoming jobs, overdue items, complaints, payments and key operational counts. |
| FR-22 | Reports and export | Not implemented | Provide date/category/city/provider/status filters and export to CSV/XLSX for requests, bookings, quotations, providers, vendors, payments and complaints. |
| FR-23 | Configuration | Not implemented | Admin can manage service categories, locations, service areas, units, status labels, banners/content, provider visibility and notification templates. |
| FR-24 | Audit trail | Not implemented | Record critical changes to quotations, approvals, bookings, payments, verification and status, including actor and timestamp. |
| FR-25 | Role-based access | Not implemented | Restrict screens and data according to role; provider/vendor must not access another provider’s private records. |
| FR-26 | Data and content management | Not implemented | Support image upload, file size/type validation, secure storage, and admin controls to hide/remove inappropriate or obsolete content. |

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

Java 21 Spring Boot modular monolith; PostgreSQL with versioned migrations; authenticated session cookies for web and securely stored tokens for native clients; private object storage for uploads; outbox-based notifications; explicit request/booking/delivery state machines. Existing H2 is a local prototype store only. Choose React/TypeScript for the eventual web client and React Native for shared Android/iOS screens. These are proposals, not implemented capabilities.

## Owner decisions and external dependencies

The BRD leaves launch languages, geographic boundaries, OTP provider, verification documents, collection/settlement rules, revenue model, taxes, cancellation/refund policy, quote validity policy, delivery model and support escalation undecided. Keep commercial settings unset until agreed. Do not invent commission percentages or mark simulated transactions paid.

Real OTP, gateway webhooks, maps, messaging, cloud deployment and app-store publication require selected providers and owner-controlled accounts. No paid services have been activated. Store credentials in deployment secrets, never in Git. Legal terms, privacy/retention rules, commercial estimates, support commitments and app-store submissions are outstanding deliverables.

## Verification status

Frontend JavaScript syntax, XML/SVG parsing and static asset references were checked. Java 21 backend compilation and execution have not been verified in this workspace (Java 17 and no Maven). Automated tests added to the repository must pass on Java 21 before any acceptance status is upgraded. No native mobile builds or deployment exist.
