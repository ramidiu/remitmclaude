# Remitm Money Transfer — LIVE Production Deployment

**Domain:** `remitm.com`
**Frontend (static):** `/var/www/remitm/data/www/remitm.com/`  (served by host nginx)
**Backend + MySQL + Redis:** Docker Compose under `/opt/remitm-live/`
**KYC documents:** `/var/www/remitm/data/kyc-uploads-live/legacy/` (bind-mounted into the backend container)
**API routing:** Angular calls `/api` (relative) → host nginx proxies → `127.0.0.1:8086` (docker backend)

> **Isolated from the existing TEST deploy** on this server. Nothing here touches
> `/opt/remitm`, the test systemd backend (port 8085), the host MySQL (3306), or
> `test.remitm.com`. Live uses its own dir, ports (8086 / 3307),
> containers (`remitm-live-*`), and KYC folder.
>
> No `rm -rf` is used anywhere. Old files are moved aside with `mv` to a timestamped backup folder instead.

| Resource | TEST (existing) | LIVE (this deploy) |
|----------|-----------------|--------------------|
| App dir | `/opt/remitm` | `/opt/remitm-live` |
| Backend port | `8085` (systemd) | `8086` (docker) |
| MySQL | host `3306` | docker `127.0.0.1:3307` |
| KYC dir | `…/kyc-uploads` | `…/kyc-uploads-live` |
| Web root | `…/test.remitm.com` | `…/remitm.com` |
| Containers | n/a | `remitm-live-mysql/redis/backend` |

---

## 0. Artifacts (in `deploy-live/`)

| File | Size | Purpose |
|------|------|---------|
| `remitm-frontend.tar.gz` | ~5 MB | Angular prod build (apiUrl = `/api`) |
| `remitm_production.sql.gz` | ~32 MB | Cleaned `remitm` DB (903 users, 2778 txns, USI test data removed) |
| `kyc-uploads-legacy.tar.gz` | ~724 MB | KYC images (`legacy/userIdentity`, `userAddress`, `transactionIdentity`) |
| `docker-compose.production.yml` | — | MySQL + Redis + backend (live, isolated) |
| `.env.production` | — | Secrets/config template (copy to `.env`) |
| `remitm.com.conf` | — | nginx site config (proxies to 8086) |
| `upload.sh` | — | one-shot uploader |
| backend source: `../remitm-backend/` | ~97 MB | built on the server by docker compose |

WSL shell vars:
```bash
DEPLOY=/mnt/c/Users/kreat/claude/remitmproject/deploy-live
SRV=root@68.169.55.246
```

---

## 1. Upload everything (run in WSL)

