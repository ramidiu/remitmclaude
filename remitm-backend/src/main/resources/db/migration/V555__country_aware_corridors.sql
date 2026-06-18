-- ============================================================================
-- V555: Country-aware corridors — schema + re-add the 25 African corridors.
-- ----------------------------------------------------------------------------
-- Companion to the code change that makes corridor lookups / payout routing key
-- on (receive_country + currency) instead of currency alone. This:
--   1. adds receive_country to the two currency-keyed config tables and backfills,
--   2. re-adds the 25 country-distinct corridors (one per country) with BANK_DEPOSIT
--      + MOBILE_WALLET + CASH_PICKUP slots left UNASSIGNED (admin assigns the gateway),
--      plus payout_types / country_bank_config / mobile_money_services,
--   3. adds a unique index enforcing one corridor per (send/receive country + currency).
-- Additive + idempotent. Scoped to the 25 target countries; existing corridors untouched.
-- ============================================================================

-- 1) Country columns on the currency-keyed config tables ----------------------
ALTER TABLE corridor_fee_config       ADD COLUMN receive_country VARCHAR(3) NULL AFTER to_currency;
ALTER TABLE corridor_partner_mappings ADD COLUMN receive_country VARCHAR(3) NULL AFTER to_currency;

-- Backfill existing rows from their currency's corridor (pre-existing rows are all
-- single-country-per-currency, so the deterministic pick is exact).
UPDATE corridor_fee_config cfc
JOIN (SELECT send_currency, receive_currency, MIN(receive_country) rc
        FROM corridors GROUP BY send_currency, receive_currency) c
  ON c.send_currency = cfc.from_currency AND c.receive_currency = cfc.to_currency
SET cfc.receive_country = c.rc
WHERE cfc.receive_country IS NULL;

UPDATE corridor_partner_mappings cpm
JOIN (SELECT send_currency, receive_currency, MIN(receive_country) rc
        FROM corridors GROUP BY send_currency, receive_currency) c
  ON c.send_currency = cpm.from_currency AND c.receive_currency = cpm.to_currency
SET cpm.receive_country = c.rc
WHERE cpm.receive_country IS NULL;

-- 2) Re-add the 25 country-distinct corridors --------------------------------
CREATE TEMPORARY TABLE IF NOT EXISTS _ctry (
    iso2  VARCHAR(3)   NOT NULL,
    iso3  VARCHAR(3)   NOT NULL,
    cname VARCHAR(100) NOT NULL,
    cur   VARCHAR(3)   NOT NULL
);

INSERT INTO _ctry (iso2, iso3, cname, cur) VALUES
    ('GH','GHA','Ghana',          'GHS'),
    ('NG','NGA','Nigeria',        'NGN'),
    ('ZM','ZMB','Zambia',         'ZMW'),
    ('ZW','ZWE','Zimbabwe',       'ZWL'),
    ('RW','RWA','Rwanda',         'RWF'),
    ('CI','CIV','Cote d''Ivoire', 'XOF'),
    ('CM','CMR','Cameroon',       'XAF'),
    ('TZ','TZA','Tanzania',       'TZS'),
    ('UG','UGA','Uganda',         'UGX'),
    ('SN','SEN','Senegal',        'XOF'),
    ('MZ','MOZ','Mozambique',     'MZN'),
    ('SL','SLE','Sierra Leone',   'SLL'),
    ('EG','EGY','Egypt',          'EGP'),
    ('BF','BFA','Burkina Faso',   'XOF'),
    ('NE','NER','Niger',          'XOF'),
    ('GN','GIN','Guinea',         'GNF'),
    ('GA','GAB','Gabon',          'XAF'),
    ('MW','MWI','Malawi',         'MWK'),
    ('CD','COD','DR Congo',       'CDF'),
    ('BJ','BEN','Benin',          'XOF'),
    ('BI','BDI','Burundi',        'BIF'),
    ('TG','TGO','Togo',           'XOF'),
    ('TD','TCD','Chad',           'XAF'),
    ('KE','KEN','Kenya',          'KES'),
    ('ET','ETH','Ethiopia',       'ETB');

INSERT INTO corridors
    (send_country, receive_country, send_currency, receive_currency,
     is_active, min_amount, max_amount, daily_limit, monthly_limit,
     required_kyc_tier, risk_level)
