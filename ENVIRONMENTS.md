# Remitm Money Transfer — Environments (DEV & PROD)

This document describes the two independent Remitm Money Transfer environments.
They run on the **same server** but are **fully isolated** — separate application
instances, separate databases, separate data, and separate web roots. Work on the
DEV environment never affects PROD.

---

## 1. Quick reference (client-facing)

| | **PRODUCTION (LIVE)** | **DEVELOPMENT (TEST)** |
|---|---|---|
| **Website** | https://sendmoney.remitm.com | https://remittance.remitm.com |
| **Purpose** | Real customers, real money | Testing & demos only |
| **Status** | Live & operational | Live & operational |
| **Data** | Real production data | Sample/test data |
| **Search engines** | Indexed | `noindex` (hidden) |
| **TLS certificate** | `*.remitm.com` wildcard (valid to 29 Nov 2026) | Same wildcard |

### Admin / staff login (DEV)

For testing the DEV admin dashboard at **https://remittance.remitm.com**:

| Account | Role | Password |
|---|---|---|
| `admin@remitm.com` | Super Admin | `Admin@123456` |
| `platformadmin@remitm.com` | Admin | `Admin@123456` |

> PROD admin credentials are managed separately by the operations team and are not
> listed in this document.

---

## 2. Technology stack (identical in both environments)

| Layer | Technology |
|---|---|
| Frontend | Angular 14 + Ionic 6 (single-page app, served as static files) |
| Backend | Java 17, Spring Boot 3.2.3 (REST API) |
| Database | MySQL 8.0.32 (Docker) |
| Cache / sessions | Redis 7 |
| Web server / TLS | nginx (reverse proxy + static hosting) |
| Containerisation | Docker / Docker Compose |

Both environments run the **same codebase**; they differ only in configuration,
data, and which ports/paths they occupy.

---

## 3. Infrastructure detail (operations / technical)

**Server:** VPS `77.68.125.96` (hostname `remitz-server`, Ubuntu). Host **nginx**
serves the Angular apps as static files and reverse-proxies `/api` and `/actuator`
to each environment's dockerised backend. Each environment is a separate Docker
Compose project, so containers, volumes, and networks never clash.

| Item | PRODUCTION | DEVELOPMENT |
|---|---|---|
| Domain | sendmoney.remitm.com | remittance.remitm.com |
| Deploy directory | `/opt/remitm-live` | `/opt/remitm-dev` |
| Compose file | `docker-compose.production.yml` | `docker-compose.dev.yml` |
| Backend container | `remitm-live-backend` | `remitm-dev-backend` |
| Backend port (localhost) | `127.0.0.1:8086` | `127.0.0.1:8087` |
| MySQL container | `remitm-live-mysql` | `remitm-dev-mysql` |
| MySQL port (localhost) | `127.0.0.1:3307` | `127.0.0.1:3308` |
| MySQL database | `remitm` | `remitm` |
| Redis container | `remitm-live-redis` | `remitm-dev-redis` |
| Web root (Angular) | `/var/www/remitm/data/www/sendmoney.remitm.com` | `/var/www/remitm/data/www/remittance.remitm.com` |
| KYC document store | `/var/www/remitm/data/kyc-uploads-live` | `/var/www/remitm/data/kyc-uploads-dev` |
| nginx vhost | `/etc/nginx/sites-available/sendmoney.remitm.com` | `/etc/nginx/sites-available/remittance.remitm.com` |
| Env file | `/opt/remitm-live/.env.production` | `/opt/remitm-dev/.env` |
| DB schema mode | `validate` (locked) | `update` |
| MFA enforced for staff | Yes | No (eased for testing) |
| Payment partners | Live/configured by ops | Sandbox/disabled |
| TLS cert path | `/etc/ssl/remitm.com/sendmoney.remitm.com.fullchain.pem` (+ `.key`) | Shares the same wildcard cert |

> The MySQL ports are bound to `127.0.0.1` only (not exposed to the internet).
> The backends are likewise localhost-only; public traffic reaches them only through
> nginx over HTTPS.

---

## 4. Deployment workflow

Both environments deploy the same way (only the directory/compose file differs):

**Backend (Java):** source is uploaded to the server and built inside Docker
(multi-stage Maven build) — no pre-built JAR is shipped.
```bash
cd /opt/remitm-dev      # or /opt/remitm-live
docker compose -f docker-compose.dev.yml up -d --build backend
```

**Frontend (Angular):** built into static files and placed in the web root; nginx
serves them. The app uses relative `/api` paths, so the same build works on any
domain.
```bash
# extract the built frontend into the web root, then:
nginx -t && systemctl reload nginx
```

**Important — CORS:** the backend keeps an explicit allow-list of browser origins in
`remitm-backend/src/main/java/com/remitm/config/SecurityConfig.java`. Any new public
domain must be added there and the backend rebuilt, or the browser API calls will be
rejected (login fails / rates show 0.00).

---

## 5. Golden rules

1. **DEV is for testing only.** No real customer data or real payment credentials.
2. **Never point DEV at the PROD database**, and never run PROD against test data.
3. The two stacks are independent — restarting/rebuilding DEV cannot affect PROD.
4. Production changes should be tested on DEV (`remittance.remitm.com`) first.

---

*Last updated: 2026-06-24*
