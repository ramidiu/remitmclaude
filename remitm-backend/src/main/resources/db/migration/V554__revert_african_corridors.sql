-- ============================================================================
-- V554: Revert V553 (add_african_corridors).
-- ----------------------------------------------------------------------------
-- V553 created one corridor PER COUNTRY, but this system requires a UNIQUE
-- corridor per (send_currency, receive_currency). Several destinations share a
-- currency (XOF: CI/SN/BF/NE/BJ/TG ; XAF: CM/GA/TD), which produced duplicate
-- GBP->XOF and GBP->XAF corridors and broke every currency-keyed lookup
-- (NonUniqueResultException) — Send Money country list, corridor config save,
-- and payout routing. This migration removes everything V553 added and restores
-- the prior state. Idempotent. Original GBR corridors kept:
-- IND,PAK,NGA,GHA,PHL,AUS,EGY,BGD,DEU.
-- ============================================================================

-- 1) Child rows first (FK: corridor_fees, corridor_delivery_methods).
DELETE cf FROM corridor_fees cf
JOIN corridors c ON c.id = cf.corridor_id
WHERE c.send_country = 'GBR'
  AND c.receive_country NOT IN ('IND','PAK','NGA','GHA','PHL','AUS','EGY','BGD','DEU');

DELETE cdm FROM corridor_delivery_methods cdm
JOIN corridors c ON c.id = cdm.corridor_id
WHERE c.send_country = 'GBR'
  AND c.receive_country NOT IN ('IND','PAK','NGA','GHA','PHL','AUS','EGY','BGD','DEU');

-- 2) The V553 corridors themselves.
DELETE FROM corridors
WHERE send_country = 'GBR'
  AND receive_country NOT IN ('IND','PAK','NGA','GHA','PHL','AUS','EGY','BGD','DEU');

-- 3) CASH_PICKUP slots V553 added to the pre-existing GHA / NGA corridors.
DELETE cdm FROM corridor_delivery_methods cdm
JOIN corridors c ON c.id = cdm.corridor_id
WHERE c.send_country = 'GBR' AND c.receive_country IN ('GHA','NGA')
  AND cdm.delivery_method = 'CASH_PICKUP';

-- 4) payout_types: remove brand-new countries + CASH_COLLECTION added to GH/NG/KE;
--    restore disabled flags for NG/EG/KE/UG (Ghana stays active, as it was before).
DELETE FROM payout_types
WHERE country_code IN ('RW','CI','CM','TZ','SN','MZ','SL','BF','NE','GN','GA','MW','ZW','CD','BJ','BI','TG','TD','ET','ZM');

DELETE FROM payout_types
WHERE country_code IN ('GH','NG','KE') AND payout_type = 'CASH_COLLECTION';

UPDATE payout_types SET is_active = 0
WHERE country_code IN ('NG','EG','KE','UG');

-- 5) country_bank_config + mobile_money_services: remove rows V553 added (new countries).
DELETE FROM country_bank_config
WHERE country_code IN ('RW','CI','CM','TZ','SN','MZ','SL','BF','NE','GN','GA','MW','ZW','CD','BJ','BI','TG','TD','ET','ZM');

DELETE FROM mobile_money_services
WHERE country_code IN ('RW','CI','CM','TZ','SN','MZ','SL','BF','NE','GN','GA','MW','ZW','CD','BJ','BI','TG','TD','ET','ZM');
