# Global Setu

Global Setu is an office-assisted marketplace for connecting customers, service professionals, vendors and delivery partners. The current pilot focuses on construction and interior services in Nashik, with categories designed to expand to other services and locations.

This repository contains an **in-development application**, not a production-ready release. It includes a Java 21 / Spring Boot backend, a responsive web interface and an Expo React Native client for Android and iOS. The BRD's acceptance criteria and production safeguards are still being completed.

## How the marketplace works

1. A customer registers, selects a service and submits a request with location, scope, budget, timing and consent. Attachments can be added to the request.
2. Operations reviews the request, asks for clarification if needed and assigns a verified professional or vendor. Partners submit itemized quotations; operations can also record quotations.
3. The customer reviews the quotes and accepts, rejects or asks for clarification. An accepted, valid quote and a verified provider are required before operations can confirm a booking.
4. Operations and the provider track the schedule and work. A delivery partner can be assigned separately, with proof of delivery recorded when applicable.
5. Operations records verified offline payments and refunds. The customer can raise a complaint, confirm completed work and submit a review for moderation.

The office controls matching and booking confirmation. The current implementation does not automatically dispatch providers or transfer money.

## Roles

| Role | Main responsibilities |
| --- | --- |
| Customer | Manage addresses, requests, decisions, bookings, complaints and reviews. |
| Professional | Submit a profile for verification, respond to assigned enquiries, quote and update work. |
| Vendor | Submit a shop profile, manage product information and respond to enquiries. |
| Delivery partner | Submit a profile and update assigned delivery stages and proof. |
| Operations | Verify partners, manage requests, quotations, bookings, delivery and support, and record offline financial events. |
| Super administrator | Manage account access and system settings, in addition to operations duties. |

## Technology and repository layout

| Area | Technology / location |
| --- | --- |
| API and business rules | Java 21, Spring Boot, Spring Security, Spring Data JPA: `src/main/java/com/globalsetu/platform/` |
| Web client | Responsive HTML, CSS and JavaScript served by Spring Boot: `src/main/resources/static/` |
| Mobile client | Expo and React Native: `mobile/` |
| Data | H2 for local development; PostgreSQL profile and Flyway migrations: `src/main/resources/` |
| Tests and CI | `src/test/` and `.github/workflows/ci.yml` |
| Requirement and owner checklists | `docs/BRD-COVERAGE.md` and `docs/OWNER-TO-DO.md` |

## Run locally

Prerequisites: Java 21 and Maven 3.9 or later. For mobile development, also install Node.js 22 or later and the required Android or iOS tools.

1. Clone the repository and enter its directory.
2. Set the initial administrator's mobile number and a unique password of at least 16 characters. These example values are placeholders:

   ```bash
   export SETU_ADMIN_MOBILE=9999999999
   export SETU_ADMIN_PASSWORD='replace-with-a-unique-long-password'
   ```

3. Start the server:

   ```bash
   mvn spring-boot:run
   ```

4. Open `http://localhost:8080`. Sign in as the administrator or register a customer account.

The administrator is created only when the configured mobile number does not already exist. Local records use an H2 file database under `data/`; uploads use `data/uploads/`. Keep these files private and back them up before changing or removing them. An older prototype H2 database is not automatically migrated to the current schema.

### Mobile client

```bash
cd mobile
npm ci
cp .env.example .env
# Set EXPO_PUBLIC_API_URL in .env to the backend origin reachable by the device.
npm start
```

A physical phone cannot use `localhost` to reach a server running on your computer. Use an HTTPS backend for device and production testing. See [mobile/README.md](mobile/README.md) for platform tooling and current mobile gaps. No signed Android/iOS release binaries are included.

### PostgreSQL with Docker Compose

Copy the root `.env.example` to `.env` and set the database password and administrator credentials. Run:

```bash
docker compose up --build
```

The configuration uses the `prod` Spring profile, PostgreSQL and Flyway migrations, with the application bound to `127.0.0.1:8080`. Database and upload data are stored in Docker volumes. Never commit `.env`, credentials or database files. The PostgreSQL migration and deployment still require validation before production use.

## Authentication and integrations

Password-based sign-in is available. SMS-code sign-in is optional: `SETU_OTP_WEBHOOK_URL` must be an HTTPS endpoint and `SETU_OTP_WEBHOOK_TOKEN` must contain its bearer token. The adapter sends a JSON POST containing `phone`, `message` and `purpose`; a 2xx response indicates that the request was accepted by the integration, not that a message was delivered. No SMS provider is configured by default.

For local testing only, `SETU_OTP_DEV=true` returns a development code in the API response. This is disabled when the `prod` profile is active. Do not enable development-code handling in a public environment.

Online payment gateways, maps, push notifications and third-party messaging are not configured. Payment and refund records represent manually verified offline events; they do not collect, transfer or refund funds. Provider selection, credentials and commercial policies must be agreed before implementing or enabling these integrations.

## API overview

The backend serves public catalogue endpoints under `/api/catalog/`, authentication endpoints under `/api/auth/`, and authenticated marketplace operations under `/api/records/`. Other endpoints provide private uploads, dashboard data, audit history and reports. Authenticated requests use a bearer token. The API implementation and authorization checks are in `Api.java`, `Security.java` and `Platform.java`; treat those files and their tests as the source of truth for supported actions.

## Checks

```bash
mvn verify
node --check src/main/resources/static/app.js
cd mobile && npm ci && npm run check
```

These commands run the Java build and tests, check web JavaScript syntax and type-check the mobile client. See [`.github/workflows/ci.yml`](.github/workflows/ci.yml) for CI. A prior Java 21 run passed seven integration tests; subsequent changes still need a fresh full run, PostgreSQL migration validation and browser/device testing.

## Before launch

Authentication, access control, partner verification, quotations, bookings, delivery, offline financial records, support and reporting have implementation foundations, but the BRD is not fully accepted. Security review, multi-instance rate limiting, secure hosting/TLS, monitored backups, upload scanning and storage, privacy/retention policies, mobile device testing, release signing and production deployment remain necessary. Do not onboard real customers on the local development configuration.

The owner still needs to confirm pilot scope and locations, verification criteria, languages, delivery model, payment and refund rules, tax/invoicing, revenue model, support arrangements and the required external accounts. No commission, escrow, split settlement or automatic payout is assumed.

- [BRD coverage and remaining engineering work](docs/BRD-COVERAGE.md)
- [Owner decisions, accounts and launch checklist](docs/OWNER-TO-DO.md)
- [Development and business workflow for coding agents](AGENTS.md)
