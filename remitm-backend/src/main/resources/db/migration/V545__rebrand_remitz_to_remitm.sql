-- ============================================================================
-- V545: Rebrand DB-seeded data from "Remitz" / "Layla" to "Remitm".
-- ----------------------------------------------------------------------------
-- The earlier migrations (V501..V519) seed notification templates, system_config
-- brand settings and the admin user with "Remitz" branding and the
-- remitz.co.uk / laylamoneytransfer.* / layla.money domains. Those migration
-- files are immutable (Flyway checksums), so this additive migration updates the
-- resulting DATA to the new "Remitm" brand and the remitm.com domain.
-- Safe to run on a fresh schema or on a restored production dump.
-- ============================================================================

-- 1) Notification template bodies + subjects: domains first, then brand words.
UPDATE notification_templates
SET body_template =
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(body_template,
            'remitz.co.uk', 'remitm.com'),
            'laylamoneytransfer.co.uk', 'remitm.com'),
            'laylamoneytransfer.com', 'remitm.com'),
            'layla.money', 'remitm.com'),
            'Remitz', 'Remitm'),
            'remitz', 'remitm'),
            'Layla', 'Remitm'),
            'layla', 'remitm'),
    subject =
        REPLACE(REPLACE(REPLACE(subject,
            'Remitz', 'Remitm'),
            'remitz', 'remitm'),
            'Layla', 'Remitm')
WHERE body_template LIKE '%remitz%' OR body_template LIKE '%Remitz%'
   OR body_template LIKE '%layla%'  OR body_template LIKE '%Layla%'
   OR subject       LIKE '%remitz%' OR subject       LIKE '%Remitz%'
   OR subject       LIKE '%Layla%'  OR subject       LIKE '%layla%';

-- 2) system_config brand settings.
UPDATE system_config
SET config_value =
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(config_value,
            'remitz.co.uk', 'remitm.com'),
            'laylamoneytransfer.co.uk', 'remitm.com'),
            'laylamoneytransfer.com', 'remitm.com'),
            'layla.money', 'remitm.com'),
            'Remitz', 'Remitm'),
            'remitz', 'remitm'),
            'Layla', 'Remitm'),
            'layla', 'remitm')
WHERE config_value LIKE '%remitz%' OR config_value LIKE '%Remitz%'
   OR config_value LIKE '%layla%'  OR config_value LIKE '%Layla%';

-- 3) Seeded user / admin email domains.
UPDATE users
SET email =
        REPLACE(REPLACE(REPLACE(email,
            '@remitz.co.uk', '@remitm.com'),
            '@laylamoneytransfer.co.uk', '@remitm.com'),
            '@laylamoneytransfer.com', '@remitm.com')
WHERE email LIKE '%@remitz.co.uk'
   OR email LIKE '%@laylamoneytransfer.co.uk'
   OR email LIKE '%@laylamoneytransfer.com';

-- 4) Deactivate the orphaned "USI Money" payout integration data. The USI Money
--    application code has been removed, but earlier migrations (V532) seeded a
--    "USI Money" payout_partner + corridor mappings. Deactivate them so the dead
--    provider no longer surfaces in admin dropdowns / corridor routing.
UPDATE corridor_partner_mappings
SET is_active = 0
WHERE partner_id IN (SELECT id FROM payout_partners WHERE partner_name = 'USI Money');

UPDATE payout_partners
SET is_active = 0
WHERE partner_name = 'USI Money';