SELECT 'GBR', x.iso3, 'GBP', x.cur,
       1, 10.00, 50000.00, 100000.00, 500000.00, 'TIER_0', 'LOW'
FROM _ctry x
WHERE NOT EXISTS (
    SELECT 1 FROM corridors c
    WHERE c.send_country = 'GBR' AND c.receive_country = x.iso3 AND c.receive_currency = x.cur
);

UPDATE corridors c
JOIN _ctry x ON c.send_country = 'GBR' AND c.receive_country = x.iso3 AND c.receive_currency = x.cur
SET c.is_active = 1;

-- Delivery-method slots, UNASSIGNED (payout_partner_id = NULL) — admin assigns the gateway.
INSERT INTO corridor_delivery_methods
    (corridor_id, delivery_method, payout_partner_id, is_active, processing_time_minutes)
SELECT c.id, m.dm, NULL, 1, m.pt
FROM _ctry x
JOIN corridors c
  ON c.send_country = 'GBR' AND c.receive_country = x.iso3
 AND c.send_currency = 'GBP' AND c.receive_currency = x.cur
JOIN (
    SELECT 'BANK_DEPOSIT'  AS dm, 1440 AS pt UNION ALL
    SELECT 'MOBILE_WALLET' AS dm,   60 AS pt UNION ALL
    SELECT 'CASH_PICKUP'   AS dm,   30 AS pt
) m
WHERE NOT EXISTS (
    SELECT 1 FROM corridor_delivery_methods cdm
    WHERE cdm.corridor_id = c.id AND cdm.delivery_method = m.dm
);

-- payout_types: ensure all three exist + active per country.
INSERT INTO payout_types (country_code, country_name, currency, payout_type, is_active)
SELECT x.iso2, x.cname, x.cur, p.pt, 1
FROM _ctry x
JOIN (
    SELECT 'BANK_TRANSFER'   AS pt UNION ALL
    SELECT 'MOBILE_MONEY'    AS pt UNION ALL
    SELECT 'CASH_COLLECTION' AS pt
) p
WHERE NOT EXISTS (
    SELECT 1 FROM payout_types t
    WHERE t.country_code = x.iso2 AND t.payout_type = p.pt
);

UPDATE payout_types t
JOIN _ctry x ON t.country_code = x.iso2
SET t.is_active = 1
WHERE t.payout_type IN ('BANK_TRANSFER', 'MOBILE_MONEY', 'CASH_COLLECTION');

-- country_bank_config: add new countries only (UNIQUE country_code).
INSERT IGNORE INTO country_bank_config
    (country_code, country_name, currency, identifier_name, identifier_label, is_active)
SELECT x.iso2, x.cname, x.cur, 'SWIFT', 'SWIFT / BIC Code', 1
FROM _ctry x;

