# Global Setu — pilot foundation

A Java 21 / Spring Boot responsive web prototype based on the supplied BRD. The application stores data in a local H2 file and includes a customer request form, an operations queue, quotation entry, comparison and recorded accept/reject decisions. It is a prototype, not a production marketplace.

## Run

Install JDK 21 and Maven 3.9+, then run `mvn spring-boot:run` from this directory. Open http://localhost:8080. Run `mvn test` for tests. Data is stored under `data/`; removing it deletes all local records. Requires internet access to download Maven dependencies on first run.

## Intended demo flow

1. Submit a service request with contact consent.
2. Open Operations, select the request, move it under review and add one or more quotations.
3. Open My requests and select a request to compare quotes and accept or reject one. Acceptance records a timestamp and changes the request to Approved. It does not create or confirm a booking.

## Critical limitations

No login, authorization or user isolation is implemented: anyone with access can see and modify all requests and quotations. Run only on a trusted local machine with fictitious data. Do not expose to the internet or enter real personal information. Provider verification, consent versioning, immutable quotation versions, booking confirmation and operational checks, concurrency/idempotency guarantees, audit log, uploads, payments, delivery, complaints, notifications, admin configuration, reports, mobile applications and integrations are not implemented. The categories are seed values in code rather than configurable records. Quotation scope is a single text field; line items, tax breakdown and verification must be added. Security, privacy, backups, migration management, testing and legal/commercial policies are required before production.

## Suggested next milestones

Agree pilot categories, language, OTP provider, provider verification criteria and payment/cancellation rules. Then add authenticated role-based access and tenant ownership; configurable catalogue and verified providers; normalized quotation line items and immutable decisions; transactional booking with approval/verification checks; file storage; delivery, payment recording, support, audit and reports. Add mobile clients and integration testing after API contracts stabilize. Integrations and charges need explicit approval.