Easiest — the one-shot script (after `ssh-copy-id $SRV` so it won't ask for a password 6×):
```bash
"$DEPLOY/upload.sh" "$SRV"
```

Or manually:
```bash
ssh "$SRV" "mkdir -p /opt/remitm-live /var/www/remitm/data/www/remitm.com /var/www/remitm/data/kyc-uploads-live"
rsync -avzP --exclude 'target/' --exclude '.git/' /mnt/c/Users/kreat/claude/remitmproject/remitm-backend/ "$SRV:/opt/remitm-live/remitm-backend/"
scp "$DEPLOY/docker-compose.production.yml" "$DEPLOY/.env.production" "$SRV:/opt/remitm-live/"
scp "$DEPLOY/remitm.com.conf" "$DEPLOY/remitm_production.sql.gz" "$DEPLOY/remitm-frontend.tar.gz" "$SRV:/tmp/"
rsync -avzP "$DEPLOY/kyc-uploads-legacy.tar.gz" "$SRV:/tmp/"
```

---

## 2. Configure secrets (on the server)

```bash
ssh "$SRV"
cd /opt/remitm-live
cp .env.production .env
openssl rand -hex 64        # paste into JWT_SECRET
nano .env                   # fill every CHANGE_ME (DB password, JWT, Brevo, USI/RemitOne)
```

---

## 3. Start MySQL + Redis, then import the database

```bash
cd /opt/remitm-live
docker compose -f docker-compose.production.yml up -d mysql redis

# wait for MySQL healthy
until [ "$(docker inspect -f '{{.State.Health.Status}}' remitm-live-mysql)" = healthy ]; do
  echo "waiting for mysql..."; sleep 3;
done

# import the production seed
set -a; . /opt/remitm-live/.env; set +a
zcat /tmp/remitm_production.sql.gz | docker exec -i remitm-live-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" remitm

# sanity check
docker exec remitm-live-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e \
  "SELECT COUNT(*) AS users FROM remitm.users; SELECT COUNT(*) AS txns FROM remitm.transactions;"
# expect users=903, txns=2778
```

---

## 4. Unpack KYC images

```bash
tar -xzf /tmp/kyc-uploads-legacy.tar.gz -C /var/www/remitm/data/kyc-uploads-live/
ls /var/www/remitm/data/kyc-uploads-live/legacy/userIdentity | wc -l   # expect 454
ls /var/www/remitm/data/kyc-uploads-live/legacy/userAddress  | wc -l   # expect 400
```

---

## 5. Build & start the backend

```bash
cd /opt/remitm-live
docker compose -f docker-compose.production.yml up -d --build backend
docker compose -f docker-compose.production.yml logs -f backend    # wait for "Started ... in N seconds"
curl -s http://127.0.0.1:8086/actuator/health                       # {"status":"UP"}
```

---

## 6. Deploy the Angular frontend (static)

```bash
WEB=/var/www/remitm/data/www/remitm.com
tar -xzf /tmp/remitm-frontend.tar.gz -C "$WEB"
chown -R $(stat -c '%U:%G' "$WEB") "$WEB"
ls "$WEB"/index.html
```

---

## 7. nginx site + SSL

```bash
cp /tmp/remitm.com.conf /etc/nginx/conf.d/remitm.com.conf
nginx -t && systemctl reload nginx

# If you use ISPmanager, add ONLY these into the panel vhost (proxy to 8086):
#   location /api/      { proxy_pass http://127.0.0.1:8086/api/; proxy_set_header Host $host;
#                         proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
#                         proxy_set_header X-Forwarded-Proto $scheme; }
#   location /actuator/ { proxy_pass http://127.0.0.1:8086/actuator/; }

dnf install -y certbot python3-certbot-nginx
certbot --nginx -d remitm.com -d www.remitm.com --redirect -m you@remitm.com --agree-tos -n
```

---

## 8. Smoke test

```bash
curl -I http://127.0.0.1:8086/actuator/health           # 200
curl -I https://remitm.com                # 200 (Angular index)
curl -s -o /dev/null -w "%{http_code}\n" -X POST \
     https://remitm.com/api/auth/login \
     -H 'Content-Type: application/json' -d '{}'         # 400/401 = reached backend (good)
```

Then open **https://remitm.com**, log in, and confirm a migrated user's KYC preview loads.

---

## 9. Updates later (NO rm -rf — move-aside)

**Frontend:**
```bash
WEB=/var/www/remitm/data/www/remitm.com
STAMP=$(date +%Y%m%d_%H%M%S)
mkdir -p /var/www/remitm/backups
mv "$WEB" "/var/www/remitm/backups/web_$STAMP"
mkdir -p "$WEB"
tar -xzf /tmp/remitm-frontend.tar.gz -C "$WEB"
chown -R $(stat -c '%U:%G' "/var/www/remitm/backups/web_$STAMP") "$WEB"
```

**Backend:**
```bash
cd /opt/remitm-live
docker compose -f docker-compose.production.yml up -d --build backend
docker compose -f docker-compose.production.yml logs -f backend
```

**Stop / start:**
```bash
cd /opt/remitm-live
docker compose -f docker-compose.production.yml stop
docker compose -f docker-compose.production.yml up -d
```

---

## 10. Daily DB backup (cron)

```bash
( crontab -l 2>/dev/null; echo '0 2 * * * set -a; . /opt/remitm-live/.env; set +a; docker exec remitm-live-mysql sh -c "mysqldump -uroot -p$MYSQL_ROOT_PASSWORD --single-transaction remitm | gzip" > /var/www/remitm/backups/remitm_$(date +\%F).sql.gz' ) | crontab -
```

---

## Notes / gotchas

- This live stack is fully isolated from the test deploy; both can run side by side.
- `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`: schema comes from the imported dump. If first boot complains about a missing table, switch to `update` for one boot, then back to `validate`.
- Backend/MySQL bind to `127.0.0.1` only — never exposed publicly; all traffic via host nginx.
- 24 KYC files (mostly test accounts) weren't in the client export; those previews 404 until supplied.
- USI/RemitOne in `.env` still point at the **test** endpoint (`test4.remit.by`). Switch to live before moving real money.
