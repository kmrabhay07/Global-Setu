# Global Setu owner checklist

The application is in development. This checklist distinguishes decisions and accounts needed from the project owner from engineering work still required before launch. Do not enter real customer information into a public or development instance until the production security and privacy work is complete.

## 1. Agree the pilot scope

- Choose which services to enable in Nashik, coverage boundaries, launch languages and operating/support hours.
- Confirm provider and vendor verification documents, review criteria, and the person who can approve profiles.
- Confirm how many quotations to seek, their validity, whether customers can negotiate, and how scheduling and completion are approved.
- Decide whether delivery is handled by your own partners, manually, or by a third-party service. Agree proof-of-delivery and failed-delivery rules.
- Decide payment collection, advance/balance, permitted offline methods, taxes/invoicing, cancellation, refunds and dispute handling. The current code records offline events; it does not collect or transfer funds.
- Decide the revenue model and amounts, if any. No commission, fees or settlement schedule has been assumed.

## 2. Provide or arrange accounts when ready

- Choose an SMS/OTP provider and supply an owner-controlled account and integration documentation. The optional webhook adapter expects an HTTPS service, not a direct connection to every SMS provider.
- If online payments are desired, select a gateway and complete its business onboarding. Share integration access through a secret manager after agreeing the commercial rules; never commit credentials to Git.
- Select hosting, domain, TLS and a managed database/storage service; decide who administers them and who pays recurring charges. No cloud services have been provisioned.
- For Android/iOS releases, arrange the relevant developer accounts, app identifiers, signing credentials, store descriptions, screenshots, privacy disclosures and app review access. Device builds, signing and submission are still pending.
- Decide whether maps, transactional email, push notifications or WhatsApp are needed. Each requires a selected provider and potentially its own account, quotas and costs.

## 3. Approve policies and content

- Obtain business/legal review for terms, privacy notice, consent wording, retention/deletion process, invoices/tax treatment, vendor agreements, refund and cancellation policies.
- Supply final branding, logo, contact details, support channels, service descriptions and any photos you have rights to use.
- Name operations owners, reviewers and the initial administrator. Create strong unique secrets outside source control and provide them only through an approved secret manager.

## 4. Validate the pilot before launch

- Review the customer, provider, delivery and operations screens and approve the final workflow and UI.
- Participate in user acceptance testing with representative requests and quotations. Verify the real business rules and sign off on the release; the current automated tests do not replace this.
- Approve the hosting, provider fees, testing scope, monitoring, backup/restore, access handover and ongoing support arrangements before incurring charges.

## Engineering work still needed

The owner should not need to implement these: finish mobile feature parity and device testing; validate PostgreSQL migrations; integrate selected providers; implement dependable background delivery and multi-instance rate limits; secure and scan uploads; perform an independent security review, accessibility and privacy checks; test recovery, deploy, and prepare signed builds and store submissions. Many BRD features have only a partial implementation. See BRD-COVERAGE.md and README.md for the current limitations.
