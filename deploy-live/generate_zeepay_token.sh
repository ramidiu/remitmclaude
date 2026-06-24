#!/usr/bin/env bash
# =============================================================================
# Generate a fresh Zeepay OAuth access token and (optionally) write it to .env.
#
# The client_id / client_secret are issued by Zeepay at onboarding.
# username = the email used to create the Zeepay account; password = its password.
#
# Usage:
#   CLIENT_ID=... CLIENT_SECRET=... ZUSER=you@email.com ZPASS=... \
#     bash deploy-live/generate_zeepay_token.sh [--write]
#
#   --write   update ZEEPAY_TOKEN= in ./.env in place (a .env.bak is kept)
#
# Endpoint:
#   sandbox    : https://test.digitaltermination.com/oauth/token   (default)
#   production : set TOKEN_URL=https://shop.digitaltermination.com/oauth/token
# =============================================================================
set -euo pipefail

TOKEN_URL="${TOKEN_URL:-https://test.digitaltermination.com/oauth/token}"
: "${CLIENT_ID:?set CLIENT_ID}"
: "${CLIENT_SECRET:?set CLIENT_SECRET}"
: "${ZUSER:?set ZUSER (account email)}"
: "${ZPASS:?set ZPASS (account password)}"

echo ">> Requesting token from: $TOKEN_URL"
RESP=$(curl -sS --fail-with-body -X POST "$TOKEN_URL" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "client_id=$CLIENT_ID" \
  --data-urlencode "client_secret=$CLIENT_SECRET" \
  --data-urlencode "username=$ZUSER" \
  --data-urlencode "password=$ZPASS" \
  --data-urlencode "grant_type=password") || { echo "Request failed:"; echo "$RESP"; exit 1; }

ACCESS=$(printf '%s' "$RESP" | sed -nE 's/.*"access_token"[[:space:]]*:[[:space:]]*"([^"]+)".*/\1/p')
EXPIRES=$(printf '%s' "$RESP" | sed -nE 's/.*"expires_in"[[:space:]]*:[[:space:]]*([0-9]+).*/\1/p')

if [ -z "$ACCESS" ]; then echo "No access_token in response:"; echo "$RESP"; exit 1; fi
echo ">> access_token obtained (expires_in=${EXPIRES:-?}s)"
echo "$ACCESS"

if [ "${1:-}" = "--write" ]; then
  ENV_FILE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/.env"
  cp "$ENV_FILE" "$ENV_FILE.bak"
  if grep -q '^ZEEPAY_TOKEN=' "$ENV_FILE"; then
    # rewrite the line safely (token may contain / and .)
    awk -v tok="$ACCESS" '/^ZEEPAY_TOKEN=/{print "ZEEPAY_TOKEN=" tok; next} {print}' "$ENV_FILE.bak" > "$ENV_FILE"
  else
    printf '\nZEEPAY_TOKEN=%s\n' "$ACCESS" >> "$ENV_FILE"
  fi
  echo ">> wrote ZEEPAY_TOKEN to $ENV_FILE (backup: $ENV_FILE.bak)"
  echo ">> restart backend to apply:  docker compose -f docker-compose.remitm.yml up -d --force-recreate backend"
fi
