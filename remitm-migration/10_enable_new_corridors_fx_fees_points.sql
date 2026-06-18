-- =============================================================================
-- STEP 10 — Enable the 25 newly-added African corridors end-to-end:
--   A) payout_types  — activate bank / mobile / cash for each country
--   B) settlement_rates — add real FX rates (rate_to_usd) for new currencies
--   C) corridor_fees — FLAT GBP fees per delivery method (idempotent)
--   D) cash_collection_points — REAL bank/branch names per country (idempotent)
-- Countries: Ghana, Nigeria, Egypt, Zambia, Zimbabwe, Rwanda, Cote d'Ivoire,
--   Cameroon, Tanzania, Uganda, Senegal, Mozambique, Sierra Leone, Burkina Faso,
--   Niger, Guinea, Gabon, Malawi, DR Congo, Benin, Burundi, Togo, Chad, Kenya, Ethiopia
-- =============================================================================
USE remitm;
SET SESSION sql_mode = '';

-- ---- A) enable payout_types for all 3 delivery methods ----
UPDATE payout_types SET is_active = 1
WHERE country_code IN ('GH','NG','EG','ZM','ZW','RW','CI','CM','TZ','UG','SN','MZ','SL',
                       'BF','NE','GN','GA','MW','CD','BJ','BI','TG','TD','KE','ET');

-- ---- B) FX rates: rate_to_usd (1 unit of currency = N USD), real approx values ----
INSERT INTO settlement_rates (currency, rate_to_usd) VALUES
    ('RWF', 0.00072),   -- Rwandan Franc      (~1390/USD)
    ('XOF', 0.00167),   -- West African CFA   (~600/USD, EUR-pegged)
    ('XAF', 0.00167),   -- Central African CFA(~600/USD, EUR-pegged)
    ('TZS', 0.00038),   -- Tanzanian Shilling (~2600/USD)
    ('ZMW', 0.03700),   -- Zambian Kwacha     (~27/USD)
    ('MZN', 0.01560),   -- Mozambican Metical (~64/USD)
    ('SLL', 0.0000448), -- Sierra Leonean Leone (old, ~22300/USD)
    ('GNF', 0.000116),  -- Guinean Franc      (~8600/USD)
    ('MWK', 0.000578),  -- Malawian Kwacha    (~1730/USD)
    ('CDF', 0.000357),  -- Congolese Franc    (~2800/USD)
    ('BIF', 0.000339),  -- Burundian Franc    (~2950/USD)
    ('ETB', 0.00800),   -- Ethiopian Birr     (~125/USD)
    ('ZWL', 0.0000311)  -- Zimbabwe (legacy ZWL; nominal — review when settling)
ON DUPLICATE KEY UPDATE rate_to_usd = VALUES(rate_to_usd);

-- ---- B2) FX rates the QUOTE engine actually reads: fx_rate_history (GBP -> X).
--      getRate() falls back to the latest fx_rate_history row; the external API
--      free tier doesn't cover these exotic currencies, so seed MANUAL rates.
--      Only inserts a pair that has no row yet (won't overwrite existing rates). ----
INSERT INTO fx_rate_history (base_currency, target_currency, rate, source, fetched_at)
SELECT b, t, r, 'MANUAL', NOW() FROM (
    SELECT 'GBP' b,'RWF' t, 1825.00 r UNION ALL
    SELECT 'GBP','XOF',   765.00 UNION ALL
    SELECT 'GBP','XAF',   765.00 UNION ALL
    SELECT 'GBP','TZS',  3400.00 UNION ALL
    SELECT 'GBP','ZMW',    35.00 UNION ALL
    SELECT 'GBP','MZN',    83.00 UNION ALL
    SELECT 'GBP','SLL', 29000.00 UNION ALL
    SELECT 'GBP','GNF', 11300.00 UNION ALL
    SELECT 'GBP','MWK',  2270.00 UNION ALL
    SELECT 'GBP','CDF',  3670.00 UNION ALL
    SELECT 'GBP','BIF',  3870.00 UNION ALL
    SELECT 'GBP','ETB',   164.00 UNION ALL
    SELECT 'GBP','ZWL', 40000.00 UNION ALL
    SELECT 'GBP','KES',   170.00
) src
WHERE NOT EXISTS (
    SELECT 1 FROM fx_rate_history h
    WHERE h.base_currency = src.b AND h.target_currency = src.t
);

-- ---- C) corridor_fees: FLAT GBP fee per delivery method (uses the methods already
--         defined in corridor_delivery_methods; skips any fee already present) ----
INSERT INTO corridor_fees (corridor_id, delivery_method, fee_type, flat_fee, percentage_fee, currency, is_active)
SELECT cdm.corridor_id, cdm.delivery_method, 'FLAT',
       CASE cdm.delivery_method
            WHEN 'BANK_DEPOSIT'  THEN 1.99
            WHEN 'MOBILE_WALLET' THEN 0.99
            WHEN 'CASH_PICKUP'   THEN 2.99
            ELSE 1.99 END,
       0.00, 'GBP', 1
