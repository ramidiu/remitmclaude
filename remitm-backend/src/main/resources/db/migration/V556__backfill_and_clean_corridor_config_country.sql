-- ============================================================================
-- V556: Finish country-scoping of corridor_fee_config / corridor_partner_mappings.
-- ----------------------------------------------------------------------------
-- V555 added receive_country and backfilled, but its backfill ran BEFORE the new
-- corridors were inserted, so rows whose only corridor is a V555 one stayed NULL.
-- It also can't disambiguate a country-less config saved for a SHARED currency
-- (XOF/XAF) — that produces a phantom "no country" corridor row in the admin UI.
--
-- This migration (idempotent):
--   1. Backfills receive_country where the currency pair maps to exactly ONE corridor.
--   2. Deletes country-less rows where the currency pair maps to MULTIPLE corridors
--      (ambiguous → invalid; the admin must configure each country explicitly).
-- ============================================================================

-- 1) Backfill where unambiguous (exactly one corridor for the pair).
UPDATE corridor_fee_config cfc
JOIN (SELECT send_currency sc, receive_currency rc, MIN(receive_country) cc, COUNT(*) n
        FROM corridors GROUP BY send_currency, receive_currency HAVING n = 1) c
  ON c.sc = cfc.from_currency AND c.rc = cfc.to_currency
SET cfc.receive_country = c.cc
WHERE cfc.receive_country IS NULL;

UPDATE corridor_partner_mappings cpm
JOIN (SELECT send_currency sc, receive_currency rc, MIN(receive_country) cc, COUNT(*) n
        FROM corridors GROUP BY send_currency, receive_currency HAVING n = 1) c
  ON c.sc = cpm.from_currency AND c.rc = cpm.to_currency
SET cpm.receive_country = c.cc
WHERE cpm.receive_country IS NULL;

-- 2) Delete ambiguous country-less rows (currency shared by multiple countries).
DELETE cfc FROM corridor_fee_config cfc
JOIN (SELECT send_currency sc, receive_currency rc, COUNT(*) n
        FROM corridors GROUP BY send_currency, receive_currency HAVING n > 1) c
  ON c.sc = cfc.from_currency AND c.rc = cfc.to_currency
WHERE cfc.receive_country IS NULL;

DELETE cpm FROM corridor_partner_mappings cpm
JOIN (SELECT send_currency sc, receive_currency rc, COUNT(*) n
        FROM corridors GROUP BY send_currency, receive_currency HAVING n > 1) c
  ON c.sc = cpm.from_currency AND c.rc = cpm.to_currency
WHERE cpm.receive_country IS NULL;
