# Remitm — Environment Credentials & Integration URLs (INTERNAL / SENSITIVE)

> ⚠️ **CONFIDENTIAL.** Contains live secrets. Do NOT commit to git, paste in chat,
> or share with anyone outside the operations team. This file is gitignored.
> The **client-facing** overview (no secrets) is `ENVIRONMENTS.md`.

Both environments run on VPS `77.68.125.96`. Partner endpoints not listed under an
environment fall back to the application defaults shown in §3.

---

## 1. DEVELOPMENT — remittance.remitm.com  (ALL SANDBOX)

DEV deliberately runs on **sandbox/staging endpoints with NO partner API keys set**,
so no real payouts/charges can occur. Config: `/opt/remitm-dev/.env`.

| Item | Value | Status |
|---|---|---|
| Site URL | https://remittance.remitm.com | live |
| Admin login | `admin@remitm.com` / `Admin@123456` (Super Admin) | active |
| Admin login | `platformadmin@remitm.com` / `Admin@123456` (Admin) | active |
| MySQL | host `127.0.0.1:3308`, db `remitm`, user `root`, pwd `RemitmDevDb2026` (own container/volume, isolated from prod) | active |
| Customer data | **none** — purged 2026-06-25; only schema + reference config + 2 admin accounts remain | clean |
| JWT secret | `hmzXFH4JkgGYz5o97yJgNefAFYAkdG7vzwrVzuhbFJVHWqBa` | dev-only |
| Email (SMTP) | `sandbox.smtp.mailtrap.io` — no username/password set | sandbox, inactive |
| SMS (Twilio) | no credentials set | disabled |
| Exchange rates API | no key set (rates not fetched) | disabled |
| Nsano (payout) | `https://staging.instantcredit.services` — **no API key** | sandbox, inert |
| Zeepay (payout) | `https://shop.digitaltermination.com/` — **no token** | sandbox, inert |
| Fire (pay-in) | `https://api-preprod.fire.com` — no client creds | pre-prod, inert |
| Kuber (pay-in) | disabled (`KUBER_ENABLED=false`) | disabled |
| RemitOne (compliance) | disabled (`REMIT_ONE_ENABLED=false`) | disabled |
| Trust Payments | `https://webservices.securetrading.net/json/`, site ref `test_remitm107329` | test, no password |

---

## 2. PRODUCTION — sendmoney.remitm.com  (LIVE)

Config: `/opt/remitm-live/.env.production`. Per its own header, most integration keys
are still **test/sandbox defaults**; only the items marked *set* below have real
credentials. Confirm/replace before processing real money on any partner.

| Item | Value | Status |
|---|---|---|
| Site URL | https://sendmoney.remitm.com | live |
| Admin login | managed by ops (not stored here) | — |
| MySQL | host `127.0.0.1:3307`, db `remitm`, user `root`, pwd `5f5fc4ad6591d5b65ad558639291de9f` | active |
| JWT secret | `d2a7038d73dd77b2861beaf726ffeafe39c82fcfcaa72398d44d772065d8096815d0a3a86fbcd3efbff1e7609d7cf32b` | live |
| Nsano (payout) | URL `https://staging.instantcredit.services` · API key `o6OJ7n3cdfVV21tyL2Sgw41F2nPdG0k2mw6zZlcpTRKNAdsS4k` | **key set** (URL still staging) |
| Zeepay (payout) | URL `https://shop.digitaltermination.com/` · token `eyJ0eXAiOiJKV1Qi…<long JWT, see .env.production>` | **token set** |
| Trust Payments | site reference `remitm107330` | **set** (live ref) |
| Email / SMS / Fire / Kuber / RemitOne / Exchange | application defaults (test/sandbox) | not overridden |

> Zeepay token is a long JWT — full value lives only in `/opt/remitm-live/.env.production`.
> (Note: the laylaremitm Zeepay JWT historically expired 2026-05-13; verify this one is current.)

---

## 3. Application defaults (used when an env var is unset)

From `remitm-backend/src/main/resources/application.yml`:

| Integration | Default URL / value |
|---|---|
| Email SMTP | `sandbox.smtp.mailtrap.io` (Mailtrap sandbox) |
| ClickSend email | `https://rest.clicksend.com/v3/email/send` · user `ramidiu@kreativwebsolutions.com` · pwd `Ram0001$U` |
| Exchange rates | `https://v6.exchangerate-api.com/v6` (key required) |
| RemitOne | `https://remitby.remitone.com/universalsecurities/ws/compliance/insertTransactionForCompliance` · agent `Universal Securities` · `enabled=false` |
| Kuber | `https://backend.kuberfinancial.com.au/api/payments` · `enabled=true` (needs merchantId/deviceId) |
| Trust Payments | `https://webservices.securetrading.net/json/` · default site ref `test_remitm107329` |
| Fire (pay-in) | api `https://api-preprod.fire.com` · payments `https://payments-preprod.fire.com` · return `https://sendmoney.remitm.com/home/trust-callback` |
| Nsano | `https://staging.instantcredit.services` |
| Zeepay | `https://shop.digitaltermination.com/` |

---

## 4. Summary

- **DEV = fully sandbox.** Safe for client demos/testing; no real money movement possible
  because no partner API keys are configured.
- **PROD** has Nsano, Zeepay, and Trust Payments credentials populated; all other partners
  are still on test/sandbox defaults and must be given real credentials before going fully live.

*Last updated: 2026-06-25*