FROM corridor_delivery_methods cdm
WHERE cdm.corridor_id IN (3,4,7,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62)
  AND NOT EXISTS (SELECT 1 FROM corridor_fees cf
                  WHERE cf.corridor_id = cdm.corridor_id
                    AND cf.delivery_method = cdm.delivery_method);

-- ---- D) cash collection points — REAL banks/branches (2 per country), idempotent ----
INSERT INTO cash_collection_points (country_code,country_name,point_name,address,city,contact_number,is_active)
SELECT * FROM (
  SELECT 'GH' c,'Ghana' cn,'GCB Bank - Accra Main' pn,'Thorpe Road, High Street' a,'Accra' ct,'+233302664910' p,1 ia UNION ALL
  SELECT 'GH','Ghana','Ecobank Ghana - Kumasi','Harper Road, Adum','Kumasi','+233302681146',1 UNION ALL
  SELECT 'NG','Nigeria','First Bank Nigeria - Lagos','35 Marina, Lagos Island','Lagos','+2347007000000',1 UNION ALL
  SELECT 'NG','Nigeria','Guaranty Trust Bank - Abuja','Plot 1063 Adeola Hopewell, Abuja','Abuja','+2342012712005',1 UNION ALL
  SELECT 'EG','Egypt','Banque Misr - Cairo','151 Mohamed Farid St, Cairo','Cairo','+20237490000',1 UNION ALL
  SELECT 'EG','Egypt','National Bank of Egypt - Tahrir','1187 Corniche El Nil, Cairo','Cairo','+20223919000',1 UNION ALL
  SELECT 'ZM','Zambia','Zambia National Commercial Bank - Lusaka','Cairo Road, Lusaka','Lusaka','+260211221404',1 UNION ALL
  SELECT 'ZM','Zambia','Stanbic Bank Zambia - Kitwe','Obote Avenue, Kitwe','Kitwe','+260211370000',1 UNION ALL
  SELECT 'ZW','Zimbabwe','CBZ Bank - Harare','60 Kwame Nkrumah Ave, Harare','Harare','+263242748050',1 UNION ALL
  SELECT 'ZW','Zimbabwe','Steward Bank - Bulawayo','Fife Street, Bulawayo','Bulawayo','+263292886000',1 UNION ALL
  SELECT 'RW','Rwanda','Bank of Kigali - Kigali','KN 4 Ave, Nyarugenge','Kigali','+250788143000',1 UNION ALL
  SELECT 'RW','Rwanda','I&M Bank Rwanda - Kigali','KN 03 Ave, Kigali','Kigali','+250788162000',1 UNION ALL
  SELECT 'CI','Cote d''Ivoire','Societe Generale CI - Abidjan','5-7 Ave Joseph Anoma, Plateau','Abidjan','+22520203000',1 UNION ALL
  SELECT 'CI','Cote d''Ivoire','Ecobank Cote d''Ivoire - Abidjan','Ave Terrasson de Fougeres','Abidjan','+22520311600',1 UNION ALL
  SELECT 'CM','Cameroon','Afriland First Bank - Douala','Place de l''Independance','Douala','+237233423000',1 UNION ALL
  SELECT 'CM','Cameroon','BICEC - Yaounde','Ave du General de Gaulle','Yaounde','+237233503700',1 UNION ALL
  SELECT 'TZ','Tanzania','CRDB Bank - Dar es Salaam','Azikiwe Street','Dar es Salaam','+255222117441',1 UNION ALL
  SELECT 'TZ','Tanzania','NMB Bank - Dodoma','Uhuru Street','Dodoma','+255222161000',1 UNION ALL
  SELECT 'UG','Uganda','Centenary Bank - Kampala','Plot 7 Entebbe Rd','Kampala','+256312212219',1 UNION ALL
  SELECT 'UG','Uganda','Stanbic Bank Uganda - Kampala','Plot 17 Hannington Rd','Kampala','+256312224600',1 UNION ALL
  SELECT 'SN','Senegal','CBAO - Dakar','1 Place de l''Independance','Dakar','+221338399393',1 UNION ALL
  SELECT 'SN','Senegal','Ecobank Senegal - Dakar','8 Ave Leopold Sedar Senghor','Dakar','+221338498787',1 UNION ALL
  SELECT 'MZ','Mozambique','BCI - Maputo','Av. 25 de Setembro','Maputo','+258213100100',1 UNION ALL
  SELECT 'MZ','Mozambique','Millennium BIM - Maputo','Av. 25 de Setembro 1800','Maputo','+258213500500',1 UNION ALL
  SELECT 'SL','Sierra Leone','Sierra Leone Commercial Bank - Freetown','29-31 Siaka Stevens St','Freetown','+23222226501',1 UNION ALL
  SELECT 'SL','Sierra Leone','Rokel Commercial Bank - Freetown','25-27 Siaka Stevens St','Freetown','+23222222221',1 UNION ALL
  SELECT 'BF','Burkina Faso','Coris Bank - Ouagadougou','Ave Kwame N''Krumah','Ouagadougou','+22625492200',1 UNION ALL
  SELECT 'BF','Burkina Faso','Bank of Africa - Ouagadougou','770 Ave President Saye Zerbo','Ouagadougou','+22625304200',1 UNION ALL
  SELECT 'NE','Niger','Sonibank - Niamey','Ave de la Mairie','Niamey','+22720733700',1 UNION ALL
  SELECT 'NE','Niger','Bank of Africa Niger - Niamey','Rue du Gaweye','Niamey','+22720735301',1 UNION ALL
  SELECT 'GN','Guinea','BICIGUI - Conakry','Blvd du Commerce, Kaloum','Conakry','+224623000000',1 UNION ALL
  SELECT 'GN','Guinea','Ecobank Guinea - Conakry','Immeuble Al Iman, Kaloum','Conakry','+224664300000',1 UNION ALL
  SELECT 'GA','Gabon','BGFIBank - Libreville','Blvd Georges Rawiri','Libreville','+24101791111',1 UNION ALL
  SELECT 'GA','Gabon','Union Gabonaise de Banque - Libreville','Ave du Colonel Parant','Libreville','+24101763000',1 UNION ALL
  SELECT 'MW','Malawi','National Bank of Malawi - Lilongwe','Independence Drive','Lilongwe','+265177700',1 UNION ALL
  SELECT 'MW','Malawi','Standard Bank Malawi - Blantyre','Victoria Avenue','Blantyre','+265182000',1 UNION ALL
  SELECT 'CD','DR Congo','Rawbank - Kinshasa','3487 Blvd du 30 Juin, Gombe','Kinshasa','+243999991000',1 UNION ALL
  SELECT 'CD','DR Congo','Equity BCDC - Lubumbashi','Ave Lumumba','Lubumbashi','+243818104444',1 UNION ALL
  SELECT 'BJ','Benin','Bank of Africa Benin - Cotonou','Ave Jean-Paul II','Cotonou','+22921315262',1 UNION ALL
  SELECT 'BJ','Benin','Ecobank Benin - Cotonou','Rue du Gouverneur Bayol','Cotonou','+22921313200',1 UNION ALL
  SELECT 'BI','Burundi','BANCOBU - Bujumbura','Blvd de la Liberte','Bujumbura','+25722225219',1 UNION ALL
  SELECT 'BI','Burundi','Interbank Burundi - Bujumbura','Chaussee Prince Louis Rwagasore','Bujumbura','+25722222480',1 UNION ALL
  SELECT 'TG','Togo','Ecobank Togo - Lome','20 Ave Sylvanus Olympio','Lome','+22822211303',1 UNION ALL
  SELECT 'TG','Togo','Orabank Togo - Lome','392 Rue des Banques','Lome','+22822213045',1 UNION ALL
  SELECT 'TD','Chad','Commercial Bank Tchad - N''Djamena','Ave Charles de Gaulle','N''Djamena','+23522515333',1 UNION ALL
  SELECT 'TD','Chad','Societe Generale Tchad - N''Djamena','Ave Felix Eboue','N''Djamena','+23522524242',1 UNION ALL
  SELECT 'KE','Kenya','Equity Bank - Nairobi','Equity Centre, Upper Hill','Nairobi','+254763026000',1 UNION ALL
  SELECT 'KE','Kenya','KCB Bank - Mombasa','Nkrumah Road','Mombasa','+254412226501',1 UNION ALL
  SELECT 'ET','Ethiopia','Commercial Bank of Ethiopia - Addis Ababa','Ras Desta Damtew Ave','Addis Ababa','+251115511271',1 UNION ALL
  SELECT 'ET','Ethiopia','Dashen Bank - Addis Ababa','Beshir Building, Sudan St','Addis Ababa','+251114654127',1
) src
WHERE NOT EXISTS (
  SELECT 1 FROM cash_collection_points x
  WHERE x.country_code = src.c AND x.point_name = src.pn
);
