# Global Setu agent workflow

## Purpose and source of truth

Global Setu is an office-assisted marketplace with customer, professional, vendor, delivery partner, operations and super-admin roles. The pilot is centred on Nashik construction/interior services, but the data model must allow additional categories and locations. The supplied `Global_Setu_BRD_Developer_V1.docx` is the requirements baseline; when it is not in the checkout, use `docs/BRD-COVERAGE.md` and `docs/OWNER-TO-DO.md` for the documented scope and unresolved business decisions. Inspect the actual code and tests before claiming a requirement is complete. The current application is a partial implementation and has not passed full BRD acceptance.

## Business workflow

1. A customer authenticates, consents and submits a request with service category, location, description, quantity, budget, preferred date and priority; attachments can be added.
2. Operations reviews the request, may ask for more information, and assigns a verified active provider. Provider/vendor verification requires an operations decision and supporting uploaded evidence.
3. A provider or operations records an itemized quote, including scope, line items, taxes/fees, delivery charge, timeline, validity, terms reference and exclusions. Quotes are presented to the customer for acceptance, rejection or clarification.
4. Only after customer acceptance of a valid quote can operations confirm a booking, with operational checks and a currently verified active provider. Prevent duplicate bookings and preserve the agreed scope and decision timestamp.
5. Operations/provider updates booking progress. Delivery is coordinated separately; only a verified delivery partner can be assigned, and proof must belong to that delivery.
6. Operations can record offline payments and refunds. Pending or failed records must not appear successful, and refunds must not exceed the successful payment. These records do not actually move funds.
7. Customers can raise complaints, confirm completed work and submit a review. Operations handles disputes and moderates reviews. Keep internal notes, verification evidence and staff-only attachments private.

Preserve ownership and role checks on every read and mutation. A provider must not access another provider's private records. Customer-uploaded request files may be visible to the assigned provider; staff-only request files must not be exposed to that provider. Retain verified evidence and recorded delivery proof. Audit critical decisions and status changes.

## Unresolved rules and integrations

Do not invent a commission, fee, tax treatment, cancellation charge, refund deadline, settlement schedule or other commercial rule. No wallet, escrow, split settlement, automated payouts, real-time dispatch or live GPS should be assumed. The owner must decide the launch geography/languages, verification policy, payment and delivery model, legal policies and selected providers. See `docs/OWNER-TO-DO.md`.

Do not commit credentials, real personal data, database files or `.env` files. Do not activate paid services or incur third-party charges without the owner's explicit approval. SMS uses an optional HTTPS webhook adapter; a provider acceptance response is not proof of delivery. Payment/refund records are offline records unless a separately approved integration is implemented and verified. Development OTP codes must remain disabled under the `prod` profile.

## Repository map

- `src/main/java/com/globalsetu/platform/`: Spring Boot API (`Api.java`), business transitions and access checks (`Platform.java`), authentication (`Auth.java`, `Otp.java`, `Security.java`), persistence (`Models.java`), uploads and reports.
- `src/main/resources/static/`: responsive web UI served by the backend.
- `src/main/resources/application.properties`: local H2 settings; `application-prod.properties` and `db/migration/` define the PostgreSQL profile and Flyway migrations.
- `mobile/`: Expo/React Native app, mobile form schema, package lock and setup instructions.
- `src/test/`: integration tests. `.github/workflows/ci.yml` runs the Java and client checks.
- `docs/BRD-COVERAGE.md`: requirement status and pending validation; `docs/OWNER-TO-DO.md`: owner decisions, accounts and launch work.

Record payloads currently use a generic persisted record model. Check existing authorization, workflow transitions, audit calls and both clients whenever modifying a record kind or its fields. The web form definitions in `src/main/resources/static/app.js` and mobile definitions in `mobile/schema.ts` are separate; keep their shared fields and API contract aligned when changing a workflow.

## Development workflow

1. Read the relevant requirements, existing implementation and tests; inspect repository status and preserve unrelated user changes.
2. Make a focused change in the backend and affected clients. Validate input and enforce permissions server-side rather than relying on hidden buttons. Use transactions, existing locking/uniqueness mechanisms and idempotency where business events can be retried.
3. For schema changes, add a new Flyway migration; do not rewrite migrations already applied. Consider H2 development compatibility, existing data and PostgreSQL validation. Do not delete user data to make a migration pass.
4. Add or update meaningful tests for changed business rules and authorization, including negative and cross-user cases. Test payment/booking concurrency or retries when relevant. Avoid tests that merely repeat the implementation.
5. Run checks appropriate to the change, investigate failures, and report any unavailable checks. Run browser/device and PostgreSQL checks when the change requires them and the environment supports them. Do not claim they passed without evidence.
6. Update `README.md`, `docs/BRD-COVERAGE.md` and/or `docs/OWNER-TO-DO.md` when capabilities, verification, setup or owner decisions change. Clearly label partial implementations, provider dependencies and outstanding production work.

## Commands

Run from the repository root unless noted:

```bash
mvn verify                              # Java 21 build and integration tests
node --check src/main/resources/static/app.js
cd mobile && npm ci && npm run check    # install locked dependencies and type-check
```

For local use, set `SETU_ADMIN_MOBILE` and a unique `SETU_ADMIN_PASSWORD` of at least 16 characters, then run `mvn spring-boot:run`. For PostgreSQL, use the documented `.env` and `docker compose up --build`. See `README.md` and `mobile/README.md` for setup, OTP configuration and known limitations.

## Release and handover

Do not describe the application as production-ready solely because unit or integration tests pass. Before launch, verify authorization and privacy, upload safety and retention, multi-instance rate limiting, backups/restoration, PostgreSQL migrations, HTTPS deployment, provider integrations, accessibility, mobile device behaviour and owner UAT. Signed mobile builds, store submissions, cloud accounts and legal/commercial approvals require the corresponding owner-provided access and decisions. State what was implemented, what was tested, and what remains incomplete in every handover.
