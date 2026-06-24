#!/usr/bin/env bash
# Deploy the Android App Links Digital Asset Links file to the host that serves
# sendmoney.remitm.com  (resolves to 77.68.125.96 — NOT the same box as remitm.com
# / 68.169.55.246). Run this ON that server, or adapt the scp line to push to it.
#
# This is purely additive: it adds ONE new file at a URL that does not currently
# exist (/.well-known/assetlinks.json). It does NOT touch nginx config, the app,
# the API, or the database. nginx serves .json as application/json by default, so
# no reload is required.
#
# Acceptance test (must print  content-type: application/json):
#   curl -i https://sendmoney.remitm.com/.well-known/assetlinks.json
set -euo pipefail

# 1) Find the webroot that serves sendmoney.remitm.com on THIS server.
#    (Override by exporting WEB=/path/to/webroot before running.)
WEB="${WEB:-}"
if [ -z "$WEB" ]; then
  CONF=$(grep -rl "sendmoney.remitm.com" /etc/nginx/ 2>/dev/null | head -1 || true)
  if [ -n "$CONF" ]; then
    WEB=$(nginx -T 2>/dev/null | awk '/server_name[^;]*sendmoney\.remitm\.com/{f=1} f&&/root /{print $2; exit}' | tr -d ';')
  fi
fi
if [ -z "$WEB" ] || [ ! -d "$WEB" ]; then
  echo "ERROR: could not auto-detect the sendmoney.remitm.com webroot."
  echo "Find it with:  grep -rl sendmoney.remitm.com /etc/nginx/   then look at its 'root'."
  echo "Then re-run:   WEB=/that/webroot  bash deploy-assetlinks.sh"
  exit 1
fi
echo "Webroot: $WEB"

# 2) Write the file. IMPORTANT: confirm package_name + fingerprint match the
#    snippet Google shows in Play Console -> Deep links (screenshot #10).
mkdir -p "$WEB/.well-known"
cat > "$WEB/.well-known/assetlinks.json" <<'JSON'
[
  {
    "relation": [
      "delegate_permission/common.handle_all_urls"
    ],
    "target": {
      "namespace": "android_app",
      "package_name": "com.remitm.app",
      "sha256_cert_fingerprints": [
        "E2:7C:CF:8C:13:08:A6:0F:A9:1F:D3:90:90:05:16:97:1C:B6:A2:5C:5E:31:E9:CE:1B:AE:7F:37:2D:0F:BA:DC"
      ]
    }
  }
]
JSON

# 3) Verify locally on the box, then over HTTPS.
echo "--- local file ---"; cat "$WEB/.well-known/assetlinks.json"
echo "--- https check (expect: content-type: application/json) ---"
curl -sS -i https://sendmoney.remitm.com/.well-known/assetlinks.json | sed -n '1,8p' || true
echo
echo "If content-type is application/json -> click 'Recheck verification' in Play Console."
echo "If it is still text/html -> the SPA fallback is winning; add this INSIDE the"
echo "sendmoney server{} block ABOVE 'location / { try_files ... }', then 'nginx -t && systemctl reload nginx':"
echo '    location = /.well-known/assetlinks.json { default_type application/json; }'