-- mobile_money_services: common networks (skips any already present).
INSERT INTO mobile_money_services (country_code, country_name, service_name, is_active)
SELECT n.iso2, n.cname, n.svc, 1
FROM (
    SELECT 'GH' iso2,'Ghana' cname,'MTN Mobile Money' svc UNION ALL
    SELECT 'GH','Ghana','Vodafone Cash'              UNION ALL
    SELECT 'GH','Ghana','AirtelTigo Money'           UNION ALL
    SELECT 'NG','Nigeria','MTN Mobile Money'         UNION ALL
    SELECT 'NG','Nigeria','Airtel Money'             UNION ALL
    SELECT 'RW','Rwanda','MTN Mobile Money'          UNION ALL
    SELECT 'RW','Rwanda','Airtel Money'              UNION ALL
    SELECT 'CI','Cote d''Ivoire','Orange Money'      UNION ALL
    SELECT 'CI','Cote d''Ivoire','MTN MoMo'          UNION ALL
    SELECT 'CI','Cote d''Ivoire','Moov Money'        UNION ALL
    SELECT 'CI','Cote d''Ivoire','Wave'              UNION ALL
    SELECT 'CM','Cameroon','MTN MoMo'                UNION ALL
    SELECT 'CM','Cameroon','Orange Money'            UNION ALL
    SELECT 'TZ','Tanzania','M-Pesa'                  UNION ALL
    SELECT 'TZ','Tanzania','Tigo Pesa'               UNION ALL
    SELECT 'TZ','Tanzania','Airtel Money'            UNION ALL
    SELECT 'TZ','Tanzania','HaloPesa'                UNION ALL
    SELECT 'UG','Uganda','MTN Mobile Money'          UNION ALL
    SELECT 'UG','Uganda','Airtel Money'              UNION ALL
    SELECT 'ZM','Zambia','MTN MoMo'                  UNION ALL
    SELECT 'ZM','Zambia','Airtel Money'              UNION ALL
    SELECT 'ZM','Zambia','Zamtel Kwacha'             UNION ALL
    SELECT 'SN','Senegal','Orange Money'             UNION ALL
    SELECT 'SN','Senegal','Wave'                     UNION ALL
    SELECT 'SN','Senegal','Free Money'               UNION ALL
    SELECT 'MZ','Mozambique','M-Pesa'                UNION ALL
    SELECT 'MZ','Mozambique','e-Mola'                UNION ALL
    SELECT 'MZ','Mozambique','mKesh'                 UNION ALL
    SELECT 'SL','Sierra Leone','Orange Money'        UNION ALL
    SELECT 'SL','Sierra Leone','Africell Money'      UNION ALL
    SELECT 'EG','Egypt','Vodafone Cash'              UNION ALL
    SELECT 'EG','Egypt','Orange Money'               UNION ALL
    SELECT 'EG','Egypt','Etisalat Cash'              UNION ALL
    SELECT 'BF','Burkina Faso','Orange Money'        UNION ALL
    SELECT 'BF','Burkina Faso','Moov Money'          UNION ALL
    SELECT 'NE','Niger','Airtel Money'               UNION ALL
    SELECT 'NE','Niger','Orange Money'               UNION ALL
    SELECT 'NE','Niger','Moov Money'                 UNION ALL
    SELECT 'GN','Guinea','Orange Money'              UNION ALL
    SELECT 'GN','Guinea','MTN MoMo'                  UNION ALL
    SELECT 'GA','Gabon','Airtel Money'               UNION ALL
    SELECT 'GA','Gabon','Moov Money'                 UNION ALL
    SELECT 'MW','Malawi','Airtel Money'              UNION ALL
    SELECT 'MW','Malawi','TNM Mpamba'                UNION ALL
    SELECT 'ZW','Zimbabwe','EcoCash'                 UNION ALL
    SELECT 'ZW','Zimbabwe','OneMoney'                UNION ALL
    SELECT 'ZW','Zimbabwe','Telecash'                UNION ALL
    SELECT 'CD','DR Congo','M-Pesa'                  UNION ALL
    SELECT 'CD','DR Congo','Orange Money'            UNION ALL
    SELECT 'CD','DR Congo','Airtel Money'            UNION ALL
    SELECT 'BJ','Benin','MTN MoMo'                   UNION ALL
    SELECT 'BJ','Benin','Moov Money'                 UNION ALL
    SELECT 'BI','Burundi','Lumicash'                 UNION ALL
    SELECT 'BI','Burundi','EcoCash'                  UNION ALL
    SELECT 'TG','Togo','T-Money'                     UNION ALL
    SELECT 'TG','Togo','Flooz'                       UNION ALL
    SELECT 'TD','Chad','Airtel Money'                UNION ALL
    SELECT 'TD','Chad','Moov Money'                  UNION ALL
    SELECT 'KE','Kenya','M-Pesa'                     UNION ALL
    SELECT 'KE','Kenya','Airtel Money'               UNION ALL
    SELECT 'ET','Ethiopia','Telebirr'               UNION ALL
    SELECT 'ET','Ethiopia','M-Pesa'
) n
WHERE NOT EXISTS (
    SELECT 1 FROM mobile_money_services m
    WHERE m.country_code = n.iso2 AND m.service_name = n.svc
);

DROP TEMPORARY TABLE IF EXISTS _ctry;

-- 3) Enforce one corridor per (send/receive country + currency) ---------------
CREATE UNIQUE INDEX uk_corridor_country_currency
    ON corridors (send_country, receive_country, send_currency, receive_currency);
