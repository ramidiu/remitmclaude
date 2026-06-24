# Layla → RemitM Rebrand — What Was Done

This project is a copy of `layla/laylanewproject`, fully rebranded to **RemitM**. The
original `layla/` project was left untouched.

## 1. Deep rename (layla + remitz → remitm)
- All brand strings, identifiers, folders, files and the **Java package** `com.remitz` → `com.remitm`
  (directories moved, classes `RemitzApplication/RemitzException/RemitzPermissionEvaluator` →
  `Remitm*`).
- **Database schema** `remitz` → `remitm` (JDBC URL + `MYSQL_DATABASE` in all compose files).
- Domains: `laylamoneytransfer.co.uk` / `.com` / `layla.money` / `remitz.co.uk` → **remitm.com**.
- Folders: `layla-backend`→`remitm-backend`, `layla-migration`→`remitm-migration`,
  `laylamoneytransfernew`→`remitmmoneytransfernew`, `docker-compose.layla.yml`→`docker-compose.remitm.yml`.
- **Flyway migrations were NOT edited or renamed** (checksums are immutable). Leftover `remitz`/`layla`
  inside old migration files is historical/internal. A new additive migration
  **`V545__rebrand_remitz_to_remitm.sql`** updates the seeded *data* (email templates, admin emails,
  system_config) to RemitM and deactivates the orphaned "USI Money" payout partner.
  - ⚠️ The seeded admin login email changes from `platformadmin@remitz.co.uk` → `platformadmin@remitm.com`
    (and `admin@…` likewise) on fresh installs.

## 2. Providers removed
| Provider | What it was | How it was removed |
|----------|-------------|--------------------|
| **Volume** | open-banking pay-in | Backend module, frontend pages, send-money Open-Banking option, config & webhook all deleted. |
| **USI Money** | payout rail (UG/TR/EG/QA/SA/AE) | Backend module, superadmin page, service, routes/menu, config deleted; orphan partner row deactivated via V545. |
| **Brevo** | email API (was the ONLY sender) | Replaced with **`SmtpEmailSender`** (Spring `JavaMailSender`). Configure SMTP via env. |
| **Smarty** | address autocomplete | Backend module deleted; frontend `AddressService` stubbed to return empty → **manual address entry**. |

## 3. Theme & assets
- Palette swapped to RemitM **navy `#003377`** (primary) + **green `#5DBB52`** (accent) across all SCSS,
  inline styles, Ionic `variables.scss`, `index.html` theme-color, and email templates.
- Logos replaced with the RemitM badge (`remitm-logo.png/.webp/-white.png`, email logo, deploy logo).
- Studied RemitM site images copied to `remitmmoneytransfernew/src/assets/images/remitm-site/`.

## 4. Landing + legal copy
Rebuilt with the real RemitM wording (captured from sendmoney.remitm.com) and authoritative company
facts: **Remitm Limited · Reg. 07956213 · 20 Kirkdale Road, London E11 1HP · 020 8556 0888 ·
info@remitm.com · www.remitm.com**. Files: landing, about, privacy-policy, mobile-privacy, terms,
mobile-terms, user-agreement, cookie-policy, complaints, contact-us.

## Manual follow-ups (could not be done here)
1. **Build** — no Maven/`node_modules` offline. Run:
   - Backend: `cd remitm-backend && mvn clean package -DskipTests`
   - Frontend: `cd remitmmoneytransfernew && npm install && npm run build`
2. **SMTP** — set `MAIL_HOST / MAIL_PORT / MAIL_USERNAME / MAIL_PASSWORD` and
   `app.notification.email.from` for real email delivery (currently a Mailtrap sandbox default).
3. **Real WebP logo** — `remitm-logo.webp` currently holds PNG bytes (renders via browser sniffing).
   Regenerate a true `.webp` and ideally a horizontal wordmark for headers.
4. **Production infra** (DNS, SSL certs, server paths) was renamed in config text only — actual
   domain/cert cutover for remitm.com is a separate ops task.
5. The repo's `CLAUDE.md` files previously advised keeping `com.remitz`; per your explicit request the
   deep rename was done anyway and those notes were updated.
