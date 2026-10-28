# Global Setu

Global Setu is an office-assisted marketplace pilot for Nashik. This repository contains a Java 21 / Spring Boot API, a responsive website and operations workspace, and an Expo React Native client for Android and iOS. It is an in-development implementation of the accompanying business requirements, not a production-ready release.

## Included workflows

- Password-based accounts and an optional SMS-code flow, bearer sessions and role-based access for customers, professionals, vendors, delivery partners, operations and super administrators.
- Categories, addresses, partner onboarding and verification, material listings, service requests, itemized quotations and customer decisions.
- Bookings gated by an accepted quotation and a verified provider; work progress, deliveries and proof uploads; offline payment and refund records; complaints, moderated reviews, in-app notifications, audit history and CSV/XLSX exports.
- Responsive web screens, role-based navigation and React Native source. See [mobile/README.md](mobile/README.md) for its current feature gaps.

## Start locally

Install Java 21 and Maven 3.9+. Define bootstrap credentials in your shell and start the API:

```bash
export SETU_ADMIN_MOBILE=9999999999
export SETU_ADMIN_PASSWORD='use-a-long-unique-password'
mvn spring-boot:run
```

Open http://localhost:8080. The administrator is created only if the supplied mobile is not already present; the password must be at least 16 characters. Customers can register from the website. Local data is stored under `data/` in an H2 file database. Treat the example number as a placeholder and avoid using real customer data in development. Delete local data only when it is safe to lose it.

To run tests: `mvn verify`. To check the mobile client: `cd mobile && npm ci && npm run check`. For PostgreSQL, copy `.env.example` to `.env`, set the values, then run `docker compose up --build`; the compose configuration binds the app to localhost. Do not commit `.env`, credentials or database files. The `prod` profile uses Flyway migrations and validates the schema on startup.

## SMS code sign-in

The optional SMS adapter requires `SETU_OTP_WEBHOOK_URL` (HTTPS) and `SETU_OTP_WEBHOOK_TOKEN`. The owner must configure an SMS integration that accepts an authenticated JSON POST with `phone`, `message` and `purpose`; an HTTP 2xx indicates only that the provider accepted the request. No SMS provider is configured by default. For local development only, `SETU_OTP_DEV=true` returns the code in the response. Development codes are forcibly disabled with the `prod` profile. Password sign-in remains available.

## Important limits

- This implementation has not completed all BRD acceptance criteria. Provider-specific SMS, payment gateway/webhooks, maps, push notifications, online payments and actual provider payouts are not configured. Payment/refund workflows record manually verified offline events; they do not move money.
- Mobile app source is included; signed Android/iOS builds, device testing and app-store submission remain outstanding. The mobile client does not yet cover every web administration screen.
- Security needs an independent review, durable multi-instance rate limiting, secure public hosting/TLS, monitored backups and a business-approved privacy/retention policy before real users or personal data are onboarded. Uploads are stored locally; object storage, malware scanning, limits and recovery procedures need production design. Do not expose the local development configuration publicly.
- Provider criteria, pilot locations, language, refund/cancellation rules, taxes, revenue model and final commercial terms require owner decisions. No wallet, escrow, split settlement or automatic payouts are implemented.
- Initial data in a pre-existing prototype H2 database is not automatically migrated to the new schema; retain a backup and plan migration before upgrading any environment with data.

See [docs/BRD-COVERAGE.md](docs/BRD-COVERAGE.md) for the requirement-by-requirement status and remaining work.

For decisions, accounts and release tasks that need the project owner, see [docs/OWNER-TO-DO.md](docs/OWNER-TO-DO.md).
